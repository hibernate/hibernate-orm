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

import java.util.Map;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDynamicComponentType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToOneType;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.ManyToOne;

public class HbmComponentBuilderTest {

	private HbmBuildContext ctx;
	private DynamicClassDetails entityClass;

	@BeforeEach
	public void setUp() {
		ctx = new HbmBuildContext();
		entityClass = new DynamicClassDetails(
				"Employee", "com.example.Employee",
				false, null, null, ctx.getModelsContext());
	}

	@Test
	public void testComponentCreatesEmbeddableClass() {
		JaxbHbmCompositeAttributeType component = new JaxbHbmCompositeAttributeType();
		component.setName("address");
		component.setClazz("Address");

		JaxbHbmBasicAttributeType street = new JaxbHbmBasicAttributeType();
		street.setName("street");
		street.setTypeAttribute("string");
		component.getAttributes().add(street);

		HbmComponentBuilder.processComponent(entityClass, component, "com.example", ctx);

		// Verify the embeddable class was registered
		ClassDetails embeddable = ctx.getModelsContext().getClassDetailsRegistry()
				.findClassDetails("com.example.Address");
		assertNotNull(embeddable, "Embeddable class should be registered");
		assertNotNull(embeddable.getAnnotationUsage(Embeddable.class, ctx.getModelsContext()),
				"Should have @Embeddable");

		// Verify the embeddable has the nested property
		FieldDetails streetField = embeddable.getFields().stream()
				.filter(f -> f.getName().equals("street"))
				.findFirst()
				.orElse(null);
		assertNotNull(streetField, "Embeddable should have 'street' field");
		Column col = streetField.getAnnotationUsage(Column.class, ctx.getModelsContext());
		assertNotNull(col);
	}

	@Test
	public void testComponentCreatesEmbeddedField() {
		JaxbHbmCompositeAttributeType component = new JaxbHbmCompositeAttributeType();
		component.setName("address");
		component.setClazz("Address");

		HbmComponentBuilder.processComponent(entityClass, component, "com.example", ctx);

		// Verify @Embedded field on the entity
		assertEquals(1, entityClass.getFields().size());
		FieldDetails field = entityClass.getFields().get(0);
		assertEquals("address", field.getName());
		assertEquals("Address", field.getType().getName());
		assertNotNull(field.getAnnotationUsage(Embedded.class, ctx.getModelsContext()));
	}

	@Test
	public void testComponentWithMultipleProperties() {
		JaxbHbmCompositeAttributeType component = new JaxbHbmCompositeAttributeType();
		component.setName("address");
		component.setClazz("Address");

		JaxbHbmBasicAttributeType street = new JaxbHbmBasicAttributeType();
		street.setName("street");
		street.setTypeAttribute("string");

		JaxbHbmBasicAttributeType city = new JaxbHbmBasicAttributeType();
		city.setName("city");
		city.setTypeAttribute("string");

		JaxbHbmBasicAttributeType zipCode = new JaxbHbmBasicAttributeType();
		zipCode.setName("zipCode");
		zipCode.setTypeAttribute("string");

		component.getAttributes().add(street);
		component.getAttributes().add(city);
		component.getAttributes().add(zipCode);

		HbmComponentBuilder.processComponent(entityClass, component, "com.example", ctx);

		ClassDetails embeddable = ctx.getModelsContext().getClassDetailsRegistry()
				.findClassDetails("com.example.Address");
		assertEquals(3, embeddable.getFields().size());
	}

	@Test
	public void testComponentWithManyToOne() {
		JaxbHbmCompositeAttributeType component = new JaxbHbmCompositeAttributeType();
		component.setName("address");
		component.setClazz("Address");

		JaxbHbmManyToOneType m2o = new JaxbHbmManyToOneType();
		m2o.setName("country");
		m2o.setClazz("Country");
		m2o.setColumnAttribute("COUNTRY_ID");

		component.getAttributes().add(m2o);

		HbmComponentBuilder.processComponent(entityClass, component, "com.example", ctx);

		ClassDetails embeddable = ctx.getModelsContext().getClassDetailsRegistry()
				.findClassDetails("com.example.Address");
		FieldDetails countryField = embeddable.getFields().stream()
				.filter(f -> f.getName().equals("country"))
				.findFirst()
				.orElse(null);
		assertNotNull(countryField);
		assertNotNull(countryField.getAnnotationUsage(ManyToOne.class, ctx.getModelsContext()));
	}

	@Test
	public void testComponentFullyQualifiedClass() {
		JaxbHbmCompositeAttributeType component = new JaxbHbmCompositeAttributeType();
		component.setName("address");
		component.setClazz("com.other.Address");

		HbmComponentBuilder.processComponent(entityClass, component, "com.example", ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertEquals("Address", field.getType().getName());
	}

	// --- Dynamic Component tests ---

	@Test
	public void testDynamicComponentCreatesMapField() {
		JaxbHbmDynamicComponentType dynComponent = new JaxbHbmDynamicComponentType();
		dynComponent.setName("attributes");

		HbmComponentBuilder.processDynamicComponent(entityClass, dynComponent, "com.example", ctx);

		assertEquals(1, entityClass.getFields().size());
		FieldDetails field = entityClass.getFields().get(0);
		assertEquals("attributes", field.getName());
		assertEquals(Map.class.getName(), field.getType().getName());
	}

	@Test
	public void testDynamicComponentProcessesNestedAttributes() {
		JaxbHbmDynamicComponentType dynComponent = new JaxbHbmDynamicComponentType();
		dynComponent.setName("extras");

		JaxbHbmBasicAttributeType prop = new JaxbHbmBasicAttributeType();
		prop.setName("color");
		prop.setTypeAttribute("string");
		dynComponent.getAttributes().add(prop);

		HbmComponentBuilder.processDynamicComponent(entityClass, dynComponent, "com.example", ctx);

		// Should have only the Map field (nested properties stored as meta attributes)
		assertEquals(1, entityClass.getFields().size());
		assertEquals("extras", entityClass.getFields().get(0).getName());

		// Nested properties are stored as meta attributes on the Map field
		var fieldMeta = ctx.getFieldMetaAttributes("com.example.Employee");
		assertNotNull(fieldMeta);
		var extrasMeta = fieldMeta.get("extras");
		assertNotNull(extrasMeta, "Field meta attributes should exist for 'extras'");
		assertTrue(extrasMeta.containsKey("hibernate.dynamic-component.property:color"));
		assertEquals("string", extrasMeta.get("hibernate.dynamic-component.property:color").get(0));
	}
}
