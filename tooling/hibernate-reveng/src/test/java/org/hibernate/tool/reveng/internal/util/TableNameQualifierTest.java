/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TableNameQualifierTest {

	@Test
	public void testQualifyAll() {
		assertEquals("cat.sch.tbl", TableNameQualifier.qualify("cat", "sch", "tbl"));
	}

	@Test
	public void testQualifyNoCatalog() {
		assertEquals("sch.tbl", TableNameQualifier.qualify(null, "sch", "tbl"));
	}

	@Test
	public void testQualifyNoSchema() {
		assertEquals("cat.tbl", TableNameQualifier.qualify("cat", null, "tbl"));
	}

	@Test
	public void testQualifyTableOnly() {
		assertEquals("tbl", TableNameQualifier.qualify(null, null, "tbl"));
	}
}
