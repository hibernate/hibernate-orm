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
import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.TemporalType;

import org.hibernate.tool.internal.export.java.ImportContextImpl;
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
		return new TemplateHelper(table, new ImportContextImpl(table.getEntityPackage()), annotated);
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
	public void testGetPackageDeclarationNull() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", null);
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

	// --- Type name resolution ---

	@Test
	public void testGetJavaTypeName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("NAME", "name", String.class);
		assertEquals("String", create(table).getJavaTypeName(col));
	}

	@Test
	public void testGetJavaTypeNameBigDecimal() {
		TableMetadata table = new TableMetadata("PRODUCT", "Product", "com.example");
		ColumnMetadata col = new ColumnMetadata("PRICE", "price", BigDecimal.class);
		TemplateHelper helper = create(table);
		assertEquals("BigDecimal", helper.getJavaTypeName(col));
		assertTrue(helper.generateImports().contains("import java.math.BigDecimal;"));
	}

	@Test
	public void testGetFieldTypeNameForeignKey() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ForeignKeyMetadata fk = new ForeignKeyMetadata("department", "DEPT_ID", "Department", "com.example");
		assertEquals("Department", create(table).getFieldTypeName(fk));
	}

	@Test
	public void testGetFieldTypeNameForeignKeyDifferentPackage() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ForeignKeyMetadata fk = new ForeignKeyMetadata("department", "DEPT_ID", "Department", "com.other");
		TemplateHelper helper = create(table);
		assertEquals("Department", helper.getFieldTypeName(fk));
		assertTrue(helper.generateImports().contains("import com.other.Department;"));
	}

	@Test
	public void testGetFieldTypeNameOneToOne() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		OneToOneMetadata o2o = new OneToOneMetadata("address", "Address", "com.example");
		assertEquals("Address", create(table).getFieldTypeName(o2o));
	}

	@Test
	public void testGetCollectionTypeNameOneToMany() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		OneToManyMetadata o2m = new OneToManyMetadata("employees", "department", "Employee", "com.example");
		TemplateHelper helper = create(table);
		assertEquals("Set<Employee>", helper.getCollectionTypeName(o2m));
		assertTrue(helper.generateImports().contains("import java.util.Set;"));
	}

	@Test
	public void testGetCollectionTypeNameManyToMany() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ManyToManyMetadata m2m = new ManyToManyMetadata("projects", "Project", "com.example");
		TemplateHelper helper = create(table);
		assertEquals("Set<Project>", helper.getCollectionTypeName(m2m));
	}

	@Test
	public void testGetEmbeddedTypeName() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		EmbeddedFieldMetadata emb = new EmbeddedFieldMetadata("address", "Address", "com.model");
		TemplateHelper helper = create(table);
		assertEquals("Address", helper.getEmbeddedTypeName(emb));
		assertTrue(helper.generateImports().contains("import com.model.Address;"));
	}

	@Test
	public void testGetCompositeIdTypeName() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		CompositeIdMetadata cid = new CompositeIdMetadata("id", "OrderLineId", "com.example");
		assertEquals("OrderLineId", create(table).getCompositeIdTypeName(cid));
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
				.discriminatorType(DiscriminatorType.STRING)
				.discriminatorColumnLength(31));
		String result = create(table).generateClassAnnotations();
		assertTrue(result.contains("@Inheritance(strategy = InheritanceType.SINGLE_TABLE)"), result);
		assertTrue(result.contains("@DiscriminatorColumn(name = \"DTYPE\""), result);
		assertTrue(result.contains("discriminatorType = DiscriminatorType.STRING"), result);
		assertTrue(result.contains("length = 31"), result);
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
		ColumnMetadata col = new ColumnMetadata("ID", "id", Long.class).primaryKey(true);
		String result = create(table).generateIdAnnotations(col);
		assertTrue(result.contains("@Id"), result);
		assertFalse(result.contains("@GeneratedValue"), result);
	}

	@Test
	public void testGenerateIdAnnotationsWithGeneratedValue() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.generationType(GenerationType.IDENTITY);
		String result = create(table).generateIdAnnotations(col);
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
		ColumnMetadata col = new ColumnMetadata("NAME", "name", String.class);
		String result = create(table).generateColumnAnnotation(col);
		assertEquals("@Column(name = \"NAME\")", result);
	}

	@Test
	public void testGenerateColumnAnnotationWithAttributes() {
		TableMetadata table = new TableMetadata("PRODUCT", "Product", "com.example");
		ColumnMetadata col = new ColumnMetadata("NAME", "name", String.class)
				.nullable(false).unique(true).length(100);
		String result = create(table).generateColumnAnnotation(col);
		assertTrue(result.contains("nullable = false"), result);
		assertTrue(result.contains("unique = true"), result);
		assertTrue(result.contains("length = 100"), result);
	}

	@Test
	public void testGenerateColumnAnnotationWithPrecisionScale() {
		TableMetadata table = new TableMetadata("PRODUCT", "Product", "com.example");
		ColumnMetadata col = new ColumnMetadata("PRICE", "price", BigDecimal.class)
				.precision(10).scale(2);
		String result = create(table).generateColumnAnnotation(col);
		assertTrue(result.contains("precision = 10"), result);
		assertTrue(result.contains("scale = 2"), result);
	}

	@Test
	public void testGenerateBasicAnnotationNoAttributes() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("NAME", "name", String.class);
		assertEquals("", create(table).generateBasicAnnotation(col));
	}

	@Test
	public void testGenerateTemporalAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("HIRE_DATE", "hireDate", Date.class)
				.temporal(TemporalType.TIMESTAMP);
		assertEquals("@Temporal(TemporalType.TIMESTAMP)", create(table).generateTemporalAnnotation(col));
	}

	@Test
	public void testGenerateTemporalAnnotationNone() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("NAME", "name", String.class);
		assertEquals("", create(table).generateTemporalAnnotation(col));
	}

	@Test
	public void testGenerateLobAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("@Lob", create(table).generateLobAnnotation());
	}

	@Test
	public void testGenerateManyToOneAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ForeignKeyMetadata fk = new ForeignKeyMetadata("department", "DEPT_ID", "Department", "com.example");
		String result = create(table).generateManyToOneAnnotation(fk);
		assertTrue(result.contains("@ManyToOne"), result);
		assertTrue(result.contains("@JoinColumn(name = \"DEPT_ID\")"), result);
	}

	@Test
	public void testGenerateManyToOneAnnotationWithFetchAndOptional() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ForeignKeyMetadata fk = new ForeignKeyMetadata("department", "DEPT_ID", "Department", "com.example")
				.fetchType(FetchType.LAZY)
				.optional(false);
		String result = create(table).generateManyToOneAnnotation(fk);
		assertTrue(result.contains("fetch = FetchType.LAZY"), result);
		assertTrue(result.contains("optional = false"), result);
	}

	@Test
	public void testGenerateManyToOneAnnotationWithReferencedColumn() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ForeignKeyMetadata fk = new ForeignKeyMetadata("department", "DEPT_CODE", "Department", "com.example")
				.referencedColumnName("CODE");
		String result = create(table).generateManyToOneAnnotation(fk);
		assertTrue(result.contains("referencedColumnName = \"CODE\""), result);
	}

	@Test
	public void testGenerateOneToManyAnnotation() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		OneToManyMetadata o2m = new OneToManyMetadata("employees", "department", "Employee", "com.example");
		String result = create(table).generateOneToManyAnnotation(o2m);
		assertEquals("@OneToMany(mappedBy = \"department\")", result);
	}

	@Test
	public void testGenerateOneToManyAnnotationWithFetchAndCascade() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		OneToManyMetadata o2m = new OneToManyMetadata("employees", "department", "Employee", "com.example")
				.fetchType(FetchType.EAGER)
				.cascade(CascadeType.ALL)
				.orphanRemoval(true);
		String result = create(table).generateOneToManyAnnotation(o2m);
		assertTrue(result.contains("fetch = FetchType.EAGER"), result);
		assertTrue(result.contains("cascade = CascadeType.ALL"), result);
		assertTrue(result.contains("orphanRemoval = true"), result);
	}

	@Test
	public void testGenerateOneToOneAnnotationOwning() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		OneToOneMetadata o2o = new OneToOneMetadata("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID");
		String result = create(table).generateOneToOneAnnotation(o2o);
		assertTrue(result.contains("@OneToOne"), result);
		assertTrue(result.contains("@JoinColumn(name = \"ADDRESS_ID\")"), result);
	}

	@Test
	public void testGenerateOneToOneAnnotationInverse() {
		TableMetadata table = new TableMetadata("ADDRESS", "Address", "com.example");
		OneToOneMetadata o2o = new OneToOneMetadata("employee", "Employee", "com.example")
				.mappedBy("address");
		String result = create(table).generateOneToOneAnnotation(o2o);
		assertTrue(result.contains("mappedBy = \"address\""), result);
		assertFalse(result.contains("@JoinColumn"), result);
	}

	@Test
	public void testGenerateOneToOneAnnotationWithCascadeAndOrphanRemoval() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		OneToOneMetadata o2o = new OneToOneMetadata("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID")
				.cascade(CascadeType.ALL)
				.orphanRemoval(true);
		String result = create(table).generateOneToOneAnnotation(o2o);
		assertTrue(result.contains("cascade = CascadeType.ALL"), result);
		assertTrue(result.contains("orphanRemoval = true"), result);
	}

	@Test
	public void testGenerateManyToManyAnnotationOwning() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ManyToManyMetadata m2m = new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID");
		String result = create(table).generateManyToManyAnnotation(m2m);
		assertTrue(result.contains("@ManyToMany"), result);
		assertTrue(result.contains("@JoinTable(name = \"EMPLOYEE_PROJECT\""), result);
		assertTrue(result.contains("joinColumns = @JoinColumn(name = \"EMPLOYEE_ID\")"), result);
		assertTrue(result.contains("inverseJoinColumns = @JoinColumn(name = \"PROJECT_ID\")"), result);
	}

	@Test
	public void testGenerateManyToManyAnnotationInverse() {
		TableMetadata table = new TableMetadata("PROJECT", "Project", "com.example");
		ManyToManyMetadata m2m = new ManyToManyMetadata("employees", "Employee", "com.example")
				.mappedBy("projects");
		String result = create(table).generateManyToManyAnnotation(m2m);
		assertTrue(result.contains("mappedBy = \"projects\""), result);
		assertFalse(result.contains("@JoinTable"), result);
	}

	@Test
	public void testGenerateManyToManyAnnotationWithMultipleCascade() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ManyToManyMetadata m2m = new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID")
				.cascade(CascadeType.PERSIST, CascadeType.MERGE);
		String result = create(table).generateManyToManyAnnotation(m2m);
		assertTrue(result.contains("cascade = { CascadeType.PERSIST, CascadeType.MERGE }"), result);
	}

	@Test
	public void testGenerateEmbeddedIdAnnotation() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		CompositeIdMetadata cid = new CompositeIdMetadata("id", "OrderLineId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID")
				.addAttributeOverride("lineNumber", "LINE_NUMBER");
		String result = create(table).generateEmbeddedIdAnnotation(cid);
		assertTrue(result.contains("@EmbeddedId"), result);
		assertTrue(result.contains("@AttributeOverrides"), result);
		assertTrue(result.contains("@AttributeOverride(name = \"orderId\", column = @Column(name = \"ORDER_ID\"))"), result);
		assertTrue(result.contains("@AttributeOverride(name = \"lineNumber\", column = @Column(name = \"LINE_NUMBER\"))"), result);
	}

	@Test
	public void testGenerateEmbeddedAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		EmbeddedFieldMetadata emb = new EmbeddedFieldMetadata("homeAddress", "Address", "com.example")
				.addAttributeOverride("street", "HOME_STREET");
		String result = create(table).generateEmbeddedAnnotation(emb);
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
		ColumnMetadata col = new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).generationType(GenerationType.IDENTITY);
		assertEquals("", create(table, false).generateIdAnnotations(col));
	}

	@Test
	public void testUnannotatedVersionAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("", create(table, false).generateVersionAnnotation());
	}

	@Test
	public void testUnannotatedColumnAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("NAME", "name", String.class);
		assertEquals("", create(table, false).generateColumnAnnotation(col));
	}

	@Test
	public void testUnannotatedBasicAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("NAME", "name", String.class);
		assertEquals("", create(table, false).generateBasicAnnotation(col));
	}

	@Test
	public void testUnannotatedTemporalAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("HIRE_DATE", "hireDate", Date.class)
				.temporal(TemporalType.TIMESTAMP);
		assertEquals("", create(table, false).generateTemporalAnnotation(col));
	}

	@Test
	public void testUnannotatedLobAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("", create(table, false).generateLobAnnotation());
	}

	@Test
	public void testUnannotatedManyToOneAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ForeignKeyMetadata fk = new ForeignKeyMetadata("department", "DEPT_ID", "Department", "com.example");
		assertEquals("", create(table, false).generateManyToOneAnnotation(fk));
	}

	@Test
	public void testUnannotatedOneToManyAnnotation() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		OneToManyMetadata o2m = new OneToManyMetadata("employees", "department", "Employee", "com.example");
		assertEquals("", create(table, false).generateOneToManyAnnotation(o2m));
	}

	@Test
	public void testUnannotatedOneToOneAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		OneToOneMetadata o2o = new OneToOneMetadata("address", "Address", "com.example");
		assertEquals("", create(table, false).generateOneToOneAnnotation(o2o));
	}

	@Test
	public void testUnannotatedManyToManyAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ManyToManyMetadata m2m = new ManyToManyMetadata("projects", "Project", "com.example");
		assertEquals("", create(table, false).generateManyToManyAnnotation(m2m));
	}

	@Test
	public void testUnannotatedEmbeddedIdAnnotation() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		CompositeIdMetadata cid = new CompositeIdMetadata("id", "OrderLineId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID");
		assertEquals("", create(table, false).generateEmbeddedIdAnnotation(cid));
	}

	@Test
	public void testUnannotatedEmbeddedAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		EmbeddedFieldMetadata emb = new EmbeddedFieldMetadata("address", "Address", "com.example");
		assertEquals("", create(table, false).generateEmbeddedAnnotation(emb));
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
	public void testFullConstructorSkipsForeignKeyColumns() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata("department", "DEPT_ID", "Department", "com.example"));
		List<TemplateHelper.FullConstructorProperty> props = create(table).getFullConstructorProperties();
		assertEquals(2, props.size());
		assertEquals("id", props.get(0).fieldName());
		assertEquals("department", props.get(1).fieldName());
	}

	@Test
	public void testFullConstructorSkipsGenPropertyFalse() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("INTERNAL", "internal", String.class)
				.addMetaAttribute("gen-property", "false"));
		List<TemplateHelper.FullConstructorProperty> props = create(table).getFullConstructorProperties();
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
	public void testToStringPropertiesSkipsForeignKeyColumns() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata("department", "DEPT_ID", "Department", "com.example"));
		List<TemplateHelper.ToStringProperty> props = create(table).getToStringProperties();
		assertEquals(1, props.size());
		assertEquals("id", props.get(0).fieldName());
	}

	@Test
	public void testToStringPropertiesExplicitUseInToString() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class)
				.addMetaAttribute("use-in-tostring", "true"));
		table.addColumn(new ColumnMetadata("SECRET", "secret", String.class));
		List<TemplateHelper.ToStringProperty> props = create(table).getToStringProperties();
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
		table.addColumn(new ColumnMetadata("EMAIL", "email", String.class)
				.addMetaAttribute("use-in-equals", "true"));
		assertTrue(create(table).needsEqualsHashCode());
	}

	@Test
	public void testHasExplicitEqualsColumns() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("EMAIL", "email", String.class)
				.addMetaAttribute("use-in-equals", "true"));
		TemplateHelper helper = create(table);
		assertTrue(helper.hasExplicitEqualsColumns());
		List<ColumnMetadata> equalsCols = helper.getEqualsColumns();
		assertEquals(1, equalsCols.size());
		assertEquals("email", equalsCols.get(0).getFieldName());
	}

	@Test
	public void testGetIdentifierColumns() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		List<ColumnMetadata> idCols = create(table).getIdentifierColumns();
		assertEquals(1, idCols.size());
		assertEquals("id", idCols.get(0).getFieldName());
	}

	@Test
	public void testGenerateEqualsExpressionPrimitive() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("AGE", "age", int.class);
		String result = create(table).generateEqualsExpression(col);
		assertEquals("this.getAge() == other.getAge()", result);
	}

	@Test
	public void testGenerateEqualsExpressionObject() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("NAME", "name", String.class);
		String result = create(table).generateEqualsExpression(col);
		assertTrue(result.contains("this.getName()"), result);
		assertTrue(result.contains("other.getName()"), result);
		assertTrue(result.contains(".equals("), result);
	}

	@Test
	public void testGenerateHashCodeExpressionInt() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("AGE", "age", int.class);
		assertEquals("this.getAge()", create(table).generateHashCodeExpression(col));
	}

	@Test
	public void testGenerateHashCodeExpressionBoolean() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("ACTIVE", "active", boolean.class);
		assertEquals("(this.getActive() ? 1 : 0)", create(table).generateHashCodeExpression(col));
	}

	@Test
	public void testGenerateHashCodeExpressionLong() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("ID", "id", long.class);
		assertEquals("(int) this.getId()", create(table).generateHashCodeExpression(col));
	}

	@Test
	public void testGenerateHashCodeExpressionObject() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("NAME", "name", String.class);
		String result = create(table).generateHashCodeExpression(col);
		assertEquals("(this.getName() == null ? 0 : this.getName().hashCode())", result);
	}

	@Test
	public void testGenerateHashCodeExpressionFloat() {
		TableMetadata table = new TableMetadata("PRODUCT", "Product", "com.example");
		ColumnMetadata col = new ColumnMetadata("WEIGHT", "weight", float.class);
		assertEquals("Float.floatToIntBits(this.getWeight())", create(table).generateHashCodeExpression(col));
	}

	@Test
	public void testGenerateHashCodeExpressionDouble() {
		TableMetadata table = new TableMetadata("PRODUCT", "Product", "com.example");
		ColumnMetadata col = new ColumnMetadata("PRICE", "price", double.class);
		assertEquals("(int) Double.doubleToLongBits(this.getPrice())", create(table).generateHashCodeExpression(col));
	}

	// --- Meta-attribute support ---

	@Test
	public void testHasClassMetaAttribute() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addMetaAttribute("class-code", "// custom");
		assertTrue(create(table).hasClassMetaAttribute("class-code"));
		assertFalse(create(table).hasClassMetaAttribute("nonexistent"));
	}

	@Test
	public void testGetClassMetaAttribute() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addMetaAttribute("class-code", "public void custom() {}");
		assertEquals("public void custom() {}", create(table).getClassMetaAttribute("class-code"));
	}

	@Test
	public void testGetClassMetaAttributeEmpty() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertEquals("", create(table).getClassMetaAttribute("nonexistent"));
	}

	@Test
	public void testHasColumnMetaAttribute() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("NAME", "name", String.class)
				.addMetaAttribute("field-description", "The name");
		assertTrue(create(table).hasColumnMetaAttribute(col, "field-description"));
		assertFalse(create(table).hasColumnMetaAttribute(col, "nonexistent"));
	}

	@Test
	public void testGetColumnMetaAsBool() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("INTERNAL", "internal", String.class)
				.addMetaAttribute("gen-property", "false");
		assertFalse(create(table).getColumnMetaAsBool(col, "gen-property", true));
	}

	@Test
	public void testGetColumnMetaAsBoolDefault() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("NAME", "name", String.class);
		assertTrue(create(table).getColumnMetaAsBool(col, "gen-property", true));
	}

	@Test
	public void testIsGenProperty() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col1 = new ColumnMetadata("NAME", "name", String.class);
		ColumnMetadata col2 = new ColumnMetadata("INTERNAL", "internal", String.class)
				.addMetaAttribute("gen-property", "false");
		TemplateHelper helper = create(table);
		assertTrue(helper.isGenProperty(col1));
		assertFalse(helper.isGenProperty(col2));
	}

	@Test
	public void testHasFieldDescription() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col1 = new ColumnMetadata("NAME", "name", String.class)
				.addMetaAttribute("field-description", "The employee name");
		ColumnMetadata col2 = new ColumnMetadata("ID", "id", Long.class);
		TemplateHelper helper = create(table);
		assertTrue(helper.hasFieldDescription(col1));
		assertFalse(helper.hasFieldDescription(col2));
	}

	@Test
	public void testGetFieldDescription() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		ColumnMetadata col = new ColumnMetadata("NAME", "name", String.class)
				.addMetaAttribute("field-description", "The employee name");
		assertEquals("The employee name", create(table).getFieldDescription(col));
	}

	@Test
	public void testHasExtraClassCode() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertFalse(create(table).hasExtraClassCode());
		table.addMetaAttribute("class-code", "// extra");
		assertTrue(create(table).hasExtraClassCode());
	}

	@Test
	public void testGetExtraClassCode() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addMetaAttribute("class-code", "public void customMethod() {}");
		assertEquals("public void customMethod() {}", create(table).getExtraClassCode());
	}

	// --- Utility ---

	@Test
	public void testIsForeignKeyColumn() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata("department", "DEPT_ID", "Department", "com.example"));
		TemplateHelper helper = create(table);
		assertTrue(helper.isForeignKeyColumn("DEPT_ID"));
		assertFalse(helper.isForeignKeyColumn("ID"));
	}

	@Test
	public void testGetTable() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		assertSame(table, create(table).getTable());
	}
}
