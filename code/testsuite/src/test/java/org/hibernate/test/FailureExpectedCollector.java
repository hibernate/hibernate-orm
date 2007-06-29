package org.hibernate.test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.hibernate.junit.TestSuiteVisitor;

/**
 * A simple class to collect the names of "failure expected" tests...
 *
 * @author Steve Ebersole
 */
public class FailureExpectedCollector {

	public static void main(String[] args) {
		Set testNames = collectAllFailureExpectedTestNames();
		Iterator itr = testNames.iterator();
		int i = 0;
		while ( itr.hasNext() ) {
			i++;
			System.out.println( i + ") " + itr.next() );
		}
	}

	public static Set collectAllFailureExpectedTestNames() {
		Set names = new HashSet();
// todo : need to come up with a new scheme to do this...
//		collectFailureExpectedTestNames( names, ( TestSuite ) AllTests.unfilteredSuite() );
		return names;
	}

	public static void collectFailureExpectedTestNames(final Set names, TestSuite suite) {
		TestSuiteVisitor.Handler handler = new TestSuiteVisitor.Handler() {
			public void handleTestCase(Test test) {
				TestCase testCase = ( TestCase ) test;
				if ( testCase.getName().endsWith( "FailureExpected" ) ) {
					names.add( testCase.getClass().getName() + "#" + testCase.getName() );
				}
			}
			public void startingTestSuite(TestSuite suite) {}
			public void completedTestSuite(TestSuite suite) {}
		};
		TestSuiteVisitor visitor = new TestSuiteVisitor( handler );
		visitor.visit( suite );
	}
}
