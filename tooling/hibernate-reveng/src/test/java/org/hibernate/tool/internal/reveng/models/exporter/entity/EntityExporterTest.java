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

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.TemporalType;

import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.EntityListenersJpaAnnotation;
import org.hibernate.models.internal.BasicModelsContextImpl;
import org.hibernate.models.internal.SimpleClassLoading;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.jdk.JdkMethodDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.MutableAnnotationTarget;
import org.hibernate.tool.internal.reveng.models.builder.DynamicEntityBuilder;
import org.hibernate.tool.internal.reveng.models.builder.EmbeddableClassBuilder;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.EmbeddableMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.CompositeIdMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.EmbeddedFieldMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ForeignKeyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.InheritanceMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ManyToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToOneMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link EntityExporter}.
 *
 * @author Koen Aers
 */
public class EntityExporterTest {

	private String export(TableMetadata table) {
		return export(table, true);
	}

	private String export(TableMetadata table, boolean annotated) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		EntityExporter exporter = EntityExporter.create(
				List.of(entity), builder.getModelsContext(), annotated);
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		return writer.toString();
	}

	private String exportWithMeta(TableMetadata table,
								  Map<String, List<String>> classMetaAttributes,
								  Map<String, Map<String, List<String>>> fieldMetaAttributes) {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		EntityExporter exporter = EntityExporter.create(
				List.of(entity), builder.getModelsContext(), true);
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity, classMetaAttributes, fieldMetaAttributes);
		return writer.toString();
	}

	@Test
	public void testGeneratedHeader() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.matches("(?s).*// Generated .+ by Hibernate Tools .+\n.*"), source);
	}

	@Test
	public void testPackageDeclaration() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.contains("package com.example;"), source);
	}

	@Test
	public void testImplementsSerializable() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.contains("implements Serializable"), source);
	}

	@Test
	public void testEntityAndTableAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.contains("@Entity"), source);
		assertTrue(source.contains("@Table(name = \"EMPLOYEE\")"), source);
	}

	@Test
	public void testTableAnnotationWithSchemaAndCatalog() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.setSchema("HR");
		table.setCatalog("MYDB");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.contains("schema = \"HR\""), source);
		assertTrue(source.contains("catalog = \"MYDB\""), source);
	}

	@Test
	public void testBasicColumn() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		String source = export(table);
		assertTrue(source.contains("private String name;"), source);
		assertTrue(source.contains("@Column(name = \"NAME\")"), source);
		assertTrue(source.contains("public String getName()"), source);
		assertTrue(source.contains("public void setName(String name)"), source);
	}

	@Test
	public void testPrimaryKeyWithGeneratedValue() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.generationType(GenerationType.IDENTITY));
		String source = export(table);
		assertTrue(source.contains("@Id"), source);
		assertTrue(source.contains("@GeneratedValue(strategy = GenerationType.IDENTITY)"), source);
	}

	@Test
	public void testVersionColumn() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("VERSION", "version", Integer.class).version(true));
		String source = export(table);
		assertTrue(source.contains("@Version"), source);
	}

	@Test
	public void testTemporalColumn() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("HIRE_DATE", "hireDate", Date.class)
				.temporal(TemporalType.TIMESTAMP));
		String source = export(table);
		assertTrue(source.contains("@Temporal(TemporalType.TIMESTAMP)"), source);
	}

	@Test
	public void testLobColumn() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("BIO", "bio", String.class).lob(true));
		String source = export(table);
		assertTrue(source.contains("@Lob"), source);
	}

	@Test
	public void testColumnLengthPrecisionScale() {
		TableMetadata table = new TableMetadata("PRODUCT", "Product", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class).length(100));
		table.addColumn(new ColumnMetadata("PRICE", "price", BigDecimal.class)
				.precision(10).scale(2));
		String source = export(table);
		assertTrue(source.contains("length = 100"), source);
		assertTrue(source.contains("precision = 10"), source);
		assertTrue(source.contains("scale = 2"), source);
	}

	@Test
	public void testManyToOneRelationship() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		String source = export(table);
		assertTrue(source.contains("@ManyToOne"), source);
		assertTrue(source.contains("@JoinColumn(name = \"DEPT_ID\")"), source);
		assertTrue(source.contains("private Department department;"), source);
		assertTrue(source.contains("public Department getDepartment()"), source);
	}

	@Test
	public void testOneToManyRelationship() {
		TableMetadata table = new TableMetadata("DEPARTMENT", "Department", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToMany(new OneToManyMetadata(
				"employees", "department", "Employee", "com.example"));
		String source = export(table);
		assertTrue(source.contains("@OneToMany(mappedBy = \"department\")"), source);
		assertTrue(source.contains("Set<Employee>"), source);
		assertTrue(source.contains("new HashSet<>(0)"), source);
	}

	@Test
	public void testOneToOneOwning() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("address", "Address", "com.example")
				.foreignKeyColumnName("ADDRESS_ID"));
		String source = export(table);
		assertTrue(source.contains("@OneToOne"), source);
		assertTrue(source.contains("@JoinColumn(name = \"ADDRESS_ID\")"), source);
		assertTrue(source.contains("private Address address;"), source);
	}

	@Test
	public void testOneToOneInverse() {
		TableMetadata table = new TableMetadata("ADDRESS", "Address", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addOneToOne(new OneToOneMetadata("employee", "Employee", "com.example")
				.mappedBy("address"));
		String source = export(table);
		assertTrue(source.contains("@OneToOne(mappedBy = \"address\")"), source);
		assertFalse(source.contains("@JoinColumn"), source);
	}

	@Test
	public void testManyToManyOwning() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("projects", "Project", "com.example")
				.joinTable("EMPLOYEE_PROJECT", "EMPLOYEE_ID", "PROJECT_ID"));
		String source = export(table);
		assertTrue(source.contains("@ManyToMany"), source);
		assertTrue(source.contains("@JoinTable(name = \"EMPLOYEE_PROJECT\""), source);
		assertTrue(source.contains("joinColumns = @JoinColumn(name = \"EMPLOYEE_ID\")"), source);
		assertTrue(source.contains("inverseJoinColumns = @JoinColumn(name = \"PROJECT_ID\")"), source);
		assertTrue(source.contains("Set<Project>"), source);
	}

	@Test
	public void testManyToManyInverse() {
		TableMetadata table = new TableMetadata("PROJECT", "Project", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addManyToMany(new ManyToManyMetadata("employees", "Employee", "com.example")
				.mappedBy("projects"));
		String source = export(table);
		assertTrue(source.contains("@ManyToMany(mappedBy = \"projects\")"), source);
		assertFalse(source.contains("@JoinTable"), source);
	}

	@Test
	public void testCompositeId() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "OrderLineId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID")
				.addAttributeOverride("lineNumber", "LINE_NUMBER"));
		String source = export(table);
		assertTrue(source.contains("@EmbeddedId"), source);
		assertTrue(source.contains("@AttributeOverrides"), source);
		assertTrue(source.contains("@AttributeOverride(name = \"orderId\", column = @Column(name = \"ORDER_ID\"))"), source);
		assertTrue(source.contains("@AttributeOverride(name = \"lineNumber\", column = @Column(name = \"LINE_NUMBER\"))"), source);
		assertTrue(source.contains("private OrderLineId id;"), source);
	}

	@Test
	public void testEmbeddedField() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addEmbeddedField(new EmbeddedFieldMetadata("homeAddress", "Address", "com.example")
				.addAttributeOverride("street", "HOME_STREET")
				.addAttributeOverride("city", "HOME_CITY"));
		String source = export(table);
		assertTrue(source.contains("@Embedded"), source);
		assertTrue(source.contains("@AttributeOverrides"), source);
		assertTrue(source.contains("@AttributeOverride(name = \"street\", column = @Column(name = \"HOME_STREET\"))"), source);
		assertTrue(source.contains("@AttributeOverride(name = \"city\", column = @Column(name = \"HOME_CITY\"))"), source);
		assertTrue(source.contains("private Address homeAddress;"), source);
	}

	@Test
	public void testInheritanceRootEntity() {
		TableMetadata table = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.inheritance(new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
				.discriminatorColumn("DTYPE")
				.discriminatorType(DiscriminatorType.INTEGER));
		String source = export(table);
		assertTrue(source.contains("@Inheritance(strategy = InheritanceType.SINGLE_TABLE)"), source);
		assertTrue(source.contains("@DiscriminatorColumn(name = \"DTYPE\""), source);
		assertTrue(source.contains("discriminatorType = DiscriminatorType.INTEGER"), source);
	}

	@Test
	public void testInheritanceSubclass() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.parent("Vehicle", "com.example");
		table.discriminatorValue("CAR");
		table.primaryKeyJoinColumn("VEHICLE_ID");
		String source = export(table);
		assertTrue(source.contains("extends Vehicle"), source);
		assertTrue(source.contains("@DiscriminatorValue(\"CAR\")"), source);
		assertTrue(source.contains("@PrimaryKeyJoinColumn(name = \"VEHICLE_ID\")"), source);
	}

	@Test
	public void testDefaultConstructor() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertTrue(source.contains("public Employee() {"), source);
	}

	@Test
	public void testImports() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.generationType(GenerationType.IDENTITY));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		String source = export(table);
		assertTrue(source.contains("import jakarta.persistence.Entity;"), source);
		assertTrue(source.contains("import jakarta.persistence.Table;"), source);
		assertTrue(source.contains("import jakarta.persistence.Id;"), source);
		assertTrue(source.contains("import jakarta.persistence.GeneratedValue;"), source);
		assertTrue(source.contains("import jakarta.persistence.Column;"), source);
		assertFalse(source.contains("import java.lang.String;"), source);
		assertFalse(source.contains("import java.lang.Long;"), source);
	}

	@Test
	public void testForeignKeyColumnSkipped() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		String source = export(table);
		// The FK column should not have its own basic field
		assertFalse(source.contains("private Long deptId;"), source);
		// But the relationship field should exist
		assertTrue(source.contains("private Department department;"), source);
	}

	@Test
	public void testFullConstructor() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		String source = export(table);
		assertTrue(source.contains("public Employee(Long id, String name)"), source);
		assertTrue(source.contains("this.id = id;"), source);
		assertTrue(source.contains("this.name = name;"), source);
	}

	@Test
	public void testFullConstructorSkipsVersion() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		table.addColumn(new ColumnMetadata("VERSION", "version", Integer.class).version(true));
		String source = export(table);
		// Full constructor should not include version field
		assertTrue(source.contains("public Employee(Long id, String name)"), source);
		assertFalse(source.contains("public Employee(Long id, String name, Integer version)"), source);
	}

	@Test
	public void testToString() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		String source = export(table);
		assertTrue(source.contains("public String toString()"), source);
		assertTrue(source.contains("getId()"), source);
		assertTrue(source.contains("getName()"), source);
	}

	@Test
	public void testToStringSkipsForeignKeyColumns() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		String source = export(table);
		assertTrue(source.contains("getId()"), source);
		assertFalse(source.contains("getDeptId()"), source);
	}

	@Test
	public void testEqualsHashCodeWithSimpleId() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		String source = export(table);
		assertTrue(source.contains("public boolean equals(Object other)"), source);
		assertTrue(source.contains("public int hashCode()"), source);
		assertTrue(source.contains("instanceof Employee"), source);
		assertTrue(source.contains("getId()"), source);
		assertTrue(source.contains("result = 37 * result"), source);
	}

	@Test
	public void testEqualsHashCodeWithCompositeId() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "OrderLineId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID")
				.addAttributeOverride("lineNumber", "LINE_NUMBER"));
		String source = export(table);
		assertTrue(source.contains("public boolean equals(Object other)"), source);
		assertTrue(source.contains("public int hashCode()"), source);
		assertTrue(source.contains("getId()"), source);
	}

	@Test
	public void testNoEqualsHashCodeForSubclass() {
		TableMetadata table = new TableMetadata("CAR", "Car", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.parent("Vehicle", "com.example");
		String source = export(table);
		assertFalse(source.contains("public boolean equals("), source);
		assertFalse(source.contains("public int hashCode()"), source);
	}

	// --- Meta-attribute tests ---

	@Test
	public void testGenPropertyFalseSkipsField() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("INTERNAL", "internal", String.class));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		Map<String, Map<String, List<String>>> fieldMeta = new java.util.HashMap<>();
		fieldMeta.put("internal", Map.of("gen-property", List.of("false")));
		String source = exportWithMeta(table, Collections.emptyMap(), fieldMeta);
		assertFalse(source.contains("private String internal;"), source);
		assertFalse(source.contains("getInternal()"), source);
		assertTrue(source.contains("private String name;"), source);
		assertTrue(source.contains("getName()"), source);
	}

	@Test
	public void testFieldDescription() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		Map<String, Map<String, List<String>>> fieldMeta = new java.util.HashMap<>();
		fieldMeta.put("name", Map.of("field-description", List.of("The employee name")));
		String source = exportWithMeta(table, Collections.emptyMap(), fieldMeta);
		assertTrue(source.contains("The employee name"), source);
	}

	@Test
	public void testFieldDefaultValue() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("STATUS", "status", String.class));
		Map<String, Map<String, List<String>>> fieldMeta = new java.util.HashMap<>();
		fieldMeta.put("status", Map.of("default-value", List.of("\"active\"")));
		String source = exportWithMeta(table, Collections.emptyMap(), fieldMeta);
		assertTrue(source.contains("String status = \"active\";"), source);
	}

	@Test
	public void testPropertyTypeOverride() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("STATUS", "status", String.class));
		Map<String, Map<String, List<String>>> fieldMeta = new java.util.HashMap<>();
		fieldMeta.put("status", Map.of("property-type", List.of("com.example.StatusEnum")));
		String source = exportWithMeta(table, Collections.emptyMap(), fieldMeta);
		assertTrue(source.contains("StatusEnum status"), source);
	}

	@Test
	public void testExtraClassCode() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		Map<String, List<String>> classMeta = Map.of(
				"class-code", List.of("    public void customMethod() { }"));
		String source = exportWithMeta(table, classMeta, Collections.emptyMap());
		assertTrue(source.contains("extra code specified in the reveng.xml"), source);
		assertTrue(source.contains("public void customMethod() { }"), source);
	}

	@Test
	public void testUseInToString() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		table.addColumn(new ColumnMetadata("SECRET", "secret", String.class));
		Map<String, Map<String, List<String>>> fieldMeta = new java.util.HashMap<>();
		fieldMeta.put("name", Map.of("use-in-tostring", List.of("true")));
		String source = exportWithMeta(table, Collections.emptyMap(), fieldMeta);
		// Extract the toString method body
		int toStringStart = source.indexOf("public String toString()");
		assertTrue(toStringStart >= 0, "toString() should be generated");
		int braceStart = source.indexOf("{", toStringStart);
		int depth = 1;
		int pos = braceStart + 1;
		while (depth > 0 && pos < source.length()) {
			if (source.charAt(pos) == '{') depth++;
			else if (source.charAt(pos) == '}') depth--;
			pos++;
		}
		String toStringBody = source.substring(toStringStart, pos);
		assertTrue(toStringBody.contains("getName()"), "name should be in toString");
		assertFalse(toStringBody.contains("getSecret()"), "secret should not be in toString");
		assertFalse(toStringBody.contains("getId()"), "id should not be in toString");
	}

	@Test
	public void testUseInEquals() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("EMAIL", "email", String.class));
		Map<String, Map<String, List<String>>> fieldMeta = new java.util.HashMap<>();
		fieldMeta.put("email", Map.of("use-in-equals", List.of("true")));
		String source = exportWithMeta(table, Collections.emptyMap(), fieldMeta);
		assertTrue(source.contains("public boolean equals(Object other)"), source);
		assertTrue(source.contains("getEmail()"), source);
	}

	@Test
	public void testNoExtraClassCodeWhenNotSet() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table);
		assertFalse(source.contains("extra code"), source);
	}

	// --- Unannotated (annotated=false) tests ---

	@Test
	public void testUnannotatedNoEntityAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		String source = export(table, false);
		assertFalse(source.contains("@Entity"), source);
		assertFalse(source.contains("@Table"), source);
		assertTrue(source.contains("public class Employee"), source);
		assertTrue(source.contains("implements Serializable"), source);
	}

	@Test
	public void testUnannotatedNoColumnAnnotations() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.generationType(GenerationType.IDENTITY));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		String source = export(table, false);
		assertFalse(source.contains("@Id"), source);
		assertFalse(source.contains("@GeneratedValue"), source);
		assertFalse(source.contains("@Column"), source);
		assertTrue(source.contains("private Long id;"), source);
		assertTrue(source.contains("private String name;"), source);
		assertTrue(source.contains("public Long getId()"), source);
		assertTrue(source.contains("public String getName()"), source);
	}

	@Test
	public void testUnannotatedNoRelationshipAnnotations() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("DEPT_ID", "deptId", Long.class));
		table.addForeignKey(new ForeignKeyMetadata(
				"department", "DEPT_ID", "Department", "com.example"));
		table.addOneToMany(new OneToManyMetadata(
				"projects", "employee", "Project", "com.example"));
		String source = export(table, false);
		assertFalse(source.contains("@ManyToOne"), source);
		assertFalse(source.contains("@JoinColumn"), source);
		assertFalse(source.contains("@OneToMany"), source);
		assertTrue(source.contains("private Department department;"), source);
		assertTrue(source.contains("Set<Project>"), source);
	}

	@Test
	public void testUnannotatedNoJakartaImports() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.generationType(GenerationType.IDENTITY));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		String source = export(table, false);
		assertFalse(source.contains("import jakarta.persistence"), source);
	}

	@Test
	public void testUnannotatedNoVersionAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("VERSION", "version", Integer.class).version(true));
		String source = export(table, false);
		assertFalse(source.contains("@Version"), source);
		assertTrue(source.contains("private Integer version;"), source);
	}

	@Test
	public void testUnannotatedNoTemporalOrLobAnnotations() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("HIRE_DATE", "hireDate", Date.class)
				.temporal(TemporalType.TIMESTAMP));
		table.addColumn(new ColumnMetadata("BIO", "bio", String.class).lob(true));
		String source = export(table, false);
		assertFalse(source.contains("@Temporal"), source);
		assertFalse(source.contains("@Lob"), source);
		assertTrue(source.contains("private Date hireDate;"), source);
		assertTrue(source.contains("private String bio;"), source);
	}

	@Test
	public void testUnannotatedNoEmbeddedAnnotations() {
		TableMetadata table = new TableMetadata("ORDER_LINE", "OrderLine", "com.example");
		table.compositeId(new CompositeIdMetadata("id", "OrderLineId", "com.example")
				.addAttributeOverride("orderId", "ORDER_ID"));
		table.addEmbeddedField(new EmbeddedFieldMetadata("address", "Address", "com.example")
				.addAttributeOverride("street", "HOME_STREET"));
		String source = export(table, false);
		assertFalse(source.contains("@EmbeddedId"), source);
		assertFalse(source.contains("@Embedded"), source);
		assertFalse(source.contains("@AttributeOverride"), source);
		assertTrue(source.contains("private OrderLineId id;"), source);
		assertTrue(source.contains("private Address address;"), source);
	}

	@Test
	public void testUnannotatedConstructorsAndToStringStillGenerated() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		String source = export(table, false);
		assertTrue(source.contains("public Employee() {"), source);
		assertTrue(source.contains("public Employee(Long id, String name)"), source);
		assertTrue(source.contains("public String toString()"), source);
		assertTrue(source.contains("public boolean equals(Object other)"), source);
		assertTrue(source.contains("public int hashCode()"), source);
	}

	// --- Custom template path tests ---

	@Test
	public void testCustomTemplatePath(@TempDir Path tempDir) throws IOException {
		Files.writeString(tempDir.resolve("main.entity.ftl"),
				"// Custom template for ${templateHelper.getDeclarationName()}");
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		EntityExporter exporter = EntityExporter.create(
				List.of(entity), builder.getModelsContext(), true,
				new String[] { tempDir.toString() });
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String source = writer.toString();
		assertEquals("// Custom template for Employee", source);
	}

	@Test
	public void testCustomTemplatePathFallsBackToDefault(@TempDir Path tempDir) {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		EntityExporter exporter = EntityExporter.create(
				List.of(entity), builder.getModelsContext(), true,
				new String[] { tempDir.toString() });
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String source = writer.toString();
		assertTrue(source.contains("@Entity"), source);
		assertTrue(source.contains("public class Employee"), source);
	}

	@Test
	public void testCustomTemplatePathNonExistentDirectoryIgnored() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		EntityExporter exporter = EntityExporter.create(
				List.of(entity), builder.getModelsContext(), true,
				new String[] { "/nonexistent/path" });
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String source = writer.toString();
		assertTrue(source.contains("@Entity"), source);
		assertTrue(source.contains("public class Employee"), source);
	}

	// --- Embeddable export ---

	private String exportEmbeddable(EmbeddableMetadata metadata) {
		ModelsContext ctx = new BasicModelsContextImpl(
				SimpleClassLoading.SIMPLE_CLASS_LOADING, false, null);
		ClassDetails embeddable = EmbeddableClassBuilder.buildEmbeddableClass(metadata, ctx);
		EntityExporter exporter = EntityExporter.create(
				List.of(embeddable), ctx, true);
		StringWriter writer = new StringWriter();
		exporter.export(writer, embeddable);
		return writer.toString();
	}

	@Test
	public void testEmbeddableExport() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("OrderLineId", "com.example")
				.addColumn(new ColumnMetadata("ORDER_ID", "orderId", Long.class))
				.addColumn(new ColumnMetadata("LINE_NUMBER", "lineNumber", Integer.class));
		String source = exportEmbeddable(metadata);
		assertTrue(source.contains("package com.example;"), source);
		assertTrue(source.contains("@Embeddable"), source);
		assertFalse(source.contains("@Entity"), source);
		assertFalse(source.contains("@Table"), source);
		assertTrue(source.contains("public class OrderLineId"), source);
		assertTrue(source.contains("implements Serializable"), source);
	}

	@Test
	public void testEmbeddableExportFields() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("OrderLineId", "com.example")
				.addColumn(new ColumnMetadata("ORDER_ID", "orderId", Long.class))
				.addColumn(new ColumnMetadata("LINE_NUMBER", "lineNumber", Integer.class));
		String source = exportEmbeddable(metadata);
		assertTrue(source.contains("private Long orderId;"), source);
		assertTrue(source.contains("private Integer lineNumber;"), source);
	}

	@Test
	public void testEmbeddableExportGettersSetters() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("OrderLineId", "com.example")
				.addColumn(new ColumnMetadata("ORDER_ID", "orderId", Long.class))
				.addColumn(new ColumnMetadata("LINE_NUMBER", "lineNumber", Integer.class));
		String source = exportEmbeddable(metadata);
		assertTrue(source.contains("public Long getOrderId()"), source);
		assertTrue(source.contains("public void setOrderId(Long orderId)"), source);
		assertTrue(source.contains("public Integer getLineNumber()"), source);
		assertTrue(source.contains("public void setLineNumber(Integer lineNumber)"), source);
	}

	@Test
	public void testEmbeddableExportEqualsHashCode() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("OrderLineId", "com.example")
				.addColumn(new ColumnMetadata("ORDER_ID", "orderId", Long.class))
				.addColumn(new ColumnMetadata("LINE_NUMBER", "lineNumber", Integer.class));
		String source = exportEmbeddable(metadata);
		assertTrue(source.contains("public boolean equals(Object other)"), source);
		assertTrue(source.contains("public int hashCode()"), source);
		assertTrue(source.contains("getOrderId()"), source);
		assertTrue(source.contains("getLineNumber()"), source);
	}

	@Test
	public void testEmbeddableExportConstructor() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("OrderLineId", "com.example")
				.addColumn(new ColumnMetadata("ORDER_ID", "orderId", Long.class))
				.addColumn(new ColumnMetadata("LINE_NUMBER", "lineNumber", Integer.class));
		String source = exportEmbeddable(metadata);
		assertTrue(source.contains("public OrderLineId()"), source);
		assertTrue(source.contains("public OrderLineId(Long orderId, Integer lineNumber)"), source);
	}

	@Test
	public void testEmbeddableExportColumnAnnotations() {
		EmbeddableMetadata metadata = new EmbeddableMetadata("OrderLineId", "com.example")
				.addColumn(new ColumnMetadata("ORDER_ID", "orderId", Long.class))
				.addColumn(new ColumnMetadata("LINE_NUMBER", "lineNumber", Integer.class));
		String source = exportEmbeddable(metadata);
		assertTrue(source.contains("@Column(name = \"ORDER_ID\")"), source);
		assertTrue(source.contains("@Column(name = \"LINE_NUMBER\")"), source);
	}

	// --- @EntityListeners ---

	@Test
	public void testEntityListenersAnnotation() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		EntityListenersJpaAnnotation el = JpaAnnotations.ENTITY_LISTENERS.createUsage(builder.getModelsContext());
		el.value(new Class<?>[] { java.io.Serializable.class });
		((MutableAnnotationTarget) entity).addAnnotationUsage(el);
		EntityExporter exporter = EntityExporter.create(List.of(entity), builder.getModelsContext(), true);
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String source = writer.toString();
		assertTrue(source.contains("@EntityListeners(Serializable.class)"), source);
		assertTrue(source.contains("import jakarta.persistence.EntityListeners;"), source);
	}

	// --- Lifecycle callbacks ---

	static class WithCallbacks {
		@jakarta.persistence.PrePersist
		void onPrePersist() {}
		@jakarta.persistence.PostLoad
		void onPostLoad() {}
	}

	@Test
	public void testLifecycleCallbackMethods() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		DynamicClassDetails dc = (DynamicClassDetails) entity;
		for (java.lang.reflect.Method method : WithCallbacks.class.getDeclaredMethods()) {
			dc.addMethod(new JdkMethodDetails(
					method, MethodDetails.MethodKind.OTHER, null, dc, builder.getModelsContext()));
		}
		EntityExporter exporter = EntityExporter.create(List.of(entity), builder.getModelsContext(), true);
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String source = writer.toString();
		assertTrue(source.contains("@PrePersist"), source);
		assertTrue(source.contains("protected void onPrePersist()"), source);
		assertTrue(source.contains("@PostLoad"), source);
		assertTrue(source.contains("protected void onPostLoad()"), source);
	}

	@Test
	public void testScopeMetaAttributes() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		Map<String, Map<String, List<String>>> fieldMeta = new HashMap<>();
		fieldMeta.put("name", Map.of(
				"scope-field", List.of("protected"),
				"scope-get", List.of("protected"),
				"scope-set", List.of("private")));
		EntityExporter exporter = EntityExporter.create(List.of(entity), builder.getModelsContext(), true);
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity, Collections.emptyMap(), fieldMeta);
		String source = writer.toString();
		assertTrue(source.contains("protected String name;"), source);
		assertTrue(source.contains("protected String getName()"), source);
		assertTrue(source.contains("private void setName("), source);
	}

	@Test
	public void testImplementsMetaAttribute() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		Map<String, List<String>> classMeta = Map.of("implements", List.of("java.lang.Comparable"));
		EntityExporter exporter = EntityExporter.create(List.of(entity), builder.getModelsContext(), true);
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity, classMeta, Collections.emptyMap());
		String source = writer.toString();
		assertTrue(source.contains("implements Comparable, Serializable"), source);
	}

	@Test
	public void testExtendsMetaAttribute() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		Map<String, List<String>> classMeta = Map.of("extends", List.of("com.example.BaseEntity"));
		EntityExporter exporter = EntityExporter.create(List.of(entity), builder.getModelsContext(), true);
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity, classMeta, Collections.emptyMap());
		String source = writer.toString();
		assertTrue(source.contains("extends BaseEntity"), source);
	}

	@Test
	public void testClassDescriptionMetaAttribute() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		Map<String, List<String>> classMeta = Map.of("class-description", List.of("Employee entity representation"));
		EntityExporter exporter = EntityExporter.create(List.of(entity), builder.getModelsContext(), true);
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity, classMeta, Collections.emptyMap());
		String source = writer.toString();
		assertTrue(source.contains("/**"), source);
		assertTrue(source.contains("Employee entity representation"), source);
		assertTrue(source.contains("*/"), source);
	}

	@Test
	public void testExtraImportMetaAttribute() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		Map<String, List<String>> classMeta = Map.of("extra-import", List.of("com.example.util.MyHelper"));
		EntityExporter exporter = EntityExporter.create(List.of(entity), builder.getModelsContext(), true);
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity, classMeta, Collections.emptyMap());
		String source = writer.toString();
		assertTrue(source.contains("import com.example.util.MyHelper;"), source);
	}

	@Test
	public void testGeneratedClassMetaAttribute() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		Map<String, List<String>> classMeta = Map.of("generated-class", List.of("com.generated.EmployeeBase"));
		EntityExporter exporter = EntityExporter.create(List.of(entity), builder.getModelsContext(), true);
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity, classMeta, Collections.emptyMap());
		String source = writer.toString();
		assertTrue(source.contains("package com.generated;"), source);
		assertTrue(source.contains("class EmployeeBase"), source);
	}

	// --- Superclass constructor super() calls ---

	@Test
	public void testSubclassFullConstructorCallsSuper() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableMetadata parentTable = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		parentTable.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		parentTable.addColumn(new ColumnMetadata("MAKE", "make", String.class));
		builder.createEntityFromTable(parentTable);
		TableMetadata childTable = new TableMetadata("CAR", "Car", "com.example");
		childTable.parent("Vehicle", "com.example");
		childTable.addColumn(new ColumnMetadata("CAR_ID", "carId", Long.class).primaryKey(true));
		childTable.addColumn(new ColumnMetadata("DOORS", "doors", Integer.class));
		ClassDetails childEntity = builder.createEntityFromTable(childTable);
		EntityExporter exporter = EntityExporter.create(
				List.of(childEntity), builder.getModelsContext(), true);
		StringWriter writer = new StringWriter();
		exporter.export(writer, childEntity);
		String source = writer.toString();
		assertTrue(source.contains("extends Vehicle"), source);
		assertTrue(source.contains("Long id, String make, Long carId, Integer doors"), source);
		assertTrue(source.contains("super(id, make)"), source);
		assertTrue(source.contains("this.carId = carId"), source);
		assertTrue(source.contains("this.doors = doors"), source);
	}

	@Test
	public void testSubclassMinimalConstructorCallsSuper() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		TableMetadata parentTable = new TableMetadata("VEHICLE", "Vehicle", "com.example");
		parentTable.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		parentTable.addColumn(new ColumnMetadata("MAKE", "make", String.class).nullable(false));
		parentTable.addColumn(new ColumnMetadata("COLOR", "color", String.class));
		builder.createEntityFromTable(parentTable);
		TableMetadata childTable = new TableMetadata("CAR", "Car", "com.example");
		childTable.parent("Vehicle", "com.example");
		childTable.addColumn(new ColumnMetadata("CAR_ID", "carId", Long.class).primaryKey(true));
		childTable.addColumn(new ColumnMetadata("DOORS", "doors", Integer.class));
		ClassDetails childEntity = builder.createEntityFromTable(childTable);
		EntityExporter exporter = EntityExporter.create(
				List.of(childEntity), builder.getModelsContext(), true);
		StringWriter writer = new StringWriter();
		exporter.export(writer, childEntity);
		String source = writer.toString();
		// Minimal constructor should include super's minimal props (id, make) + own minimal (carId)
		assertTrue(source.contains("super(id, make)"), source);
	}

	@Test
	public void testNonSubclassNoSuperCall() {
		TableMetadata table = new TableMetadata("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnMetadata("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnMetadata("NAME", "name", String.class));
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails entity = builder.createEntityFromTable(table);
		EntityExporter exporter = EntityExporter.create(
				List.of(entity), builder.getModelsContext(), true);
		StringWriter writer = new StringWriter();
		exporter.export(writer, entity);
		String source = writer.toString();
		assertFalse(source.contains("super("), source);
	}
}
