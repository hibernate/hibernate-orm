/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.testing.junit4;

import org.junit.runners.model.Statement;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestLogger;

/**
* @author Steve Ebersole
*/
class TestMethodInvoker extends Statement {
	private final TestClassMetadata testClassMetadata;
	private final ExtendedFrameworkMethod extendedFrameworkMethod;
	private final Statement realInvoker;
	private final Object testInstance;

	public TestMethodInvoker(
			Statement realInvoker,
			TestClassMetadata testClassMetadata,
			ExtendedFrameworkMethod extendedFrameworkMethod,
			Object testInstance) {
		this.realInvoker = realInvoker;
		this.testClassMetadata = testClassMetadata;
		this.extendedFrameworkMethod = extendedFrameworkMethod;
		this.testInstance = testInstance;
	}

	@Override
	public void evaluate() throws Throwable {
		final FailureExpected failureExpected = extendedFrameworkMethod.getFailureExpectedAnnotation();
		try {
			realInvoker.evaluate();
			// reaching here is expected, unless the test is marked as an expected failure
			if ( failureExpected != null ) {
				throw new CustomRunner.FailureExpectedTestPassedException( extendedFrameworkMethod );
			}
		}
		catch (CustomRunner.FailureExpectedTestPassedException e) {
			// just pass this along
			throw e;
		}
		catch (Throwable e) {
			// on error handling is very different based on whether the test was marked as an expected failure
			if ( failureExpected != null ) {
				// handle the expected failure case
				TestLogger.LOG.infof(
						"Ignoring expected failure [{}] : {}",
						Helper.extractTestName( extendedFrameworkMethod ),
						Helper.extractMessage( failureExpected )
				);
				testClassMetadata.performOnExpectedFailureCallback( testInstance );
				// most importantly, do not propagate exception...
			}
			else {
				// handle the non-expected failure case
				testClassMetadata.performOnFailureCallback( testInstance );
				throw e;
			}
		}
	}
}
