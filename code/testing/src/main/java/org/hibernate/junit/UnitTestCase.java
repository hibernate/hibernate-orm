package org.hibernate.junit;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import junit.framework.AssertionFailedError;


/**
 * A basic JUnit {@link junit.framework.TestCase} subclass for
 * adding some Hibernate specific behavior and functionality.
 *
 * @author Steve Ebersole
 */
public abstract class UnitTestCase extends junit.framework.TestCase {

	private static final Log log = LogFactory.getLog( UnitTestCase.class );

	public UnitTestCase(String string) {
		super( string );
	}

	/**
	 * runBare overridden in order to apply FailureExpected validations
	 * as well as start/complete logging
	 *
	 * @throws Throwable
	 */
	public void runBare() throws Throwable {
		final boolean doValidate = getName().endsWith( "FailureExpected" ) && Boolean.getBoolean( "hibernate.test.validatefailureexpected" );
		try {
			log.info( "Starting test [" + fullTestName() + "]" );
			super.runBare();
			if ( doValidate ) {
				fail( "Test marked as FailureExpected, but did not fail!" );
			}
		}
		catch( Throwable t ) {
			if ( doValidate ) {
				skipExpectedFailure( t );
			}
			else {
				throw t;
			}
		}
		finally {
			log.info( "Completed test [" + fullTestName() + "]" );
		}
	}

	protected void skipExpectedFailure(Throwable error) {
		reportSkip( "ignoring *FailuredExpected methods", "Failed with: " + error.toString() );
	}

	// additional assertions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public static void assertElementTypeAssignability(java.util.Collection collection, Class clazz) throws AssertionFailedError {
		Iterator itr = collection.iterator();
		while ( itr.hasNext() ) {
			assertClassAssignability( itr.next().getClass(), clazz );
		}
	}

	public static void assertClassAssignability(Class source, Class target) throws AssertionFailedError {
		if ( !target.isAssignableFrom( source ) ) {
			throw new AssertionFailedError(
			        "Classes were not assignment-compatible : source<" + source.getName() +
			        "> target<" + target.getName() + ">"
			);
		}
	}


	// test skipping ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public String fullTestName() {
		return this.getClass().getName() + "#" + this.getName();
	}

	protected void reportSkip(String reason, String testDescription) {
		SkipLog.LOG.warn( "*** skipping [" + fullTestName() + "] - " + testDescription + " : " + reason, new Exception()  );
	}
}
