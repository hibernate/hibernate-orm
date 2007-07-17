package org.hibernate.junit;

import java.util.Enumeration;

import junit.framework.TestSuite;
import junit.framework.Test;

/**
 * Handles walking a TestSuite hierarchy for recognition of individual tests.
 *
 * @author Steve Ebersole
 */
public class TestSuiteVisitor {

	private final TestSuiteVisitor.Handler handler;

	public TestSuiteVisitor(TestSuiteVisitor.Handler handler) {
		this.handler = handler;
	}

	public void visit(TestSuite testSuite) {
		handler.startingTestSuite( testSuite );
		Enumeration tests = testSuite.tests();
		while ( tests.hasMoreElements() ) {
			Test test = ( Test ) tests.nextElement();
			if ( test instanceof TestSuite ) {
				visit( ( TestSuite ) test );
			}
			else {
				handler.handleTestCase( test );
			}
		}
		handler.completedTestSuite( testSuite );
	}

	public static interface Handler {
		public void handleTestCase(Test test);
		public void startingTestSuite(TestSuite suite);
		public void completedTestSuite(TestSuite suite);
	}

}
