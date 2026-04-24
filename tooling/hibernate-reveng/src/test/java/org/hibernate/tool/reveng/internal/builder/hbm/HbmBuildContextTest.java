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

import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.GenerationType;

public class HbmBuildContextTest {

	private HbmBuildContext ctx;

	@BeforeEach
	public void setUp() {
		ctx = new HbmBuildContext();
	}

	// --- Type resolution ---

	@Test
	public void testResolveJavaTypeString() {
		assertEquals("java.lang.String", ctx.resolveJavaType("string"));
	}

	@Test
	public void testResolveJavaTypeLong() {
		assertEquals("long", ctx.resolveJavaType("long"));
	}

	@Test
	public void testResolveJavaTypeInteger() {
		assertEquals("java.lang.Integer", ctx.resolveJavaType("integer"));
	}

	@Test
	public void testResolveJavaTypeInt() {
		assertEquals("int", ctx.resolveJavaType("int"));
	}

	@Test
	public void testResolveJavaTypeBoolean() {
		assertEquals("boolean", ctx.resolveJavaType("boolean"));
	}

	@Test
	public void testResolveJavaTypeYesNo() {
		assertEquals("java.lang.Boolean", ctx.resolveJavaType("yes_no"));
	}

	@Test
	public void testResolveJavaTypeBigDecimal() {
		assertEquals("java.math.BigDecimal", ctx.resolveJavaType("big_decimal"));
	}

	@Test
	public void testResolveJavaTypeDate() {
		assertEquals("java.util.Date", ctx.resolveJavaType("date"));
	}

	@Test
	public void testResolveJavaTypeTimestamp() {
		assertEquals("java.util.Date", ctx.resolveJavaType("timestamp"));
	}

	@Test
	public void testResolveJavaTypeBinary() {
		assertEquals("byte[]", ctx.resolveJavaType("binary"));
	}

	@Test
	public void testResolveJavaTypeFullyQualified() {
		assertEquals("com.example.MyType", ctx.resolveJavaType("com.example.MyType"));
	}

	@Test
	public void testResolveJavaTypeNull() {
		assertEquals("java.lang.String", ctx.resolveJavaType(null));
	}

	@Test
	public void testResolveJavaTypeEmpty() {
		assertEquals("java.lang.String", ctx.resolveJavaType(""));
	}

	@Test
	public void testResolveJavaTypeUnknown() {
		assertEquals("java.lang.String", ctx.resolveJavaType("unknown_type"));
	}

	@Test
	public void testResolveJavaTypeCaseInsensitive() {
		assertEquals("java.lang.String", ctx.resolveJavaType("STRING"));
		assertEquals("long", ctx.resolveJavaType("LONG"));
	}

	// --- Generator class mapping ---

	@Test
	public void testMapGeneratorIdentity() {
		assertEquals(GenerationType.IDENTITY, ctx.mapGeneratorClass("identity"));
	}

	@Test
	public void testMapGeneratorNative() {
		assertEquals(GenerationType.IDENTITY, ctx.mapGeneratorClass("native"));
	}

	@Test
	public void testMapGeneratorSequence() {
		assertEquals(GenerationType.SEQUENCE, ctx.mapGeneratorClass("sequence"));
	}

	@Test
	public void testMapGeneratorEnhancedSequence() {
		assertEquals(GenerationType.SEQUENCE, ctx.mapGeneratorClass("enhanced-sequence"));
	}

	@Test
	public void testMapGeneratorSequenceStyleGenerator() {
		assertEquals(GenerationType.SEQUENCE,
				ctx.mapGeneratorClass("org.hibernate.id.enhanced.SequenceStyleGenerator"));
	}

	@Test
	public void testMapGeneratorTable() {
		assertEquals(GenerationType.TABLE, ctx.mapGeneratorClass("enhanced-table"));
	}

	@Test
	public void testMapGeneratorTableGenerator() {
		assertEquals(GenerationType.TABLE,
				ctx.mapGeneratorClass("org.hibernate.id.enhanced.TableGenerator"));
	}

