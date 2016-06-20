/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.util;

import org.junit.Test;

import org.hibernate.internal.util.compare.ArrayComparator;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */
public class ByteArrayComparatorTest extends BaseUnitTestCase {

	@Test
	public void testNull() {
		try {
			ArrayComparator.BYTE_ARRAY_COMPARATOR.compare( null, null );
			fail( "should have thrown NullPointerException" );
		}
		catch ( NullPointerException ex ) {
			// expected
		}

		try {
			ArrayComparator.BYTE_ARRAY_COMPARATOR.compare( null, new Byte[] { 1 } );
			fail( "should have thrown NullPointerException" );
		}
		catch ( NullPointerException ex ) {
			// expected
		}

		try {
			ArrayComparator.BYTE_ARRAY_COMPARATOR.compare( new Byte[] { 1 }, null );
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
				ArrayComparator.BYTE_ARRAY_COMPARATOR.compare(
						new Byte[] {},
						new Byte[] {}
				)
		);
		assertEquals(
				0,
				ArrayComparator.BYTE_ARRAY_COMPARATOR.compare(
						new Byte[] { 1 },
						new Byte[] { 1 }
				)
		);
		assertEquals(
				0,
				ArrayComparator.BYTE_ARRAY_COMPARATOR.compare(
						new Byte[] { 1, 2 },
						new Byte[] { 1, 2 }
				)
		);
		assertTrue(
				ArrayComparator.BYTE_ARRAY_COMPARATOR.compare(
						new Byte[] { 0, 2 },
						new Byte[] { 1, 2 }
				) < 0
		);

		assertTrue(
				ArrayComparator.BYTE_ARRAY_COMPARATOR.compare(
						new Byte[] { 1, 1 },
						new Byte[] { 1, 2 }
				) < 0
		);

		assertTrue( ArrayComparator.BYTE_ARRAY_COMPARATOR.compare(
						new Byte[] { 2, 2 },
						new Byte[] { 1, 2 }
				) > 0
		);

		assertTrue( ArrayComparator.BYTE_ARRAY_COMPARATOR.compare(
						new Byte[] { 2, 2 },
						new Byte[] { 2, 1 }
				) > 0
		);
	}

	@Test
	public void testArraysDifferentLength() {
		assertTrue( ArrayComparator.BYTE_ARRAY_COMPARATOR.compare(
						new Byte[] {},
						new Byte[] { 1 }
				) < 0
		);

		assertTrue( ArrayComparator.BYTE_ARRAY_COMPARATOR.compare(
						new Byte[] { 1 },
						new Byte[] {}
				) > 0
		);

		assertTrue( ArrayComparator.BYTE_ARRAY_COMPARATOR.compare(
						new Byte[] { 1 },
						new Byte[] { 1, 2 }
				) < 0
		);
		assertTrue( ArrayComparator.BYTE_ARRAY_COMPARATOR.compare(
						new Byte[] { 1, 2 },
						new Byte[] { 1 }
				) > 0
		);

		assertTrue( ArrayComparator.BYTE_ARRAY_COMPARATOR.compare(
						new Byte[] { 2 },
						new Byte[] { 1, 2 }
				) > 0
		);
		assertTrue( ArrayComparator.BYTE_ARRAY_COMPARATOR.compare(
						new Byte[] { 1, 2 },
						new Byte[] { 2 }
				) < 0
		);

	}


}
