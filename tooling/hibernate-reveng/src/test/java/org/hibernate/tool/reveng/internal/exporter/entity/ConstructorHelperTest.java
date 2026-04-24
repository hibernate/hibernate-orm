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
package org.hibernate.tool.reveng.internal.exporter.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.tool.reveng.internal.builder.db.DynamicEntityBuilder;
import org.hibernate.tool.reveng.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.ForeignKeyDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.TableDescriptor;
import org.hibernate.tool.reveng.internal.exporter.entity.TemplateHelper.FullConstructorProperty;
import org.junit.jupiter.api.Test;

class ConstructorHelperTest {

	private record TestContext(
			ConstructorHelper helper,
			TemplateHelper templateHelper,
			ModelsContext modelsContext,
			ClassDetails classDetails) {}

	private TestContext create(TableDescriptor table) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails classDetails = builder.createEntityFromTable(table);
		String pkg = table.getEntityPackage() != null ? table.getEntityPackage() : "";
		ImportContext importContext = new ImportContext(pkg);
		TemplateHelper templateHelper = new TemplateHelper(classDetails,
				builder.getModelsContext(), importContext, true,
				Collections.emptyMap(), Collections.emptyMap());
		ConstructorHelper helper = new ConstructorHelper(templateHelper);
		return new TestContext(helper, templateHelper, builder.getModelsContext(), classDetails);
	}

	// --- Full constructor ---

	@Test
	void testFullConstructorWithBasicFields() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		table.addColumn(new ColumnDescriptor("AGE", "age", int.class));
		TestContext ctx = create(table);
		assertTrue(ctx.helper().needsFullConstructor());
		List<FullConstructorProperty> props = ctx.helper().getFullConstructorProperties();
		assertEquals(3, props.size());
		assertEquals("id", props.get(0).fieldName());
		assertEquals("name", props.get(1).fieldName());
		assertEquals("age", props.get(2).fieldName());
	}

	@Test
	void testFullConstructorSkipsGeneratedId() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		TestContext ctx = create(table);
		List<FullConstructorProperty> props = ctx.helper().getFullConstructorProperties();
		assertEquals(1, props.size());
		assertEquals("name", props.get(0).fieldName());
	}

	@Test
	void testFullConstructorIncludesAssignedId() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = create(table);
		List<FullConstructorProperty> props = ctx.helper().getFullConstructorProperties();
		assertEquals(1, props.size());
		assertEquals("id", props.get(0).fieldName());
	}

	@Test
	void testFullConstructorSkipsVersion() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("VERSION", "version", int.class).version(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		TestContext ctx = create(table);
		List<FullConstructorProperty> props = ctx.helper().getFullConstructorProperties();
		assertFalse(props.stream().anyMatch(p -> "version".equals(p.fieldName())));
	}

	@Test
	void testFullConstructorWithManyToOne() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		ForeignKeyDescriptor fk = new ForeignKeyDescriptor(
				"department", "Department", "com.example");
		fk.addJoinColumn("DEPT_ID", null);
		table.addForeignKey(fk);
		TestContext ctx = create(table);
		List<FullConstructorProperty> props = ctx.helper().getFullConstructorProperties();
		assertTrue(props.stream().anyMatch(p -> "department".equals(p.fieldName())));
	}

	@Test
	void testNeedsFullConstructorFalseWhenEmpty() {
		TableDescriptor table = new TableDescriptor("EMPTY", "Empty", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true));
		TestContext ctx = create(table);
		assertFalse(ctx.helper().needsFullConstructor());
	}

	// --- Minimal constructor ---

	@Test
	void testMinimalConstructorIncludesNonNullableColumns() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class).nullable(false));
		table.addColumn(new ColumnDescriptor("BIO", "bio", String.class));
		TestContext ctx = create(table);
		List<FullConstructorProperty> minProps = ctx.helper().getMinimalConstructorProperties();
		assertEquals(1, minProps.size());
		assertEquals("name", minProps.get(0).fieldName());
	}

	@Test
	void testMinimalConstructorIncludesAssignedId() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		TestContext ctx = create(table);
		List<FullConstructorProperty> minProps = ctx.helper().getMinimalConstructorProperties();
		assertTrue(minProps.stream().anyMatch(p -> "id".equals(p.fieldName())));
	}

	@Test
	void testMinimalConstructorExcludesGeneratedId() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class).nullable(false));
		TestContext ctx = create(table);
		List<FullConstructorProperty> minProps = ctx.helper().getMinimalConstructorProperties();
		assertFalse(minProps.stream().anyMatch(p -> "id".equals(p.fieldName())));
	}

	@Test
	void testMinimalConstructorIncludesNonOptionalManyToOne() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		ForeignKeyDescriptor fk = new ForeignKeyDescriptor(
				"department", "Department", "com.example");
		fk.addJoinColumn("DEPT_ID", null);
		fk.optional(false);
		table.addForeignKey(fk);
		TestContext ctx = create(table);
		List<FullConstructorProperty> minProps = ctx.helper().getMinimalConstructorProperties();
		assertTrue(minProps.stream().anyMatch(p -> "department".equals(p.fieldName())));
	}

	@Test
	void testMinimalConstructorExcludesOptionalManyToOne() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		ForeignKeyDescriptor fk = new ForeignKeyDescriptor(
				"department", "Department", "com.example");
		fk.addJoinColumn("DEPT_ID", null);
		fk.optional(true);
		table.addForeignKey(fk);
		TestContext ctx = create(table);
		List<FullConstructorProperty> minProps = ctx.helper().getMinimalConstructorProperties();
		assertFalse(minProps.stream().anyMatch(p -> "department".equals(p.fieldName())));
	}

	@Test
	void testNeedsMinimalConstructorWhenDifferentFromFull() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class).nullable(false));
		table.addColumn(new ColumnDescriptor("BIO", "bio", String.class));
		TestContext ctx = create(table);
		assertTrue(ctx.helper().needsMinimalConstructor());
	}

	@Test
	void testNeedsMinimalConstructorFalseWhenSameAsFull() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class).nullable(false));
		TestContext ctx = create(table);
		assertFalse(ctx.helper().needsMinimalConstructor());
	}

	// --- Parameter lists ---

	@Test
	void testFullConstructorParameterList() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		table.addColumn(new ColumnDescriptor("AGE", "age", int.class));
		TestContext ctx = create(table);
		String paramList = ctx.helper().getFullConstructorParameterList();
		assertEquals("String name, int age", paramList);
	}

	@Test
	void testFullConstructorParameterListWithAssignedId() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		TestContext ctx = create(table);
		String paramList = ctx.helper().getFullConstructorParameterList();
		assertEquals("Long id, String name", paramList);
	}

	@Test
	void testMinimalConstructorParameterList() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class).nullable(false));
		table.addColumn(new ColumnDescriptor("BIO", "bio", String.class));
		TestContext ctx = create(table);
		String paramList = ctx.helper().getMinimalConstructorParameterList();
		assertEquals("String name", paramList);
	}

	// --- Superclass constructor ---

	@Test
	void testSuperclassConstructorPropertiesEmptyForRootEntity() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = create(table);
		assertTrue(ctx.helper().getSuperclassFullConstructorProperties().isEmpty());
		assertTrue(ctx.helper().getSuperclassMinimalConstructorProperties().isEmpty());
	}

	@Test
	void testSuperclassArgumentListEmptyForRootEntity() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = create(table);
		assertEquals("", ctx.helper().getSuperclassFullConstructorArgumentList());
		assertEquals("", ctx.helper().getSuperclassMinimalConstructorArgumentList());
	}
}
