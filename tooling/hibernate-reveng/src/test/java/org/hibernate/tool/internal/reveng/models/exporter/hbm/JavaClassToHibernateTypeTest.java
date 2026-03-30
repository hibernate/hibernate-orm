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
package org.hibernate.tool.internal.reveng.models.exporter.hbm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JavaClassToHibernateType}.
 *
 * @author Koen Aers
 */
public class JavaClassToHibernateTypeTest {

	// --- Primitive types ---

	@Test
	public void testInt() {
		assertEquals("int", JavaClassToHibernateType.toHibernateType(int.class));
	}

	@Test
	public void testLong() {
		assertEquals("long", JavaClassToHibernateType.toHibernateType(long.class));
	}

	@Test
	public void testShort() {
		assertEquals("short", JavaClassToHibernateType.toHibernateType(short.class));
	}

	@Test
	public void testByte() {
		assertEquals("byte", JavaClassToHibernateType.toHibernateType(byte.class));
	}

	@Test
	public void testFloat() {
		assertEquals("float", JavaClassToHibernateType.toHibernateType(float.class));
	}

	@Test
	public void testDouble() {
		assertEquals("double", JavaClassToHibernateType.toHibernateType(double.class));
	}

	@Test
	public void testBoolean() {
		assertEquals("boolean", JavaClassToHibernateType.toHibernateType(boolean.class));
	}

	@Test
	public void testChar() {
		assertEquals("character", JavaClassToHibernateType.toHibernateType(char.class));
	}

	// --- Wrapper types ---

	@Test
	public void testIntegerWrapper() {
		assertEquals("java.lang.Integer", JavaClassToHibernateType.toHibernateType(Integer.class));
	}

	@Test
	public void testLongWrapper() {
		assertEquals("java.lang.Long", JavaClassToHibernateType.toHibernateType(Long.class));
	}

	@Test
	public void testShortWrapper() {
		assertEquals("java.lang.Short", JavaClassToHibernateType.toHibernateType(Short.class));
	}

	@Test
	public void testByteWrapper() {
		assertEquals("java.lang.Byte", JavaClassToHibernateType.toHibernateType(Byte.class));
	}

	@Test
	public void testFloatWrapper() {
		assertEquals("java.lang.Float", JavaClassToHibernateType.toHibernateType(Float.class));
	}

	@Test
	public void testDoubleWrapper() {
		assertEquals("java.lang.Double", JavaClassToHibernateType.toHibernateType(Double.class));
	}

	@Test
	public void testBooleanWrapper() {
		assertEquals("java.lang.Boolean", JavaClassToHibernateType.toHibernateType(Boolean.class));
	}

	@Test
	public void testCharacterWrapper() {
		assertEquals("java.lang.Character", JavaClassToHibernateType.toHibernateType(Character.class));
	}

	// --- String ---

	@Test
	public void testString() {
		assertEquals("string", JavaClassToHibernateType.toHibernateType(String.class));
	}

	// --- Numeric types ---

	@Test
	public void testBigDecimal() {
		assertEquals("big_decimal", JavaClassToHibernateType.toHibernateType(BigDecimal.class));
	}

	@Test
	public void testBigInteger() {
		assertEquals("big_integer", JavaClassToHibernateType.toHibernateType(BigInteger.class));
	}

	// --- Date/time ---

	@Test
	public void testDate() {
		assertEquals("timestamp", JavaClassToHibernateType.toHibernateType(Date.class));
	}

	// --- Binary ---

	@Test
	public void testByteArray() {
		assertEquals("binary", JavaClassToHibernateType.toHibernateType(byte[].class));
	}

	// --- Serializable ---

	@Test
	public void testSerializable() {
		assertEquals("serializable", JavaClassToHibernateType.toHibernateType(Serializable.class));
	}

	// --- Null ---

	@Test
	public void testNull() {
		assertEquals("serializable", JavaClassToHibernateType.toHibernateType((Class<?>) null));
	}

	// --- Unknown class falls back to class name ---

	@Test
	public void testUnknownClass() {
		assertEquals("java.util.UUID", JavaClassToHibernateType.toHibernateType(java.util.UUID.class));
	}
}
