/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.hbm;

import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HBMTagVisitorTest {

	@Test
	public void testManyToOneTag() {
		assertEquals("many-to-one", HBMTagForValueVisitor.INSTANCE.accept((ManyToOne) null));
	}

	@Test
	public void testOneToOneTag() {
		assertEquals("one-to-one", HBMTagForValueVisitor.INSTANCE.accept((OneToOne) null));
	}

	@Test
	public void testPersistentClassVisitorInstance() {
		assertNotNull(HBMTagForPersistentClassVisitor.INSTANCE);
	}

	@Test
	public void testValueVisitorInstance() {
		assertNotNull(HBMTagForValueVisitor.INSTANCE);
	}
}
