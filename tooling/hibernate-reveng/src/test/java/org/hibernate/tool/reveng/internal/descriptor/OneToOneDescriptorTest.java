/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.descriptor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;

/**
 * Tests for {@link OneToOneDescriptor}.
 *
 * @author Koen Aers
 */
public class OneToOneDescriptorTest {

	@Test
	public void testConstructorAndDefaults() {
		OneToOneDescriptor oto = new OneToOneDescriptor(
			"address", "Address", "com.example");

		assertEquals("address", oto.getFieldName());
		assertEquals("Address", oto.getTargetEntityClassName());
		assertEquals("com.example", oto.getTargetEntityPackage());
		assertTrue(oto.isOptional(), "Should default to optional");
		assertFalse(oto.isOrphanRemoval());
		assertNull(oto.getForeignKeyColumnName());
		assertNull(oto.getReferencedColumnName());
		assertNull(oto.getMappedBy());
		assertNull(oto.getFetchType());
		assertNull(oto.getCascadeTypes());
	}

	@Test
	public void testOwningSide() {
		OneToOneDescriptor oto = new OneToOneDescriptor(
			"address", "Address", "com.example")
			.foreignKeyColumnName("ADDRESS_ID")
			.fetchType(FetchType.LAZY)
			.cascade(CascadeType.ALL);

		assertEquals("ADDRESS_ID", oto.getForeignKeyColumnName());
		assertEquals(FetchType.LAZY, oto.getFetchType());
		assertArrayEquals(new CascadeType[]{ CascadeType.ALL }, oto.getCascadeTypes());
	}

	@Test
	public void testInverseSide() {
		OneToOneDescriptor oto = new OneToOneDescriptor(
			"user", "User", "com.example")
			.mappedBy("address")
			.fetchType(FetchType.LAZY);

		assertEquals("address", oto.getMappedBy());
		assertEquals(FetchType.LAZY, oto.getFetchType());
	}

	@Test
	public void testReferencedColumnName() {
		OneToOneDescriptor oto = new OneToOneDescriptor(
			"address", "Address", "com.example")
			.foreignKeyColumnName("ADDR_ID")
			.referencedColumnName("ADDRESS_PK");

		assertEquals("ADDRESS_PK", oto.getReferencedColumnName());
	}

	@Test
	public void testOptional() {
		OneToOneDescriptor oto = new OneToOneDescriptor(
			"address", "Address", "com.example")
			.optional(false);

		assertFalse(oto.isOptional());
	}

	@Test
	public void testOrphanRemoval() {
		OneToOneDescriptor oto = new OneToOneDescriptor(
			"address", "Address", "com.example")
			.orphanRemoval(true);

		assertTrue(oto.isOrphanRemoval());
	}

	@Test
	public void testFluentChaining() {
		OneToOneDescriptor oto = new OneToOneDescriptor(
			"address", "Address", "com.example")
			.foreignKeyColumnName("ADDRESS_ID")
			.referencedColumnName("ID")
			.fetchType(FetchType.LAZY)
			.cascade(CascadeType.ALL)
			.optional(false)
			.orphanRemoval(true);

		assertEquals("ADDRESS_ID", oto.getForeignKeyColumnName());
		assertEquals("ID", oto.getReferencedColumnName());
		assertEquals(FetchType.LAZY, oto.getFetchType());
		assertArrayEquals(new CascadeType[]{ CascadeType.ALL }, oto.getCascadeTypes());
		assertFalse(oto.isOptional());
		assertTrue(oto.isOrphanRemoval());
	}
}
