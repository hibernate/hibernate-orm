package org.hibernate.junit.functional;

import org.hibernate.junit.SkipLog;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public abstract class DatabaseSpecificFunctionalTestCase extends FunctionalTestCase {
	public DatabaseSpecificFunctionalTestCase(String string) {
		super( string );
	}

	protected void runTest() throws Throwable {
		// Note: this protection comes into play when running
		// tests individually.  The suite as a whole is already
		// "protected" by the fact that these tests are actually
		// filtered out of the suite
		if ( appliesTo( getDialect() ) ) {
			super.runTest();
		}
		else {
			SkipLog.LOG.warn( "skipping database-specific test [" + fullTestName() + "] for dialect [" + getDialect().getClass().getName() + "]" );
		}
	}
}
