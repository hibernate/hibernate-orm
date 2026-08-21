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
	public void testSplitWithSeparator() {
		String[] result = StringUtil.split("a.b.c", ".");
		assertArrayEquals(new String[] {"a", "b", "c"}, result);
	}

	@Test
	public void testSplitWithNullSeparator() {
		String[] result = StringUtil.split("hello world foo", null);
		assertArrayEquals(new String[] {"hello", "world", "foo"}, result);
	}

	@Test
	public void testSplitSingleToken() {
		String[] result = StringUtil.split("hello", ".");
		assertArrayEquals(new String[] {"hello"}, result);
	}

	@Test
	public void testSplitWithMultiCharSeparator() {
		String[] result = StringUtil.split("a,b;c", ",;");
		assertArrayEquals(new String[] {"a", "b", "c"}, result);
	}

	@Test
	public void testLeftPadShorterString() {
		assertEquals("   hi", StringUtil.leftPad("hi", 5));
	}

	@Test
	public void testLeftPadExactLength() {
		assertEquals("hello", StringUtil.leftPad("hello", 5));
	}

	@Test
	public void testLeftPadLongerString() {
		assertEquals("hello", StringUtil.leftPad("hello", 3));
	}

	@Test
	public void testLeftPadSingleChar() {
		assertEquals("    x", StringUtil.leftPad("x", 5));
	}

	@Test
	public void testIsEqualBothNull() {
		assertTrue(StringUtil.isEqual(null, null));
	}

	@Test
	public void testIsEqualSameString() {
		assertTrue(StringUtil.isEqual("abc", "abc"));
	}

	@Test
	public void testIsEqualDifferentStrings() {
		assertFalse(StringUtil.isEqual("abc", "def"));
	}

	@Test
	public void testIsEqualOneNull() {
		assertFalse(StringUtil.isEqual("abc", null));
		assertFalse(StringUtil.isEqual(null, "abc"));
	}

	@Test
	public void testIsEmptyOrNullWithNull() {
		assertTrue(StringUtil.isEmptyOrNull(null));
	}

	@Test
	public void testIsEmptyOrNullWithEmpty() {
		assertTrue(StringUtil.isEmptyOrNull(""));
	}

	@Test
	public void testIsEmptyOrNullWithContent() {
		assertFalse(StringUtil.isEmptyOrNull("hello"));
	}

	@Test
	public void testIsEmptyOrNullWithWhitespace() {
		assertFalse(StringUtil.isEmptyOrNull(" "));
	}
}