	@Test
	public void testMapGeneratorUuid() {
		assertEquals(GenerationType.UUID, ctx.mapGeneratorClass("uuid"));
	}

	@Test
	public void testMapGeneratorUuid2() {
		assertEquals(GenerationType.UUID, ctx.mapGeneratorClass("uuid2"));
	}

	@Test
	public void testMapGeneratorGuid() {
		assertEquals(GenerationType.UUID, ctx.mapGeneratorClass("guid"));
	}

	@Test
	public void testMapGeneratorAssigned() {
		assertNull(ctx.mapGeneratorClass("assigned"));
	}

	@Test
	public void testMapGeneratorNull() {
		assertNull(ctx.mapGeneratorClass(null));
	}

	@Test
	public void testMapGeneratorEmpty() {
		assertNull(ctx.mapGeneratorClass(""));
	}

	@Test
	public void testMapGeneratorUnknown() {
		assertEquals(GenerationType.AUTO, ctx.mapGeneratorClass("custom-generator"));
	}

	// --- Instance resolveClassName (delegates to HbmTypeResolver) ---

	@Test
	public void testResolveClassNameInstance() {
		ctx.setDefaultPackage("com.example");
		assertEquals("com.example.Employee", ctx.resolveClassName("Employee"));
	}

	@Test
	public void testResolveClassNameInstanceAlreadyQualified() {
		ctx.setDefaultPackage("com.example");
		assertEquals("com.other.Employee", ctx.resolveClassName("com.other.Employee"));
	}

	// --- Field creation ---

	@Test
	public void testCreateField() {
		DynamicClassDetails entityClass = new DynamicClassDetails(
				"Test", "com.example.Test", false, null, null, ctx.getModelsContext());
		DynamicFieldDetails field = ctx.createField(entityClass, "name", "java.lang.String");
		assertNotNull(field);
		assertEquals("name", field.getName());
	}

	@Test
	public void testCreateCollectionField() {
		DynamicClassDetails entityClass = new DynamicClassDetails(
				"Test", "com.example.Test", false, null, null, ctx.getModelsContext());
		ClassDetails elementClass = ctx.getModelsContext().getClassDetailsRegistry()
				.resolveClassDetails("java.lang.String");
		DynamicFieldDetails field = ctx.createCollectionField(entityClass, "items", elementClass);
		assertNotNull(field);
		assertEquals("items", field.getName());
		assertTrue(field.isPlural());
	}

	// --- Registry helpers ---

	@Test
	public void testResolveOrCreateClassDetails() {
		ClassDetails details = ctx.resolveOrCreateClassDetails("Foo", "com.example.Foo");
		assertNotNull(details);
		assertEquals("com.example.Foo", details.getClassName());
	}

	@Test
	public void testResolveOrCreateReturnsSameInstance() {
		ClassDetails first = ctx.resolveOrCreateClassDetails("Foo", "com.example.Foo");
		ClassDetails second = ctx.resolveOrCreateClassDetails("Foo", "com.example.Foo");
		assertSame(first, second);
	}

	// --- Column annotation ---

	@Test
	public void testAddColumnAnnotation() {
		DynamicClassDetails entityClass = new DynamicClassDetails(
				"Test", "com.example.Test", false, null, null, ctx.getModelsContext());
		DynamicFieldDetails field = ctx.createField(entityClass, "name", "java.lang.String");
		ctx.addColumnAnnotation(field, null, "USER_NAME", "name");

		Column col = field.getAnnotationUsage(Column.class, ctx.getModelsContext());
		assertNotNull(col);
		assertEquals("USER_NAME", col.name());
	}

	@Test
	public void testAddColumnAnnotationDefaultName() {
		DynamicClassDetails entityClass = new DynamicClassDetails(
				"Test", "com.example.Test", false, null, null, ctx.getModelsContext());
		DynamicFieldDetails field = ctx.createField(entityClass, "name", "java.lang.String");
		ctx.addColumnAnnotation(field, null, null, "name");

		Column col = field.getAnnotationUsage(Column.class, ctx.getModelsContext());
		assertNotNull(col);
		assertEquals("name", col.name());
	}
}
