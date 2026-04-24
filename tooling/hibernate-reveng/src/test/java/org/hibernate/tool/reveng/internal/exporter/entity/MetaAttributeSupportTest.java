/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.tool.reveng.internal.builder.db.DynamicEntityBuilder;
import org.hibernate.tool.reveng.internal.descriptor.ColumnDescriptor;
import org.hibernate.tool.reveng.internal.descriptor.TableDescriptor;
import org.junit.jupiter.api.Test;

class MetaAttributeSupportTest {

	private FieldDetails getField(ClassDetails classDetails, String name) {
		for (FieldDetails field : classDetails.getFields()) {
			if (field.getName().equals(name)) {
				return field;
			}
		}
		throw new IllegalArgumentException("Field not found: " + name);
	}

	private ClassDetails createEntity() {
		TableDescriptor table = new TableDescriptor("EMPLOYEE", "Employee", "com.example");
		table.addColumn(new ColumnDescriptor("ID", "id", Long.class).primaryKey(true));
		table.addColumn(new ColumnDescriptor("NAME", "name", String.class));
		table.addColumn(new ColumnDescriptor("ACTIVE", "active", boolean.class));
		return new DynamicEntityBuilder().createEntityFromTable(table);
	}

	// --- Null-safe construction ---

	@Test
	void testConstructorHandlesNullMaps() {
		MetaAttributeSupport support = new MetaAttributeSupport(null, null);
		assertFalse(support.hasClassMetaAttribute("anything"));
		assertEquals("", support.getClassMetaAttribute("anything"));
	}

	// --- Class meta-attribute lookups ---

	@Test
	void testHasClassMetaAttribute() {
		MetaAttributeSupport support = new MetaAttributeSupport(
				Map.of("class-description", List.of("A test entity")),
				Collections.emptyMap());
		assertTrue(support.hasClassMetaAttribute("class-description"));
		assertFalse(support.hasClassMetaAttribute("nonexistent"));
	}

	@Test
	void testGetClassMetaAttributeSingleValue() {
		MetaAttributeSupport support = new MetaAttributeSupport(
				Map.of("class-description", List.of("A test entity")),
				Collections.emptyMap());
		assertEquals("A test entity", support.getClassMetaAttribute("class-description"));
	}

	@Test
	void testGetClassMetaAttributeMultipleValues() {
		MetaAttributeSupport support = new MetaAttributeSupport(
				Map.of("class-description", List.of("Line 1", "Line 2")),
				Collections.emptyMap());
		assertEquals("Line 1\nLine 2", support.getClassMetaAttribute("class-description"));
	}

	@Test
	void testGetClassMetaAttributeMissing() {
		MetaAttributeSupport support = new MetaAttributeSupport(
				Collections.emptyMap(), Collections.emptyMap());
		assertEquals("", support.getClassMetaAttribute("nonexistent"));
	}

	@Test
	void testGetClassMetaAttributeValues() {
		MetaAttributeSupport support = new MetaAttributeSupport(
				Map.of("implements", List.of("com.example.Auditable", "java.io.Serializable")),
				Collections.emptyMap());
		List<String> values = support.getClassMetaAttributeValues("implements");
		assertEquals(2, values.size());
		assertEquals("com.example.Auditable", values.get(0));
	}

	@Test
	void testGetClassMetaAttributeValuesEmpty() {
		MetaAttributeSupport support = new MetaAttributeSupport(
				Collections.emptyMap(), Collections.emptyMap());
		assertTrue(support.getClassMetaAttributeValues("nonexistent").isEmpty());
	}

	// --- Field meta-attribute lookups ---

	@Test
	void testHasFieldMetaAttribute() {
		ClassDetails entity = createEntity();
		FieldDetails nameField = getField(entity, "name");
		MetaAttributeSupport support = new MetaAttributeSupport(
				Collections.emptyMap(),
				Map.of("name", Map.of("field-description", List.of("Employee name"))));
		assertTrue(support.hasFieldMetaAttribute(nameField, "field-description"));
		assertFalse(support.hasFieldMetaAttribute(nameField, "nonexistent"));
	}

	@Test
	void testGetFieldMetaAsBoolPresent() {
		ClassDetails entity = createEntity();
		FieldDetails nameField = getField(entity, "name");
		MetaAttributeSupport support = new MetaAttributeSupport(
				Collections.emptyMap(),
				Map.of("name", Map.of("gen-property", List.of("false"))));
		assertFalse(support.getFieldMetaAsBool(nameField, "gen-property", true));
	}

	@Test
	void testGetFieldMetaAsBoolDefault() {
		ClassDetails entity = createEntity();
		FieldDetails nameField = getField(entity, "name");
		MetaAttributeSupport support = new MetaAttributeSupport(
				Collections.emptyMap(), Collections.emptyMap());
		assertTrue(support.getFieldMetaAsBool(nameField, "gen-property", true));
	}

	@Test
	void testGetFieldMetaAttribute() {
		ClassDetails entity = createEntity();
		FieldDetails nameField = getField(entity, "name");
		MetaAttributeSupport support = new MetaAttributeSupport(
				Collections.emptyMap(),
				Map.of("name", Map.of("field-description", List.of("The name"))));
		assertEquals("The name", support.getFieldMetaAttribute(nameField, "field-description"));
	}

	@Test
	void testGetFieldMetaAttributeMissing() {
		ClassDetails entity = createEntity();
		FieldDetails nameField = getField(entity, "name");
		MetaAttributeSupport support = new MetaAttributeSupport(
				Collections.emptyMap(), Collections.emptyMap());
		assertEquals("", support.getFieldMetaAttribute(nameField, "nonexistent"));
	}

