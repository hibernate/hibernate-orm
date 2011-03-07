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

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.OnExpectedFailure;
import org.hibernate.testing.OnFailure;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class FailureExpectedTest {
	private static int onFailureCount = 0;

	@Test
	public void testWithoutValidation() {
		onFailureCount = 0;
		Result result = JUnitCore.runClasses( FixtureTests.class );
		assertEquals( 3, result.getRunCount() );
		assertEquals( 2, result.getFailureCount() );
		assertEquals( 2, onFailureCount );
		assertEquals( 0, result.getIgnoreCount() );
	}

	@Test
	public void testWithValidation() {
		onFailureCount = 0;
		String original = System.getProperty( Helper.VALIDATE_FAILURE_EXPECTED );
		System.setProperty( Helper.VALIDATE_FAILURE_EXPECTED, "true" );
		try {
			Result result = JUnitCore.runClasses( FixtureTests.class );
			assertEquals( 5, result.getRunCount() );
			assertEquals( 3, result.getFailureCount() );
			assertEquals( 3, onFailureCount );
			assertEquals( 0, result.getIgnoreCount() );
		}
		finally {
			if ( original == null ) {
				System.getProperties().remove( Helper.VALIDATE_FAILURE_EXPECTED );
			}
			else {
				System.setProperty( Helper.VALIDATE_FAILURE_EXPECTED, original );
			}
		}
	}

	public static class FixtureTests extends BaseUnitTestCase {
		@Test
		public void control() {
		}

		@Test
		public void failureControl() {
			assertTrue( false );
		}

		@Test
		public void exceptionControl() {
			throw new RuntimeException();
		}

		@Test
		@FailureExpected( jiraKey = "n/a" )
		public void correctlyDefinedFailureExcepted() {
			throw new RuntimeException();
		}

		@Test
		@FailureExpected( jiraKey = "n/a" )
		public void incorrectlyDefinedFailureExcepted() {
		}

		@OnFailure
		@OnExpectedFailure
		void handleError() {
			onFailureCount++;
		}
	}
}
