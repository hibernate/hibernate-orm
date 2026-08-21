/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.ide.completion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class EntityNameReferenceTest {

	@Test
	public void testConstructorAndGetters() {
		EntityNameReference ref = new EntityNameReference("Product", "p");
		assertEquals("Product", ref.getEntityName());
		assertEquals("p", ref.getAlias());
	}

	@Test
	public void testToString() {
		EntityNameReference ref = new EntityNameReference("com.example.Order", "o");
		assertEquals("o:com.example.Order", ref.toString());
	}

	@Test
	public void testSameEntityAndAlias() {
		EntityNameReference ref = new EntityNameReference("Product", "Product");
		assertEquals("Product", ref.getEntityName());
		assertEquals("Product", ref.getAlias());
		assertEquals("Product:Product", ref.toString());
	}

	@Test
	public void testNullValues() {
		EntityNameReference ref = new EntityNameReference(null, null);
		assertNull(ref.getEntityName());
		assertNull(ref.getAlias());
		assertEquals("null:null", ref.toString());
	}
}
