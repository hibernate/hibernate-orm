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
package org.hibernate.tool.internal.reveng.models.builder;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.internal.reveng.models.metadata.AttributeOverrideMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ColumnMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.CompositeIdMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.EmbeddableMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.EmbeddedFieldMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ForeignKeyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.InheritanceMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.ManyToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToManyMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.OneToOneMetadata;
import org.hibernate.tool.internal.reveng.models.metadata.TableMetadata;
import org.hibernate.models.spi.FieldDetails;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;

/**
 * Tests for DynamicEntityBuilder demonstrating how to build
 * entity metadata from database tables using Hibernate Models API.
 *
 * @author Koen Aers
 */
public class DynamicEntityBuilderTest {

	@Test
	public void testCreateSimpleEntity() {
		// GIVEN: Database table metadata for a PERSON table
		TableMetadata tableMetadata =
			new TableMetadata("PERSON", "Person", "com.example.entity");
		tableMetadata.setSchema("public");

		tableMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.autoIncrement(true)
		);

		tableMetadata.addColumn(
			new ColumnMetadata("NAME", "name", String.class)
				.length(255)
				.nullable(false)
		);

		tableMetadata.addColumn(
			new ColumnMetadata("EMAIL", "email", String.class)
				.length(100)
		);

		// WHEN: Building the entity using Hibernate Models
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails personEntity = builder.createEntityFromTable(tableMetadata);

		// THEN: Verify the entity structure
		assertNotNull(personEntity);
		// getName() returns the simple name, getClassName() returns the fully qualified name
		assertEquals("Person", personEntity.getName());
		assertEquals("com.example.entity.Person", personEntity.getClassName());

		// Verify @Entity annotation - returned as the actual annotation type
		Entity entityAnnotation =
			personEntity.getAnnotationUsage(Entity.class, builder.getModelsContext());
		assertNotNull(entityAnnotation, "Should have @Entity annotation");
		assertEquals("Person", entityAnnotation.name());

		// Verify @Table annotation
		Table tableAnnotation =
			personEntity.getAnnotationUsage(Table.class, builder.getModelsContext());
		assertNotNull(tableAnnotation, "Should have @Table annotation");
		assertEquals("PERSON", tableAnnotation.name());
		assertEquals("public", tableAnnotation.schema());

		// Verify fields
		List<FieldDetails> fields = personEntity.getFields();
		assertEquals(3, fields.size(), "Should have 3 fields");

		// Verify ID field
		FieldDetails idField = findField(fields, "id");
		assertNotNull(idField, "Should have id field");
		assertEquals("java.lang.Long", idField.getType().determineRawClass().getName());

		Id idAnnotation =
			idField.getAnnotationUsage(Id.class, builder.getModelsContext());
		assertNotNull(idAnnotation, "id field should have @Id annotation");

		GeneratedValue generatedAnnotation =
			idField.getAnnotationUsage(GeneratedValue.class, builder.getModelsContext());
		assertNotNull(generatedAnnotation, "id field should have @GeneratedValue annotation");
		assertEquals(GenerationType.IDENTITY, generatedAnnotation.strategy());

		Column idColumnAnnotation =
			idField.getAnnotationUsage(Column.class, builder.getModelsContext());
		assertNotNull(idColumnAnnotation, "id field should have @Column annotation");
		assertEquals("ID", idColumnAnnotation.name());
		assertFalse(idColumnAnnotation.nullable());

		// Verify NAME field
		FieldDetails nameField = findField(fields, "name");
		assertNotNull(nameField, "Should have name field");
		assertEquals("java.lang.String", nameField.getType().determineRawClass().getName());

		Column nameColumnAnnotation =
			nameField.getAnnotationUsage(Column.class, builder.getModelsContext());
		assertNotNull(nameColumnAnnotation, "name field should have @Column annotation");
		assertEquals("NAME", nameColumnAnnotation.name());
		assertEquals(255, nameColumnAnnotation.length());
		assertFalse(nameColumnAnnotation.nullable());

		// Verify EMAIL field
		FieldDetails emailField = findField(fields, "email");
		assertNotNull(emailField, "Should have email field");

