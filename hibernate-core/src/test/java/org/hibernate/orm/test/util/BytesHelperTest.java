/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.util;

import org.hibernate.internal.util.BytesHelper;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.ExpectedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Benoit W
 */
@BaseUnitTest
public class BytesHelperTest {

	@Test
	public void testAsLongNullArray() {
		assertEquals(0, BytesHelper.asLong(null, 0));
	}

	@Test()
	@ExpectedException(IllegalArgumentException.class)
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