	// --- Field modifiers ---

	@Test
	void testGetFieldModifiersDefault() {
		ClassDetails entity = createEntity();
		FieldDetails nameField = getField(entity, "name");
		MetaAttributeSupport support = new MetaAttributeSupport(
				Collections.emptyMap(), Collections.emptyMap());
		assertEquals("private", support.getFieldModifiers(nameField));
	}

	@Test
	void testGetFieldModifiersCustom() {
		ClassDetails entity = createEntity();
		FieldDetails nameField = getField(entity, "name");
		MetaAttributeSupport support = new MetaAttributeSupport(
				Collections.emptyMap(),
				Map.of("name", Map.of("scope-field", List.of("protected"))));
		assertEquals("protected", support.getFieldModifiers(nameField));
	}

	@Test
	void testGetPropertyGetModifiersDefault() {
		ClassDetails entity = createEntity();
		FieldDetails nameField = getField(entity, "name");
		MetaAttributeSupport support = new MetaAttributeSupport(
				Collections.emptyMap(), Collections.emptyMap());
		assertEquals("public", support.getPropertyGetModifiers(nameField));
	}

	@Test
	void testGetPropertyGetModifiersCustom() {
		ClassDetails entity = createEntity();
		FieldDetails nameField = getField(entity, "name");
		MetaAttributeSupport support = new MetaAttributeSupport(
				Collections.emptyMap(),
				Map.of("name", Map.of("scope-get", List.of("protected"))));
		assertEquals("protected", support.getPropertyGetModifiers(nameField));
	}

	@Test
	void testGetPropertySetModifiersDefault() {
		ClassDetails entity = createEntity();
		FieldDetails nameField = getField(entity, "name");
		MetaAttributeSupport support = new MetaAttributeSupport(
				Collections.emptyMap(), Collections.emptyMap());
		assertEquals("public", support.getPropertySetModifiers(nameField));
	}

	@Test
	void testGetPropertySetModifiersCustom() {
		ClassDetails entity = createEntity();
		FieldDetails nameField = getField(entity, "name");
		MetaAttributeSupport support = new MetaAttributeSupport(
				Collections.emptyMap(),
				Map.of("name", Map.of("scope-set", List.of("private"))));
		assertEquals("private", support.getPropertySetModifiers(nameField));
	}

	// --- Class-level convenience ---

	@Test
	void testHasClassDescription() {
		MetaAttributeSupport support = new MetaAttributeSupport(
				Map.of("class-description", List.of("Describes the entity")),
				Collections.emptyMap());
		assertTrue(support.hasClassDescription());
	}

	@Test
	void testGetClassDescription() {
		MetaAttributeSupport support = new MetaAttributeSupport(
				Map.of("class-description", List.of("Describes the entity")),
				Collections.emptyMap());
		assertEquals("Describes the entity", support.getClassDescription());
	}

	@Test
	void testHasExtraClassCode() {
		MetaAttributeSupport support = new MetaAttributeSupport(
				Map.of("class-code", List.of("// extra code")),
				Collections.emptyMap());
		assertTrue(support.hasExtraClassCode());
	}

	@Test
	void testGetExtraClassCode() {
		MetaAttributeSupport support = new MetaAttributeSupport(
				Map.of("class-code", List.of("// extra code")),
				Collections.emptyMap());
		assertEquals("// extra code", support.getExtraClassCode());
	}

	// --- Field-level convenience ---

	@Test
	void testIsGenPropertyDefault() {
		ClassDetails entity = createEntity();
		FieldDetails nameField = getField(entity, "name");
		MetaAttributeSupport support = new MetaAttributeSupport(
				Collections.emptyMap(), Collections.emptyMap());
		assertTrue(support.isGenProperty(nameField));
	}

	@Test
	void testIsGenPropertyFalse() {
		ClassDetails entity = createEntity();
		FieldDetails nameField = getField(entity, "name");
		MetaAttributeSupport support = new MetaAttributeSupport(
				Collections.emptyMap(),
				Map.of("name", Map.of("gen-property", List.of("false"))));
		assertFalse(support.isGenProperty(nameField));
	}

	@Test
	void testHasFieldDescription() {
		ClassDetails entity = createEntity();
		FieldDetails nameField = getField(entity, "name");
		MetaAttributeSupport support = new MetaAttributeSupport(
				Collections.emptyMap(),
				Map.of("name", Map.of("field-description", List.of("The name"))));
		assertTrue(support.hasFieldDescription(nameField));
	}

	@Test
	void testGetFieldDescription() {
		ClassDetails entity = createEntity();
		FieldDetails nameField = getField(entity, "name");
		MetaAttributeSupport support = new MetaAttributeSupport(
				Collections.emptyMap(),
				Map.of("name", Map.of("field-description", List.of("The name"))));
		assertEquals("The name", support.getFieldDescription(nameField));
	}

	@Test
	void testHasFieldDefaultValue() {
		ClassDetails entity = createEntity();
		FieldDetails nameField = getField(entity, "name");
		MetaAttributeSupport support = new MetaAttributeSupport(
				Collections.emptyMap(),
				Map.of("name", Map.of("default-value", List.of("\"unknown\""))));
		assertTrue(support.hasFieldDefaultValue(nameField));
	}

	@Test
	void testGetFieldDefaultValue() {
		ClassDetails entity = createEntity();
		FieldDetails nameField = getField(entity, "name");
		MetaAttributeSupport support = new MetaAttributeSupport(
				Collections.emptyMap(),
				Map.of("name", Map.of("default-value", List.of("\"unknown\""))));
		assertEquals("\"unknown\"", support.getFieldDefaultValue(nameField));
	}
}
