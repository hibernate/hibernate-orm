/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.builder.hbm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Integration tests for {@link HbmClassDetailsBuilder}: parses actual hbm.xml
 * files and verifies the resulting ClassDetails with JPA annotations.
 */
public class HbmClassDetailsBuilderTest {

	@TempDir
	Path tempDir;

	private HbmClassDetailsBuilder builder;

	@BeforeEach
	public void setUp() {
		builder = new HbmClassDetailsBuilder();
	}

	private File writeHbmFile(String fileName, String content) throws IOException {
		Path file = tempDir.resolve(fileName);
		Files.writeString(file, content);
		return file.toFile();
	}

	@Test
	public void testSimpleEntity() throws IOException {
		File hbm = writeHbmFile("Employee.hbm.xml", """
				<?xml version="1.0"?>
				<!DOCTYPE hibernate-mapping PUBLIC
				  "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
				  "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
				<hibernate-mapping package="com.example">
				  <class name="Employee" table="EMPLOYEES">
				    <id name="id" type="long" column="EMP_ID">
				      <generator class="identity"/>
				    </id>
				    <property name="name" type="string" column="EMP_NAME" not-null="true"/>
				    <property name="salary" type="big_decimal" column="SALARY"/>
				  </class>
				</hibernate-mapping>
				""");

		List<ClassDetails> entities = builder.buildFromFiles(hbm);
		assertEquals(1, entities.size());

		ClassDetails employee = entities.get(0);
		assertEquals("com.example.Employee", employee.getClassName());

		// @Entity
		Entity entity = employee.getAnnotationUsage(Entity.class, builder.getModelsContext());
		assertNotNull(entity);
		assertEquals("Employee", entity.name());

		// @Table
		Table table = employee.getAnnotationUsage(Table.class, builder.getModelsContext());
		assertNotNull(table);
		assertEquals("EMPLOYEES", table.name());

		// @Id with @GeneratedValue
		FieldDetails idField = findField(employee, "id");
		assertNotNull(idField);
		assertNotNull(idField.getAnnotationUsage(Id.class, builder.getModelsContext()));
		GeneratedValue gen = idField.getAnnotationUsage(GeneratedValue.class, builder.getModelsContext());
		assertNotNull(gen);
		assertEquals(GenerationType.IDENTITY, gen.strategy());

		// Properties
		FieldDetails nameField = findField(employee, "name");
		assertNotNull(nameField);
		Column nameCol = nameField.getAnnotationUsage(Column.class, builder.getModelsContext());
		assertNotNull(nameCol);
		assertEquals("EMP_NAME", nameCol.name());
		assertFalse(nameCol.nullable());

		FieldDetails salaryField = findField(employee, "salary");
		assertNotNull(salaryField);
	}

	@Test
	public void testEntityWithVersion() throws IOException {
		File hbm = writeHbmFile("Versioned.hbm.xml", """
				<?xml version="1.0"?>
				<!DOCTYPE hibernate-mapping PUBLIC
				  "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
				  "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
				<hibernate-mapping package="com.example">
				  <class name="Versioned" table="VERSIONED">
				    <id name="id" type="long">
				      <generator class="native"/>
				    </id>
				    <version name="version" column="OPT_LOCK"/>
				    <property name="data" type="string"/>
				  </class>
				</hibernate-mapping>
				""");

		List<ClassDetails> entities = builder.buildFromFiles(hbm);
		ClassDetails versioned = entities.get(0);

		FieldDetails versionField = findField(versioned, "version");
		assertNotNull(versionField);
		assertNotNull(versionField.getAnnotationUsage(Version.class, builder.getModelsContext()));
	}

	@Test
	public void testEntityWithTimestamp() throws IOException {
		File hbm = writeHbmFile("Timestamped.hbm.xml", """
				<?xml version="1.0"?>
				<!DOCTYPE hibernate-mapping PUBLIC
				  "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
				  "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
				<hibernate-mapping package="com.example">
				  <class name="Timestamped" table="TIMESTAMPED">
				    <id name="id" type="long">
				      <generator class="native"/>
				    </id>
				    <timestamp name="lastModified" column="LAST_MODIFIED"/>
				    <property name="data" type="string"/>
				  </class>
				</hibernate-mapping>
				""");

		List<ClassDetails> entities = builder.buildFromFiles(hbm);
		ClassDetails timestamped = entities.get(0);

		FieldDetails tsField = findField(timestamped, "lastModified");
		assertNotNull(tsField);
		assertNotNull(tsField.getAnnotationUsage(Version.class, builder.getModelsContext()));
		assertEquals("java.util.Date", tsField.getType().getName());
	}

	@Test
	public void testEntityWithManyToOne() throws IOException {
		File hbm = writeHbmFile("Employee.hbm.xml", """
				<?xml version="1.0"?>
				<!DOCTYPE hibernate-mapping PUBLIC
				  "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
				  "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
				<hibernate-mapping package="com.example">
				  <class name="Employee" table="EMPLOYEES">
				    <id name="id" type="long">
				      <generator class="identity"/>
				    </id>
				    <many-to-one name="department" class="Department" column="DEPT_ID"/>
				  </class>
				</hibernate-mapping>
				""");

		List<ClassDetails> entities = builder.buildFromFiles(hbm);
		ClassDetails employee = entities.get(0);

		FieldDetails deptField = findField(employee, "department");
		assertNotNull(deptField);
		assertNotNull(deptField.getAnnotationUsage(ManyToOne.class, builder.getModelsContext()));
	}

