/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.Cfg2HbmToolTest;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.tool.reveng.internal.builder.hbm.HbmBuildContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests basic ClassDetails annotation handling for inheritance-related
 * table decisions (replaces old Cfg2HbmTool.needsTable() test).
 *
 * @author Dmitry Geraskov
 * @author koen
 */
public class TestCase {

	@Test
	public void testNeedsTable() {
		var ctx = new HbmBuildContext();
		var mc = ctx.getModelsContext();
		// Root class with @Table needs a table
		DynamicClassDetails root = new DynamicClassDetails(
				"Root", "org.test.Root", Object.class,
				false, null, null, mc);
		root.addAnnotationUsage(JpaAnnotations.ENTITY.createUsage(mc));
		root.addAnnotationUsage(JpaAnnotations.TABLE.createUsage(mc));
		assertTrue(root.hasDirectAnnotationUsage(jakarta.persistence.Table.class),
				"Root entity with @Table should need a table");
		// Single-table subclass does not have its own @Table
		DynamicClassDetails singleTableSub = new DynamicClassDetails(
				"Sub", "org.test.Sub", Object.class,
				false, null, null, mc);
		singleTableSub.addAnnotationUsage(JpaAnnotations.ENTITY.createUsage(mc));
		assertFalse(singleTableSub.hasDirectAnnotationUsage(jakarta.persistence.Table.class),
				"Single-table subclass should not have @Table");
	}

}
