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
package org.hibernate.tool.reveng.internal.builder.hbm;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTimestampAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmVersionAttributeType;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

public class HbmPropertyBuilderTest {

	private HbmBuildContext ctx;
	private DynamicClassDetails entityClass;

	@BeforeEach
	public void setUp() {
		ctx = new HbmBuildContext();
		entityClass = new DynamicClassDetails(
				"Employee", "com.example.Employee",
				false, null, null, ctx.getModelsContext());
	}

	// --- Property tests ---

	@Test
	public void testSimpleProperty() {
		JaxbHbmBasicAttributeType attr = new JaxbHbmBasicAttributeType();
		attr.setName("name");
		attr.setTypeAttribute("string");
		attr.setColumnAttribute("EMP_NAME");

		HbmPropertyBuilder.processProperty(entityClass, attr, ctx);

		List<FieldDetails> fields = entityClass.getFields();
		assertEquals(1, fields.size());
		FieldDetails field = fields.get(0);
		assertEquals("name", field.getName());
		assertEquals("java.lang.String", field.getType().getName());

		Column col = field.getAnnotationUsage(Column.class, ctx.getModelsContext());
		assertNotNull(col);
		assertEquals("EMP_NAME", col.name());
	}

	@Test
	public void testPropertyDefaultType() {
		JaxbHbmBasicAttributeType attr = new JaxbHbmBasicAttributeType();
		attr.setName("description");
		// No type set — should default to "string"

		HbmPropertyBuilder.processProperty(entityClass, attr, ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertEquals("java.lang.String", field.getType().getName());
	}

	@Test
	public void testPropertyColumnDefaultsToFieldName() {
		JaxbHbmBasicAttributeType attr = new JaxbHbmBasicAttributeType();
		attr.setName("age");
		attr.setTypeAttribute("int");
		// No column set — should default to field name

		HbmPropertyBuilder.processProperty(entityClass, attr, ctx);

		FieldDetails field = entityClass.getFields().get(0);
		Column col = field.getAnnotationUsage(Column.class, ctx.getModelsContext());
		assertNotNull(col);
		assertEquals("age", col.name());
	}

	@Test
	public void testPropertyNotNull() {
		JaxbHbmBasicAttributeType attr = new JaxbHbmBasicAttributeType();
		attr.setName("email");
		attr.setTypeAttribute("string");
		attr.setNotNull(true);

		HbmPropertyBuilder.processProperty(entityClass, attr, ctx);

		FieldDetails field = entityClass.getFields().get(0);
		Column col = field.getAnnotationUsage(Column.class, ctx.getModelsContext());
		assertNotNull(col);
		assertFalse(col.nullable());
	}

	@Test
	public void testPropertyWithLength() {
		JaxbHbmBasicAttributeType attr = new JaxbHbmBasicAttributeType();
		attr.setName("name");
		attr.setTypeAttribute("string");
		attr.setLength(255);

		HbmPropertyBuilder.processProperty(entityClass, attr, ctx);

		FieldDetails field = entityClass.getFields().get(0);
		Column col = field.getAnnotationUsage(Column.class, ctx.getModelsContext());
		assertNotNull(col);
		assertEquals(255, col.length());
	}

	@Test
	public void testPropertyUnique() {
		JaxbHbmBasicAttributeType attr = new JaxbHbmBasicAttributeType();
		attr.setName("email");
		attr.setTypeAttribute("string");
		attr.setUnique(true);

		HbmPropertyBuilder.processProperty(entityClass, attr, ctx);

		FieldDetails field = entityClass.getFields().get(0);
		Column col = field.getAnnotationUsage(Column.class, ctx.getModelsContext());
		assertNotNull(col);
		assertTrue(col.unique());
	}

	@Test
	public void testMultipleProperties() {
		JaxbHbmBasicAttributeType attr1 = new JaxbHbmBasicAttributeType();
		attr1.setName("firstName");
		attr1.setTypeAttribute("string");

		JaxbHbmBasicAttributeType attr2 = new JaxbHbmBasicAttributeType();
		attr2.setName("lastName");
		attr2.setTypeAttribute("string");

		HbmPropertyBuilder.processProperty(entityClass, attr1, ctx);
		HbmPropertyBuilder.processProperty(entityClass, attr2, ctx);

		assertEquals(2, entityClass.getFields().size());
	}

	// --- Version tests ---

	@Test
	public void testVersion() {
		JaxbHbmVersionAttributeType version = new JaxbHbmVersionAttributeType();
		version.setName("version");
		version.setColumnAttribute("OPT_LOCK");

		HbmPropertyBuilder.processVersion(entityClass, version, ctx);

		List<FieldDetails> fields = entityClass.getFields();
		assertEquals(1, fields.size());
		FieldDetails field = fields.get(0);
		assertEquals("version", field.getName());

		assertNotNull(field.getAnnotationUsage(Version.class, ctx.getModelsContext()));

		Column col = field.getAnnotationUsage(Column.class, ctx.getModelsContext());
		assertNotNull(col);
		assertEquals("OPT_LOCK", col.name());
	}

	@Test
	public void testVersionDefaultType() {
		JaxbHbmVersionAttributeType version = new JaxbHbmVersionAttributeType();
		version.setName("version");
		// No type — should default to "integer" → java.lang.Integer

		HbmPropertyBuilder.processVersion(entityClass, version, ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertEquals("java.lang.Integer", field.getType().getName());
	}

	@Test
	public void testVersionWithLongType() {
		JaxbHbmVersionAttributeType version = new JaxbHbmVersionAttributeType();
		version.setName("version");
		version.setType("long");

		HbmPropertyBuilder.processVersion(entityClass, version, ctx);

		FieldDetails field = entityClass.getFields().get(0);
		assertEquals("long", field.getType().getName());
	}

	@Test
	public void testVersionNull() {
		HbmPropertyBuilder.processVersion(entityClass, null, ctx);
		assertTrue(entityClass.getFields().isEmpty());
	}

	// --- Timestamp tests ---

	@Test
	public void testTimestamp() {
		JaxbHbmTimestampAttributeType timestamp = new JaxbHbmTimestampAttributeType();
		timestamp.setName("lastModified");
		timestamp.setColumnAttribute("LAST_MODIFIED");

		HbmPropertyBuilder.processTimestamp(entityClass, timestamp, ctx);

		List<FieldDetails> fields = entityClass.getFields();
		assertEquals(1, fields.size());
		FieldDetails field = fields.get(0);
		assertEquals("lastModified", field.getName());
		assertEquals("java.util.Date", field.getType().getName());

		assertNotNull(field.getAnnotationUsage(Version.class, ctx.getModelsContext()));

		Column col = field.getAnnotationUsage(Column.class, ctx.getModelsContext());
		assertNotNull(col);
		assertEquals("LAST_MODIFIED", col.name());
	}

	@Test
	public void testTimestampDefaultColumn() {
		JaxbHbmTimestampAttributeType timestamp = new JaxbHbmTimestampAttributeType();
		timestamp.setName("createdAt");
		// No column — should default to field name

		HbmPropertyBuilder.processTimestamp(entityClass, timestamp, ctx);

		FieldDetails field = entityClass.getFields().get(0);
		Column col = field.getAnnotationUsage(Column.class, ctx.getModelsContext());
		assertNotNull(col);
		assertEquals("createdAt", col.name());
	}

	@Test
	public void testTimestampNull() {
		HbmPropertyBuilder.processTimestamp(entityClass, null, ctx);
		assertTrue(entityClass.getFields().isEmpty());
	}

	// --- NaturalId tests ---

	@Test
	public void testMarkNaturalIdFields() {
		// Add two regular properties first
		JaxbHbmBasicAttributeType attr1 = new JaxbHbmBasicAttributeType();
		attr1.setName("id");
		attr1.setTypeAttribute("long");
		HbmPropertyBuilder.processProperty(entityClass, attr1, ctx);

		int fieldCountBefore = entityClass.getFields().size();

		// Add natural-id properties
		JaxbHbmBasicAttributeType attr2 = new JaxbHbmBasicAttributeType();
		attr2.setName("ssn");
		attr2.setTypeAttribute("string");
		HbmPropertyBuilder.processProperty(entityClass, attr2, ctx);

		HbmPropertyBuilder.markNaturalIdFields(entityClass,
				fieldCountBefore, false, ctx);

		// "id" should NOT have @NaturalId
		assertNull(entityClass.getFields().get(0).getAnnotationUsage(
				NaturalId.class, ctx.getModelsContext()));

		// "ssn" should have @NaturalId(mutable=false)
		NaturalId natIdAnn = entityClass.getFields().get(1).getAnnotationUsage(
				NaturalId.class, ctx.getModelsContext());
		assertNotNull(natIdAnn);
		assertFalse(natIdAnn.mutable());
	}

	@Test
	public void testMarkNaturalIdFieldsMutable() {
		JaxbHbmBasicAttributeType attr = new JaxbHbmBasicAttributeType();
		attr.setName("email");
		attr.setTypeAttribute("string");
		HbmPropertyBuilder.processProperty(entityClass, attr, ctx);

		HbmPropertyBuilder.markNaturalIdFields(entityClass, 0, true, ctx);

		NaturalId natIdAnn = entityClass.getFields().get(0).getAnnotationUsage(
				NaturalId.class, ctx.getModelsContext());
		assertNotNull(natIdAnn);
		assertTrue(natIdAnn.mutable());
	}
}
