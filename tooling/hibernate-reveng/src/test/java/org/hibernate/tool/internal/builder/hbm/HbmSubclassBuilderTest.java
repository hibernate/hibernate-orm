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
package org.hibernate.tool.internal.builder.hbm;

import static org.junit.jupiter.api.Assertions.*;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDiscriminatorSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmEntityDiscriminatorType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmJoinedSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmUnionSubclassEntityType;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

public class HbmSubclassBuilderTest {

	private HbmBuildContext ctx;
	private DynamicClassDetails rootEntity;

	@BeforeEach
	public void setUp() {
		ctx = new HbmBuildContext();
		rootEntity = new DynamicClassDetails(
				"Vehicle", "com.example.Vehicle",
				false, null, null, ctx.getModelsContext());
	}

	// --- Single-table (discriminator) inheritance ---

	@Test
	public void testSingleTableInheritance() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setName("Vehicle");

		JaxbHbmEntityDiscriminatorType discriminator = new JaxbHbmEntityDiscriminatorType();
		discriminator.setColumnAttribute("VEHICLE_TYPE");
		discriminator.setType("string");
		entityType.setDiscriminator(discriminator);
		entityType.setDiscriminatorValue("VEHICLE");

		JaxbHbmDiscriminatorSubclassEntityType car = new JaxbHbmDiscriminatorSubclassEntityType();
		car.setName("Car");
		car.setDiscriminatorValue("CAR");
		entityType.getSubclass().add(car);

		HbmSubclassBuilder.processSubclasses(rootEntity, entityType, "com.example", ctx);

		// Root entity should have @Inheritance(SINGLE_TABLE)
		Inheritance inheritance = rootEntity.getAnnotationUsage(
				Inheritance.class, ctx.getModelsContext());
		assertNotNull(inheritance);
		assertEquals(InheritanceType.SINGLE_TABLE, inheritance.strategy());

		// Root should have @DiscriminatorColumn
		DiscriminatorColumn discCol = rootEntity.getAnnotationUsage(
				DiscriminatorColumn.class, ctx.getModelsContext());
		assertNotNull(discCol);
		assertEquals("VEHICLE_TYPE", discCol.name());

		// Root should have @DiscriminatorValue
		DiscriminatorValue rootDiscVal = rootEntity.getAnnotationUsage(
				DiscriminatorValue.class, ctx.getModelsContext());
		assertNotNull(rootDiscVal);
		assertEquals("VEHICLE", rootDiscVal.value());

		// Subclass should be registered
		ClassDetails carClass = ctx.getModelsContext().getClassDetailsRegistry()
				.findClassDetails("com.example.Car");
		assertNotNull(carClass, "Car subclass should be registered");
		assertNotNull(carClass.getAnnotationUsage(Entity.class, ctx.getModelsContext()));

