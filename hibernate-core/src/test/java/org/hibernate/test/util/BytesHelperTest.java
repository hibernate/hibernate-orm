/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.hibernate.internal.util.BytesHelper;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Benoit W
 */
public class BytesHelperTest extends BaseUnitTestCase {

	@Test
	public void testAsLongNullArray() {
		assertEquals(0, BytesHelper.asLong(null, 0));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testAsLongArrayTooSmall() {
		byte[] src = new byte[16];
		assertEquals(0, BytesHelper.asLong(src, 9));
	}
	
	@Test
	public void testAsLong() {
		byte[] src = new byte[] {-92, -120, -59, -64, 97, 55, -41, -55, 64, -43, 20, 109, -7, -95, 77, -115};
		assertEquals(-6590800624601278519L, BytesHelper.asLong(src, 0));
		assertEquals(4671662651038846349L, BytesHelper.asLong(src, 8));
	}
	
	@Test
	public void testfromLong() {
		byte[] expected = new byte[] {-92, -120, -59, -64, 97, 55, -41, -55, 64, -43, 20, 109, -7, -95, 77, -115};
		byte[] dest = new byte[16];
		BytesHelper.fromLong(-6590800624601278519L, dest, 0);
		BytesHelper.fromLong(4671662651038846349L, dest, 8);
		assertArrayEquals(expected, dest);
	}
}
