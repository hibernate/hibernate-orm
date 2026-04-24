/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.descriptor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import jakarta.persistence.FetchType;

/**
 * Tests for {@link ForeignKeyDescriptor}.
 *
 * @author Koen Aers
 */
public class ForeignKeyDescriptorTest {

	@Test
	public void testConstructorAndDefaults() {
		ForeignKeyDescriptor fk = new ForeignKeyDescriptor(
			"department", "DEPARTMENT_ID", "Department", "com.example");

		assertEquals("department", fk.getFieldName());
		assertEquals("DEPARTMENT_ID", fk.getForeignKeyColumnName());
		assertEquals("Department", fk.getTargetEntityClassName());
		assertEquals("com.example", fk.getTargetEntityPackage());
		assertTrue(fk.isOptional(), "Should default to optional");
		assertNull(fk.getFetchType());
		assertNull(fk.getReferencedColumnName());
	}

	@Test
	public void testReferencedColumnName() {
		ForeignKeyDescriptor fk = new ForeignKeyDescriptor(
			"department", "DEPT_ID", "Department", "com.example")
			.referencedColumnName("ID");

		assertEquals("ID", fk.getReferencedColumnName());
	}

	@Test
	public void testFetchType() {
		ForeignKeyDescriptor fk = new ForeignKeyDescriptor(
			"department", "DEPARTMENT_ID", "Department", "com.example")
			.fetchType(FetchType.LAZY);

		assertEquals(FetchType.LAZY, fk.getFetchType());
	}

	@Test
	public void testOptional() {
		ForeignKeyDescriptor fk = new ForeignKeyDescriptor(
			"department", "DEPARTMENT_ID", "Department", "com.example")
			.optional(false);

		assertFalse(fk.isOptional());
	}

	@Test
	public void testFluentChaining() {
		ForeignKeyDescriptor fk = new ForeignKeyDescriptor(
			"department", "DEPARTMENT_ID", "Department", "com.example")
			.referencedColumnName("DEPT_PK")
			.fetchType(FetchType.EAGER)
			.optional(false);

		assertEquals("DEPT_PK", fk.getReferencedColumnName());
		assertEquals(FetchType.EAGER, fk.getFetchType());
		assertFalse(fk.isOptional());
	}
}