	@Test
	public void testEntityWithOneToManySet() throws IOException {
		File hbm = writeHbmFile("Department.hbm.xml", """
				<?xml version="1.0"?>
				<!DOCTYPE hibernate-mapping PUBLIC
				  "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
				  "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
				<hibernate-mapping package="com.example">
				  <class name="Department" table="DEPARTMENTS">
				    <id name="id" type="long">
				      <generator class="identity"/>
				    </id>
				    <property name="name" type="string"/>
				    <set name="employees">
				      <key column="DEPT_ID"/>
				      <one-to-many class="Employee"/>
				    </set>
				  </class>
				</hibernate-mapping>
				""");

		List<ClassDetails> entities = builder.buildFromFiles(hbm);
		ClassDetails department = entities.get(0);

		FieldDetails empField = findField(department, "employees");
		assertNotNull(empField);
		assertTrue(empField.isPlural());
		assertNotNull(empField.getAnnotationUsage(OneToMany.class, builder.getModelsContext()));
	}

	@Test
	public void testEntityWithComponent() throws IOException {
		File hbm = writeHbmFile("Person.hbm.xml", """
				<?xml version="1.0"?>
				<!DOCTYPE hibernate-mapping PUBLIC
				  "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
				  "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
				<hibernate-mapping package="com.example">
				  <class name="Person" table="PERSONS">
				    <id name="id" type="long">
				      <generator class="identity"/>
				    </id>
				    <component name="address" class="Address">
				      <property name="street" type="string"/>
				      <property name="city" type="string"/>
				    </component>
				  </class>
				</hibernate-mapping>
				""");

		List<ClassDetails> entities = builder.buildFromFiles(hbm);
		ClassDetails person = entities.get(0);

		FieldDetails addressField = findField(person, "address");
		assertNotNull(addressField);
		assertNotNull(addressField.getAnnotationUsage(Embedded.class, builder.getModelsContext()));

		// Verify the embeddable class
		ClassDetails addressClass = builder.getModelsContext().getClassDetailsRegistry()
				.findClassDetails("com.example.Address");
		assertNotNull(addressClass);
		assertNotNull(addressClass.getAnnotationUsage(Embeddable.class, builder.getModelsContext()));
		assertEquals(2, addressClass.getFields().size());
	}

	@Test
	public void testSingleTableInheritance() throws IOException {
		File hbm = writeHbmFile("Vehicle.hbm.xml", """
				<?xml version="1.0"?>
				<!DOCTYPE hibernate-mapping PUBLIC
				  "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
				  "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
				<hibernate-mapping package="com.example">
				  <class name="Vehicle" table="VEHICLES" discriminator-value="VEHICLE">
				    <id name="id" type="long">
				      <generator class="identity"/>
				    </id>
				    <discriminator column="VEHICLE_TYPE" type="string"/>
				    <property name="name" type="string"/>
				    <subclass name="Car" discriminator-value="CAR">
				      <property name="doorCount" type="int"/>
				    </subclass>
				    <subclass name="Truck" discriminator-value="TRUCK">
				      <property name="payloadCapacity" type="double"/>
				    </subclass>
				  </class>
				</hibernate-mapping>
				""");

		List<ClassDetails> entities = builder.buildFromFiles(hbm);
		ClassDetails vehicle = entities.get(0);

		// Root
		Inheritance inheritance = vehicle.getAnnotationUsage(
				Inheritance.class, builder.getModelsContext());
		assertNotNull(inheritance);
		assertEquals(InheritanceType.SINGLE_TABLE, inheritance.strategy());

		DiscriminatorColumn discCol = vehicle.getAnnotationUsage(
				DiscriminatorColumn.class, builder.getModelsContext());
		assertNotNull(discCol);
		assertEquals("VEHICLE_TYPE", discCol.name());

		DiscriminatorValue rootDiscVal = vehicle.getAnnotationUsage(
				DiscriminatorValue.class, builder.getModelsContext());
		assertNotNull(rootDiscVal);
		assertEquals("VEHICLE", rootDiscVal.value());

		// Subclasses
		ClassDetails car = builder.getModelsContext().getClassDetailsRegistry()
				.findClassDetails("com.example.Car");
		assertNotNull(car);
		DiscriminatorValue carDiscVal = car.getAnnotationUsage(
				DiscriminatorValue.class, builder.getModelsContext());
		assertNotNull(carDiscVal);
		assertEquals("CAR", carDiscVal.value());

		ClassDetails truck = builder.getModelsContext().getClassDetailsRegistry()
				.findClassDetails("com.example.Truck");
		assertNotNull(truck);
	}

	@Test
	public void testMultipleFiles() throws IOException {
		File hbm1 = writeHbmFile("Employee.hbm.xml", """
				<?xml version="1.0"?>
				<!DOCTYPE hibernate-mapping PUBLIC
				  "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
				  "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
				<hibernate-mapping package="com.example">
				  <class name="Employee" table="EMPLOYEES">
				    <id name="id" type="long">
				      <generator class="identity"/>
				    </id>
				    <property name="name" type="string"/>
				  </class>
				</hibernate-mapping>
				""");

		File hbm2 = writeHbmFile("Department.hbm.xml", """
				<?xml version="1.0"?>
				<!DOCTYPE hibernate-mapping PUBLIC
				  "-//Hibernate/Hibernate Mapping DTD 3.0//EN"
				  "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
				<hibernate-mapping package="com.example">
				  <class name="Department" table="DEPARTMENTS">
				    <id name="id" type="long">
				      <generator class="identity"/>
				    </id>
				    <property name="name" type="string"/>
				  </class>
				</hibernate-mapping>
				""");

		List<ClassDetails> entities = builder.buildFromFiles(hbm1, hbm2);
		assertEquals(2, entities.size());
	}

	private FieldDetails findField(ClassDetails classDetails, String name) {
		return classDetails.getFields().stream()
				.filter(f -> f.getName().equals(name))
				.findFirst()
				.orElse(null);
	}
}
