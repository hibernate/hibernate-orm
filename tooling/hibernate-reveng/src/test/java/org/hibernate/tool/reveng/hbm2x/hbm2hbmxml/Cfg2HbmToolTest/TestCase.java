/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.Cfg2HbmToolTest;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.tool.reveng.internal.export.hbm.Cfg2HbmTool;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dmitry Geraskov
 * @author koen
 */
public class TestCase {

	@Test
	public void testNeedsTable(){
		MetadataBuildingContext mdbc = createMetadataBuildingContext();
		Cfg2HbmTool c2h = new Cfg2HbmTool();
		PersistentClass pc = new RootClass(mdbc);
		assertTrue(c2h.needsTable(pc));
		assertTrue(c2h.needsTable(new JoinedSubclass(pc, mdbc)));
		assertTrue(c2h.needsTable(new UnionSubclass(pc, mdbc)));
		assertFalse(c2h.needsTable(new SingleTableSubclass(pc, mdbc)));
		assertFalse(c2h.needsTable(new Subclass(pc, mdbc)));
	}

	private MetadataBuildingContext createMetadataBuildingContext() {
		return (MetadataBuildingContext)Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class[] { MetadataBuildingContext.class },
				(proxy, method, args) -> null);
	}

}
