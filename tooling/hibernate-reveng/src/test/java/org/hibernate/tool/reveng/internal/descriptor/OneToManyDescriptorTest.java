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
 * Tests for {@link OneToManyDescriptor}.
 *
 * @author Koen Aers
 */
public class OneToManyDescriptorTest {

	@Test
	public void testConstructorAndDefaults() {
		OneToManyDescriptor otm = new OneToManyDescriptor(
			"employees", "department", "Employee", "com.example");

		assertEquals("employees", otm.getFieldName());
		assertEquals("department", otm.getMappedBy());
		assertEquals("Employee", otm.getElementEntityClassName());
		assertEquals("com.example", otm.getElementEntityPackage());
		assertNull(otm.getFetchType());
		assertNull(otm.getCascadeTypes());
		assertFalse(otm.isOrphanRemoval());
	}

	@Test
	public void testFetchType() {
		OneToManyDescriptor otm = new OneToManyDescriptor(
			"employees", "department", "Employee", "com.example")
			.fetchType(FetchType.EAGER);

		assertEquals(FetchType.EAGER, otm.getFetchType());
	}

	@Test
	public void testCascadeTypes() {
		OneToManyDescriptor otm = new OneToManyDescriptor(
			"employees", "department", "Employee", "com.example")
			.cascade(CascadeType.ALL);

		assertArrayEquals(new CascadeType[]{ CascadeType.ALL }, otm.getCascadeTypes());
	}

	@Test
	public void testMultipleCascadeTypes() {
		OneToManyDescriptor otm = new OneToManyDescriptor(
			"employees", "department", "Employee", "com.example")
			.cascade(CascadeType.PERSIST, CascadeType.MERGE);

		assertArrayEquals(
			new CascadeType[]{ CascadeType.PERSIST, CascadeType.MERGE },
			otm.getCascadeTypes());
	}

	@Test
	public void testOrphanRemoval() {
		OneToManyDescriptor otm = new OneToManyDescriptor(
			"employees", "department", "Employee", "com.example")
			.orphanRemoval(true);

		assertTrue(otm.isOrphanRemoval());
	}

	@Test
	public void testFluentChaining() {
		OneToManyDescriptor otm = new OneToManyDescriptor(
			"employees", "department", "Employee", "com.example")
			.fetchType(FetchType.LAZY)
			.cascade(CascadeType.ALL)
			.orphanRemoval(true);

		assertEquals(FetchType.LAZY, otm.getFetchType());
		assertArrayEquals(new CascadeType[]{ CascadeType.ALL }, otm.getCascadeTypes());
		assertTrue(otm.isOrphanRemoval());
	}
}
