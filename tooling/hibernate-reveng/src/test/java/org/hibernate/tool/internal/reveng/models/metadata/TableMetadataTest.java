/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.metadata;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import jakarta.persistence.InheritanceType;

/**
 * Tests for {@link TableMetadata}.
 *
 * @author Koen Aers
 */
public class TableMetadataTest {

	@Test
	public void testConstructorAndDefaults() {
		TableMetadata table = new TableMetadata("PERSON", "Person", "com.example");

		assertEquals("PERSON", table.getTableName());
		assertEquals("Person", table.getEntityClassName());
		assertEquals("com.example", table.getEntityPackage());
		assertNull(table.getSchema());
		assertNull(table.getCatalog());
		assertNotNull(table.getColumns());
		assertTrue(table.getColumns().isEmpty());
		assertNotNull(table.getForeignKeys());
		assertTrue(table.getForeignKeys().isEmpty());
		assertNotNull(table.getOneToManys());
		assertTrue(table.getOneToManys().isEmpty());
		assertNotNull(table.getOneToOnes());
		assertTrue(table.getOneToOnes().isEmpty());
		assertNotNull(table.getManyToManys());
		assertTrue(table.getManyToManys().isEmpty());
		assertNotNull(table.getEmbeddedFields());
		assertTrue(table.getEmbeddedFields().isEmpty());
		assertNull(table.getInheritance());
		assertNull(table.getDiscriminatorValue());
		assertNull(table.getParentEntityClassName());
		assertNull(table.getParentEntityPackage());
		assertNull(table.getPrimaryKeyJoinColumnName());
		assertNull(table.getCompositeId());
	}

	@Test
	public void testSetters() {
		TableMetadata table = new TableMetadata("PERSON", "Person", "com.example");

		table.setTableName("EMPLOYEE");
		table.setEntityClassName("Employee");
		table.setEntityPackage("com.example.entity");
		table.setSchema("public");
		table.setCatalog("mydb");

		assertEquals("EMPLOYEE", table.getTableName());
		assertEquals("Employee", table.getEntityClassName());
		assertEquals("com.example.entity", table.getEntityPackage());
		assertEquals("public", table.getSchema());
		assertEquals("mydb", table.getCatalog());
	}

	@Test
	public void testAddColumn() {
		TableMetadata table = new TableMetadata("PERSON", "Person", "com.example")
			.addColumn(new ColumnMetadata("ID", "id", Long.class))
			.addColumn(new ColumnMetadata("NAME", "name", String.class));

		assertEquals(2, table.getColumns().size());
		assertEquals("ID", table.getColumns().get(0).getColumnName());
		assertEquals("NAME", table.getColumns().get(1).getColumnName());
	}

	@Test
	public void testAddForeignKey() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example")
			.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPARTMENT_ID", "Department", "com.example"));

		assertEquals(1, table.getForeignKeys().size());
		assertEquals("department", table.getForeignKeys().get(0).getFieldName());
	}

	@Test
	public void testAddOneToMany() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example")
			.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));

		assertEquals(1, table.getOneToManys().size());
		assertEquals("employees", table.getOneToManys().get(0).getFieldName());
	}

	@Test
	public void testAddOneToOne() {
		TableMetadata table = new TableMetadata("USER_TABLE", "User", "com.example")
			.addOneToOne(new OneToOneMetadata(
				"address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID"));

		assertEquals(1, table.getOneToOnes().size());
		assertEquals("address", table.getOneToOnes().get(0).getFieldName());
	}

	@Test
	public void testAddManyToMany() {
		TableMetadata table = new TableMetadata("STUDENT", "Student", "com.example")
			.addManyToMany(new ManyToManyMetadata(
				"courses", "Course", "com.example")
				.joinTable("STUDENT_COURSE", "STUDENT_ID", "COURSE_ID"));

		assertEquals(1, table.getManyToManys().size());
		assertEquals("courses", table.getManyToManys().get(0).getFieldName());
	}

	@Test
	public void testAddEmbeddedField() {
		TableMetadata table = new TableMetadata("PERSON", "Person", "com.example")
			.addEmbeddedField(new EmbeddedFieldMetadata(
				"address", "Address", "com.example"));

		assertEquals(1, table.getEmbeddedFields().size());
		assertEquals("address", table.getEmbeddedFields().get(0).getFieldName());
	}

	@Test
	public void testInheritance() {
		InheritanceMetadata inheritance = new InheritanceMetadata(InheritanceType.SINGLE_TABLE);
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example")
			.inheritance(inheritance);

		assertSame(inheritance, table.getInheritance());
	}

	@Test
	public void testDiscriminatorValue() {
		TableMetadata table = new TableMetadata("VEHICLE", "Car", "com.example")
			.discriminatorValue("CAR");

		assertEquals("CAR", table.getDiscriminatorValue());
	}

	@Test
	public void testParent() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example")
			.parent("Vehicle", "com.example");

		assertEquals("Vehicle", table.getParentEntityClassName());
		assertEquals("com.example", table.getParentEntityPackage());
	}

	@Test
	public void testPrimaryKeyJoinColumn() {
		TableMetadata table = new TableMetadata("CREDIT_CARD_PAYMENT", "CreditCardPayment", "com.example")
			.primaryKeyJoinColumn("PAYMENT_ID");

		assertEquals("PAYMENT_ID", table.getPrimaryKeyJoinColumnName());
	}

	@Test
	public void testCompositeId() {
		CompositeIdMetadata compositeId =
			new CompositeIdMetadata("id", "OrderItemId", "com.example");
		TableMetadata table = new TableMetadata("ORDER_ITEM", "OrderItem", "com.example")
			.compositeId(compositeId);

		assertSame(compositeId, table.getCompositeId());
	}

	@Test
	public void testIsForeignKeyColumnWithForeignKey() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example")
			.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPARTMENT_ID", "Department", "com.example"));

		assertTrue(table.isForeignKeyColumn("DEPARTMENT_ID"));
		assertFalse(table.isForeignKeyColumn("NAME"));
	}

	@Test
	public void testIsForeignKeyColumnWithOneToOne() {
		TableMetadata table = new TableMetadata("USER_TABLE", "User", "com.example")
			.addOneToOne(new OneToOneMetadata(
				"address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID"));

		assertTrue(table.isForeignKeyColumn("ADDRESS_ID"));
		assertFalse(table.isForeignKeyColumn("NAME"));
	}

	@Test
	public void testIsForeignKeyColumnWithInverseOneToOne() {
		TableMetadata table = new TableMetadata("ADDRESS", "Address", "com.example")
			.addOneToOne(new OneToOneMetadata(
				"user", "User", "com.example")
				.mappedBy("address"));

		assertFalse(table.isForeignKeyColumn("USER_ID"),
			"Inverse side (mappedBy) should not be treated as FK column");
	}
}
