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
	public void testConstructorWithForeignKeyColumn() {
		ForeignKeyDescriptor fk = new ForeignKeyDescriptor(
			"department", "DEPARTMENT_ID", "Department", "com.example");

		assertEquals("department", fk.getFieldName());
		assertEquals("DEPARTMENT_ID", fk.getForeignKeyColumnName());
		assertEquals("Department", fk.getTargetEntityClassName());
		assertEquals("com.example", fk.getTargetEntityPackage());
		assertTrue(fk.isOptional());
		assertNull(fk.getFetchType());
		assertNull(fk.getReferencedColumnName());
		assertFalse(fk.isPartOfCompositeKey());
		assertEquals(1, fk.getJoinColumns().size());
		assertEquals(1, fk.getForeignKeyColumnNames().size());
		assertEquals("DEPARTMENT_ID", fk.getForeignKeyColumnNames().get(0));
	}

	@Test
	public void testConstructorWithoutForeignKeyColumn() {
		ForeignKeyDescriptor fk = new ForeignKeyDescriptor(
			"department", "Department", "com.example");

		assertEquals("department", fk.getFieldName());
		assertNull(fk.getForeignKeyColumnName());
		assertEquals("Department", fk.getTargetEntityClassName());
		assertEquals("com.example", fk.getTargetEntityPackage());
		assertTrue(fk.isOptional());
		assertTrue(fk.getJoinColumns().isEmpty());
		assertTrue(fk.getForeignKeyColumnNames().isEmpty());
	}

	@Test
	public void testReferencedColumnName() {
		ForeignKeyDescriptor fk = new ForeignKeyDescriptor(
			"department", "DEPT_ID", "Department", "com.example")
			.referencedColumnName("ID");

		assertEquals("ID", fk.getReferencedColumnName());
		assertEquals("DEPT_ID", fk.getJoinColumns().get(0).fkColumnName());
		assertEquals("ID", fk.getJoinColumns().get(0).referencedColumnName());
	}

	@Test
	public void testAddJoinColumn() {
		ForeignKeyDescriptor fk = new ForeignKeyDescriptor(
			"department", "Department", "com.example")
			.addJoinColumn("DEPT_ID", "ID")
			.addJoinColumn("REGION_ID", "REGION_PK");

		assertEquals(2, fk.getJoinColumns().size());
		assertEquals("DEPT_ID", fk.getJoinColumns().get(0).fkColumnName());
		assertEquals("ID", fk.getJoinColumns().get(0).referencedColumnName());
		assertEquals("REGION_ID", fk.getJoinColumns().get(1).fkColumnName());
		assertEquals("REGION_PK", fk.getJoinColumns().get(1).referencedColumnName());

		assertEquals("DEPT_ID", fk.getForeignKeyColumnName());
		assertEquals("ID", fk.getReferencedColumnName());
		assertEquals(2, fk.getForeignKeyColumnNames().size());
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
	public void testPartOfCompositeKey() {
		ForeignKeyDescriptor fk = new ForeignKeyDescriptor(
			"department", "DEPARTMENT_ID", "Department", "com.example")
			.partOfCompositeKey(true);

		assertTrue(fk.isPartOfCompositeKey());
	}

	@Test
	public void testFluentChaining() {
		ForeignKeyDescriptor fk = new ForeignKeyDescriptor(
			"department", "DEPARTMENT_ID", "Department", "com.example")
			.referencedColumnName("DEPT_PK")
			.fetchType(FetchType.EAGER)
			.optional(false)
			.partOfCompositeKey(true);

		assertEquals("DEPT_PK", fk.getReferencedColumnName());
		assertEquals(FetchType.EAGER, fk.getFetchType());
		assertFalse(fk.isOptional());
		assertTrue(fk.isPartOfCompositeKey());
	}

	@Test
	public void testJoinColumnsUnmodifiable() {
		ForeignKeyDescriptor fk = new ForeignKeyDescriptor(
			"department", "DEPARTMENT_ID", "Department", "com.example");

		assertThrows(UnsupportedOperationException.class,
			() -> fk.getJoinColumns().add(new JoinColumnPair("X", "Y")));
	}
}
