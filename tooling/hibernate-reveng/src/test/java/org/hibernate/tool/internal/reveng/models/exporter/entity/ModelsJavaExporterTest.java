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

import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.TemporalType;

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
 * Tests for {@link ModelsJavaExporter}.
 *
 * @author Koen Aers
 */
public class ModelsJavaExporterTest {

	private String export(TableMetadata table) {
		ModelsJavaExporter exporter = ModelsJavaExporter.create(List.of(table));
		StringWriter writer = new StringWriter();
		exporter.export(writer, table);
		return writer.toString();
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
				.discriminatorType(DiscriminatorType.STRING));
		String source = export(table);
		assertTrue(source.contains("@Inheritance(strategy = InheritanceType.SINGLE_TABLE)"), source);
		assertTrue(source.contains("@DiscriminatorColumn(name = \"DTYPE\""), source);
		assertTrue(source.contains("discriminatorType = DiscriminatorType.STRING"), source);
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
}
