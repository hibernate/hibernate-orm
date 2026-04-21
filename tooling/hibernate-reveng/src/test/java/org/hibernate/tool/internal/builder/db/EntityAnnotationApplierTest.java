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
package org.hibernate.tool.internal.builder.db;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.tool.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.internal.descriptor.IndexDescriptor;
import org.hibernate.tool.internal.descriptor.InheritanceDescriptor;
import org.hibernate.tool.internal.descriptor.TableDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

class EntityAnnotationApplierTest {

	private ModelsContext modelsContext;

	@BeforeEach
	void setUp() {
		modelsContext = new BasicModelsContextImpl(
				SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
	}

	private DynamicClassDetails createClass(String name) {
		return new DynamicClassDetails(
				name, name, Object.class, false, null, null, modelsContext);
	}

	@Test
	void testAddEntityAnnotation() {
		DynamicClassDetails entityClass = createClass("com.example.MyEntity");
		EntityAnnotationApplier.addEntityAnnotation(entityClass, modelsContext);
		Entity entity = entityClass.getAnnotationUsage(
				Entity.class, modelsContext);
		assertNotNull(entity);
	}

	@Test
	void testAddTableAnnotation() {
		DynamicClassDetails entityClass = createClass("com.example.MyEntity");
		TableDescriptor table = new TableDescriptor(
				"MY_TABLE", "MyEntity", "com.example");
		EntityAnnotationApplier.addTableAnnotation(
				entityClass, table, modelsContext);
		Table tableAnn = entityClass.getAnnotationUsage(
				Table.class, modelsContext);
		assertNotNull(tableAnn);
		assertEquals("MY_TABLE", tableAnn.name());
	}

	@Test
	void testAddTableAnnotationWithSchema() {
		DynamicClassDetails entityClass = createClass("com.example.MyEntity");
		TableDescriptor table = new TableDescriptor(
				"MY_TABLE", "MyEntity", "com.example");
		table.setSchema("public");
		EntityAnnotationApplier.addTableAnnotation(
				entityClass, table, modelsContext);
		Table tableAnn = entityClass.getAnnotationUsage(
				Table.class, modelsContext);
		assertEquals("public", tableAnn.schema());
	}

	@Test
	void testAddTableAnnotationWithCatalog() {
		DynamicClassDetails entityClass = createClass("com.example.MyEntity");
		TableDescriptor table = new TableDescriptor(
				"MY_TABLE", "MyEntity", "com.example");
		table.setCatalog("mydb");
		EntityAnnotationApplier.addTableAnnotation(
				entityClass, table, modelsContext);
		Table tableAnn = entityClass.getAnnotationUsage(
				Table.class, modelsContext);
		assertEquals("mydb", tableAnn.catalog());
	}

	@Test
	void testAddTableAnnotationWithUniqueConstraints() {
		DynamicClassDetails entityClass = createClass("com.example.MyEntity");
		TableDescriptor table = new TableDescriptor(
				"MY_TABLE", "MyEntity", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class)
				.primaryKey(true));
		table.addColumn(new ColumnDescriptor("EMAIL", "email", String.class));
		table.addIndex(new IndexDescriptor("UK_EMAIL", true).addColumn("EMAIL"));
		EntityAnnotationApplier.addTableAnnotation(
				entityClass, table, modelsContext);
		Table tableAnn = entityClass.getAnnotationUsage(
				Table.class, modelsContext);
		UniqueConstraint[] constraints = tableAnn.uniqueConstraints();
		assertEquals(1, constraints.length);
		assertEquals("UK_EMAIL", constraints[0].name());
		assertArrayEquals(new String[]{"EMAIL"}, constraints[0].columnNames());
	}

