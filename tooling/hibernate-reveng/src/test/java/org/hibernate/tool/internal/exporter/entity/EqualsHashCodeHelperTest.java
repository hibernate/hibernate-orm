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
package org.hibernate.tool.internal.exporter.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.tool.internal.builder.db.DynamicEntityBuilder;
import org.hibernate.tool.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.internal.descriptor.TableDescriptor;
import org.junit.jupiter.api.Test;

class EqualsHashCodeHelperTest {

	private record TestContext(
			EqualsHashCodeHelper helper,
			TemplateHelper templateHelper,
			ImportContext importContext,
			ModelsContext modelsContext,
			ClassDetails classDetails) {}

	private TestContext create(TableDescriptor table) {
		return create(table, Collections.emptyMap(), Collections.emptyMap());
	}

	private TestContext create(TableDescriptor table,
			Map<String, List<String>> classMetaAttributes,
			Map<String, Map<String, List<String>>> fieldMetaAttributes) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails classDetails = builder.createEntityFromTable(table);
		String pkg = table.getEntityPackage() != null ? table.getEntityPackage() : "";
		ImportContext importContext = new ImportContextImpl(pkg);
		TemplateHelper templateHelper = new TemplateHelper(classDetails,
				builder.getModelsContext(), importContext, true,
				classMetaAttributes, fieldMetaAttributes);
		EqualsHashCodeHelper helper = new EqualsHashCodeHelper(templateHelper, importContext);
		return new TestContext(helper, templateHelper, importContext,
				builder.getModelsContext(), classDetails);
	}

	private FieldDetails getField(ClassDetails classDetails, String name) {
		for (FieldDetails field : classDetails.getFields()) {
			if (field.getName().equals(name)) {
				return field;
			}
		}
		throw new IllegalArgumentException("Field not found: " + name);
	}

	// --- hasCompositeId ---

	@Test
	void testHasCompositeIdFalseForSimplePk() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		assertFalse(create(table).helper().hasCompositeId());
	}

	// --- needsEqualsHashCode ---

	@Test
	void testNeedsEqualsHashCodeForEntityWithPk() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		assertTrue(create(table).helper().needsEqualsHashCode());
	}

	@Test
	void testNeedsEqualsHashCodeForEmbeddable() {
		TableDescriptor table = new TableDescriptor("ADDRESS", "Address", "com.example");
		table.addColumn(new ColumnDescriptor("STREET", "street", String.class));
		TestContext ctx = create(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		dc.addAnnotationUsage(JpaAnnotations.EMBEDDABLE.createUsage(ctx.modelsContext()));
		assertTrue(ctx.helper().needsEqualsHashCode());
	}

	@Test
	void testNeedsEqualsHashCodeWithExplicitEqualsColumns() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true));
		table.addColumn(new ColumnDescriptor("EMAIL", "email", String.class));
		Map<String, Map<String, List<String>>> fieldMeta = Map.of(
				"email", Map.of("use-in-equals", List.of("true")));
		assertTrue(create(table, Collections.emptyMap(), fieldMeta).helper().needsEqualsHashCode());
	}

	// --- hasNaturalId / getNaturalIdFields ---

	@Test
	void testHasNaturalIdFalse() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		assertFalse(create(table).helper().hasNaturalId());
	}

	@Test
	void testHasNaturalIdTrue() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("EMAIL", "email", String.class));
		TestContext ctx = create(table);
		FieldDetails emailField = getField(ctx.classDetails(), "email");
		((MutableAnnotationTarget) emailField).addAnnotationUsage(
				org.hibernate.boot.models.HibernateAnnotations.NATURAL_ID.createUsage(
						ctx.modelsContext()));
		assertTrue(ctx.helper().hasNaturalId());
	}

	@Test
	void testGetNaturalIdFields() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("EMAIL", "email", String.class));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		TestContext ctx = create(table);
		FieldDetails emailField = getField(ctx.classDetails(), "email");
		((MutableAnnotationTarget) emailField).addAnnotationUsage(
				org.hibernate.boot.models.HibernateAnnotations.NATURAL_ID.createUsage(
						ctx.modelsContext()));
		List<FieldDetails> naturalIdFields = ctx.helper().getNaturalIdFields();
		assertEquals(1, naturalIdFields.size());
		assertEquals("email", naturalIdFields.get(0).getName());
	}

	// --- getEqualsFields ---

	@Test
	void testGetEqualsFieldsEmpty() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		assertTrue(create(table).helper().getEqualsFields().isEmpty());
	}

	@Test
	void testGetEqualsFieldsFromMetaAttribute() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("EMAIL", "email", String.class));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		Map<String, Map<String, List<String>>> fieldMeta = Map.of(
				"email", Map.of("use-in-equals", List.of("true")));
		TestContext ctx = create(table, Collections.emptyMap(), fieldMeta);
		List<FieldDetails> equalsFields = ctx.helper().getEqualsFields();
		assertEquals(1, equalsFields.size());
		assertEquals("email", equalsFields.get(0).getName());
	}

	// --- getIdentifierFields ---

	@Test
	void testGetIdentifierFieldsReturnsPk() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		TestContext ctx = create(table);
		List<FieldDetails> idFields = ctx.helper().getIdentifierFields();
		assertEquals(1, idFields.size());
		assertEquals("id", idFields.get(0).getName());
	}

	@Test
	void testGetIdentifierFieldsPrefersNaturalId() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("EMAIL", "email", String.class));
		TestContext ctx = create(table);
		FieldDetails emailField = getField(ctx.classDetails(), "email");
		((MutableAnnotationTarget) emailField).addAnnotationUsage(
				org.hibernate.boot.models.HibernateAnnotations.NATURAL_ID.createUsage(
						ctx.modelsContext()));
		List<FieldDetails> idFields = ctx.helper().getIdentifierFields();
		assertEquals(1, idFields.size());
		assertEquals("email", idFields.get(0).getName());
	}

	@Test
	void testGetIdentifierFieldsReturnsAllBasicForEmbeddable() {
		TableDescriptor table = new TableDescriptor("PK", "EmployeePk", "com.example");
		table.addColumn(new ColumnDescriptor("DEPT_ID", "deptId", Long.class));
		table.addColumn(new ColumnDescriptor("EMP_NO", "empNo", Long.class));
		TestContext ctx = create(table);
		DynamicClassDetails dc = (DynamicClassDetails) ctx.classDetails();
		dc.addAnnotationUsage(JpaAnnotations.EMBEDDABLE.createUsage(ctx.modelsContext()));
		List<FieldDetails> idFields = ctx.helper().getIdentifierFields();
		assertEquals(2, idFields.size());
	}

	// --- generateEqualsExpression ---

	@Test
	void testEqualsExpressionForObject() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "name");
		String result = ctx.helper().generateEqualsExpression(field);
		assertTrue(result.contains("this.getName()"), result);
		assertTrue(result.contains("castOther.getName()"), result);
		assertTrue(result.contains(".equals("), result);
	}

	@Test
	void testEqualsExpressionForPrimitive() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("AGE", "age", int.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "age");
		String result = ctx.helper().generateEqualsExpression(field);
		assertEquals("this.getAge() == castOther.getAge()", result);
	}

	// --- generateHashCodeExpression ---

	@Test
	void testHashCodeExpressionForInt() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("AGE", "age", int.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "age");
		assertEquals("this.getAge()", ctx.helper().generateHashCodeExpression(field));
	}

	@Test
	void testHashCodeExpressionForLong() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", long.class).primaryKey(true));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "id");
		assertEquals("(int) this.getId()", ctx.helper().generateHashCodeExpression(field));
	}

	@Test
	void testHashCodeExpressionForBoolean() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("ACTIVE", "active", boolean.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "active");
		assertEquals("(this.isActive() ? 1 : 0)", ctx.helper().generateHashCodeExpression(field));
	}

	@Test
	void testHashCodeExpressionForObject() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "name");
		String result = ctx.helper().generateHashCodeExpression(field);
		assertTrue(result.contains("this.getName() == null ? 0"), result);
		assertTrue(result.contains(".hashCode()"), result);
	}

	@Test
	void testHashCodeExpressionForFloat() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("SCORE", "score", float.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "score");
		assertEquals("Float.floatToIntBits(this.getScore())",
				ctx.helper().generateHashCodeExpression(field));
	}

	@Test
	void testHashCodeExpressionForDouble() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("SALARY", "salary", double.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "salary");
		assertEquals("(int) Double.doubleToLongBits(this.getSalary())",
				ctx.helper().generateHashCodeExpression(field));
	}
}
