/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.util;

import org.junit.Test;

import org.hibernate.internal.util.compare.RowVersionComparator;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */
public class RowVersionComparatorTest extends BaseUnitTestCase {

	@Test
	public void testNull() {
		try {
			RowVersionComparator.INSTANCE.compare( null, null );
			fail( "should have thrown NullPointerException" );
		}
		catch ( NullPointerException ex ) {
			// expected
		}

		try {
			RowVersionComparator.INSTANCE.compare( null, new byte[] { 1 } );
			fail( "should have thrown NullPointerException" );
		}
		catch ( NullPointerException ex ) {
			// expected
		}

		try {
			RowVersionComparator.INSTANCE.compare( new byte[] { 1 }, null );
			fail( "should have thrown NullPointerException" );
		}
		catch ( NullPointerException ex ) {
			// expected
		}
	}

	@Test
	public void testArraysSameLength() {
		assertEquals(
				0,
				RowVersionComparator.INSTANCE.compare(
						new byte[] {},
						new byte[] {}
				)
		);
		assertEquals(
				0,
				RowVersionComparator.INSTANCE.compare(
						new byte[] { 1 },
						new byte[] { 1 }
				)
		);
		assertEquals(
				0,
				RowVersionComparator.INSTANCE.compare(
						new byte[] { 1, 2 },
						new byte[] { 1, 2 }
				)
		);
		assertTrue(
				RowVersionComparator.INSTANCE.compare(
						new byte[] { 0, 2 },
						new byte[] { 1, 2 }
				) < 0
		);

		assertTrue(
				RowVersionComparator.INSTANCE.compare(
						new byte[] { 1, 1 },
						new byte[] { 1, 2 }
				) < 0
		);

		assertTrue( RowVersionComparator.INSTANCE.compare(
						new byte[] { 2, 2 },
						new byte[] { 1, 2 }
				) > 0
		);

		assertTrue( RowVersionComparator.INSTANCE.compare(
						new byte[] { 2, 2 },
						new byte[] { 2, 1 }
				) > 0
		);
	}

	@Test
	public void testArraysDifferentLength() {
		assertTrue( RowVersionComparator.INSTANCE.compare(
						new byte[] {},
						new byte[] { 1 }
				) < 0
		);

		assertTrue( RowVersionComparator.INSTANCE.compare(
						new byte[] { 1 },
						new byte[] {}
				) > 0
		);

		assertTrue( RowVersionComparator.INSTANCE.compare(
						new byte[] { 1 },
						new byte[] { 1, 2 }
				) < 0
		);
		assertTrue( RowVersionComparator.INSTANCE.compare(
						new byte[] { 1, 2 },
						new byte[] { 1 }
				) > 0
		);

		assertTrue( RowVersionComparator.INSTANCE.compare(
						new byte[] { 2 },
						new byte[] { 1, 2 }
				) > 0
		);
		assertTrue( RowVersionComparator.INSTANCE.compare(
						new byte[] { 1, 2 },
						new byte[] { 2 }
				) < 0
		);

	}


}
