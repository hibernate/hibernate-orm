/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.util;

import org.hibernate.tool.reveng.api.core.AssociationInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RevengUtilsTest {

	@Test
	public void testCreateAssociationInfoWithValues() {
		AssociationInfo info = RevengUtils.createAssociationInfo("all", "lazy", Boolean.TRUE, Boolean.FALSE);
		assertEquals("all", info.getCascade());
		assertEquals("lazy", info.getFetch());
		assertEquals(Boolean.TRUE, info.getInsert());
		assertEquals(Boolean.FALSE, info.getUpdate());
	}

	@Test
	public void testCreateAssociationInfoWithNulls() {
		AssociationInfo info = RevengUtils.createAssociationInfo(null, null, null, null);
		assertNull(info.getCascade());
		assertNull(info.getFetch());
		assertNull(info.getInsert());
		assertNull(info.getUpdate());
	}
}
