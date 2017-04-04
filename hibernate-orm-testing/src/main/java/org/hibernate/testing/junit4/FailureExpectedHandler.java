/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	private final TestClassMetadata testClassMetadata;
	private final ExtendedFrameworkMethod extendedFrameworkMethod;
	private final Statement realInvoker;
	private final Object testInstance;

	public FailureExpectedHandler(
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
						"Ignoring expected failure [%s] : %s",
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
