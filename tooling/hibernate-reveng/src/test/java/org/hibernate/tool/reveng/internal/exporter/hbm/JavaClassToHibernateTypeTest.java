/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.hbm;

import static org.junit.jupiter.api.Assertions.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import org.hibernate.tool.reveng.internal.util.TypeHelper;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TypeHelper} Java class to Hibernate type mappings.
 *
 * @author Koen Aers
 */
public class JavaClassToHibernateTypeTest {

	// --- Primitive types ---

	@Test
	public void testInt() {
		assertEquals("int", TypeHelper.toHibernateType(int.class));
	}

	@Test
	public void testLong() {
		assertEquals("long", TypeHelper.toHibernateType(long.class));
	}

	@Test
	public void testShort() {
		assertEquals("short", TypeHelper.toHibernateType(short.class));
	}

	@Test
	public void testByte() {
		assertEquals("byte", TypeHelper.toHibernateType(byte.class));
	}

	@Test
	public void testFloat() {
		assertEquals("float", TypeHelper.toHibernateType(float.class));
	}

	@Test
	public void testDouble() {
		assertEquals("double", TypeHelper.toHibernateType(double.class));
	}

	@Test
	public void testBoolean() {
		assertEquals("boolean", TypeHelper.toHibernateType(boolean.class));
	}

	@Test
	public void testChar() {
		assertEquals("character", TypeHelper.toHibernateType(char.class));
	}

	// --- Wrapper types ---

	@Test
	public void testIntegerWrapper() {
		assertEquals("java.lang.Integer", TypeHelper.toHibernateType(Integer.class));
	}

	@Test
	public void testLongWrapper() {
		assertEquals("java.lang.Long", TypeHelper.toHibernateType(Long.class));
	}

	@Test
	public void testShortWrapper() {
		assertEquals("java.lang.Short", TypeHelper.toHibernateType(Short.class));
	}

	@Test
	public void testByteWrapper() {
		assertEquals("java.lang.Byte", TypeHelper.toHibernateType(Byte.class));
	}

	@Test
	public void testFloatWrapper() {
		assertEquals("java.lang.Float", TypeHelper.toHibernateType(Float.class));
	}

	@Test
	public void testDoubleWrapper() {
		assertEquals("java.lang.Double", TypeHelper.toHibernateType(Double.class));
	}

	@Test
	public void testBooleanWrapper() {
		assertEquals("java.lang.Boolean", TypeHelper.toHibernateType(Boolean.class));
	}

	@Test
	public void testCharacterWrapper() {
		assertEquals("java.lang.Character", TypeHelper.toHibernateType(Character.class));
	}

	// --- String ---

	@Test
	public void testString() {
		assertEquals("string", TypeHelper.toHibernateType(String.class));
	}

	// --- Numeric types ---

	@Test
	public void testBigDecimal() {
		assertEquals("big_decimal", TypeHelper.toHibernateType(BigDecimal.class));
	}

	@Test
	public void testBigInteger() {
		assertEquals("big_integer", TypeHelper.toHibernateType(BigInteger.class));
	}

	// --- Date/time ---

	@Test
	public void testDate() {
		assertEquals("timestamp", TypeHelper.toHibernateType(Date.class));
	}

	// --- Binary ---

	@Test
	public void testByteArray() {
		assertEquals("binary", TypeHelper.toHibernateType(byte[].class));
	}

	// --- Serializable ---

	@Test
	public void testSerializable() {
		assertEquals("serializable", TypeHelper.toHibernateType(Serializable.class));
	}

	// --- Null ---

	@Test
	public void testNull() {
		assertEquals("serializable", TypeHelper.toHibernateType((Class<?>) null));
	}

	// --- Unknown class falls back to class name ---

	@Test
	public void testUnknownClass() {
		assertEquals("java.util.UUID", TypeHelper.toHibernateType(java.util.UUID.class));
	}
}
