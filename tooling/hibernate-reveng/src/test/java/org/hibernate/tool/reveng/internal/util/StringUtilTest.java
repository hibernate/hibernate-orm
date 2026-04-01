/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StringUtilTest {

	@Test
	public void testIsEmptyOrNull() {
		assertTrue(StringUtil.isEmptyOrNull(null));
		assertTrue(StringUtil.isEmptyOrNull(""));
		assertFalse(StringUtil.isEmptyOrNull("foo"));
	}

	@Test
	public void testSplitWithSeparator() {
		String[] result = StringUtil.split("a,b,c", ",");
		assertArrayEquals(new String[]{"a", "b", "c"}, result);
	}

	@Test
	public void testSplitWithNullSeparator() {
		String[] result = StringUtil.split("a b c", null);
		assertArrayEquals(new String[]{"a", "b", "c"}, result);
	}

	@Test
	public void testSplitSingleElement() {
		String[] result = StringUtil.split("abc", ",");
		assertArrayEquals(new String[]{"abc"}, result);
	}

	@Test
	public void testLeftPad() {
		assertEquals("  abc", StringUtil.leftPad("abc", 5));
	}

	@Test
	public void testLeftPadNoChange() {
		assertEquals("abc", StringUtil.leftPad("abc", 3));
	}

	@Test
	public void testLeftPadShorter() {
		assertEquals("abc", StringUtil.leftPad("abc", 2));
	}

	@Test
	public void testIsEqual() {
		assertTrue(StringUtil.isEqual("abc", "abc"));
		assertFalse(StringUtil.isEqual("abc", "def"));
		assertFalse(StringUtil.isEqual("abc", null));
		assertFalse(StringUtil.isEqual(null, "abc"));
		assertTrue(StringUtil.isEqual(null, null));
	}

	@Test
	public void testIsEmptyOrNullWithSpace() {
		assertFalse(StringUtil.isEmptyOrNull(" "));
	}
}
