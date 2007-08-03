package org.hibernate.junit.functional;

import junit.framework.TestSuite;
import junit.framework.Test;
import junit.framework.TestResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A specialized {@link junit.framework.TestSuite} implementation intended
 * for use as an aggregate for a single test class specifically for the purpose
 * of maintaing a single {@link org.hibernate.SessionFactory} for executings all
 * tests defined as part of the given functional test class.
 *
 * @author Steve Ebersole
 */
public class FunctionalTestClassTestSuite extends TestSuite {

	private static final Logger log = LoggerFactory.getLogger( FunctionalTestClassTestSuite.class );

	private ExecutionEnvironment.Settings settings;
	private ExecutionEnvironment environment;
	private Throwable environmentSetupError;
	private int testCount;
	private int testPosition;

	public FunctionalTestClassTestSuite(Class testClass, String name) {
		super( testClass, name );
	}

	public FunctionalTestClassTestSuite(Class testClass) {
		this( testClass, testClass.getName() );
	}


	public void addTest(Test test) {
		log.trace( "adding test [" + test + "]" );
		if ( settings == null ) {
			if ( test instanceof ExecutionEnvironment.Settings ) {
				settings = ( ExecutionEnvironment.Settings ) test;
				// todo : we could also centralize the skipping of "database specific" tests here
				// instead of duplicating this notion in AllTests and DatabaseSpecificFunctionalTestCase.
				// as a test gets added, simply check to see if we should really add it via
				// DatabaseSpecificFunctionalTestCase.appliesTo( ExecutionEnvironment.DIALECT )...
			}
		}
		testCount++;
		super.addTest( test );
	}

	public void run(TestResult testResult) {
		if ( testCount == 0 ) {
			// might be zero if database-specific...
			return;
		}
		try {
			log.info( "Starting test-suite [" + getName() + "]" );
			setUp();
			testPosition = 0;
			super.run( testResult );
		}
		finally {
			try {
				tearDown();
			}
			catch( Throwable ignore ) {
			}
			log.info( "Completed test-suite [" + getName() + "]" );
		}
	}

	public void runTest(Test test, TestResult testResult) {
		testPosition++;
		if ( environmentSetupError != null ) {
			testResult.startTest( test );
			testResult.addError( test, environmentSetupError );
			testResult.endTest( test );
			return;
		}
		if ( ! ( test instanceof FunctionalTestCase ) ) {
			super.runTest( test, testResult );
		}
		else {
			FunctionalTestCase functionalTest = ( ( FunctionalTestCase ) test );
			try {
				// disallow rebuilding the schema because this is the last test
				// in this suite, thus it is about to get dropped immediately
				// afterwards anyway...
				environment.setAllowRebuild( testPosition < testCount );
				functionalTest.setEnvironment( environment );
				super.runTest( functionalTest, testResult );
			}
			finally {
				functionalTest.setEnvironment( null );
			}
		}
	}

	protected void setUp() {
		if ( settings == null ) {
			return;
		}
		log.info( "Building aggregated execution environment" );
		try {
			environment = new ExecutionEnvironment( settings );
			environment.initialize();
		}
		catch( Throwable t ) {
			environmentSetupError = t;
		}
	}

	protected void tearDown() {
		if ( environment != null ) {
			log.info( "Destroying aggregated execution environment" );
			environment.complete();
			this.environment = null;
		}
	}
}