	@Test
	void testAddTableAnnotationExcludesPkOnlyUniqueIndex() {
		DynamicClassDetails entityClass = createClass("com.example.MyEntity");
		TableDescriptor table = new TableDescriptor(
				"MY_TABLE", "MyEntity", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class)
				.primaryKey(true));
		table.addIndex(new IndexDescriptor("PK_IDX", true).addColumn("ID"));
		EntityAnnotationApplier.addTableAnnotation(
				entityClass, table, modelsContext);
		Table tableAnn = entityClass.getAnnotationUsage(
				Table.class, modelsContext);
		assertEquals(0, tableAnn.uniqueConstraints().length);
	}

	@Test
	void testAddInheritanceSingleTable() {
		DynamicClassDetails entityClass = createClass("com.example.Vehicle");
		TableDescriptor table = new TableDescriptor(
				"VEHICLE", "Vehicle", "com.example");
		table.inheritance(
				new InheritanceDescriptor(InheritanceType.SINGLE_TABLE)
						.discriminatorColumn("DTYPE")
						.discriminatorType(DiscriminatorType.STRING)
						.discriminatorColumnLength(50));
		table.discriminatorValue("VEHICLE");
		EntityAnnotationApplier.addInheritanceAnnotations(
				entityClass, table, modelsContext);
		Inheritance inh = entityClass.getAnnotationUsage(
				Inheritance.class, modelsContext);
		assertNotNull(inh);
		assertEquals(InheritanceType.SINGLE_TABLE, inh.strategy());
		DiscriminatorColumn dc = entityClass.getAnnotationUsage(
				DiscriminatorColumn.class, modelsContext);
		assertNotNull(dc);
		assertEquals("DTYPE", dc.name());
		assertEquals(DiscriminatorType.STRING, dc.discriminatorType());
		assertEquals(50, dc.length());
		DiscriminatorValue dv = entityClass.getAnnotationUsage(
				DiscriminatorValue.class, modelsContext);
		assertNotNull(dv);
		assertEquals("VEHICLE", dv.value());
	}

	@Test
	void testAddInheritanceJoined() {
		DynamicClassDetails entityClass = createClass("com.example.Payment");
		TableDescriptor table = new TableDescriptor(
				"PAYMENT", "Payment", "com.example");
		table.inheritance(new InheritanceDescriptor(InheritanceType.JOINED));
		EntityAnnotationApplier.addInheritanceAnnotations(
				entityClass, table, modelsContext);
		Inheritance inh = entityClass.getAnnotationUsage(
				Inheritance.class, modelsContext);
		assertNotNull(inh);
		assertEquals(InheritanceType.JOINED, inh.strategy());
		assertFalse(entityClass.hasAnnotationUsage(
				DiscriminatorColumn.class, modelsContext));
	}

	@Test
	void testAddDiscriminatorValueOnly() {
		DynamicClassDetails entityClass = createClass("com.example.Car");
		TableDescriptor table = new TableDescriptor(
				"VEHICLE", "Car", "com.example");
		table.discriminatorValue("CAR");
		EntityAnnotationApplier.addInheritanceAnnotations(
				entityClass, table, modelsContext);
		assertFalse(entityClass.hasAnnotationUsage(
				Inheritance.class, modelsContext));
		DiscriminatorValue dv = entityClass.getAnnotationUsage(
				DiscriminatorValue.class, modelsContext);
		assertNotNull(dv);
		assertEquals("CAR", dv.value());
	}

	@Test
	void testAddPrimaryKeyJoinColumn() {
		DynamicClassDetails entityClass = createClass("com.example.CreditCard");
		TableDescriptor table = new TableDescriptor(
				"CREDIT_CARD", "CreditCard", "com.example");
		table.primaryKeyJoinColumn("PAYMENT_ID");
		EntityAnnotationApplier.addInheritanceAnnotations(
				entityClass, table, modelsContext);
		PrimaryKeyJoinColumn pkjc = entityClass.getAnnotationUsage(
				PrimaryKeyJoinColumn.class, modelsContext);
		assertNotNull(pkjc);
		assertEquals("PAYMENT_ID", pkjc.name());
	}

	@Test
	void testNoInheritanceAnnotationsWhenNoneConfigured() {
		DynamicClassDetails entityClass = createClass("com.example.Simple");
		TableDescriptor table = new TableDescriptor(
				"SIMPLE", "Simple", "com.example");
		EntityAnnotationApplier.addInheritanceAnnotations(
				entityClass, table, modelsContext);
		assertFalse(entityClass.hasAnnotationUsage(
				Inheritance.class, modelsContext));
		assertFalse(entityClass.hasAnnotationUsage(
				DiscriminatorColumn.class, modelsContext));
		assertFalse(entityClass.hasAnnotationUsage(
				DiscriminatorValue.class, modelsContext));
		assertFalse(entityClass.hasAnnotationUsage(
				PrimaryKeyJoinColumn.class, modelsContext));
	}

	@Test
	void testCollectPrimaryKeyColumnNames() {
		TableDescriptor table = new TableDescriptor(
				"MY_TABLE", "MyEntity", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class)
				.primaryKey(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		table.addColumn(new ColumnDescriptor("VERSION", "version", Long.class)
				.primaryKey(true));
		Set<String> pkCols =
				EntityAnnotationApplier.collectPrimaryKeyColumnNames(table);
		assertEquals(Set.of("ID", "VERSION"), pkCols);
	}

	@Test
	void testCollectPrimaryKeyColumnNamesEmpty() {
		TableDescriptor table = new TableDescriptor(
				"MY_TABLE", "MyEntity", "com.example");
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		Set<String> pkCols =
				EntityAnnotationApplier.collectPrimaryKeyColumnNames(table);
		assertTrue(pkCols.isEmpty());
	}
}
