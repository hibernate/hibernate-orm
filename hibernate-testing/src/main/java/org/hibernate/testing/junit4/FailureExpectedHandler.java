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

import org.hibernate.testing.FailureExpected;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import org.jboss.logging.Logger;

/**
* @author Steve Ebersole
*/
class FailureExpectedHandler extends Statement {
	private static final Logger log = Logger.getLogger( FailureExpectedHandler.class );

	private final CustomRunner runner;
	private final TestClassMetadata testClassMetadata;
	private final ExtendedFrameworkMethod extendedFrameworkMethod;
	private final Statement realInvoker;
	private final Object testInstance;

	public FailureExpectedHandler(
			CustomRunner runner,
			Statement realInvoker,
			TestClassMetadata testClassMetadata,
			ExtendedFrameworkMethod extendedFrameworkMethod,
			Object testInstance) {
		this.runner = runner;
		this.realInvoker = realInvoker;
		this.testClassMetadata = testClassMetadata;
		this.extendedFrameworkMethod = extendedFrameworkMethod;
		this.testInstance = testInstance;
	}

	@Override
	public void evaluate() throws Throwable {
		if ( runner.beforeClassMethodFailed() ) {
			return;
		}
		final FailureExpected failureExpected = extendedFrameworkMethod.getFailureExpectedAnnotation();
		try {
			realInvoker.evaluate();
			// reaching here is expected, unless the test is marked as an expected failure
			if ( failureExpected != null ) {
				throw new FailureExpectedTestPassedException( extendedFrameworkMethod );
			}
		}
		catch (FailureExpectedTestPassedException e) {
			// just pass this along
			throw e;
		}
		catch (Throwable e) {
			// on error handling is very different based on whether the test was marked as an expected failure
			if ( failureExpected != null ) {

				// handle the expected failure case
				log.infof(
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

	public static class FailureExpectedTestPassedException extends Exception {
		public FailureExpectedTestPassedException(FrameworkMethod frameworkMethod) {
			super( "Test marked as FailureExpected, but did not fail : " + Helper.extractTestName( frameworkMethod ) );
		}
	}
}
