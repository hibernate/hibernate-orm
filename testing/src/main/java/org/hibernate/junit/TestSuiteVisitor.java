/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
