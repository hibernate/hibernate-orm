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

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.AttributeOverrideJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.AttributeOverridesJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinColumnsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.JoinTableJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.ManyToOneJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToManyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.OneToOneJpaAnnotation;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.tool.internal.builder.db.DynamicEntityBuilder;
import org.hibernate.tool.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.internal.descriptor.TableDescriptor;
import org.junit.jupiter.api.Test;

class RelationshipAnnotationGeneratorTest {

	private record TestContext(
			RelationshipAnnotationGenerator generator,
			TemplateHelper helper,
			ModelsContext modelsContext,
			ClassDetails classDetails) {}

	private TestContext create(TableDescriptor table) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails classDetails = builder.createEntityFromTable(table);
		String pkg = table.getEntityPackage() != null ? table.getEntityPackage() : "";
		ImportContext importContext = new ImportContextImpl(pkg);
		TemplateHelper helper = new TemplateHelper(classDetails, builder.getModelsContext(),
				importContext, true, Collections.emptyMap(), Collections.emptyMap());
		RelationshipAnnotationGenerator generator =
				new RelationshipAnnotationGenerator(importContext, helper);
		return new TestContext(generator, helper, builder.getModelsContext(), classDetails);
	}

	private FieldDetails getField(ClassDetails classDetails, String name) {
		for (FieldDetails field : classDetails.getFields()) {
			if (field.getName().equals(name)) {
				return field;
			}
		}
		throw new IllegalArgumentException("Field not found: " + name);
	}

	// --- @ManyToOne ---

	@Test
	void testManyToOneBasic() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("DEPT_ID", "department", Long.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "department");
		ManyToOneJpaAnnotation m2o = JpaAnnotations.MANY_TO_ONE.createUsage(ctx.modelsContext());
		((MutableAnnotationTarget) field).addAnnotationUsage(m2o);
		JoinColumnJpaAnnotation jc = JpaAnnotations.JOIN_COLUMN.createUsage(ctx.modelsContext());
		jc.name("DEPT_ID");
		((MutableAnnotationTarget) field).addAnnotationUsage(jc);
		String result = ctx.generator().generateManyToOneAnnotation(field);
		assertTrue(result.contains("@ManyToOne"), result);
		assertFalse(result.contains("fetch ="), result);
		assertTrue(result.contains("@JoinColumn(name = \"DEPT_ID\")"), result);
	}

	@Test
	void testManyToOneWithFetchAndOptional() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("DEPT_ID", "department", Long.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "department");
		ManyToOneJpaAnnotation m2o = JpaAnnotations.MANY_TO_ONE.createUsage(ctx.modelsContext());
		m2o.fetch(FetchType.LAZY);
		m2o.optional(false);
		((MutableAnnotationTarget) field).addAnnotationUsage(m2o);
		String result = ctx.generator().generateManyToOneAnnotation(field);
		assertTrue(result.contains("fetch = FetchType.LAZY"), result);
		assertTrue(result.contains("optional = false"), result);
	}

	@Test
	void testManyToOneMultipleJoinColumns() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("FK1", "department", Long.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "department");
		ManyToOneJpaAnnotation m2o = JpaAnnotations.MANY_TO_ONE.createUsage(ctx.modelsContext());
		((MutableAnnotationTarget) field).addAnnotationUsage(m2o);
		JoinColumnJpaAnnotation jc1 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx.modelsContext());
		jc1.name("FK1");
		jc1.referencedColumnName("PK1");
		JoinColumnJpaAnnotation jc2 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx.modelsContext());
		jc2.name("FK2");
		jc2.referencedColumnName("PK2");
		JoinColumnsJpaAnnotation jcs = JpaAnnotations.JOIN_COLUMNS.createUsage(ctx.modelsContext());
		jcs.value(new jakarta.persistence.JoinColumn[] { jc1, jc2 });
		((MutableAnnotationTarget) field).addAnnotationUsage(jcs);
		String result = ctx.generator().generateManyToOneAnnotation(field);
		assertTrue(result.contains("@JoinColumns({"), result);
		assertTrue(result.contains("@JoinColumn(name = \"FK1\", referencedColumnName = \"PK1\")"), result);
		assertTrue(result.contains("@JoinColumn(name = \"FK2\", referencedColumnName = \"PK2\")"), result);
	}

	@Test
	void testManyToOneJoinColumnInsertableUpdatable() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("DEPT_ID", "department", Long.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "department");
		ManyToOneJpaAnnotation m2o = JpaAnnotations.MANY_TO_ONE.createUsage(ctx.modelsContext());
		((MutableAnnotationTarget) field).addAnnotationUsage(m2o);
		JoinColumnJpaAnnotation jc = JpaAnnotations.JOIN_COLUMN.createUsage(ctx.modelsContext());
		jc.name("DEPT_ID");
		jc.insertable(false);
		jc.updatable(false);
		((MutableAnnotationTarget) field).addAnnotationUsage(jc);
		String result = ctx.generator().generateManyToOneAnnotation(field);
		assertTrue(result.contains("insertable = false"), result);
		assertTrue(result.contains("updatable = false"), result);
	}

	@Test
	void testManyToOneNoAnnotationReturnsEmpty() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "id");
		String result = ctx.generator().generateManyToOneAnnotation(field);
		assertEquals("", result);
	}

	// --- @OneToMany ---

	@Test
	void testOneToManyWithMappedBy() {
		TableDescriptor table = new TableDescriptor("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("DUMMY", "employees", Long.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "employees");
		OneToManyJpaAnnotation o2m = JpaAnnotations.ONE_TO_MANY.createUsage(ctx.modelsContext());
		o2m.mappedBy("department");
		((MutableAnnotationTarget) field).addAnnotationUsage(o2m);
		String result = ctx.generator().generateOneToManyAnnotation(field);
		assertTrue(result.contains("@OneToMany("), result);
		assertTrue(result.contains("mappedBy = \"department\""), result);
	}

	@Test
	void testOneToManyWithFetchCascadeOrphan() {
		TableDescriptor table = new TableDescriptor("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("DUMMY", "employees", Long.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "employees");
		OneToManyJpaAnnotation o2m = JpaAnnotations.ONE_TO_MANY.createUsage(ctx.modelsContext());
		o2m.mappedBy("department");
		o2m.fetch(FetchType.EAGER);
		o2m.cascade(new CascadeType[] { CascadeType.ALL });
		o2m.orphanRemoval(true);
		((MutableAnnotationTarget) field).addAnnotationUsage(o2m);
		String result = ctx.generator().generateOneToManyAnnotation(field);
		assertTrue(result.contains("fetch = FetchType.EAGER"), result);
		assertTrue(result.contains("cascade = CascadeType.ALL"), result);
		assertTrue(result.contains("orphanRemoval = true"), result);
	}

	@Test
	void testOneToManyMultipleCascade() {
		TableDescriptor table = new TableDescriptor("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("DUMMY", "employees", Long.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "employees");
		OneToManyJpaAnnotation o2m = JpaAnnotations.ONE_TO_MANY.createUsage(ctx.modelsContext());
		o2m.cascade(new CascadeType[] { CascadeType.PERSIST, CascadeType.MERGE });
		((MutableAnnotationTarget) field).addAnnotationUsage(o2m);
		String result = ctx.generator().generateOneToManyAnnotation(field);
		assertTrue(result.contains("cascade = { CascadeType.PERSIST, CascadeType.MERGE }"), result);
	}

	@Test
	void testOneToManyNoAnnotationReturnsEmpty() {
		TableDescriptor table = new TableDescriptor("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "id");
		String result = ctx.generator().generateOneToManyAnnotation(field);
		assertEquals("", result);
	}

	// --- @OneToOne ---

	@Test
	void testOneToOneBasic() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("BADGE_ID", "badge", Long.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "badge");
		OneToOneJpaAnnotation o2o = JpaAnnotations.ONE_TO_ONE.createUsage(ctx.modelsContext());
		((MutableAnnotationTarget) field).addAnnotationUsage(o2o);
		JoinColumnJpaAnnotation jc = JpaAnnotations.JOIN_COLUMN.createUsage(ctx.modelsContext());
		jc.name("BADGE_ID");
		((MutableAnnotationTarget) field).addAnnotationUsage(jc);
		String result = ctx.generator().generateOneToOneAnnotation(field);
		assertTrue(result.contains("@OneToOne"), result);
		assertTrue(result.contains("@JoinColumn(name = \"BADGE_ID\")"), result);
	}

	@Test
	void testOneToOneWithMappedByAndAttributes() {
		TableDescriptor table = new TableDescriptor("BADGE", "Badge", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("DUMMY", "employee", Long.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "employee");
		OneToOneJpaAnnotation o2o = JpaAnnotations.ONE_TO_ONE.createUsage(ctx.modelsContext());
		o2o.mappedBy("badge");
		o2o.fetch(FetchType.LAZY);
		o2o.optional(false);
		o2o.cascade(new CascadeType[] { CascadeType.ALL });
		o2o.orphanRemoval(true);
		((MutableAnnotationTarget) field).addAnnotationUsage(o2o);
		String result = ctx.generator().generateOneToOneAnnotation(field);
		assertTrue(result.contains("mappedBy = \"badge\""), result);
		assertTrue(result.contains("fetch = FetchType.LAZY"), result);
		assertTrue(result.contains("optional = false"), result);
		assertTrue(result.contains("cascade = CascadeType.ALL"), result);
		assertTrue(result.contains("orphanRemoval = true"), result);
	}

	@Test
	void testOneToOneWithMapsId() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("BADGE_ID", "badge", Long.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "badge");
		OneToOneJpaAnnotation o2o = JpaAnnotations.ONE_TO_ONE.createUsage(ctx.modelsContext());
		((MutableAnnotationTarget) field).addAnnotationUsage(o2o);
		((MutableAnnotationTarget) field).addAnnotationUsage(
				JpaAnnotations.MAPS_ID.createUsage(ctx.modelsContext()));
		String result = ctx.generator().generateOneToOneAnnotation(field);
		assertTrue(result.contains("@OneToOne"), result);
		assertTrue(result.contains("@MapsId"), result);
	}

	@Test
	void testOneToOneMultipleJoinColumns() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("FK1", "badge", Long.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "badge");
		OneToOneJpaAnnotation o2o = JpaAnnotations.ONE_TO_ONE.createUsage(ctx.modelsContext());
		((MutableAnnotationTarget) field).addAnnotationUsage(o2o);
		JoinColumnJpaAnnotation jc1 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx.modelsContext());
		jc1.name("FK1");
		JoinColumnJpaAnnotation jc2 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx.modelsContext());
		jc2.name("FK2");
		JoinColumnsJpaAnnotation jcs = JpaAnnotations.JOIN_COLUMNS.createUsage(ctx.modelsContext());
		jcs.value(new jakarta.persistence.JoinColumn[] { jc1, jc2 });
		((MutableAnnotationTarget) field).addAnnotationUsage(jcs);
		String result = ctx.generator().generateOneToOneAnnotation(field);
		assertTrue(result.contains("@JoinColumns({"), result);
		assertTrue(result.contains("@JoinColumn(name = \"FK1\")"), result);
		assertTrue(result.contains("@JoinColumn(name = \"FK2\")"), result);
	}

	@Test
	void testOneToOneNoAnnotationReturnsEmpty() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "id");
		String result = ctx.generator().generateOneToOneAnnotation(field);
		assertEquals("", result);
	}

	// --- @ManyToMany ---

	@Test
	void testManyToManyWithMappedBy() {
		TableDescriptor table = new TableDescriptor("STUDENT", "Student", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("DUMMY", "courses", Long.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "courses");
		ManyToManyJpaAnnotation m2m = JpaAnnotations.MANY_TO_MANY.createUsage(ctx.modelsContext());
		m2m.mappedBy("students");
		((MutableAnnotationTarget) field).addAnnotationUsage(m2m);
		String result = ctx.generator().generateManyToManyAnnotation(field);
		assertTrue(result.contains("@ManyToMany("), result);
		assertTrue(result.contains("mappedBy = \"students\""), result);
	}

	@Test
	void testManyToManyWithJoinTable() {
		TableDescriptor table = new TableDescriptor("COURSE", "Course", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("DUMMY", "students", Long.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "students");
		ManyToManyJpaAnnotation m2m = JpaAnnotations.MANY_TO_MANY.createUsage(ctx.modelsContext());
		((MutableAnnotationTarget) field).addAnnotationUsage(m2m);
		JoinTableJpaAnnotation jt = JpaAnnotations.JOIN_TABLE.createUsage(ctx.modelsContext());
		jt.name("COURSE_STUDENT");
		JoinColumnJpaAnnotation jc1 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx.modelsContext());
		jc1.name("COURSE_ID");
		jt.joinColumns(new jakarta.persistence.JoinColumn[] { jc1 });
		JoinColumnJpaAnnotation jc2 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx.modelsContext());
		jc2.name("STUDENT_ID");
		jt.inverseJoinColumns(new jakarta.persistence.JoinColumn[] { jc2 });
		((MutableAnnotationTarget) field).addAnnotationUsage(jt);
		String result = ctx.generator().generateManyToManyAnnotation(field);
		assertTrue(result.contains("@ManyToMany"), result);
		assertTrue(result.contains("@JoinTable(name = \"COURSE_STUDENT\""), result);
		assertTrue(result.contains("joinColumns = @JoinColumn(name = \"COURSE_ID\")"), result);
		assertTrue(result.contains("inverseJoinColumns = @JoinColumn(name = \"STUDENT_ID\")"), result);
	}

	@Test
	void testManyToManyWithFetchAndCascade() {
		TableDescriptor table = new TableDescriptor("STUDENT", "Student", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("DUMMY", "courses", Long.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "courses");
		ManyToManyJpaAnnotation m2m = JpaAnnotations.MANY_TO_MANY.createUsage(ctx.modelsContext());
		m2m.fetch(FetchType.EAGER);
		m2m.cascade(new CascadeType[] { CascadeType.PERSIST, CascadeType.MERGE });
		((MutableAnnotationTarget) field).addAnnotationUsage(m2m);
		String result = ctx.generator().generateManyToManyAnnotation(field);
		assertTrue(result.contains("fetch = FetchType.EAGER"), result);
		assertTrue(result.contains("cascade = { CascadeType.PERSIST, CascadeType.MERGE }"), result);
	}

	@Test
	void testManyToManyJoinTableMultipleColumns() {
		TableDescriptor table = new TableDescriptor("COURSE", "Course", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("DUMMY", "students", Long.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "students");
		ManyToManyJpaAnnotation m2m = JpaAnnotations.MANY_TO_MANY.createUsage(ctx.modelsContext());
		((MutableAnnotationTarget) field).addAnnotationUsage(m2m);
		JoinTableJpaAnnotation jt = JpaAnnotations.JOIN_TABLE.createUsage(ctx.modelsContext());
		jt.name("ENROLLMENT");
		JoinColumnJpaAnnotation jc1 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx.modelsContext());
		jc1.name("COURSE_ID");
		JoinColumnJpaAnnotation jc2 = JpaAnnotations.JOIN_COLUMN.createUsage(ctx.modelsContext());
		jc2.name("SECTION_ID");
		jt.joinColumns(new jakarta.persistence.JoinColumn[] { jc1, jc2 });
		JoinColumnJpaAnnotation ijc = JpaAnnotations.JOIN_COLUMN.createUsage(ctx.modelsContext());
		ijc.name("STUDENT_ID");
		jt.inverseJoinColumns(new jakarta.persistence.JoinColumn[] { ijc });
		((MutableAnnotationTarget) field).addAnnotationUsage(jt);
		String result = ctx.generator().generateManyToManyAnnotation(field);
		assertTrue(result.contains("joinColumns = {"), result);
		assertTrue(result.contains("@JoinColumn(name = \"COURSE_ID\")"), result);
		assertTrue(result.contains("@JoinColumn(name = \"SECTION_ID\")"), result);
		assertTrue(result.contains("inverseJoinColumns = @JoinColumn(name = \"STUDENT_ID\")"), result);
	}

	@Test
	void testManyToManyNoAnnotationReturnsEmpty() {
		TableDescriptor table = new TableDescriptor("STUDENT", "Student", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "id");
		String result = ctx.generator().generateManyToManyAnnotation(field);
		assertEquals("", result);
	}

	// --- @EmbeddedId ---

	@Test
	void testEmbeddedIdBasic() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "id");
		((MutableAnnotationTarget) field).addAnnotationUsage(
				JpaAnnotations.EMBEDDED_ID.createUsage(ctx.modelsContext()));
		String result = ctx.generator().generateEmbeddedIdAnnotation(field);
		assertTrue(result.contains("@EmbeddedId"), result);
		assertFalse(result.contains("@AttributeOverrides"), result);
	}

	@Test
	void testEmbeddedIdWithAttributeOverrides() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "compositeKey", Long.class).primaryKey(true));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "compositeKey");
		((MutableAnnotationTarget) field).addAnnotationUsage(
				JpaAnnotations.EMBEDDED_ID.createUsage(ctx.modelsContext()));
		AttributeOverrideJpaAnnotation ao1 =
				JpaAnnotations.ATTRIBUTE_OVERRIDE.createUsage(ctx.modelsContext());
		ao1.name("firstName");
		ColumnJpaAnnotation col1 = JpaAnnotations.COLUMN.createUsage(ctx.modelsContext());
		col1.name("FIRST_NAME");
		ao1.column(col1);
		AttributeOverrideJpaAnnotation ao2 =
				JpaAnnotations.ATTRIBUTE_OVERRIDE.createUsage(ctx.modelsContext());
		ao2.name("lastName");
		ColumnJpaAnnotation col2 = JpaAnnotations.COLUMN.createUsage(ctx.modelsContext());
		col2.name("LAST_NAME");
		ao2.column(col2);
		AttributeOverridesJpaAnnotation overrides =
				JpaAnnotations.ATTRIBUTE_OVERRIDES.createUsage(ctx.modelsContext());
		overrides.value(new jakarta.persistence.AttributeOverride[] { ao1, ao2 });
		((MutableAnnotationTarget) field).addAnnotationUsage(overrides);
		String result = ctx.generator().generateEmbeddedIdAnnotation(field);
		assertTrue(result.contains("@EmbeddedId"), result);
		assertTrue(result.contains("@AttributeOverrides({"), result);
		assertTrue(result.contains("@AttributeOverride(name = \"firstName\", column = @Column(name = \"FIRST_NAME\"))"), result);
		assertTrue(result.contains("@AttributeOverride(name = \"lastName\", column = @Column(name = \"LAST_NAME\"))"), result);
	}

	@Test
	void testEmbeddedIdNoAnnotationReturnsEmpty() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "id");
		String result = ctx.generator().generateEmbeddedIdAnnotation(field);
		assertEquals("", result);
	}

	// --- @Embedded ---

	@Test
	void testEmbeddedBasic() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("ADDR", "address", String.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "address");
		((MutableAnnotationTarget) field).addAnnotationUsage(
				JpaAnnotations.EMBEDDED.createUsage(ctx.modelsContext()));
		String result = ctx.generator().generateEmbeddedAnnotation(field);
		assertTrue(result.contains("@Embedded"), result);
		assertFalse(result.contains("@AttributeOverrides"), result);
	}

	@Test
	void testEmbeddedWithAttributeOverrides() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("ADDR", "address", String.class));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "address");
		((MutableAnnotationTarget) field).addAnnotationUsage(
				JpaAnnotations.EMBEDDED.createUsage(ctx.modelsContext()));
		AttributeOverrideJpaAnnotation ao =
				JpaAnnotations.ATTRIBUTE_OVERRIDE.createUsage(ctx.modelsContext());
		ao.name("street");
		ColumnJpaAnnotation col = JpaAnnotations.COLUMN.createUsage(ctx.modelsContext());
		col.name("STREET_NAME");
		ao.column(col);
		AttributeOverridesJpaAnnotation overrides =
				JpaAnnotations.ATTRIBUTE_OVERRIDES.createUsage(ctx.modelsContext());
		overrides.value(new jakarta.persistence.AttributeOverride[] { ao });
		((MutableAnnotationTarget) field).addAnnotationUsage(overrides);
		String result = ctx.generator().generateEmbeddedAnnotation(field);
		assertTrue(result.contains("@Embedded"), result);
		assertTrue(result.contains("@AttributeOverride(name = \"street\", column = @Column(name = \"STREET_NAME\"))"), result);
	}

	@Test
	void testEmbeddedNoAnnotationReturnsEmpty() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		TestContext ctx = create(table);
		FieldDetails field = getField(ctx.classDetails(), "id");
		String result = ctx.generator().generateEmbeddedAnnotation(field);
		assertEquals("", result);
	}
}