		Column emailColumnAnnotation =
			emailField.getAnnotationUsage(Column.class, builder.getModelsContext());
		assertNotNull(emailColumnAnnotation, "email field should have @Column annotation");
		assertEquals("EMAIL", emailColumnAnnotation.name());
		assertEquals(100, emailColumnAnnotation.length());
		assertTrue(emailColumnAnnotation.nullable());
	}

	@Test
	public void testCreateEntityWithDifferentDataTypes() {
		// GIVEN: Database table with various column types
		TableMetadata tableMetadata =
			new TableMetadata("PRODUCT", "Product", "com.example.entity");

		tableMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.autoIncrement(true)
		);

		tableMetadata.addColumn(
			new ColumnMetadata("PRICE", "price", BigDecimal.class)
				.precision(10)
				.scale(2)
		);

		tableMetadata.addColumn(
			new ColumnMetadata("QUANTITY", "quantity", Integer.class)
		);

		tableMetadata.addColumn(
			new ColumnMetadata("ACTIVE", "active", Boolean.class)
		);

		// WHEN: Building the entity
		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails productEntity = builder.createEntityFromTable(tableMetadata);

		// THEN: Verify all field types are correct
		assertNotNull(productEntity);
		List<FieldDetails> fields = productEntity.getFields();
		assertEquals(4, fields.size());

		// Verify price field with precision/scale
		FieldDetails priceField = findField(fields, "price");
		assertNotNull(priceField);
		assertEquals("java.math.BigDecimal", priceField.getType().determineRawClass().getName());

		Column priceColumnAnnotation =
			priceField.getAnnotationUsage(Column.class, builder.getModelsContext());
		assertEquals(10, priceColumnAnnotation.precision());
		assertEquals(2, priceColumnAnnotation.scale());

		// Verify integer field
		FieldDetails quantityField = findField(fields, "quantity");
		assertNotNull(quantityField);
		assertEquals("java.lang.Integer", quantityField.getType().determineRawClass().getName());

		// Verify boolean field
		FieldDetails activeField = findField(fields, "active");
		assertNotNull(activeField);
		assertEquals("java.lang.Boolean", activeField.getType().determineRawClass().getName());
	}

	@Test
	public void testEntityMetadataCanBeUsedForCodeGeneration() {
		// GIVEN: An entity built from database metadata
		TableMetadata tableMetadata =
			new TableMetadata("USER_TABLE", "User", "com.example.entity");

		tableMetadata.addColumn(
			new ColumnMetadata("USER_ID", "userId", Long.class)
				.primaryKey(true)
		);

		tableMetadata.addColumn(
			new ColumnMetadata("USERNAME", "username", String.class)
				.length(50)
				.nullable(false)
		);

		DynamicEntityBuilder builder = new DynamicEntityBuilder();
		ClassDetails userEntity = builder.createEntityFromTable(tableMetadata);

		// WHEN: Generating code structure (simulating what code generators would do)
		StringBuilder generatedCode = new StringBuilder();
		// Use getClassName() for the fully qualified name
		generatedCode.append("package ").append(getPackageName(userEntity.getClassName())).append(";\n\n");

		// Add imports
		generatedCode.append("import jakarta.persistence.*;\n\n");

		// Add class annotations - access via actual annotation type methods
		Entity entityAnnotation =
			userEntity.getAnnotationUsage(Entity.class, builder.getModelsContext());
		generatedCode.append("@Entity(name = \"").append(entityAnnotation.name()).append("\")\n");

		Table tableAnnotation =
			userEntity.getAnnotationUsage(Table.class, builder.getModelsContext());
		generatedCode.append("@Table(name = \"").append(tableAnnotation.name()).append("\")\n");

		// Add class declaration - getName() gives the simple name
		generatedCode.append("public class ").append(userEntity.getName()).append(" {\n\n");

		// Add fields with annotations
		for (FieldDetails field : userEntity.getFields()) {
			if (field.hasAnnotationUsage(Id.class, builder.getModelsContext())) {
				generatedCode.append("\t@Id\n");
			}

			Column columnAnnotation =
				field.getAnnotationUsage(Column.class, builder.getModelsContext());
			generatedCode.append("\t@Column(name = \"").append(columnAnnotation.name()).append("\")\n");

			generatedCode.append("\tprivate ").append(getSimpleClassName(field.getType().determineRawClass().getName()))
				.append(" ").append(field.getName()).append(";\n\n");
		}

		generatedCode.append("}\n");

		// THEN: Verify the generated code structure
		String code = generatedCode.toString();
		assertTrue(code.contains("package com.example.entity;"));
		assertTrue(code.contains("@Entity(name = \"User\")"));
		assertTrue(code.contains("@Table(name = \"USER_TABLE\")"));
		assertTrue(code.contains("public class User {"));
		assertTrue(code.contains("@Id"));
		assertTrue(code.contains("@Column(name = \"USER_ID\")"));
		assertTrue(code.contains("private Long userId;"));
		assertTrue(code.contains("@Column(name = \"USERNAME\")"));
		assertTrue(code.contains("private String username;"));

		System.out.println("Generated code structure:");
		System.out.println(code);
	}

	@Test
	public void testCreateEntityWithManyToOneRelationship() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// GIVEN: A DEPARTMENT table (the target of the relationship)
		TableMetadata departmentMetadata =
			new TableMetadata("DEPARTMENT", "Department", "com.example.entity");

		departmentMetadata.addColumn(
			new ColumnMetadata("DEPT_ID", "deptId", Long.class)
				.primaryKey(true)
				.autoIncrement(true)
		);

		departmentMetadata.addColumn(
			new ColumnMetadata("DEPT_NAME", "deptName", String.class)
				.length(100)
				.nullable(false)
		);

		// Build the Department entity first so it's registered in the context
		ClassDetails departmentEntity = builder.createEntityFromTable(departmentMetadata);

		// GIVEN: An EMPLOYEE table with a foreign key to DEPARTMENT
		TableMetadata employeeMetadata =
			new TableMetadata("EMPLOYEE", "Employee", "com.example.entity");

		employeeMetadata.addColumn(
			new ColumnMetadata("EMP_ID", "empId", Long.class)
				.primaryKey(true)
				.autoIncrement(true)
		);

		employeeMetadata.addColumn(
			new ColumnMetadata("EMP_NAME", "empName", String.class)
				.length(200)
		);

		// The FK column is still declared as a column but will be replaced by the relationship
		employeeMetadata.addColumn(
			new ColumnMetadata("DEPT_ID", "deptId", Long.class)
		);

		// Add foreign key: EMPLOYEE.DEPT_ID -> DEPARTMENT.DEPT_ID
		employeeMetadata.addForeignKey(
			new ForeignKeyMetadata(
				"department",      // field name on the entity
				"DEPT_ID",         // FK column name
				"Department",      // target entity class name
				"com.example.entity" // target entity package
			)
			.referencedColumnName("DEPT_ID")
			.fetchType(FetchType.LAZY)
			.optional(false)
		);

		// WHEN: Building the Employee entity
		ClassDetails employeeEntity = builder.createEntityFromTable(employeeMetadata);

		// THEN: Verify the Employee entity structure
		assertNotNull(employeeEntity);
		assertEquals("Employee", employeeEntity.getName());
		assertEquals("com.example.entity.Employee", employeeEntity.getClassName());

		List<FieldDetails> fields = employeeEntity.getFields();
		// Should have: empId, empName, department (FK column replaced by relationship)
		assertEquals(3, fields.size(), "Should have 3 fields (empId, empName, department)");

		// Verify the FK column (DEPT_ID) is NOT a regular field
		assertNull(findField(fields, "deptId"), "FK column should not be a regular field");

		// Verify the ManyToOne relationship field
		FieldDetails departmentField = findField(fields, "department");
		assertNotNull(departmentField, "Should have department relationship field");

		// The field type should be the target entity
		assertEquals(
			"com.example.entity.Department",
			departmentField.getType().determineRawClass().getClassName()
		);

		// Verify @ManyToOne annotation
		ManyToOne manyToOneAnnotation =
			departmentField.getAnnotationUsage(ManyToOne.class, builder.getModelsContext());
		assertNotNull(manyToOneAnnotation, "Should have @ManyToOne annotation");
		assertEquals(FetchType.LAZY, manyToOneAnnotation.fetch());
		assertFalse(manyToOneAnnotation.optional());

		// Verify @JoinColumn annotation
		JoinColumn joinColumnAnnotation =
			departmentField.getAnnotationUsage(JoinColumn.class, builder.getModelsContext());
		assertNotNull(joinColumnAnnotation, "Should have @JoinColumn annotation");
		assertEquals("DEPT_ID", joinColumnAnnotation.name());
		assertEquals("DEPT_ID", joinColumnAnnotation.referencedColumnName());
		assertFalse(joinColumnAnnotation.nullable());
	}

	@Test
	public void testManyToOneCodeGeneration() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// Build Department first
		TableMetadata departmentMetadata =
			new TableMetadata("DEPARTMENT", "Department", "com.example.entity");
		departmentMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.autoIncrement(true)
		);
		departmentMetadata.addColumn(
			new ColumnMetadata("NAME", "name", String.class)
				.length(100)
		);
		builder.createEntityFromTable(departmentMetadata);

		// Build Employee with FK
		TableMetadata employeeMetadata =
			new TableMetadata("EMPLOYEE", "Employee", "com.example.entity");
		employeeMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.autoIncrement(true)
		);
		employeeMetadata.addColumn(
			new ColumnMetadata("NAME", "name", String.class)
				.length(200)
		);
		employeeMetadata.addColumn(
			new ColumnMetadata("DEPARTMENT_ID", "departmentId", Long.class)
		);
		employeeMetadata.addForeignKey(
			new ForeignKeyMetadata(
				"department", "DEPARTMENT_ID", "Department", "com.example.entity"
			).fetchType(FetchType.LAZY)
		);

		ClassDetails employeeEntity = builder.createEntityFromTable(employeeMetadata);

		// WHEN: Generate code from the ClassDetails
		StringBuilder code = new StringBuilder();
		code.append("package ").append(getPackageName(employeeEntity.getClassName())).append(";\n\n");
		code.append("import jakarta.persistence.*;\n\n");

		Entity entityAnn = employeeEntity.getAnnotationUsage(Entity.class, builder.getModelsContext());
		code.append("@Entity(name = \"").append(entityAnn.name()).append("\")\n");

		Table tableAnn = employeeEntity.getAnnotationUsage(Table.class, builder.getModelsContext());
		code.append("@Table(name = \"").append(tableAnn.name()).append("\")\n");

		code.append("public class ").append(employeeEntity.getName()).append(" {\n\n");

		for (FieldDetails field : employeeEntity.getFields()) {
			// @Id
			if (field.hasAnnotationUsage(Id.class, builder.getModelsContext())) {
				code.append("\t@Id\n");
				GeneratedValue gv = field.getAnnotationUsage(GeneratedValue.class, builder.getModelsContext());
				if (gv != null) {
					code.append("\t@GeneratedValue(strategy = GenerationType.").append(gv.strategy().name()).append(")\n");
				}
			}

			// @ManyToOne + @JoinColumn
			if (field.hasAnnotationUsage(ManyToOne.class, builder.getModelsContext())) {
				ManyToOne m2o = field.getAnnotationUsage(ManyToOne.class, builder.getModelsContext());
				code.append("\t@ManyToOne(fetch = FetchType.").append(m2o.fetch().name()).append(")\n");

				JoinColumn jc = field.getAnnotationUsage(JoinColumn.class, builder.getModelsContext());
				code.append("\t@JoinColumn(name = \"").append(jc.name()).append("\")\n");
			}

			// @Column (for basic fields)
			if (field.hasAnnotationUsage(Column.class, builder.getModelsContext())) {
				Column col = field.getAnnotationUsage(Column.class, builder.getModelsContext());
				code.append("\t@Column(name = \"").append(col.name()).append("\")\n");
			}

			String typeName = field.getType().determineRawClass().getName();
			code.append("\tprivate ").append(getSimpleClassName(typeName))
				.append(" ").append(field.getName()).append(";\n\n");
		}

		code.append("}\n");

		String result = code.toString();
		assertTrue(result.contains("@ManyToOne(fetch = FetchType.LAZY)"));
		assertTrue(result.contains("@JoinColumn(name = \"DEPARTMENT_ID\")"));
		assertTrue(result.contains("private Department department;"));
		assertFalse(result.contains("private Long departmentId;"),
			"FK column should not appear as a separate field");

		System.out.println("Generated Employee entity with ManyToOne:");
		System.out.println(result);
	}

	@Test
	public void testCreateEntityWithOneToManyRelationship() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// GIVEN: A DEPARTMENT table with a OneToMany to EMPLOYEE
		TableMetadata departmentMetadata =
			new TableMetadata("DEPARTMENT", "Department", "com.example.entity");

		departmentMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.autoIncrement(true)
		);

		departmentMetadata.addColumn(
			new ColumnMetadata("NAME", "name", String.class)
				.length(100)
		);

		// The inverse side: Department has a Set<Employee>
		departmentMetadata.addOneToMany(
			new OneToManyMetadata(
				"employees",         // field name
				"department",        // mappedBy (the field on Employee)
				"Employee",          // element entity class name
				"com.example.entity" // element entity package
			)
			.fetchType(FetchType.LAZY)
			.cascade(CascadeType.ALL)
			.orphanRemoval(true)
		);

		// WHEN: Building the Department entity
		ClassDetails departmentEntity = builder.createEntityFromTable(departmentMetadata);

		// THEN: Verify the entity
		assertNotNull(departmentEntity);
		assertEquals("Department", departmentEntity.getName());

		List<FieldDetails> fields = departmentEntity.getFields();
		assertEquals(3, fields.size(), "Should have 3 fields (id, name, employees)");

		// Verify the OneToMany field
		FieldDetails employeesField = findField(fields, "employees");
		assertNotNull(employeesField, "Should have employees field");
		assertTrue(employeesField.isPlural(), "employees field should be plural");

		// The raw type should be Set
		assertEquals("java.util.Set", employeesField.getType().determineRawClass().getClassName());

		// Verify @OneToMany annotation
		OneToMany oneToManyAnnotation =
			employeesField.getAnnotationUsage(OneToMany.class, builder.getModelsContext());
		assertNotNull(oneToManyAnnotation, "Should have @OneToMany annotation");
		assertEquals("department", oneToManyAnnotation.mappedBy());
		assertEquals(FetchType.LAZY, oneToManyAnnotation.fetch());
		assertTrue(oneToManyAnnotation.orphanRemoval());

		CascadeType[] cascades = oneToManyAnnotation.cascade();
		assertEquals(1, cascades.length);
		assertEquals(CascadeType.ALL, cascades[0]);

		// Verify no @JoinColumn on the inverse side
		assertFalse(
			employeesField.hasAnnotationUsage(JoinColumn.class, builder.getModelsContext()),
			"Inverse side should not have @JoinColumn"
		);
	}

	@Test
	public void testBidirectionalOneToManyCodeGeneration() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// Build Department with OneToMany
		TableMetadata departmentMetadata =
			new TableMetadata("DEPARTMENT", "Department", "com.example.entity");
		departmentMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.autoIncrement(true)
		);
		departmentMetadata.addColumn(
			new ColumnMetadata("NAME", "name", String.class)
				.length(100)
		);
		departmentMetadata.addOneToMany(
			new OneToManyMetadata(
				"employees", "department", "Employee", "com.example.entity"
			).fetchType(FetchType.LAZY).cascade(CascadeType.ALL)
		);
		ClassDetails departmentEntity = builder.createEntityFromTable(departmentMetadata);

		// Build Employee with ManyToOne
		TableMetadata employeeMetadata =
			new TableMetadata("EMPLOYEE", "Employee", "com.example.entity");
		employeeMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.autoIncrement(true)
		);
		employeeMetadata.addColumn(
			new ColumnMetadata("NAME", "name", String.class)
				.length(200)
		);
		employeeMetadata.addColumn(
			new ColumnMetadata("DEPARTMENT_ID", "departmentId", Long.class)
		);
		employeeMetadata.addForeignKey(
			new ForeignKeyMetadata(
				"department", "DEPARTMENT_ID", "Department", "com.example.entity"
			).fetchType(FetchType.LAZY)
		);
		ClassDetails employeeEntity = builder.createEntityFromTable(employeeMetadata);

		// WHEN: Generate code for both entities
		String departmentCode = generateEntityCode(departmentEntity, builder);
		String employeeCode = generateEntityCode(employeeEntity, builder);

		// THEN: Verify Department code
		assertTrue(departmentCode.contains("@OneToMany(mappedBy = \"department\""));
		assertTrue(departmentCode.contains("private Set<Employee> employees;"));
		assertFalse(departmentCode.contains("@JoinColumn"));

		// THEN: Verify Employee code
		assertTrue(employeeCode.contains("@ManyToOne(fetch = FetchType.LAZY)"));
		assertTrue(employeeCode.contains("@JoinColumn(name = \"DEPARTMENT_ID\")"));
		assertTrue(employeeCode.contains("private Department department;"));
		assertFalse(employeeCode.contains("private Long departmentId;"));

		System.out.println("=== Department (OneToMany side) ===");
		System.out.println(departmentCode);
		System.out.println("=== Employee (ManyToOne side) ===");
		System.out.println(employeeCode);
	}

	@Test
	public void testCreateEntityWithOneToOneOwningAndInverse() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// GIVEN: A USER table (owning side holds the FK to ADDRESS)
		TableMetadata userMetadata =
			new TableMetadata("USER_TABLE", "User", "com.example.entity");

		userMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.autoIncrement(true)
		);

		userMetadata.addColumn(
			new ColumnMetadata("USERNAME", "username", String.class)
				.length(100)
		);

		// FK column that will be replaced by the relationship
		userMetadata.addColumn(
			new ColumnMetadata("ADDRESS_ID", "addressId", Long.class)
		);

		// OneToOne owning side: User -> Address via ADDRESS_ID
		userMetadata.addOneToOne(
			new OneToOneMetadata(
				"address", "Address", "com.example.entity"
			)
			.foreignKeyColumnName("ADDRESS_ID")
			.fetchType(FetchType.LAZY)
			.optional(false)
			.cascade(CascadeType.ALL)
		);

		ClassDetails userEntity = builder.createEntityFromTable(userMetadata);

		// GIVEN: An ADDRESS table (inverse side, mapped by user.address)
		TableMetadata addressMetadata =
			new TableMetadata("ADDRESS", "Address", "com.example.entity");

		addressMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.autoIncrement(true)
		);

		addressMetadata.addColumn(
			new ColumnMetadata("STREET", "street", String.class)
				.length(255)
		);

		addressMetadata.addColumn(
			new ColumnMetadata("CITY", "city", String.class)
				.length(100)
		);

		// OneToOne inverse side
		addressMetadata.addOneToOne(
			new OneToOneMetadata(
				"user", "User", "com.example.entity"
			)
			.mappedBy("address")
			.fetchType(FetchType.LAZY)
		);

		ClassDetails addressEntity = builder.createEntityFromTable(addressMetadata);

		// THEN: Verify User entity (owning side)
		List<FieldDetails> userFields = userEntity.getFields();
		assertEquals(3, userFields.size(), "User should have 3 fields (id, username, address)");
		assertNull(findField(userFields, "addressId"), "FK column should not be a regular field");

		FieldDetails addressField = findField(userFields, "address");
		assertNotNull(addressField, "Should have address field");
		assertFalse(addressField.isPlural(), "OneToOne field should not be plural");
		assertEquals("com.example.entity.Address",
			addressField.getType().determineRawClass().getClassName());

		OneToOne userO2O = addressField.getAnnotationUsage(OneToOne.class, builder.getModelsContext());
		assertNotNull(userO2O);
		assertEquals(FetchType.LAZY, userO2O.fetch());
		assertFalse(userO2O.optional());
		assertEquals("", userO2O.mappedBy(), "Owning side should not have mappedBy");
		assertEquals(CascadeType.ALL, userO2O.cascade()[0]);

		JoinColumn userJC = addressField.getAnnotationUsage(JoinColumn.class, builder.getModelsContext());
		assertNotNull(userJC, "Owning side should have @JoinColumn");
		assertEquals("ADDRESS_ID", userJC.name());
		assertTrue(userJC.unique());
		assertFalse(userJC.nullable());

		// THEN: Verify Address entity (inverse side)
		List<FieldDetails> addressFields = addressEntity.getFields();
		assertEquals(4, addressFields.size(), "Address should have 4 fields (id, street, city, user)");

		FieldDetails userField = findField(addressFields, "user");
		assertNotNull(userField, "Should have user field");
		assertEquals("com.example.entity.User",
			userField.getType().determineRawClass().getClassName());

		OneToOne addrO2O = userField.getAnnotationUsage(OneToOne.class, builder.getModelsContext());
		assertNotNull(addrO2O);
		assertEquals("address", addrO2O.mappedBy());
		assertEquals(FetchType.LAZY, addrO2O.fetch());

		assertFalse(userField.hasAnnotationUsage(JoinColumn.class, builder.getModelsContext()),
			"Inverse side should not have @JoinColumn");
	}

	@Test
	public void testBidirectionalOneToOneCodeGeneration() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// Build User (owning side)
		TableMetadata userMetadata =
			new TableMetadata("USER_TABLE", "User", "com.example.entity");
		userMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true)
		);
		userMetadata.addColumn(
			new ColumnMetadata("USERNAME", "username", String.class).length(100)
		);
		userMetadata.addColumn(
			new ColumnMetadata("ADDRESS_ID", "addressId", Long.class)
		);
		userMetadata.addOneToOne(
			new OneToOneMetadata("address", "Address", "com.example.entity")
				.foreignKeyColumnName("ADDRESS_ID")
				.fetchType(FetchType.LAZY)
				.cascade(CascadeType.ALL)
				.optional(false)
		);
		ClassDetails userEntity = builder.createEntityFromTable(userMetadata);

		// Build Address (inverse side)
		TableMetadata addressMetadata =
			new TableMetadata("ADDRESS", "Address", "com.example.entity");
		addressMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true)
		);
		addressMetadata.addColumn(
			new ColumnMetadata("STREET", "street", String.class).length(255)
		);
		addressMetadata.addColumn(
			new ColumnMetadata("CITY", "city", String.class).length(100)
		);
		addressMetadata.addOneToOne(
			new OneToOneMetadata("user", "User", "com.example.entity")
				.mappedBy("address")
				.fetchType(FetchType.LAZY)
		);
		ClassDetails addressEntity = builder.createEntityFromTable(addressMetadata);

		// WHEN: Generate code
		String userCode = generateEntityCode(userEntity, builder);
		String addressCode = generateEntityCode(addressEntity, builder);

		// THEN: Verify User code (owning side)
		assertTrue(userCode.contains("@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)"));
		assertTrue(userCode.contains("@JoinColumn(name = \"ADDRESS_ID\", unique = true)"));
		assertTrue(userCode.contains("private Address address;"));
		assertFalse(userCode.contains("private Long addressId;"));

		// THEN: Verify Address code (inverse side)
		assertTrue(addressCode.contains("@OneToOne(mappedBy = \"address\", fetch = FetchType.LAZY)"));
		assertTrue(addressCode.contains("private User user;"));
		assertFalse(addressCode.contains("@JoinColumn"),
			"Inverse side should not have @JoinColumn");

		System.out.println("=== User (OneToOne owning side) ===");
		System.out.println(userCode);
		System.out.println("=== Address (OneToOne inverse side) ===");
		System.out.println(addressCode);
	}

	@Test
	public void testCreateEntityWithManyToManyOwningAndInverse() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// GIVEN: A STUDENT table (owning side of ManyToMany with COURSE)
		TableMetadata studentMetadata =
			new TableMetadata("STUDENT", "Student", "com.example.entity");

		studentMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.autoIncrement(true)
		);

		studentMetadata.addColumn(
			new ColumnMetadata("NAME", "name", String.class)
				.length(100)
		);

		// Owning side: Student owns the join table
		studentMetadata.addManyToMany(
			new ManyToManyMetadata(
				"courses", "Course", "com.example.entity"
			)
			.joinTable("STUDENT_COURSE", "STUDENT_ID", "COURSE_ID")
			.fetchType(FetchType.LAZY)
			.cascade(CascadeType.PERSIST, CascadeType.MERGE)
		);

		ClassDetails studentEntity = builder.createEntityFromTable(studentMetadata);

		// GIVEN: A COURSE table (inverse side)
		TableMetadata courseMetadata =
			new TableMetadata("COURSE", "Course", "com.example.entity");

		courseMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.autoIncrement(true)
		);

		courseMetadata.addColumn(
			new ColumnMetadata("TITLE", "title", String.class)
				.length(200)
		);

		// Inverse side
		courseMetadata.addManyToMany(
			new ManyToManyMetadata(
				"students", "Student", "com.example.entity"
			)
			.mappedBy("courses")
		);

		ClassDetails courseEntity = builder.createEntityFromTable(courseMetadata);

		// THEN: Verify Student entity (owning side)
		List<FieldDetails> studentFields = studentEntity.getFields();
		assertEquals(3, studentFields.size(), "Student should have 3 fields (id, name, courses)");

		FieldDetails coursesField = findField(studentFields, "courses");
		assertNotNull(coursesField);
		assertTrue(coursesField.isPlural());
		assertEquals("java.util.Set", coursesField.getType().determineRawClass().getClassName());

		ManyToMany studentM2M = coursesField.getAnnotationUsage(ManyToMany.class, builder.getModelsContext());
		assertNotNull(studentM2M);
		assertEquals("", studentM2M.mappedBy(), "Owning side should not have mappedBy");
		assertEquals(FetchType.LAZY, studentM2M.fetch());
		assertEquals(2, studentM2M.cascade().length);
		assertEquals(CascadeType.PERSIST, studentM2M.cascade()[0]);
		assertEquals(CascadeType.MERGE, studentM2M.cascade()[1]);

		JoinTable joinTable = coursesField.getAnnotationUsage(JoinTable.class, builder.getModelsContext());
		assertNotNull(joinTable, "Owning side should have @JoinTable");
		assertEquals("STUDENT_COURSE", joinTable.name());
		assertEquals(1, joinTable.joinColumns().length);
		assertEquals("STUDENT_ID", joinTable.joinColumns()[0].name());
		assertEquals(1, joinTable.inverseJoinColumns().length);
		assertEquals("COURSE_ID", joinTable.inverseJoinColumns()[0].name());

		// THEN: Verify Course entity (inverse side)
		List<FieldDetails> courseFields = courseEntity.getFields();
		assertEquals(3, courseFields.size(), "Course should have 3 fields (id, title, students)");

		FieldDetails studentsField = findField(courseFields, "students");
		assertNotNull(studentsField);
		assertTrue(studentsField.isPlural());

		ManyToMany courseM2M = studentsField.getAnnotationUsage(ManyToMany.class, builder.getModelsContext());
		assertNotNull(courseM2M);
		assertEquals("courses", courseM2M.mappedBy());

		assertFalse(studentsField.hasAnnotationUsage(JoinTable.class, builder.getModelsContext()),
			"Inverse side should not have @JoinTable");
	}

	@Test
	public void testBidirectionalManyToManyCodeGeneration() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// Build Student (owning side)
		TableMetadata studentMetadata =
			new TableMetadata("STUDENT", "Student", "com.example.entity");
		studentMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true)
		);
		studentMetadata.addColumn(
			new ColumnMetadata("NAME", "name", String.class).length(100)
		);
		studentMetadata.addManyToMany(
			new ManyToManyMetadata("courses", "Course", "com.example.entity")
				.joinTable("STUDENT_COURSE", "STUDENT_ID", "COURSE_ID")
				.cascade(CascadeType.PERSIST, CascadeType.MERGE)
		);
		ClassDetails studentEntity = builder.createEntityFromTable(studentMetadata);

		// Build Course (inverse side)
		TableMetadata courseMetadata =
			new TableMetadata("COURSE", "Course", "com.example.entity");
		courseMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true)
		);
		courseMetadata.addColumn(
			new ColumnMetadata("TITLE", "title", String.class).length(200)
		);
		courseMetadata.addManyToMany(
			new ManyToManyMetadata("students", "Student", "com.example.entity")
				.mappedBy("courses")
		);
		ClassDetails courseEntity = builder.createEntityFromTable(courseMetadata);

		// WHEN: Generate code
		String studentCode = generateEntityCode(studentEntity, builder);
		String courseCode = generateEntityCode(courseEntity, builder);

		// THEN: Verify Student code (owning side)
		assertTrue(studentCode.contains("@ManyToMany(cascade = CascadeType.PERSIST)"));
		assertTrue(studentCode.contains("@JoinTable(name = \"STUDENT_COURSE\""));
		assertTrue(studentCode.contains("joinColumns = @JoinColumn(name = \"STUDENT_ID\")"));
		assertTrue(studentCode.contains("inverseJoinColumns = @JoinColumn(name = \"COURSE_ID\")"));
		assertTrue(studentCode.contains("private Set<Course> courses;"));

		// THEN: Verify Course code (inverse side)
		assertTrue(courseCode.contains("@ManyToMany(mappedBy = \"courses\")"));
		assertTrue(courseCode.contains("private Set<Student> students;"));
		assertFalse(courseCode.contains("@JoinTable"), "Inverse side should not have @JoinTable");

		System.out.println("=== Student (ManyToMany owning side) ===");
		System.out.println(studentCode);
		System.out.println("=== Course (ManyToMany inverse side) ===");
		System.out.println(courseCode);
	}

	@Test
	public void testCreateEntityWithEmbeddedAndEmbeddable() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// GIVEN: An embeddable Address class (from columns spread across tables)
		EmbeddableMetadata addressEmbeddable =
			new EmbeddableMetadata("Address", "com.example.entity");

		addressEmbeddable.addColumn(
			new ColumnMetadata("STREET", "street", String.class)
				.length(255)
		);

		addressEmbeddable.addColumn(
			new ColumnMetadata("CITY", "city", String.class)
				.length(100)
		);

		addressEmbeddable.addColumn(
			new ColumnMetadata("ZIP_CODE", "zipCode", String.class)
				.length(20)
		);

		// Build the embeddable
		ClassDetails addressClass = EmbeddableClassBuilder.buildEmbeddableClass(addressEmbeddable, builder.getModelsContext());

		// THEN: Verify @Embeddable annotation
		assertNotNull(addressClass);
		assertEquals("Address", addressClass.getName());
		assertEquals("com.example.entity.Address", addressClass.getClassName());

		Embeddable embeddableAnnotation =
			addressClass.getAnnotationUsage(Embeddable.class, builder.getModelsContext());
		assertNotNull(embeddableAnnotation, "Should have @Embeddable annotation");

		// Should NOT have @Entity or @Table
		assertFalse(addressClass.hasAnnotationUsage(Entity.class, builder.getModelsContext()),
			"Embeddable should not have @Entity");
		assertFalse(addressClass.hasAnnotationUsage(Table.class, builder.getModelsContext()),
			"Embeddable should not have @Table");

		// Verify fields
		List<FieldDetails> addressFields = addressClass.getFields();
		assertEquals(3, addressFields.size(), "Should have 3 fields");

		FieldDetails streetField = findField(addressFields, "street");
		assertNotNull(streetField);
		Column streetCol = streetField.getAnnotationUsage(Column.class, builder.getModelsContext());
		assertEquals("STREET", streetCol.name());
		assertEquals(255, streetCol.length());

		// GIVEN: A PERSON table with two embedded Address fields (home and work)
		TableMetadata personMetadata =
			new TableMetadata("PERSON", "Person", "com.example.entity");

		personMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.autoIncrement(true)
		);

		personMetadata.addColumn(
			new ColumnMetadata("NAME", "name", String.class)
				.length(100)
		);

		// Home address: columns prefixed with HOME_
		personMetadata.addEmbeddedField(
			new EmbeddedFieldMetadata(
				"homeAddress", "Address", "com.example.entity"
			)
			.addAttributeOverride("street", "HOME_STREET")
			.addAttributeOverride("city", "HOME_CITY")
			.addAttributeOverride("zipCode", "HOME_ZIP_CODE")
		);

		// Work address: columns prefixed with WORK_
		personMetadata.addEmbeddedField(
			new EmbeddedFieldMetadata(
				"workAddress", "Address", "com.example.entity"
			)
			.addAttributeOverride("street", "WORK_STREET")
			.addAttributeOverride("city", "WORK_CITY")
			.addAttributeOverride("zipCode", "WORK_ZIP_CODE")
		);

		// WHEN: Building the Person entity
		ClassDetails personEntity = builder.createEntityFromTable(personMetadata);

		// THEN: Verify Person entity
		assertNotNull(personEntity);
		List<FieldDetails> personFields = personEntity.getFields();
		assertEquals(4, personFields.size(), "Should have 4 fields (id, name, homeAddress, workAddress)");

		// Verify homeAddress field
		FieldDetails homeAddressField = findField(personFields, "homeAddress");
		assertNotNull(homeAddressField, "Should have homeAddress field");
		assertEquals("com.example.entity.Address",
			homeAddressField.getType().determineRawClass().getClassName());
		assertFalse(homeAddressField.isPlural(), "Embedded field should not be plural");

		Embedded homeEmbedded =
			homeAddressField.getAnnotationUsage(Embedded.class, builder.getModelsContext());
		assertNotNull(homeEmbedded, "Should have @Embedded annotation");

		AttributeOverrides homeOverrides =
			homeAddressField.getAnnotationUsage(AttributeOverrides.class, builder.getModelsContext());
		assertNotNull(homeOverrides, "Should have @AttributeOverrides annotation");
		assertEquals(3, homeOverrides.value().length, "Should have 3 attribute overrides");

		// Verify individual overrides
		AttributeOverride[] homeOverrideArray = homeOverrides.value();
		assertEquals("street", homeOverrideArray[0].name());
		assertEquals("HOME_STREET", homeOverrideArray[0].column().name());
		assertEquals("city", homeOverrideArray[1].name());
		assertEquals("HOME_CITY", homeOverrideArray[1].column().name());
		assertEquals("zipCode", homeOverrideArray[2].name());
		assertEquals("HOME_ZIP_CODE", homeOverrideArray[2].column().name());

		// Verify workAddress field
		FieldDetails workAddressField = findField(personFields, "workAddress");
		assertNotNull(workAddressField, "Should have workAddress field");

		Embedded workEmbedded =
			workAddressField.getAnnotationUsage(Embedded.class, builder.getModelsContext());
		assertNotNull(workEmbedded, "Should have @Embedded annotation");

		AttributeOverrides workOverrides =
			workAddressField.getAnnotationUsage(AttributeOverrides.class, builder.getModelsContext());
		assertNotNull(workOverrides);
		assertEquals("WORK_STREET", workOverrides.value()[0].column().name());
	}

	@Test
	public void testEmbeddedEmbeddableCodeGeneration() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// Build embeddable Address
		EmbeddableMetadata addressEmbeddable =
			new EmbeddableMetadata("Address", "com.example.entity");
		addressEmbeddable.addColumn(
			new ColumnMetadata("STREET", "street", String.class).length(255)
		);
		addressEmbeddable.addColumn(
			new ColumnMetadata("CITY", "city", String.class).length(100)
		);
		ClassDetails addressClass = EmbeddableClassBuilder.buildEmbeddableClass(addressEmbeddable, builder.getModelsContext());

		// Build Person with embedded Address
		TableMetadata personMetadata =
			new TableMetadata("PERSON", "Person", "com.example.entity");
		personMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true)
		);
		personMetadata.addColumn(
			new ColumnMetadata("NAME", "name", String.class).length(100)
		);
		personMetadata.addEmbeddedField(
			new EmbeddedFieldMetadata(
				"address", "Address", "com.example.entity"
			)
			.addAttributeOverride("street", "HOME_STREET")
			.addAttributeOverride("city", "HOME_CITY")
		);
		ClassDetails personEntity = builder.createEntityFromTable(personMetadata);

		// WHEN: Generate code
		String addressCode = generateEmbeddableCode(addressClass, builder);
		String personCode = generateEntityCode(personEntity, builder);

		// THEN: Verify embeddable code
		assertTrue(addressCode.contains("@Embeddable"));
		assertFalse(addressCode.contains("@Entity"));
		assertFalse(addressCode.contains("@Table"));
		assertTrue(addressCode.contains("public class Address {"));
		assertTrue(addressCode.contains("@Column(name = \"STREET\")"));
		assertTrue(addressCode.contains("private String street;"));
		assertTrue(addressCode.contains("@Column(name = \"CITY\")"));
		assertTrue(addressCode.contains("private String city;"));

		// THEN: Verify entity code with embedded
		assertTrue(personCode.contains("@Embedded"));
		assertTrue(personCode.contains("@AttributeOverride(name = \"street\", column = @Column(name = \"HOME_STREET\"))"));
		assertTrue(personCode.contains("@AttributeOverride(name = \"city\", column = @Column(name = \"HOME_CITY\"))"));
		assertTrue(personCode.contains("private Address address;"));

		System.out.println("=== Address (Embeddable) ===");
		System.out.println(addressCode);
		System.out.println("=== Person (with Embedded Address) ===");
		System.out.println(personCode);
	}

	@Test
	public void testCreateEntityWithSingleTableInheritance() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// GIVEN: A VEHICLE root table with SINGLE_TABLE inheritance
		TableMetadata vehicleMetadata =
			new TableMetadata("VEHICLE", "Vehicle", "com.example.entity");

		vehicleMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.autoIncrement(true)
		);

		vehicleMetadata.addColumn(
			new ColumnMetadata("MAKE", "make", String.class)
				.length(100)
		);

		vehicleMetadata
			.inheritance(
				new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
					.discriminatorColumn("VEHICLE_TYPE")
					.discriminatorType(DiscriminatorType.STRING)
					.discriminatorColumnLength(50)
			)
			.discriminatorValue("VEHICLE");

		// WHEN: Building the Vehicle root entity
		ClassDetails vehicleEntity = builder.createEntityFromTable(vehicleMetadata);

		// THEN: Verify root entity annotations
		assertNotNull(vehicleEntity);
		assertEquals("Vehicle", vehicleEntity.getName());

		Inheritance inheritanceAnn =
			vehicleEntity.getAnnotationUsage(Inheritance.class, builder.getModelsContext());
		assertNotNull(inheritanceAnn, "Root should have @Inheritance");
		assertEquals(InheritanceType.SINGLE_TABLE, inheritanceAnn.strategy());

		DiscriminatorColumn discColAnn =
			vehicleEntity.getAnnotationUsage(DiscriminatorColumn.class, builder.getModelsContext());
		assertNotNull(discColAnn, "Root should have @DiscriminatorColumn");
		assertEquals("VEHICLE_TYPE", discColAnn.name());
		assertEquals(DiscriminatorType.STRING, discColAnn.discriminatorType());
		assertEquals(50, discColAnn.length());

		DiscriminatorValue discValAnn =
			vehicleEntity.getAnnotationUsage(DiscriminatorValue.class, builder.getModelsContext());
		assertNotNull(discValAnn, "Root should have @DiscriminatorValue");
		assertEquals("VEHICLE", discValAnn.value());

		// Verify no superclass
		assertNull(vehicleEntity.getSuperClass(), "Root entity should have no superclass");

		// GIVEN: A CAR subclass in the same table
		TableMetadata carMetadata =
			new TableMetadata("VEHICLE", "Car", "com.example.entity");

		carMetadata.addColumn(
			new ColumnMetadata("NUM_DOORS", "numDoors", Integer.class)
		);

		carMetadata
			.parent("Vehicle", "com.example.entity")
			.discriminatorValue("CAR");

		ClassDetails carEntity = builder.createEntityFromTable(carMetadata);

		// THEN: Verify Car subclass
		assertNotNull(carEntity);
		assertEquals("Car", carEntity.getName());

		// Verify superclass relationship
		ClassDetails carSuperClass = carEntity.getSuperClass();
		assertNotNull(carSuperClass, "Car should have a superclass");
		assertEquals("com.example.entity.Vehicle", carSuperClass.getClassName());

		// Car should have @Entity and @DiscriminatorValue
		assertNotNull(carEntity.getAnnotationUsage(Entity.class, builder.getModelsContext()));

		DiscriminatorValue carDiscVal =
			carEntity.getAnnotationUsage(DiscriminatorValue.class, builder.getModelsContext());
		assertNotNull(carDiscVal, "Car should have @DiscriminatorValue");
		assertEquals("CAR", carDiscVal.value());

		// Car should NOT have @Inheritance or @DiscriminatorColumn
		assertFalse(carEntity.hasAnnotationUsage(Inheritance.class, builder.getModelsContext()),
			"Subclass should not have @Inheritance");
		assertFalse(carEntity.hasAnnotationUsage(DiscriminatorColumn.class, builder.getModelsContext()),
			"Subclass should not have @DiscriminatorColumn");

		// Car should only have its own fields (not inherited ones)
		assertEquals(1, carEntity.getFields().size(), "Car should only have its own field");
		assertNotNull(findField(carEntity.getFields(), "numDoors"));

		// GIVEN: A TRUCK subclass
		TableMetadata truckMetadata =
			new TableMetadata("VEHICLE", "Truck", "com.example.entity");

		truckMetadata.addColumn(
			new ColumnMetadata("PAYLOAD_CAPACITY", "payloadCapacity", Double.class)
		);

		truckMetadata
			.parent("Vehicle", "com.example.entity")
			.discriminatorValue("TRUCK");

		ClassDetails truckEntity = builder.createEntityFromTable(truckMetadata);

		// THEN: Verify Truck subclass
		assertEquals("Truck", truckEntity.getName());
		assertEquals("com.example.entity.Vehicle", truckEntity.getSuperClass().getClassName());

		DiscriminatorValue truckDiscVal =
			truckEntity.getAnnotationUsage(DiscriminatorValue.class, builder.getModelsContext());
		assertEquals("TRUCK", truckDiscVal.value());
		assertEquals(1, truckEntity.getFields().size());
	}

	@Test
	public void testSingleTableInheritanceCodeGeneration() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// Build Vehicle root
		TableMetadata vehicleMetadata =
			new TableMetadata("VEHICLE", "Vehicle", "com.example.entity");
		vehicleMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true)
		);
		vehicleMetadata.addColumn(
			new ColumnMetadata("MAKE", "make", String.class).length(100)
		);
		vehicleMetadata
			.inheritance(
				new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
					.discriminatorColumn("VEHICLE_TYPE")
					.discriminatorType(DiscriminatorType.STRING)
			)
			.discriminatorValue("VEHICLE");
		ClassDetails vehicleEntity = builder.createEntityFromTable(vehicleMetadata);

		// Build Car subclass
		TableMetadata carMetadata =
			new TableMetadata("VEHICLE", "Car", "com.example.entity");
		carMetadata.addColumn(
			new ColumnMetadata("NUM_DOORS", "numDoors", Integer.class)
		);
		carMetadata
			.parent("Vehicle", "com.example.entity")
			.discriminatorValue("CAR");
		ClassDetails carEntity = builder.createEntityFromTable(carMetadata);

		// WHEN: Generate code
		String vehicleCode = generateEntityCode(vehicleEntity, builder);
		String carCode = generateEntityCode(carEntity, builder);

		// THEN: Verify Vehicle code
		assertTrue(vehicleCode.contains("@Inheritance(strategy = InheritanceType.SINGLE_TABLE)"));
		assertTrue(vehicleCode.contains("@DiscriminatorColumn(name = \"VEHICLE_TYPE\""));
		assertTrue(vehicleCode.contains("@DiscriminatorValue(\"VEHICLE\")"));
		assertTrue(vehicleCode.contains("public class Vehicle {"));

		// THEN: Verify Car code
		assertTrue(carCode.contains("@DiscriminatorValue(\"CAR\")"));
		assertTrue(carCode.contains("public class Car extends Vehicle {"));
		assertFalse(carCode.contains("@Inheritance"), "Subclass should not have @Inheritance");
		assertFalse(carCode.contains("@DiscriminatorColumn"), "Subclass should not have @DiscriminatorColumn");
		assertTrue(carCode.contains("private Integer numDoors;"));

		System.out.println("=== Vehicle (SINGLE_TABLE root) ===");
		System.out.println(vehicleCode);
		System.out.println("=== Car (SINGLE_TABLE subclass) ===");
		System.out.println(carCode);
	}

	@Test
	public void testCreateEntityWithJoinedInheritance() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// GIVEN: A PAYMENT root table with JOINED inheritance
		TableMetadata paymentMetadata =
			new TableMetadata("PAYMENT", "Payment", "com.example.entity");

		paymentMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.autoIncrement(true)
		);

		paymentMetadata.addColumn(
			new ColumnMetadata("AMOUNT", "amount", java.math.BigDecimal.class)
				.precision(10).scale(2)
		);

		paymentMetadata
			.inheritance(new InheritanceMetadata(InheritanceType.JOINED));

		ClassDetails paymentEntity = builder.createEntityFromTable(paymentMetadata);

		// THEN: Verify Payment root
		Inheritance inheritanceAnn =
			paymentEntity.getAnnotationUsage(Inheritance.class, builder.getModelsContext());
		assertNotNull(inheritanceAnn);
		assertEquals(InheritanceType.JOINED, inheritanceAnn.strategy());

		// Should NOT have @DiscriminatorColumn (JOINED doesn't require it)
		assertFalse(paymentEntity.hasAnnotationUsage(DiscriminatorColumn.class, builder.getModelsContext()));

		// GIVEN: A CREDIT_CARD_PAYMENT subclass table with PK join column
		TableMetadata ccPaymentMetadata =
			new TableMetadata("CREDIT_CARD_PAYMENT", "CreditCardPayment", "com.example.entity");

		ccPaymentMetadata.addColumn(
			new ColumnMetadata("CARD_NUMBER", "cardNumber", String.class)
				.length(19)
		);

		ccPaymentMetadata
			.parent("Payment", "com.example.entity")
			.primaryKeyJoinColumn("PAYMENT_ID");

		ClassDetails ccPaymentEntity = builder.createEntityFromTable(ccPaymentMetadata);

		// THEN: Verify CreditCardPayment subclass
		assertNotNull(ccPaymentEntity);
		assertEquals("CreditCardPayment", ccPaymentEntity.getName());

		// Verify superclass relationship
		assertNotNull(ccPaymentEntity.getSuperClass());
		assertEquals("com.example.entity.Payment", ccPaymentEntity.getSuperClass().getClassName());

		// Verify @PrimaryKeyJoinColumn
		PrimaryKeyJoinColumn pkJoinCol =
			ccPaymentEntity.getAnnotationUsage(PrimaryKeyJoinColumn.class, builder.getModelsContext());
		assertNotNull(pkJoinCol, "JOINED subclass should have @PrimaryKeyJoinColumn");
		assertEquals("PAYMENT_ID", pkJoinCol.name());

		// Verify own @Table
		Table ccTableAnn =
			ccPaymentEntity.getAnnotationUsage(Table.class, builder.getModelsContext());
		assertEquals("CREDIT_CARD_PAYMENT", ccTableAnn.name());

		// Subclass should not have @Inheritance
		assertFalse(ccPaymentEntity.hasAnnotationUsage(Inheritance.class, builder.getModelsContext()));

		// Subclass should only have its own fields
		assertEquals(1, ccPaymentEntity.getFields().size());
		assertNotNull(findField(ccPaymentEntity.getFields(), "cardNumber"));
	}

	@Test
	public void testJoinedInheritanceCodeGeneration() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// Build Payment root
		TableMetadata paymentMetadata =
			new TableMetadata("PAYMENT", "Payment", "com.example.entity");
		paymentMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true)
		);
		paymentMetadata.addColumn(
			new ColumnMetadata("AMOUNT", "amount", java.math.BigDecimal.class)
				.precision(10).scale(2)
		);
		paymentMetadata
			.inheritance(new InheritanceMetadata(InheritanceType.JOINED));
		ClassDetails paymentEntity = builder.createEntityFromTable(paymentMetadata);

		// Build CreditCardPayment subclass
		TableMetadata ccPaymentMetadata =
			new TableMetadata("CREDIT_CARD_PAYMENT", "CreditCardPayment", "com.example.entity");
		ccPaymentMetadata.addColumn(
			new ColumnMetadata("CARD_NUMBER", "cardNumber", String.class).length(19)
		);
		ccPaymentMetadata
			.parent("Payment", "com.example.entity")
			.primaryKeyJoinColumn("PAYMENT_ID");
		ClassDetails ccPaymentEntity = builder.createEntityFromTable(ccPaymentMetadata);

		// Build BankTransferPayment subclass (no PK join column override)
		TableMetadata btPaymentMetadata =
			new TableMetadata("BANK_TRANSFER_PAYMENT", "BankTransferPayment", "com.example.entity");
		btPaymentMetadata.addColumn(
			new ColumnMetadata("BANK_NAME", "bankName", String.class).length(100)
		);
		btPaymentMetadata.addColumn(
			new ColumnMetadata("ACCOUNT_NUMBER", "accountNumber", String.class).length(34)
		);
		btPaymentMetadata
			.parent("Payment", "com.example.entity");
		ClassDetails btPaymentEntity = builder.createEntityFromTable(btPaymentMetadata);

		// WHEN: Generate code
		String paymentCode = generateEntityCode(paymentEntity, builder);
		String ccPaymentCode = generateEntityCode(ccPaymentEntity, builder);
		String btPaymentCode = generateEntityCode(btPaymentEntity, builder);

		// THEN: Verify Payment code
		assertTrue(paymentCode.contains("@Inheritance(strategy = InheritanceType.JOINED)"));
		assertTrue(paymentCode.contains("public class Payment {"));
		assertFalse(paymentCode.contains("@DiscriminatorColumn"));
		assertFalse(paymentCode.contains("@PrimaryKeyJoinColumn"));

		// THEN: Verify CreditCardPayment code
		assertTrue(ccPaymentCode.contains("@Table(name = \"CREDIT_CARD_PAYMENT\")"));
		assertTrue(ccPaymentCode.contains("@PrimaryKeyJoinColumn(name = \"PAYMENT_ID\")"));
		assertTrue(ccPaymentCode.contains("public class CreditCardPayment extends Payment {"));
		assertTrue(ccPaymentCode.contains("private String cardNumber;"));
		assertFalse(ccPaymentCode.contains("@Inheritance"));

		// THEN: Verify BankTransferPayment code
		assertTrue(btPaymentCode.contains("@Table(name = \"BANK_TRANSFER_PAYMENT\")"));
		assertTrue(btPaymentCode.contains("public class BankTransferPayment extends Payment {"));
		assertFalse(btPaymentCode.contains("@PrimaryKeyJoinColumn"));
		assertTrue(btPaymentCode.contains("private String bankName;"));
		assertTrue(btPaymentCode.contains("private String accountNumber;"));

		System.out.println("=== Payment (JOINED root) ===");
		System.out.println(paymentCode);
		System.out.println("=== CreditCardPayment (JOINED subclass) ===");
		System.out.println(ccPaymentCode);
		System.out.println("=== BankTransferPayment (JOINED subclass) ===");
		System.out.println(btPaymentCode);
	}

	@Test
	public void testCreateEntityWithCompositeId() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// GIVEN: An embeddable ID class for ORDER_ITEM composite PK
		EmbeddableMetadata idClassMetadata =
			new EmbeddableMetadata("OrderItemId", "com.example.entity");

		idClassMetadata.addColumn(
			new ColumnMetadata("ORDER_ID", "orderId", Long.class)
				.primaryKey(true)
		);

		idClassMetadata.addColumn(
			new ColumnMetadata("PRODUCT_ID", "productId", Long.class)
				.primaryKey(true)
		);

		// Build the embeddable ID class
		ClassDetails idClass = EmbeddableClassBuilder.buildEmbeddableClass(idClassMetadata, builder.getModelsContext());

		// THEN: Verify the ID class
		assertNotNull(idClass);
		assertEquals("OrderItemId", idClass.getName());
		assertEquals("com.example.entity.OrderItemId", idClass.getClassName());

		Embeddable embeddableAnn =
			idClass.getAnnotationUsage(Embeddable.class, builder.getModelsContext());
		assertNotNull(embeddableAnn, "ID class should have @Embeddable");

		List<FieldDetails> idFields = idClass.getFields();
		assertEquals(2, idFields.size(), "ID class should have 2 fields");
		assertNotNull(findField(idFields, "orderId"));
		assertNotNull(findField(idFields, "productId"));

		// GIVEN: An ORDER_ITEM table with composite PK via @EmbeddedId
		TableMetadata orderItemMetadata =
			new TableMetadata("ORDER_ITEM", "OrderItem", "com.example.entity");

		orderItemMetadata.addColumn(
			new ColumnMetadata("QUANTITY", "quantity", Integer.class)
		);

		orderItemMetadata.addColumn(
			new ColumnMetadata("UNIT_PRICE", "unitPrice", java.math.BigDecimal.class)
				.precision(10).scale(2)
		);

		// Configure composite ID
		orderItemMetadata.compositeId(
			new CompositeIdMetadata(
				"id", "OrderItemId", "com.example.entity"
			)
		);

		// WHEN: Building the OrderItem entity
		ClassDetails orderItemEntity = builder.createEntityFromTable(orderItemMetadata);

		// THEN: Verify entity structure
		assertNotNull(orderItemEntity);
		assertEquals("OrderItem", orderItemEntity.getName());

		List<FieldDetails> entityFields = orderItemEntity.getFields();
		assertEquals(3, entityFields.size(), "Should have 3 fields (quantity, unitPrice, id)");

		// Verify @EmbeddedId field
		FieldDetails idField = findField(entityFields, "id");
		assertNotNull(idField, "Should have id field");
		assertEquals("com.example.entity.OrderItemId",
			idField.getType().determineRawClass().getClassName());
		assertFalse(idField.isPlural());

		EmbeddedId embeddedIdAnn =
			idField.getAnnotationUsage(EmbeddedId.class, builder.getModelsContext());
		assertNotNull(embeddedIdAnn, "Should have @EmbeddedId annotation");

		// Should NOT have @Id annotation (composite ID uses @EmbeddedId instead)
		assertFalse(idField.hasAnnotationUsage(Id.class, builder.getModelsContext()),
			"@EmbeddedId field should not have @Id");

		// Should NOT have @Column annotation
		assertFalse(idField.hasAnnotationUsage(Column.class, builder.getModelsContext()),
			"@EmbeddedId field should not have @Column");

		// Verify regular fields still have @Column
		FieldDetails quantityField = findField(entityFields, "quantity");
		assertNotNull(quantityField);
		Column quantityCol = quantityField.getAnnotationUsage(Column.class, builder.getModelsContext());
		assertEquals("QUANTITY", quantityCol.name());
	}

	@Test
	public void testCompositeIdWithAttributeOverrides() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// Build embeddable ID class
		EmbeddableMetadata idClassMetadata =
			new EmbeddableMetadata("FlightId", "com.example.entity");
		idClassMetadata.addColumn(
			new ColumnMetadata("DEPARTURE", "departure", String.class).length(3)
		);
		idClassMetadata.addColumn(
			new ColumnMetadata("ARRIVAL", "arrival", String.class).length(3)
		);
		EmbeddableClassBuilder.buildEmbeddableClass(idClassMetadata, builder.getModelsContext());

		// Build entity with @EmbeddedId and attribute overrides
		TableMetadata flightMetadata =
			new TableMetadata("FLIGHT", "Flight", "com.example.entity");
		flightMetadata.addColumn(
			new ColumnMetadata("FLIGHT_NUMBER", "flightNumber", String.class).length(10)
		);
		flightMetadata.compositeId(
			new CompositeIdMetadata(
				"id", "FlightId", "com.example.entity"
			)
			.addAttributeOverride("departure", "DEPARTURE_AIRPORT")
			.addAttributeOverride("arrival", "ARRIVAL_AIRPORT")
		);

		ClassDetails flightEntity = builder.createEntityFromTable(flightMetadata);

		// Verify @EmbeddedId field
		FieldDetails idField = findField(flightEntity.getFields(), "id");
		assertNotNull(idField);

		EmbeddedId embeddedIdAnn =
			idField.getAnnotationUsage(EmbeddedId.class, builder.getModelsContext());
		assertNotNull(embeddedIdAnn);

		// Verify @AttributeOverrides
		AttributeOverrides overrides =
			idField.getAnnotationUsage(AttributeOverrides.class, builder.getModelsContext());
		assertNotNull(overrides, "Should have @AttributeOverrides");
		assertEquals(2, overrides.value().length);
		assertEquals("departure", overrides.value()[0].name());
		assertEquals("DEPARTURE_AIRPORT", overrides.value()[0].column().name());
		assertEquals("arrival", overrides.value()[1].name());
		assertEquals("ARRIVAL_AIRPORT", overrides.value()[1].column().name());
	}

	@Test
	public void testCompositeIdCodeGeneration() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// Build embeddable ID class
		EmbeddableMetadata idClassMetadata =
			new EmbeddableMetadata("OrderItemId", "com.example.entity");
		idClassMetadata.addColumn(
			new ColumnMetadata("ORDER_ID", "orderId", Long.class)
		);
		idClassMetadata.addColumn(
			new ColumnMetadata("PRODUCT_ID", "productId", Long.class)
		);
		ClassDetails idClass = EmbeddableClassBuilder.buildEmbeddableClass(idClassMetadata, builder.getModelsContext());

		// Build entity with @EmbeddedId
		TableMetadata orderItemMetadata =
			new TableMetadata("ORDER_ITEM", "OrderItem", "com.example.entity");
		orderItemMetadata.addColumn(
			new ColumnMetadata("QUANTITY", "quantity", Integer.class)
		);
		orderItemMetadata.addColumn(
			new ColumnMetadata("UNIT_PRICE", "unitPrice", java.math.BigDecimal.class)
				.precision(10).scale(2)
		);
		orderItemMetadata.compositeId(
			new CompositeIdMetadata(
				"id", "OrderItemId", "com.example.entity"
			)
		);
		ClassDetails orderItemEntity = builder.createEntityFromTable(orderItemMetadata);

		// WHEN: Generate code
		String idClassCode = generateEmbeddableCode(idClass, builder);
		String entityCode = generateEntityCode(orderItemEntity, builder);

		// THEN: Verify ID class code
		assertTrue(idClassCode.contains("@Embeddable"));
		assertTrue(idClassCode.contains("public class OrderItemId {"));
		assertTrue(idClassCode.contains("private Long orderId;"));
		assertTrue(idClassCode.contains("private Long productId;"));

		// THEN: Verify entity code
		assertTrue(entityCode.contains("@EmbeddedId"));
		assertTrue(entityCode.contains("private OrderItemId id;"));
		assertFalse(entityCode.contains("@Id"), "Should not have @Id when using @EmbeddedId");
		assertTrue(entityCode.contains("private Integer quantity;"));
		assertTrue(entityCode.contains("private BigDecimal unitPrice;"));

		System.out.println("=== OrderItemId (Embeddable ID class) ===");
		System.out.println(idClassCode);
		System.out.println("=== OrderItem (with @EmbeddedId) ===");
		System.out.println(entityCode);
	}

	@Test
	public void testCreateEntityWithVersionField() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// GIVEN: A table with a version column for optimistic locking
		TableMetadata tableMetadata =
			new TableMetadata("ARTICLE", "Article", "com.example.entity");

		tableMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.autoIncrement(true)
		);

		tableMetadata.addColumn(
			new ColumnMetadata("TITLE", "title", String.class)
				.length(200)
				.nullable(false)
		);

		tableMetadata.addColumn(
			new ColumnMetadata("CONTENT", "content", String.class)
		);

		tableMetadata.addColumn(
			new ColumnMetadata("VERSION", "version", Integer.class)
				.version(true)
		);

		// WHEN: Building the entity
		ClassDetails articleEntity = builder.createEntityFromTable(tableMetadata);

		// THEN: Verify entity structure
		assertNotNull(articleEntity);
		List<FieldDetails> fields = articleEntity.getFields();
		assertEquals(4, fields.size());

		// Verify version field
		FieldDetails versionField = findField(fields, "version");
		assertNotNull(versionField, "Should have version field");
		assertEquals("java.lang.Integer", versionField.getType().determineRawClass().getName());

		Version versionAnn =
			versionField.getAnnotationUsage(Version.class, builder.getModelsContext());
		assertNotNull(versionAnn, "Should have @Version annotation");

		Column versionCol =
			versionField.getAnnotationUsage(Column.class, builder.getModelsContext());
		assertNotNull(versionCol);
		assertEquals("VERSION", versionCol.name());

		// Version field should NOT have @Id
		assertFalse(versionField.hasAnnotationUsage(Id.class, builder.getModelsContext()),
			"Version field should not have @Id");

		// Verify other fields are unaffected
		FieldDetails idField = findField(fields, "id");
		assertTrue(idField.hasAnnotationUsage(Id.class, builder.getModelsContext()));
		assertFalse(idField.hasAnnotationUsage(Version.class, builder.getModelsContext()),
			"ID field should not have @Version");
	}

	@Test
	public void testVersionFieldCodeGeneration() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// Build entity with version field
		TableMetadata tableMetadata =
			new TableMetadata("ARTICLE", "Article", "com.example.entity");
		tableMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true)
		);
		tableMetadata.addColumn(
			new ColumnMetadata("TITLE", "title", String.class).length(200)
		);
		tableMetadata.addColumn(
			new ColumnMetadata("VERSION", "version", Long.class)
				.version(true)
		);
		ClassDetails articleEntity = builder.createEntityFromTable(tableMetadata);

		// WHEN: Generate code
		String code = generateEntityCode(articleEntity, builder);

		// THEN: Verify generated code
		assertTrue(code.contains("@Version"));
		assertTrue(code.contains("@Column(name = \"VERSION\")"));
		assertTrue(code.contains("private Long version;"));
		assertTrue(code.contains("@Id"));

		System.out.println("=== Article (with @Version) ===");
		System.out.println(code);
	}

	@Test
	public void testCreateEntityWithBasicAndTemporal() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		// GIVEN: A table with @Basic and @Temporal fields
		TableMetadata tableMetadata =
			new TableMetadata("EVENT", "Event", "com.example.entity");

		tableMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.autoIncrement(true)
		);

		tableMetadata.addColumn(
			new ColumnMetadata("NAME", "name", String.class)
				.length(200)
		);

		// A LOB-like field with lazy fetch
		tableMetadata.addColumn(
			new ColumnMetadata("DESCRIPTION", "description", String.class)
				.basicFetch(FetchType.LAZY)
				.basicOptional(true)
		);

		// A non-optional basic field
		tableMetadata.addColumn(
			new ColumnMetadata("STATUS", "status", String.class)
				.length(20)
				.basicOptional(false)
		);

		// Date field with @Temporal(DATE)
		tableMetadata.addColumn(
			new ColumnMetadata("EVENT_DATE", "eventDate", java.util.Date.class)
				.temporal(TemporalType.DATE)
		);

		// Timestamp field with @Temporal(TIMESTAMP)
		tableMetadata.addColumn(
			new ColumnMetadata("CREATED_AT", "createdAt", java.util.Date.class)
				.temporal(TemporalType.TIMESTAMP)
		);

		// Time field with @Temporal(TIME)
		tableMetadata.addColumn(
			new ColumnMetadata("START_TIME", "startTime", java.util.Date.class)
				.temporal(TemporalType.TIME)
		);

		// WHEN: Building the entity
		ClassDetails eventEntity = builder.createEntityFromTable(tableMetadata);

		// THEN: Verify entity
		assertNotNull(eventEntity);
		List<FieldDetails> fields = eventEntity.getFields();
		assertEquals(7, fields.size());

		// Verify @Basic(fetch = LAZY) on description
		FieldDetails descField = findField(fields, "description");
		assertNotNull(descField);
		Basic descBasic = descField.getAnnotationUsage(Basic.class, builder.getModelsContext());
		assertNotNull(descBasic, "description should have @Basic");
		assertEquals(FetchType.LAZY, descBasic.fetch());
		assertTrue(descBasic.optional());

		// Verify @Basic(optional = false) on status
		FieldDetails statusField = findField(fields, "status");
		Basic statusBasic = statusField.getAnnotationUsage(Basic.class, builder.getModelsContext());
		assertNotNull(statusBasic, "status should have @Basic");
		assertFalse(statusBasic.optional());

		// Verify name field has no @Basic (not explicitly configured)
		FieldDetails nameField = findField(fields, "name");
		assertFalse(nameField.hasAnnotationUsage(Basic.class, builder.getModelsContext()),
			"name should not have @Basic when not explicitly configured");

		// Verify @Temporal(DATE) on eventDate
		FieldDetails eventDateField = findField(fields, "eventDate");
		Temporal eventDateTemporal = eventDateField.getAnnotationUsage(Temporal.class, builder.getModelsContext());
		assertNotNull(eventDateTemporal, "eventDate should have @Temporal");
		assertEquals(TemporalType.DATE, eventDateTemporal.value());

		// Verify @Temporal(TIMESTAMP) on createdAt
		FieldDetails createdAtField = findField(fields, "createdAt");
		Temporal createdAtTemporal = createdAtField.getAnnotationUsage(Temporal.class, builder.getModelsContext());
		assertNotNull(createdAtTemporal);
		assertEquals(TemporalType.TIMESTAMP, createdAtTemporal.value());

		// Verify @Temporal(TIME) on startTime
		FieldDetails startTimeField = findField(fields, "startTime");
		Temporal startTimeTemporal = startTimeField.getAnnotationUsage(Temporal.class, builder.getModelsContext());
		assertNotNull(startTimeTemporal);
		assertEquals(TemporalType.TIME, startTimeTemporal.value());

		// Verify temporal fields don't have @Basic unless explicitly set
		assertFalse(eventDateField.hasAnnotationUsage(Basic.class, builder.getModelsContext()));
	}

	@Test
	public void testBasicAndTemporalCodeGeneration() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		TableMetadata tableMetadata =
			new TableMetadata("EVENT", "Event", "com.example.entity");

		tableMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true)
		);

		tableMetadata.addColumn(
			new ColumnMetadata("DESCRIPTION", "description", String.class)
				.basicFetch(FetchType.LAZY)
		);

		tableMetadata.addColumn(
			new ColumnMetadata("STATUS", "status", String.class)
				.basicOptional(false)
		);

		tableMetadata.addColumn(
			new ColumnMetadata("EVENT_DATE", "eventDate", java.util.Date.class)
				.temporal(TemporalType.DATE)
		);

		tableMetadata.addColumn(
			new ColumnMetadata("CREATED_AT", "createdAt", java.util.Date.class)
				.temporal(TemporalType.TIMESTAMP)
				.basicFetch(FetchType.EAGER)
		);

		ClassDetails eventEntity = builder.createEntityFromTable(tableMetadata);

		// WHEN: Generate code
		String code = generateEntityCode(eventEntity, builder);

		// THEN: Verify
		assertTrue(code.contains("@Basic(fetch = FetchType.LAZY)"));
		assertTrue(code.contains("private String description;"));

		assertTrue(code.contains("@Basic(optional = false)"));
		assertTrue(code.contains("private String status;"));

		assertTrue(code.contains("@Temporal(TemporalType.DATE)"));
		assertTrue(code.contains("private Date eventDate;"));

		assertTrue(code.contains("@Basic(fetch = FetchType.EAGER)"));
		assertTrue(code.contains("@Temporal(TemporalType.TIMESTAMP)"));
		assertTrue(code.contains("private Date createdAt;"));

		System.out.println("=== Event (with @Basic and @Temporal) ===");
		System.out.println(code);
	}

	@Test
	public void testCreateEntityWithLobField() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		TableMetadata tableMetadata =
			new TableMetadata("DOCUMENT", "Document", "com.example.entity");

		tableMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true)
				.autoIncrement(true)
		);

		tableMetadata.addColumn(
			new ColumnMetadata("TITLE", "title", String.class)
				.length(200)
		);

		// CLOB field
		tableMetadata.addColumn(
			new ColumnMetadata("CONTENT", "content", String.class)
				.lob(true)
		);

		// BLOB field
		tableMetadata.addColumn(
			new ColumnMetadata("ATTACHMENT", "attachment", byte[].class)
				.lob(true)
		);

		// LOB with @Basic(fetch = LAZY)
		tableMetadata.addColumn(
			new ColumnMetadata("THUMBNAIL", "thumbnail", byte[].class)
				.lob(true)
				.basicFetch(FetchType.LAZY)
		);

		// WHEN
		ClassDetails docEntity = builder.createEntityFromTable(tableMetadata);

		// THEN
		assertNotNull(docEntity);
		List<FieldDetails> fields = docEntity.getFields();
		assertEquals(5, fields.size());

		// Verify CLOB field
		FieldDetails contentField = findField(fields, "content");
		assertNotNull(contentField);
		Lob contentLob = contentField.getAnnotationUsage(Lob.class, builder.getModelsContext());
		assertNotNull(contentLob, "content should have @Lob");
		Column contentCol = contentField.getAnnotationUsage(Column.class, builder.getModelsContext());
		assertEquals("CONTENT", contentCol.name());

		// Verify BLOB field
		FieldDetails attachmentField = findField(fields, "attachment");
		Lob attachmentLob = attachmentField.getAnnotationUsage(Lob.class, builder.getModelsContext());
		assertNotNull(attachmentLob, "attachment should have @Lob");

		// Verify LOB + @Basic(fetch = LAZY) combination
		FieldDetails thumbnailField = findField(fields, "thumbnail");
		assertNotNull(thumbnailField.getAnnotationUsage(Lob.class, builder.getModelsContext()));
		Basic thumbnailBasic = thumbnailField.getAnnotationUsage(Basic.class, builder.getModelsContext());
		assertNotNull(thumbnailBasic, "thumbnail should have @Basic");
		assertEquals(FetchType.LAZY, thumbnailBasic.fetch());

		// Verify non-LOB field does not have @Lob
		FieldDetails titleField = findField(fields, "title");
		assertFalse(titleField.hasAnnotationUsage(Lob.class, builder.getModelsContext()));
	}

	@Test
	public void testLobFieldCodeGeneration() {
		DynamicEntityBuilder builder = new DynamicEntityBuilder();

		TableMetadata tableMetadata =
			new TableMetadata("DOCUMENT", "Document", "com.example.entity");

		tableMetadata.addColumn(
			new ColumnMetadata("ID", "id", Long.class)
				.primaryKey(true).autoIncrement(true)
		);

		tableMetadata.addColumn(
			new ColumnMetadata("CONTENT", "content", String.class)
				.lob(true)
		);

		tableMetadata.addColumn(
			new ColumnMetadata("DATA", "data", byte[].class)
				.lob(true)
				.basicFetch(FetchType.LAZY)
		);

		ClassDetails docEntity = builder.createEntityFromTable(tableMetadata);

		// WHEN
		String code = generateEntityCode(docEntity, builder);

		// THEN
		assertTrue(code.contains("@Lob"));
		assertTrue(code.contains("@Column(name = \"CONTENT\")"));
		assertTrue(code.contains("private String content;"));

		assertTrue(code.contains("@Basic(fetch = FetchType.LAZY)"));
		assertTrue(code.contains("private byte[] data;"));

		System.out.println("=== Document (with @Lob) ===");
		System.out.println(code);
	}

	/**
	 * Generates a Java entity source code string from ClassDetails metadata.
	 */
	private String generateEntityCode(ClassDetails entity, DynamicEntityBuilder builder) {
		StringBuilder code = new StringBuilder();
		code.append("package ").append(getPackageName(entity.getClassName())).append(";\n\n");
		code.append("import java.util.Set;\n");
		code.append("import jakarta.persistence.*;\n\n");

		Entity entityAnn = entity.getAnnotationUsage(Entity.class, builder.getModelsContext());
		code.append("@Entity(name = \"").append(entityAnn.name()).append("\")\n");

		Table tableAnn = entity.getAnnotationUsage(Table.class, builder.getModelsContext());
		code.append("@Table(name = \"").append(tableAnn.name()).append("\")\n");

		// @Inheritance
		if (entity.hasAnnotationUsage(Inheritance.class, builder.getModelsContext())) {
			Inheritance inhAnn = entity.getAnnotationUsage(Inheritance.class, builder.getModelsContext());
			code.append("@Inheritance(strategy = InheritanceType.").append(inhAnn.strategy().name()).append(")\n");
		}

		// @DiscriminatorColumn
		if (entity.hasAnnotationUsage(DiscriminatorColumn.class, builder.getModelsContext())) {
			DiscriminatorColumn dcAnn = entity.getAnnotationUsage(DiscriminatorColumn.class, builder.getModelsContext());
			code.append("@DiscriminatorColumn(name = \"").append(dcAnn.name()).append("\"");
			if (dcAnn.discriminatorType() != DiscriminatorType.STRING) {
				code.append(", discriminatorType = DiscriminatorType.").append(dcAnn.discriminatorType().name());
			}
			code.append(")\n");
		}

		// @DiscriminatorValue
		if (entity.hasAnnotationUsage(DiscriminatorValue.class, builder.getModelsContext())) {
			DiscriminatorValue dvAnn = entity.getAnnotationUsage(DiscriminatorValue.class, builder.getModelsContext());
			code.append("@DiscriminatorValue(\"").append(dvAnn.value()).append("\")\n");
		}

		// @PrimaryKeyJoinColumn
		if (entity.hasAnnotationUsage(PrimaryKeyJoinColumn.class, builder.getModelsContext())) {
			PrimaryKeyJoinColumn pkjcAnn = entity.getAnnotationUsage(PrimaryKeyJoinColumn.class, builder.getModelsContext());
			code.append("@PrimaryKeyJoinColumn(name = \"").append(pkjcAnn.name()).append("\")\n");
		}

		// Class declaration with optional extends
		code.append("public class ").append(entity.getName());
		ClassDetails superClass = entity.getSuperClass();
		if (superClass != null) {
			code.append(" extends ").append(getSimpleClassName(superClass.getClassName()));
		}
		code.append(" {\n\n");

		for (FieldDetails field : entity.getFields()) {
			// @EmbeddedId
			if (field.hasAnnotationUsage(EmbeddedId.class, builder.getModelsContext())) {
				code.append("\t@EmbeddedId\n");

				AttributeOverrides attrOverrides =
					field.getAnnotationUsage(AttributeOverrides.class, builder.getModelsContext());
				if (attrOverrides != null && attrOverrides.value().length > 0) {
					for (AttributeOverride ao : attrOverrides.value()) {
						code.append("\t@AttributeOverride(name = \"").append(ao.name())
							.append("\", column = @Column(name = \"").append(ao.column().name())
							.append("\"))\n");
					}
				}
			}

			// @Id + @GeneratedValue (only if not @EmbeddedId)
			if (field.hasAnnotationUsage(Id.class, builder.getModelsContext())
					&& !field.hasAnnotationUsage(EmbeddedId.class, builder.getModelsContext())) {
				code.append("\t@Id\n");
				GeneratedValue gv = field.getAnnotationUsage(GeneratedValue.class, builder.getModelsContext());
				if (gv != null) {
					code.append("\t@GeneratedValue(strategy = GenerationType.")
						.append(gv.strategy().name()).append(")\n");
				}
			}

			// @ManyToOne + @JoinColumn
			if (field.hasAnnotationUsage(ManyToOne.class, builder.getModelsContext())) {
				ManyToOne m2o = field.getAnnotationUsage(ManyToOne.class, builder.getModelsContext());
				code.append("\t@ManyToOne(fetch = FetchType.").append(m2o.fetch().name()).append(")\n");

				JoinColumn jc = field.getAnnotationUsage(JoinColumn.class, builder.getModelsContext());
				if (jc != null) {
					code.append("\t@JoinColumn(name = \"").append(jc.name()).append("\")\n");
				}
			}

			// @OneToOne + @JoinColumn
			if (field.hasAnnotationUsage(OneToOne.class, builder.getModelsContext())) {
				OneToOne o2o = field.getAnnotationUsage(OneToOne.class, builder.getModelsContext());
				code.append("\t@OneToOne(");
				boolean needsComma = false;
				if (o2o.mappedBy() != null && !o2o.mappedBy().isEmpty()) {
					code.append("mappedBy = \"").append(o2o.mappedBy()).append("\"");
					needsComma = true;
				}
				if (o2o.fetch() != FetchType.EAGER) {
					if (needsComma) code.append(", ");
					code.append("fetch = FetchType.").append(o2o.fetch().name());
					needsComma = true;
				}
				if (o2o.cascade().length > 0) {
					if (needsComma) code.append(", ");
					code.append("cascade = CascadeType.").append(o2o.cascade()[0].name());
				}
				code.append(")\n");

				JoinColumn jc = field.getAnnotationUsage(JoinColumn.class, builder.getModelsContext());
				if (jc != null) {
					code.append("\t@JoinColumn(name = \"").append(jc.name()).append("\"");
					if (jc.unique()) {
						code.append(", unique = true");
					}
					code.append(")\n");
				}
			}

			// @OneToMany
			if (field.hasAnnotationUsage(OneToMany.class, builder.getModelsContext())) {
				OneToMany o2m = field.getAnnotationUsage(OneToMany.class, builder.getModelsContext());
				code.append("\t@OneToMany(mappedBy = \"").append(o2m.mappedBy()).append("\"");
				if (o2m.fetch() != FetchType.LAZY) {
					code.append(", fetch = FetchType.").append(o2m.fetch().name());
				}
				if (o2m.cascade().length > 0) {
					code.append(", cascade = CascadeType.").append(o2m.cascade()[0].name());
				}
				code.append(")\n");
			}

			// @ManyToMany + @JoinTable
			if (field.hasAnnotationUsage(ManyToMany.class, builder.getModelsContext())) {
				ManyToMany m2m = field.getAnnotationUsage(ManyToMany.class, builder.getModelsContext());
				code.append("\t@ManyToMany(");
				boolean needsComma = false;
				if (m2m.mappedBy() != null && !m2m.mappedBy().isEmpty()) {
					code.append("mappedBy = \"").append(m2m.mappedBy()).append("\"");
					needsComma = true;
				}
				if (m2m.fetch() != FetchType.LAZY) {
					if (needsComma) code.append(", ");
					code.append("fetch = FetchType.").append(m2m.fetch().name());
					needsComma = true;
				}
				if (m2m.cascade().length > 0) {
					if (needsComma) code.append(", ");
					code.append("cascade = CascadeType.").append(m2m.cascade()[0].name());
				}
				code.append(")\n");

				JoinTable jt = field.getAnnotationUsage(JoinTable.class, builder.getModelsContext());
				if (jt != null) {
					code.append("\t@JoinTable(name = \"").append(jt.name()).append("\"");
					if (jt.joinColumns().length > 0) {
						code.append(",\n\t\tjoinColumns = @JoinColumn(name = \"")
							.append(jt.joinColumns()[0].name()).append("\")");
					}
					if (jt.inverseJoinColumns().length > 0) {
						code.append(",\n\t\tinverseJoinColumns = @JoinColumn(name = \"")
							.append(jt.inverseJoinColumns()[0].name()).append("\")");
					}
					code.append(")\n");
				}
			}

			// @Embedded + @AttributeOverrides
			if (field.hasAnnotationUsage(Embedded.class, builder.getModelsContext())) {
				code.append("\t@Embedded\n");

				AttributeOverrides attrOverrides =
					field.getAnnotationUsage(AttributeOverrides.class, builder.getModelsContext());
				if (attrOverrides != null && attrOverrides.value().length > 0) {
					for (AttributeOverride ao : attrOverrides.value()) {
						code.append("\t@AttributeOverride(name = \"").append(ao.name())
							.append("\", column = @Column(name = \"").append(ao.column().name())
							.append("\"))\n");
					}
				}
			}

			// @Version
			if (field.hasAnnotationUsage(Version.class, builder.getModelsContext())) {
				code.append("\t@Version\n");
			}

			// @Basic
			if (field.hasAnnotationUsage(Basic.class, builder.getModelsContext())) {
				Basic basic = field.getAnnotationUsage(Basic.class, builder.getModelsContext());
				StringBuilder basicStr = new StringBuilder("\t@Basic(");
				boolean needsComma = false;
				if (basic.fetch() != FetchType.EAGER) {
					basicStr.append("fetch = FetchType.").append(basic.fetch().name());
					needsComma = true;
				}
				if (!basic.optional()) {
					if (needsComma) basicStr.append(", ");
					basicStr.append("optional = false");
					needsComma = true;
				}
				// If neither attribute was non-default, still show the relevant one
				if (!needsComma) {
					// Determine which attribute was explicitly set
					if (basic.fetch() == FetchType.EAGER) {
						basicStr.append("fetch = FetchType.EAGER");
					} else {
						basicStr.append("optional = true");
					}
				}
				basicStr.append(")\n");
				code.append(basicStr);
			}

			// @Temporal
			if (field.hasAnnotationUsage(Temporal.class, builder.getModelsContext())) {
				Temporal temporal = field.getAnnotationUsage(Temporal.class, builder.getModelsContext());
				code.append("\t@Temporal(TemporalType.").append(temporal.value().name()).append(")\n");
			}

			// @Lob
			if (field.hasAnnotationUsage(Lob.class, builder.getModelsContext())) {
				code.append("\t@Lob\n");
			}

			// @Column (basic fields only)
			if (field.hasAnnotationUsage(Column.class, builder.getModelsContext())) {
				Column col = field.getAnnotationUsage(Column.class, builder.getModelsContext());
				code.append("\t@Column(name = \"").append(col.name()).append("\")\n");
			}

			// Field declaration
			String rawTypeName = field.getType().determineRawClass().getName();
			String simpleType = getSimpleClassName(rawTypeName);
			if (field.isPlural()) {
				// For collections, include the generic type argument
				String elementType = getSimpleClassName(
					field.getType().asParameterizedType().getArguments().get(0).getName()
				);
				code.append("\tprivate ").append(simpleType).append("<").append(elementType).append(">")
					.append(" ").append(field.getName()).append(";\n\n");
			} else {
				code.append("\tprivate ").append(simpleType)
					.append(" ").append(field.getName()).append(";\n\n");
			}
		}

		code.append("}\n");
		return code.toString();
	}

	/**
	 * Generates a Java embeddable source code string from ClassDetails metadata.
	 */
	private String generateEmbeddableCode(ClassDetails embeddable, DynamicEntityBuilder builder) {
		StringBuilder code = new StringBuilder();
		code.append("package ").append(getPackageName(embeddable.getClassName())).append(";\n\n");
		code.append("import jakarta.persistence.*;\n\n");

		code.append("@Embeddable\n");
		code.append("public class ").append(embeddable.getName()).append(" {\n\n");

		for (FieldDetails field : embeddable.getFields()) {
			// @Column
			if (field.hasAnnotationUsage(Column.class, builder.getModelsContext())) {
				Column col = field.getAnnotationUsage(Column.class, builder.getModelsContext());
				code.append("\t@Column(name = \"").append(col.name()).append("\")\n");
			}

			String typeName = field.getType().determineRawClass().getName();
			code.append("\tprivate ").append(getSimpleClassName(typeName))
				.append(" ").append(field.getName()).append(";\n\n");
		}

		code.append("}\n");
		return code.toString();
	}

	// Helper methods
	private FieldDetails findField(List<FieldDetails> fields, String name) {
		return fields.stream()
			.filter(f -> f.getName().equals(name))
			.findFirst()
			.orElse(null);
	}

	private String getPackageName(String fullyQualifiedName) {
		int lastDot = fullyQualifiedName.lastIndexOf('.');
		return lastDot > 0 ? fullyQualifiedName.substring(0, lastDot) : "";
	}

	private String getSimpleClassName(String fullyQualifiedName) {
		// Handle JVM array type descriptors (e.g., "[B" for byte[])
		if (fullyQualifiedName.startsWith("[")) {
			String component = fullyQualifiedName.substring(1);
			switch (component) {
				case "B": return "byte[]";
				case "C": return "char[]";
				case "D": return "double[]";
				case "F": return "float[]";
				case "I": return "int[]";
				case "J": return "long[]";
				case "S": return "short[]";
				case "Z": return "boolean[]";
				default:
					// Object arrays: "[Ljava.lang.String;" -> "String[]"
					if (component.startsWith("L") && component.endsWith(";")) {
						return getSimpleClassName(component.substring(1, component.length() - 1)) + "[]";
					}
					return fullyQualifiedName;
			}
		}
		int lastDot = fullyQualifiedName.lastIndexOf('.');
		return lastDot > 0 ? fullyQualifiedName.substring(lastDot + 1) : fullyQualifiedName;
	}
}
