/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TableNameQualifierTest {

	@Test
	public void testQualifyAllParts() {
		assertEquals("MY_CAT.MY_SCH.MY_TABLE",
				TableNameQualifier.qualify("MY_CAT", "MY_SCH", "MY_TABLE"));
	}

	@Test
	public void testQualifyNoCatalog() {
		assertEquals("MY_SCH.MY_TABLE",
				TableNameQualifier.qualify(null, "MY_SCH", "MY_TABLE"));
	}

	@Test
	public void testQualifyNoSchema() {
		assertEquals("MY_CAT.MY_TABLE",
				TableNameQualifier.qualify("MY_CAT", null, "MY_TABLE"));
	}

	@Test
	public void testQualifyTableOnly() {
		assertEquals("MY_TABLE",
				TableNameQualifier.qualify(null, null, "MY_TABLE"));
	}
}