		DiscriminatorValue carDiscVal = carClass.getAnnotationUsage(
				DiscriminatorValue.class, ctx.getModelsContext());
		assertNotNull(carDiscVal);
		assertEquals("CAR", carDiscVal.value());
	}

	@Test
	public void testSingleTableSubclassHasAttributes() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setName("Vehicle");

		JaxbHbmDiscriminatorSubclassEntityType car = new JaxbHbmDiscriminatorSubclassEntityType();
		car.setName("Car");
		car.setDiscriminatorValue("CAR");

		JaxbHbmBasicAttributeType doorCount = new JaxbHbmBasicAttributeType();
		doorCount.setName("doorCount");
		doorCount.setTypeAttribute("int");
		car.getAttributes().add(doorCount);

		entityType.getSubclass().add(car);

		HbmSubclassBuilder.processSubclasses(rootEntity, entityType, "com.example", ctx);

		ClassDetails carClass = ctx.getModelsContext().getClassDetailsRegistry()
				.findClassDetails("com.example.Car");
		assertEquals(1, carClass.getFields().size());
		assertEquals("doorCount", carClass.getFields().get(0).getName());
		assertNotNull(carClass.getFields().get(0).getAnnotationUsage(
				Column.class, ctx.getModelsContext()));
	}

	// --- Joined inheritance ---

	@Test
	public void testJoinedInheritance() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setName("Vehicle");

		JaxbHbmJoinedSubclassEntityType car = new JaxbHbmJoinedSubclassEntityType();
		car.setName("Car");
		car.setTable("CARS");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("VEHICLE_ID");
		car.setKey(key);
		entityType.getJoinedSubclass().add(car);

		HbmSubclassBuilder.processSubclasses(rootEntity, entityType, "com.example", ctx);

		// Root should have @Inheritance(JOINED)
		Inheritance inheritance = rootEntity.getAnnotationUsage(
				Inheritance.class, ctx.getModelsContext());
		assertNotNull(inheritance);
		assertEquals(InheritanceType.JOINED, inheritance.strategy());

		// Subclass should have @Table and @PrimaryKeyJoinColumn
		ClassDetails carClass = ctx.getModelsContext().getClassDetailsRegistry()
				.findClassDetails("com.example.Car");
		assertNotNull(carClass);
		assertNotNull(carClass.getAnnotationUsage(Entity.class, ctx.getModelsContext()));

		Table table = carClass.getAnnotationUsage(Table.class, ctx.getModelsContext());
		assertNotNull(table);
		assertEquals("CARS", table.name());

		PrimaryKeyJoinColumn pkJoinCol = carClass.getAnnotationUsage(
				PrimaryKeyJoinColumn.class, ctx.getModelsContext());
		assertNotNull(pkJoinCol);
		assertEquals("VEHICLE_ID", pkJoinCol.name());
	}

	@Test
	public void testJoinedSubclassWithSchemaAndCatalog() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setName("Vehicle");

		JaxbHbmJoinedSubclassEntityType car = new JaxbHbmJoinedSubclassEntityType();
		car.setName("Car");
		car.setTable("CARS");
		car.setSchema("HR");
		car.setCatalog("MY_CATALOG");
		JaxbHbmKeyType key = new JaxbHbmKeyType();
		key.setColumnAttribute("VEHICLE_ID");
		car.setKey(key);
		entityType.getJoinedSubclass().add(car);

		HbmSubclassBuilder.processSubclasses(rootEntity, entityType, "com.example", ctx);

		ClassDetails carClass = ctx.getModelsContext().getClassDetailsRegistry()
				.findClassDetails("com.example.Car");
		Table table = carClass.getAnnotationUsage(Table.class, ctx.getModelsContext());
		assertEquals("HR", table.schema());
		assertEquals("MY_CATALOG", table.catalog());
	}

	// --- Union (table-per-class) inheritance ---

	@Test
	public void testUnionInheritance() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setName("Vehicle");

		JaxbHbmUnionSubclassEntityType car = new JaxbHbmUnionSubclassEntityType();
		car.setName("Car");
		car.setTable("CARS");
		entityType.getUnionSubclass().add(car);

		HbmSubclassBuilder.processSubclasses(rootEntity, entityType, "com.example", ctx);

		// Root should have @Inheritance(TABLE_PER_CLASS)
		Inheritance inheritance = rootEntity.getAnnotationUsage(
				Inheritance.class, ctx.getModelsContext());
		assertNotNull(inheritance);
		assertEquals(InheritanceType.TABLE_PER_CLASS, inheritance.strategy());

		// Subclass should have @Table
		ClassDetails carClass = ctx.getModelsContext().getClassDetailsRegistry()
				.findClassDetails("com.example.Car");
		assertNotNull(carClass);

		Table table = carClass.getAnnotationUsage(Table.class, ctx.getModelsContext());
		assertNotNull(table);
		assertEquals("CARS", table.name());
	}

	@Test
	public void testUnionSubclassHasAttributes() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setName("Vehicle");

		JaxbHbmUnionSubclassEntityType car = new JaxbHbmUnionSubclassEntityType();
		car.setName("Car");
		car.setTable("CARS");

		JaxbHbmBasicAttributeType doorCount = new JaxbHbmBasicAttributeType();
		doorCount.setName("doorCount");
		doorCount.setTypeAttribute("int");
		car.getAttributes().add(doorCount);

		entityType.getUnionSubclass().add(car);

		HbmSubclassBuilder.processSubclasses(rootEntity, entityType, "com.example", ctx);

		ClassDetails carClass = ctx.getModelsContext().getClassDetailsRegistry()
				.findClassDetails("com.example.Car");
		assertEquals(1, carClass.getFields().size());
		assertEquals("doorCount", carClass.getFields().get(0).getName());
	}

	// --- No subclasses ---

	@Test
	public void testNoSubclasses() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setName("Vehicle");

		HbmSubclassBuilder.processSubclasses(rootEntity, entityType, "com.example", ctx);

		// Should not add @Inheritance annotation
		assertNull(rootEntity.getAnnotationUsage(Inheritance.class, ctx.getModelsContext()));
	}

	// --- Nested subclasses ---

	@Test
	public void testNestedDiscriminatorSubclasses() {
		JaxbHbmRootEntityType entityType = new JaxbHbmRootEntityType();
		entityType.setName("Vehicle");

		JaxbHbmDiscriminatorSubclassEntityType car = new JaxbHbmDiscriminatorSubclassEntityType();
		car.setName("Car");
		car.setDiscriminatorValue("CAR");

		JaxbHbmDiscriminatorSubclassEntityType sportsCar = new JaxbHbmDiscriminatorSubclassEntityType();
		sportsCar.setName("SportsCar");
		sportsCar.setDiscriminatorValue("SPORTS_CAR");
		car.getSubclass().add(sportsCar);

		entityType.getSubclass().add(car);

		HbmSubclassBuilder.processSubclasses(rootEntity, entityType, "com.example", ctx);

		ClassDetails sportsCarClass = ctx.getModelsContext().getClassDetailsRegistry()
				.findClassDetails("com.example.SportsCar");
		assertNotNull(sportsCarClass, "Nested subclass should be registered");
		assertNotNull(sportsCarClass.getAnnotationUsage(Entity.class, ctx.getModelsContext()));

		DiscriminatorValue discVal = sportsCarClass.getAnnotationUsage(
				DiscriminatorValue.class, ctx.getModelsContext());
		assertNotNull(discVal);
		assertEquals("SPORTS_CAR", discVal.value());
	}
}
