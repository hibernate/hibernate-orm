/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StringUtilTest {

	@Test
	public void testIsEmptyOrNull() {
		assertTrue(StringUtil.isEmptyOrNull(null));
		assertTrue(StringUtil.isEmptyOrNull(""));
		assertFalse(StringUtil.isEmptyOrNull("foo"));
	}

}
