package org.hibernate.junit;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import junit.framework.TestSuite;
import junit.framework.Test;
import junit.framework.TestCase;

import org.hibernate.test.AllTests;

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
		collectFailureExpectedTestNames( names, ( TestSuite ) AllTests.unfilteredSuite() );
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
