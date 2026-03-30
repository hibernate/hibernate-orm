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
package org.hibernate.tool.internal.reveng.models.exporter.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.TemporalType;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.tool.internal.export.java.ImportContextImpl;
import org.hibernate.tool.internal.reveng.models.builder.DynamicEntityBuilder;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.CompositeIdMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.EmbeddedFieldMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ForeignKeyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.InheritanceMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ManyToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToOneMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TemplateHelper}.
 *
 * @author Koen Aers
 */
public class TemplateHelperTest {

	private TemplateHelper create(TableMetadata table) {
		return create(table, true);
	}

	private TemplateHelper create(TableMetadata table, boolean annotated) {
		return create(table, annotated, Collections.emptyMap(), Collections.emptyMap());
	}

	private TemplateHelper create(TableMetadata table, boolean annotated,
								  Map<String, List<String>> classMetaAttributes,
								  Map<String, Map<String, List<String>>> fieldMetaAttributes) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails classDetails = builder.createEntityFromTable(table);
		String pkg = table.getEntityPackage() != null ? table.getEntityPackage() : "";
		return new TemplateHelper(classDetails, builder.getModelsContext(),
				new ImportContextImpl(pkg), annotated,
				classMetaAttributes, fieldMetaAttributes);
	}

	private Map<String, Map<String, List<String>>> fieldMeta(String fieldName, String key, String value) {
		Map<String, Map<String, List<String>>> result = new HashMap<>();
		result.put(fieldName, Map.of(key, List.of(value)));
		return result;
	}

	// --- Package / class ---

	@Test
	public void testGetPackageDeclaration() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("package com.example;", create(table).getPackageDeclaration());
	}

	@Test
	public void testGetPackageDeclarationEmpty() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "");
		assertEquals("", create(table).getPackageDeclaration());
	}

	@Test
	public void testGetDeclarationName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("Employee", create(table).getDeclarationName());
	}

	@Test
	public void testGetExtendsDeclarationNoParent() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("", create(table).getExtendsDeclaration());
	}

	@Test
	public void testGetExtendsDeclarationWithParent() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.parent("Vehicle", "com.example");
		assertEquals("extends Vehicle ", create(table).getExtendsDeclaration());
	}

	@Test
	public void testGetImplementsDeclaration() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("implements Serializable", create(table).getImplementsDeclaration());
	}

	// --- Field categorization ---

	@Test
	public void testGetBasicFields() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		List<FieldDetails> fields = helper.getBasicFields();
		assertEquals(2, fields.size());
		assertEquals("id", fields.get(0).getName());
		assertEquals("name", fields.get(1).getName());
	}

	@Test
	public void testGetManyToOneFields() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		TemplateHelper helper = create(table);
		List<FieldDetails> m2oFields = helper.getManyToOneFields();
		assertEquals(1, m2oFields.size());
		assertEquals("department", m2oFields.get(0).getName());
	}

	@Test
	public void testGetOneToManyFields() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TemplateHelper helper = create(table);
		List<FieldDetails> o2mFields = helper.getOneToManyFields();
		assertEquals(1, o2mFields.size());
		assertEquals("employees", o2mFields.get(0).getName());
	}

	@Test
	public void testGetCompositeIdField() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "OrderLineId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID"));
		TemplateHelper helper = create(table);
		FieldDetails cid = helper.getCompositeIdField();
		assertNotNull(cid);
		assertEquals("id", cid.getName());
	}

	// --- Type name resolution ---

	@Test
	public void testGetJavaTypeName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("name")).findFirst().orElseThrow();
		assertEquals("String", helper.getJavaTypeName(field));
	}

	@Test
	public void testGetJavaTypeNameBigDecimal() {
		TableMetadata table = new TableMetadata("PRODUCT", "Product", "com.example");
		table.addColumn(new ColumnMetadata("PRICE", "price", BigDecimal.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("BigDecimal", helper.getJavaTypeName(field));
		assertTrue(helper.generateImports().contains("import java.math.BigDecimal;"));
	}

	@Test
	public void testGetCollectionTypeNameOneToMany() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToManyFields().get(0);
		assertEquals("Set<Employee>", helper.getCollectionTypeName(field));
		assertTrue(helper.generateImports().contains("import java.util.Set;"));
	}

	// --- Getter/setter names ---

	@Test
	public void testGetterName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("getName", create(table).getGetterName("name"));
	}

	@Test
	public void testSetterName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("setName", create(table).getSetterName("name"));
	}

	@Test
	public void testGetterNameSingleChar() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("getX", create(table).getGetterName("x"));
	}

	// --- Annotation generation (annotated=true) ---

	@Test
	public void testGenerateClassAnnotations() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		String result = create(table).generateClassAnnotations();
		assertTrue(result.contains("@Entity"), result);
		assertTrue(result.contains("@Table(name = \"EMPLOYEE\")"), result);
	}

	@Test
	public void testGenerateClassAnnotationsWithSchemaAndCatalog() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.setSchema("HR");
		table.setCatalog("MYDB");
		String result = create(table).generateClassAnnotations();
		assertTrue(result.contains("schema = \"HR\""), result);
		assertTrue(result.contains("catalog = \"MYDB\""), result);
	}

	@Test
	public void testGenerateClassAnnotationsWithInheritance() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
				.discriminatorColumn("DTYPE")
				.discriminatorType(DiscriminatorType.INTEGER)
				.discriminatorColumnLength(10));
		String result = create(table).generateClassAnnotations();
		assertTrue(result.contains("@Inheritance(strategy = InheritanceType.SINGLE_TABLE)"), result);
		assertTrue(result.contains("@DiscriminatorColumn(name = \"DTYPE\""), result);
		assertTrue(result.contains("discriminatorType = DiscriminatorType.INTEGER"), result);
		assertTrue(result.contains("length = 10"), result);
	}

	@Test
	public void testGenerateClassAnnotationsWithDiscriminatorValue() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.discriminatorValue("CAR");
		String result = create(table).generateClassAnnotations();
		assertTrue(result.contains("@DiscriminatorValue(\"CAR\")"), result);
	}

	@Test
	public void testGenerateClassAnnotationsWithPrimaryKeyJoinColumn() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.primaryKeyJoinColumn("VEHICLE_ID");
		String result = create(table).generateClassAnnotations();
		assertTrue(result.contains("@PrimaryKeyJoinColumn(name = \"VEHICLE_ID\")"), result);
	}

	@Test
	public void testGenerateIdAnnotations() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("id")).findFirst().orElseThrow();
		String result = helper.generateIdAnnotations(field);
		assertTrue(result.contains("@Id"), result);
		assertFalse(result.contains("@GeneratedValue"), result);
	}

	@Test
	public void testGenerateIdAnnotationsWithGeneratedValue() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.generationType(GenerationType.IDENTITY));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("id")).findFirst().orElseThrow();
		String result = helper.generateIdAnnotations(field);
		assertTrue(result.contains("@Id"), result);
		assertTrue(result.contains("@GeneratedValue(strategy = GenerationType.IDENTITY)"), result);
	}

	@Test
	public void testGenerateVersionAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("@Version", create(table).generateVersionAnnotation());
	}

	@Test
	public void testGenerateColumnAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("name")).findFirst().orElseThrow();
		String result = helper.generateColumnAnnotation(field);
		assertEquals("@Column(name = \"NAME\")", result);
	}

	@Test
	public void testGenerateColumnAnnotationWithAttributes() {
		TableMetadata table = new TableMetadata("PRODUCT", "Product", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class)
				.nullable(false).unique(true).length(100));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		String result = helper.generateColumnAnnotation(field);
		assertTrue(result.contains("nullable = false"), result);
		assertTrue(result.contains("unique = true"), result);
		assertTrue(result.contains("length = 100"), result);
	}

	@Test
	public void testGenerateColumnAnnotationWithPrecisionScale() {
		TableMetadata table = new TableMetadata("PRODUCT", "Product", "com.example");
		table.addColumn(new ColumnMetadata("PRICE", "price", BigDecimal.class)
				.precision(10).scale(2));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		String result = helper.generateColumnAnnotation(field);
		assertTrue(result.contains("precision = 10"), result);
		assertTrue(result.contains("scale = 2"), result);
	}

	@Test
	public void testGenerateBasicAnnotationNoAttributes() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("", helper.generateBasicAnnotation(field));
	}

	@Test
	public void testGenerateTemporalAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("HIRE_DATE", "hireDate", Date.class)
				.temporal(TemporalType.TIMESTAMP));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("@Temporal(TemporalType.TIMESTAMP)", helper.generateTemporalAnnotation(field));
	}

	@Test
	public void testGenerateTemporalAnnotationNone() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("", helper.generateTemporalAnnotation(field));
	}

	@Test
	public void testGenerateLobAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("@Lob", create(table).generateLobAnnotation());
	}

	@Test
	public void testGenerateManyToOneAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getManyToOneFields().get(0);
		String result = helper.generateManyToOneAnnotation(field);
		assertTrue(result.contains("@ManyToOne"), result);
		assertTrue(result.contains("@JoinColumn(name = \"DEPT_ID\")"), result);
	}

	@Test
	public void testGenerateManyToOneAnnotationWithFetchAndOptional() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example")
				.fetchType(FetchType.LAZY)
				.optional(false));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getManyToOneFields().get(0);
		String result = helper.generateManyToOneAnnotation(field);
		assertTrue(result.contains("fetch = FetchType.LAZY"), result);
		assertTrue(result.contains("optional = false"), result);
	}

	@Test
	public void testGenerateManyToOneAnnotationWithReferencedColumn() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("DEPT_CODE", "deptCode", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_CODE", "Department", "com.example")
				.referencedColumnName("CODE"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getManyToOneFields().get(0);
		String result = helper.generateManyToOneAnnotation(field);
		assertTrue(result.contains("referencedColumnName = \"CODE\""), result);
	}

	@Test
	public void testGenerateOneToManyAnnotation() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToManyFields().get(0);
		assertEquals("@OneToMany(mappedBy = \"department\")",
				helper.generateOneToManyAnnotation(field));
	}

	@Test
	public void testGenerateOneToManyAnnotationWithFetchAndCascade() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example")
				.fetchType(FetchType.EAGER)
				.cascade(CascadeType.ALL)
				.orphanRemoval(true));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToManyFields().get(0);
		String result = helper.generateOneToManyAnnotation(field);
		assertTrue(result.contains("fetch = FetchType.EAGER"), result);
		assertTrue(result.contains("cascade = CascadeType.ALL"), result);
		assertTrue(result.contains("orphanRemoval = true"), result);
	}

	@Test
	public void testGenerateOneToOneAnnotationOwning() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToOneFields().get(0);
		String result = helper.generateOneToOneAnnotation(field);
		assertTrue(result.contains("@OneToOne"), result);
		assertTrue(result.contains("@JoinColumn(name = \"ADDRESS_ID\")"), result);
	}

	@Test
	public void testGenerateOneToOneAnnotationInverse() {
		TableMetadata table = new TableMetadata("ADDRESS", "Address", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("employee", "Employee", "com.example")
				.mappedBy("address"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToOneFields().get(0);
		String result = helper.generateOneToOneAnnotation(field);
		assertTrue(result.contains("mappedBy = \"address\""), result);
		assertFalse(result.contains("@JoinColumn"), result);
	}

	@Test
	public void testGenerateOneToOneAnnotationWithCascadeAndOrphanRemoval() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID")
				.cascade(CascadeType.ALL)
				.orphanRemoval(true));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getOneToOneFields().get(0);
		String result = helper.generateOneToOneAnnotation(field);
		assertTrue(result.contains("cascade = CascadeType.ALL"), result);
		assertTrue(result.contains("orphanRemoval = true"), result);
	}

	@Test
	public void testGenerateManyToManyAnnotationOwning() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getManyToManyFields().get(0);
		String result = helper.generateManyToManyAnnotation(field);
		assertTrue(result.contains("@ManyToMany"), result);
		assertTrue(result.contains("@JoinTable(name = \"EMPLOYEE_PROJECT\""), result);
		assertTrue(result.contains("joinColumns = @JoinColumn(name = \"EMPLOYEE_ID\")"), result);
		assertTrue(result.contains("inverseJoinColumns = @JoinColumn(name = \"PROJECT_ID\")"), result);
	}

	@Test
	public void testGenerateManyToManyAnnotationInverse() {
		TableMetadata table = new TableMetadata("PROJECT", "Project", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("employees", "Employee", "com.example")
				.mappedBy("projects"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getManyToManyFields().get(0);
		String result = helper.generateManyToManyAnnotation(field);
		assertTrue(result.contains("mappedBy = \"projects\""), result);
		assertFalse(result.contains("@JoinTable"), result);
	}

	@Test
	public void testGenerateManyToManyAnnotationWithMultipleCascade() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID")
				.cascade(CascadeType.PERSIST, CascadeType.MERGE));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getManyToManyFields().get(0);
		String result = helper.generateManyToManyAnnotation(field);
		assertTrue(result.contains("cascade = { CascadeType.PERSIST, CascadeType.MERGE }"), result);
	}

	@Test
	public void testGenerateEmbeddedIdAnnotation() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "OrderLineId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID")
				.addAttributeOverride("lineNumber", "LINE_NUMBER"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getCompositeIdField();
		String result = helper.generateEmbeddedIdAnnotation(field);
		assertTrue(result.contains("@EmbeddedId"), result);
		assertTrue(result.contains("@AttributeOverrides"), result);
		assertTrue(result.contains("@AttributeOverride(name = \"orderId\", column = @Column(name = \"ORDER_ID\"))"), result);
		assertTrue(result.contains("@AttributeOverride(name = \"lineNumber\", column = @Column(name = \"LINE_NUMBER\"))"), result);
	}

	@Test
	public void testGenerateEmbeddedAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addEmbeddedField(new EmbeddedFieldMetadata("homeAddress", "Address", "com.example")
				.addAttributeOverride("street", "HOME_STREET"));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getEmbeddedFields().get(0);
		String result = helper.generateEmbeddedAnnotation(field);
		assertTrue(result.contains("@Embedded"), result);
		assertTrue(result.contains("@AttributeOverride(name = \"street\", column = @Column(name = \"HOME_STREET\"))"), result);
	}

	// --- Annotation generation (annotated=false) ---

	@Test
	public void testUnannotatedClassAnnotations() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("", create(table, false).generateClassAnnotations());
	}

	@Test
	public void testUnannotatedIdAnnotations() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).generationType(GenerationType.IDENTITY));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("", helper.generateIdAnnotations(field));
	}

	@Test
	public void testUnannotatedVersionAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("", create(table, false).generateVersionAnnotation());
	}

	@Test
	public void testUnannotatedColumnAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("", helper.generateColumnAnnotation(field));
	}

	@Test
	public void testUnannotatedBasicAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("", helper.generateBasicAnnotation(field));
	}

	@Test
	public void testUnannotatedTemporalAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("HIRE_DATE", "hireDate", Date.class)
				.temporal(TemporalType.TIMESTAMP));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("", helper.generateTemporalAnnotation(field));
	}

	@Test
	public void testUnannotatedLobAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("", create(table, false).generateLobAnnotation());
	}

	@Test
	public void testUnannotatedManyToOneAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getManyToOneFields().get(0);
		assertEquals("", helper.generateManyToOneAnnotation(field));
	}

	@Test
	public void testUnannotatedOneToManyAnnotation() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getOneToManyFields().get(0);
		assertEquals("", helper.generateOneToManyAnnotation(field));
	}

	@Test
	public void testUnannotatedOneToOneAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example"));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getOneToOneFields().get(0);
		assertEquals("", helper.generateOneToOneAnnotation(field));
	}

	@Test
	public void testUnannotatedManyToManyAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example"));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getManyToManyFields().get(0);
		assertEquals("", helper.generateManyToManyAnnotation(field));
	}

	@Test
	public void testUnannotatedEmbeddedIdAnnotation() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "OrderLineId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID"));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getCompositeIdField();
		assertEquals("", helper.generateEmbeddedIdAnnotation(field));
	}

	@Test
	public void testUnannotatedEmbeddedAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addEmbeddedField(new EmbeddedFieldMetadata("address", "Address", "com.example"));
		TemplateHelper helper = create(table, false);
		FieldDetails field = helper.getEmbeddedFields().get(0);
		assertEquals("", helper.generateEmbeddedAnnotation(field));
	}

	// --- Subclass check ---

	@Test
	public void testIsSubclassFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertFalse(create(table).isSubclass());
	}

	@Test
	public void testIsSubclassTrue() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.parent("Vehicle", "com.example");
		assertTrue(create(table).isSubclass());
	}

	// --- Constructor support ---

	@Test
	public void testNeedsFullConstructorWithColumns() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		assertTrue(create(table).needsFullConstructor());
	}

	@Test
	public void testFullConstructorPropertiesBasicColumns() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		List<TemplateHelper.FullConstructorProperty> props = create(table).getFullConstructorProperties();
		assertEquals(2, props.size());
		assertEquals("Long", props.get(0).typeName());
		assertEquals("id", props.get(0).fieldName());
		assertEquals("String", props.get(1).typeName());
		assertEquals("name", props.get(1).fieldName());
	}

	@Test
	public void testFullConstructorSkipsVersion() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("VERSION", "version", Integer.class).version(true));
		List<TemplateHelper.FullConstructorProperty> props = create(table).getFullConstructorProperties();
		assertEquals(1, props.size());
		assertEquals("id", props.get(0).fieldName());
	}

	@Test
	public void testFullConstructorSkipsGenPropertyFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("INTERNAL", "internal", String.class));
		TemplateHelper helper = create(table, true,
				Collections.emptyMap(),
				fieldMeta("internal", "gen-property", "false"));
		List<TemplateHelper.FullConstructorProperty> props = helper.getFullConstructorProperties();
		assertEquals(1, props.size());
		assertEquals("id", props.get(0).fieldName());
	}

	@Test
	public void testFullConstructorIncludesCompositeId() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "OrderLineId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID"));
		List<TemplateHelper.FullConstructorProperty> props = create(table).getFullConstructorProperties();
		assertEquals(1, props.size());
		assertEquals("OrderLineId", props.get(0).typeName());
		assertEquals("id", props.get(0).fieldName());
	}

	@Test
	public void testFullConstructorIncludesRelationships() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example"));
		table.addOneToMany(new OneToManyMetadata("projects", "employee", "Project", "com.example"));
		table.addManyToMany(new ManyToManyMetadata("skills", "Skill", "com.example"));
		table.addEmbeddedField(new EmbeddedFieldMetadata("info", "Info", "com.example"));
		List<TemplateHelper.FullConstructorProperty> props = create(table).getFullConstructorProperties();
		assertEquals(5, props.size());
		assertEquals("id", props.get(0).fieldName());
		assertEquals("address", props.get(1).fieldName());
		assertEquals("projects", props.get(2).fieldName());
		assertEquals("skills", props.get(3).fieldName());
		assertEquals("info", props.get(4).fieldName());
	}

	@Test
	public void testGetFullConstructorParameterList() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		assertEquals("Long id, String name", create(table).getFullConstructorParameterList());
	}

	// --- toString support ---

	@Test
	public void testToStringPropertiesDefault() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		List<TemplateHelper.ToStringProperty> props = create(table).getToStringProperties();
		assertEquals(2, props.size());
		assertEquals("id", props.get(0).fieldName());
		assertEquals("getId", props.get(0).getterName());
		assertEquals("name", props.get(1).fieldName());
		assertEquals("getName", props.get(1).getterName());
	}

	@Test
	public void testToStringPropertiesExplicitUseInToString() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		table.addColumn(new ColumnMetadata("SECRET", "secret", String.class));
		TemplateHelper helper = create(table, true,
				Collections.emptyMap(),
				fieldMeta("name", "use-in-tostring", "true"));
		List<TemplateHelper.ToStringProperty> props = helper.getToStringProperties();
		assertEquals(1, props.size());
		assertEquals("name", props.get(0).fieldName());
	}

	@Test
	public void testToStringPropertiesWithCompositeId() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "OrderLineId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID"));
		List<TemplateHelper.ToStringProperty> props = create(table).getToStringProperties();
		assertEquals(1, props.size());
		assertEquals("id", props.get(0).fieldName());
	}

	// --- equals/hashCode support ---

	@Test
	public void testHasCompositeIdFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertFalse(create(table).hasCompositeId());
	}

	@Test
	public void testHasCompositeIdTrue() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "OrderLineId", "com.example"));
		assertTrue(create(table).hasCompositeId());
	}

	@Test
	public void testNeedsEqualsHashCodeWithPrimaryKey() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		assertTrue(create(table).needsEqualsHashCode());
	}

	@Test
	public void testNeedsEqualsHashCodeWithExplicitEquals() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("EMAIL", "email", String.class));
		TemplateHelper helper = create(table, true,
				Collections.emptyMap(),
				fieldMeta("email", "use-in-equals", "true"));
		assertTrue(helper.needsEqualsHashCode());
	}

	@Test
	public void testHasExplicitEqualsColumns() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("EMAIL", "email", String.class));
		TemplateHelper helper = create(table, true,
				Collections.emptyMap(),
				fieldMeta("email", "use-in-equals", "true"));
		assertTrue(helper.hasExplicitEqualsColumns());
		List<FieldDetails> equalsFields = helper.getEqualsFields();
		assertEquals(1, equalsFields.size());
		assertEquals("email", equalsFields.get(0).getName());
	}

	@Test
	public void testGetIdentifierFields() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		List<FieldDetails> idFields = create(table).getIdentifierFields();
		assertEquals(1, idFields.size());
		assertEquals("id", idFields.get(0).getName());
	}

	@Test
	public void testGenerateEqualsExpressionObject() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		String result = helper.generateEqualsExpression(field);
		assertTrue(result.contains("this.getName()"), result);
		assertTrue(result.contains("other.getName()"), result);
		assertTrue(result.contains(".equals("), result);
	}

	@Test
	public void testGenerateHashCodeExpressionObject() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		String result = helper.generateHashCodeExpression(field);
		assertEquals("(this.getName() == null ? 0 : this.getName().hashCode())", result);
	}

	// --- Meta-attribute support ---

	@Test
	public void testHasClassMetaAttribute() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TemplateHelper helper = create(table, true,
				Map.of("class-code", List.of("// custom")),
				Collections.emptyMap());
		assertTrue(helper.hasClassMetaAttribute("class-code"));
		assertFalse(helper.hasClassMetaAttribute("nonexistent"));
	}

	@Test
	public void testGetClassMetaAttribute() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TemplateHelper helper = create(table, true,
				Map.of("class-code", List.of("public void custom() {}")),
				Collections.emptyMap());
		assertEquals("public void custom() {}", helper.getClassMetaAttribute("class-code"));
	}

	@Test
	public void testGetClassMetaAttributeEmpty() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("", create(table).getClassMetaAttribute("nonexistent"));
	}

	@Test
	public void testHasFieldMetaAttribute() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table, true,
				Collections.emptyMap(),
				fieldMeta("name", "field-description", "The name"));
		FieldDetails field = helper.getBasicFields().get(0);
		assertTrue(helper.hasFieldMetaAttribute(field, "field-description"));
		assertFalse(helper.hasFieldMetaAttribute(field, "nonexistent"));
	}

	@Test
	public void testGetFieldMetaAsBool() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("INTERNAL", "internal", String.class));
		TemplateHelper helper = create(table, true,
				Collections.emptyMap(),
				fieldMeta("internal", "gen-property", "false"));
		FieldDetails field = helper.getBasicFields().get(0);
		assertFalse(helper.getFieldMetaAsBool(field, "gen-property", true));
	}

	@Test
	public void testGetFieldMetaAsBoolDefault() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertTrue(helper.getFieldMetaAsBool(field, "gen-property", true));
	}

	@Test
	public void testIsGenProperty() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		table.addColumn(new ColumnMetadata("INTERNAL", "internal", String.class));
		TemplateHelper helper = create(table, true,
				Collections.emptyMap(),
				fieldMeta("internal", "gen-property", "false"));
		List<FieldDetails> fields = helper.getBasicFields();
		FieldDetails nameField = fields.stream()
				.filter(f -> f.getName().equals("name")).findFirst().orElseThrow();
		FieldDetails internalField = fields.stream()
				.filter(f -> f.getName().equals("internal")).findFirst().orElseThrow();
		assertTrue(helper.isGenProperty(nameField));
		assertFalse(helper.isGenProperty(internalField));
	}

	@Test
	public void testHasFieldDescription() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		TemplateHelper helper = create(table, true,
				Collections.emptyMap(),
				fieldMeta("name", "field-description", "The employee name"));
		FieldDetails nameField = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("name")).findFirst().orElseThrow();
		FieldDetails idField = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("id")).findFirst().orElseThrow();
		assertTrue(helper.hasFieldDescription(nameField));
		assertFalse(helper.hasFieldDescription(idField));
	}

	@Test
	public void testGetFieldDescription() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table, true,
				Collections.emptyMap(),
				fieldMeta("name", "field-description", "The employee name"));
		FieldDetails field = helper.getBasicFields().get(0);
		assertEquals("The employee name", helper.getFieldDescription(field));
	}

	@Test
	public void testHasExtraClassCode() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertFalse(create(table).hasExtraClassCode());
		TemplateHelper helper = create(table, true,
				Map.of("class-code", List.of("// extra")),
				Collections.emptyMap());
		assertTrue(helper.hasExtraClassCode());
	}

	@Test
	public void testGetExtraClassCode() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		TemplateHelper helper = create(table, true,
				Map.of("class-code", List.of("public void customMethod() {}")),
				Collections.emptyMap());
		assertEquals("public void customMethod() {}", helper.getExtraClassCode());
	}

	// --- Field info methods ---

	@Test
	public void testIsPrimaryKey() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails idField = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("id")).findFirst().orElseThrow();
		FieldDetails nameField = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("name")).findFirst().orElseThrow();
		assertTrue(helper.isPrimaryKey(idField));
		assertFalse(helper.isPrimaryKey(nameField));
	}

	@Test
	public void testIsVersion() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("VERSION", "version", Integer.class).version(true));
		TemplateHelper helper = create(table);
		FieldDetails versionField = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("version")).findFirst().orElseThrow();
		assertTrue(helper.isVersion(versionField));
	}

	@Test
	public void testIsLob() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("BIO", "bio", String.class).lob(true));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		assertTrue(helper.isLob(field));
	}

	// --- Boolean getter prefix ---

	@Test
	public void testGetterNameBooleanField() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("ACTIVE", "active", boolean.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("active")).findFirst().orElseThrow();
		assertEquals("isActive", helper.getGetterName(field));
	}

	@Test
	public void testGetterNameNonBooleanField() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("name")).findFirst().orElseThrow();
		assertEquals("getName", helper.getGetterName(field));
	}

	@Test
	public void testGetterNameBooleanWrapperUsesGet() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("ACTIVE", "active", Boolean.class));
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().stream()
				.filter(f -> f.getName().equals("active")).findFirst().orElseThrow();
		assertEquals("getActive", helper.getGetterName(field));
	}

	// --- @Column insertable/updatable ---

	@Test
	public void testColumnAnnotationInsertableFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("COMPUTED", "computed", String.class);
		col.insertable(false);
		table.addColumn(col);
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		String ann = helper.generateColumnAnnotation(field);
		assertTrue(ann.contains("insertable = false"), ann);
	}

	@Test
	public void testColumnAnnotationUpdatableFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("COMPUTED", "computed", String.class);
		col.updatable(false);
		table.addColumn(col);
		TemplateHelper helper = create(table);
		FieldDetails field = helper.getBasicFields().get(0);
		String ann = helper.generateColumnAnnotation(field);
		assertTrue(ann.contains("updatable = false"), ann);
	}
}
