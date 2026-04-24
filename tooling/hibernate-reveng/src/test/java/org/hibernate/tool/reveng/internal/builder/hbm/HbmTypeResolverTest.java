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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.GenerationType;
import org.junit.jupiter.api.Test;

class HbmTypeResolverTest {

	@Test
	void testResolveJavaTypeKnownTypes() {
		assertEquals("java.lang.String", HbmTypeResolver.resolveJavaType("string"));
		assertEquals("long", HbmTypeResolver.resolveJavaType("long"));
		assertEquals("int", HbmTypeResolver.resolveJavaType("int"));
		assertEquals("java.lang.Integer", HbmTypeResolver.resolveJavaType("integer"));
		assertEquals("java.math.BigDecimal", HbmTypeResolver.resolveJavaType("big_decimal"));
		assertEquals("java.math.BigInteger", HbmTypeResolver.resolveJavaType("big_integer"));
		assertEquals("java.util.Date", HbmTypeResolver.resolveJavaType("date"));
		assertEquals("java.util.Date", HbmTypeResolver.resolveJavaType("time"));
		assertEquals("java.util.Date", HbmTypeResolver.resolveJavaType("timestamp"));
		assertEquals("java.util.Calendar", HbmTypeResolver.resolveJavaType("calendar"));
		assertEquals("byte[]", HbmTypeResolver.resolveJavaType("binary"));
		assertEquals("java.sql.Clob", HbmTypeResolver.resolveJavaType("clob"));
		assertEquals("java.sql.Blob", HbmTypeResolver.resolveJavaType("blob"));
		assertEquals("java.io.Serializable", HbmTypeResolver.resolveJavaType("serializable"));
		assertEquals("java.lang.Boolean", HbmTypeResolver.resolveJavaType("yes_no"));
		assertEquals("java.lang.Boolean", HbmTypeResolver.resolveJavaType("true_false"));
	}

	@Test
	void testResolveJavaTypeCaseInsensitive() {
		assertEquals("java.lang.String", HbmTypeResolver.resolveJavaType("String"));
		assertEquals("java.lang.String", HbmTypeResolver.resolveJavaType("STRING"));
		assertEquals("java.math.BigDecimal", HbmTypeResolver.resolveJavaType("BIG_DECIMAL"));
	}

	@Test
	void testResolveJavaTypeFullyQualified() {
		assertEquals("com.example.MyType", HbmTypeResolver.resolveJavaType("com.example.MyType"));
	}

	@Test
	void testResolveJavaTypeNullOrEmpty() {
		assertEquals("java.lang.String", HbmTypeResolver.resolveJavaType(null));
		assertEquals("java.lang.String", HbmTypeResolver.resolveJavaType(""));
	}

	@Test
	void testResolveJavaTypeUnknown() {
		assertEquals("java.lang.String", HbmTypeResolver.resolveJavaType("unknown_type"));
	}

	@Test
	void testIsPrimitiveType() {
		assertTrue(HbmTypeResolver.isPrimitiveType("boolean"));
		assertTrue(HbmTypeResolver.isPrimitiveType("byte"));
		assertTrue(HbmTypeResolver.isPrimitiveType("char"));
		assertTrue(HbmTypeResolver.isPrimitiveType("short"));
		assertTrue(HbmTypeResolver.isPrimitiveType("int"));
		assertTrue(HbmTypeResolver.isPrimitiveType("long"));
		assertTrue(HbmTypeResolver.isPrimitiveType("float"));
		assertTrue(HbmTypeResolver.isPrimitiveType("double"));
	}

	@Test
	void testIsNotPrimitiveType() {
		assertFalse(HbmTypeResolver.isPrimitiveType("java.lang.String"));
		assertFalse(HbmTypeResolver.isPrimitiveType("Boolean"));
		assertFalse(HbmTypeResolver.isPrimitiveType("Integer"));
	}

	@Test
	void testResolveClassNameWithPackage() {
		assertEquals("com.example.Person",
				HbmTypeResolver.resolveClassName("Person", "com.example"));
	}

	@Test
	void testResolveClassNameAlreadyQualified() {
		assertEquals("com.example.Person",
				HbmTypeResolver.resolveClassName("com.example.Person", "other.pkg"));
	}

	@Test
	void testResolveClassNameNoPackage() {
		assertEquals("Person", HbmTypeResolver.resolveClassName("Person", null));
		assertEquals("Person", HbmTypeResolver.resolveClassName("Person", ""));
	}

	@Test
	void testResolveClassNameNullOrEmpty() {
		assertNull(HbmTypeResolver.resolveClassName(null, "com.example"));
		assertEquals("", HbmTypeResolver.resolveClassName("", "com.example"));
	}

	@Test
	void testSimpleName() {
		assertEquals("Person", HbmTypeResolver.simpleName("com.example.Person"));
		assertEquals("Person", HbmTypeResolver.simpleName("Person"));
	}

	@Test
	void testMapGeneratorClassIdentity() {
		assertEquals(GenerationType.IDENTITY, HbmTypeResolver.mapGeneratorClass("identity"));
		assertEquals(GenerationType.IDENTITY, HbmTypeResolver.mapGeneratorClass("native"));
	}

	@Test
	void testMapGeneratorClassSequence() {
		assertEquals(GenerationType.SEQUENCE, HbmTypeResolver.mapGeneratorClass("sequence"));
		assertEquals(GenerationType.SEQUENCE, HbmTypeResolver.mapGeneratorClass("seqhilo"));
		assertEquals(GenerationType.SEQUENCE, HbmTypeResolver.mapGeneratorClass("enhanced-sequence"));
		assertEquals(GenerationType.SEQUENCE,
				HbmTypeResolver.mapGeneratorClass("org.hibernate.id.enhanced.SequenceStyleGenerator"));
	}

	@Test
	void testMapGeneratorClassTable() {
		assertEquals(GenerationType.TABLE, HbmTypeResolver.mapGeneratorClass("enhanced-table"));
		assertEquals(GenerationType.TABLE,
				HbmTypeResolver.mapGeneratorClass("org.hibernate.id.enhanced.TableGenerator"));
	}

	@Test
	void testMapGeneratorClassUuid() {
		assertEquals(GenerationType.UUID, HbmTypeResolver.mapGeneratorClass("uuid"));
		assertEquals(GenerationType.UUID, HbmTypeResolver.mapGeneratorClass("uuid2"));
		assertEquals(GenerationType.UUID, HbmTypeResolver.mapGeneratorClass("guid"));
	}

	@Test
	void testMapGeneratorClassAssigned() {
		assertNull(HbmTypeResolver.mapGeneratorClass("assigned"));
	}

	@Test
	void testMapGeneratorClassNullOrEmpty() {
		assertNull(HbmTypeResolver.mapGeneratorClass(null));
		assertNull(HbmTypeResolver.mapGeneratorClass(""));
	}

	@Test
	void testMapGeneratorClassDefault() {
		assertEquals(GenerationType.AUTO, HbmTypeResolver.mapGeneratorClass("custom-gen"));
	}
}
