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
package org.hibernate.tool.internal.reveng.models.reader;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import jakarta.persistence.TemporalType;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HibernateTypeToJavaClass}, verifying all type name
 * to Java class mappings.
 *
 * @author Koen Aers
 */
public class HibernateTypeToJavaClassTest {

	@Test
	public void testStringType() {
		assertEquals(String.class, HibernateTypeToJavaClass.toJavaClass("string"));
	}

	@Test
	public void testIntType() {
		assertEquals(int.class, HibernateTypeToJavaClass.toJavaClass("int"));
	}

	@Test
	public void testIntegerWrapperType() {
		assertEquals(Integer.class, HibernateTypeToJavaClass.toJavaClass("java.lang.Integer"));
	}

	@Test
	public void testLongType() {
		assertEquals(long.class, HibernateTypeToJavaClass.toJavaClass("long"));
	}

	@Test
	public void testLongWrapperType() {
		assertEquals(Long.class, HibernateTypeToJavaClass.toJavaClass("java.lang.Long"));
	}

	@Test
	public void testShortType() {
		assertEquals(short.class, HibernateTypeToJavaClass.toJavaClass("short"));
	}

	@Test
	public void testByteType() {
		assertEquals(byte.class, HibernateTypeToJavaClass.toJavaClass("byte"));
	}

	@Test
	public void testFloatType() {
		assertEquals(float.class, HibernateTypeToJavaClass.toJavaClass("float"));
	}

	@Test
	public void testDoubleType() {
		assertEquals(double.class, HibernateTypeToJavaClass.toJavaClass("double"));
	}

	@Test
	public void testBooleanType() {
		assertEquals(boolean.class, HibernateTypeToJavaClass.toJavaClass("boolean"));
	}

	@Test
	public void testCharType() {
		assertEquals(char.class, HibernateTypeToJavaClass.toJavaClass("char"));
	}

	@Test
	public void testCharacterType() {
		assertEquals(char.class, HibernateTypeToJavaClass.toJavaClass("character"));
	}

	@Test
	public void testBigDecimalType() {
		assertEquals(BigDecimal.class, HibernateTypeToJavaClass.toJavaClass("big_decimal"));
	}

	@Test
	public void testBigIntegerType() {
		assertEquals(BigInteger.class, HibernateTypeToJavaClass.toJavaClass("big_integer"));
	}

	@Test
	public void testDateType() {
		assertEquals(Date.class, HibernateTypeToJavaClass.toJavaClass("date"));
	}

	@Test
	public void testTimeType() {
		assertEquals(Date.class, HibernateTypeToJavaClass.toJavaClass("time"));
	}

	@Test
	public void testTimestampType() {
		assertEquals(Date.class, HibernateTypeToJavaClass.toJavaClass("timestamp"));
	}

	@Test
	public void testBinaryType() {
		assertEquals(byte[].class, HibernateTypeToJavaClass.toJavaClass("binary"));
	}

	@Test
	public void testBlobType() {
		assertEquals(byte[].class, HibernateTypeToJavaClass.toJavaClass("blob"));
	}

	@Test
	public void testClobType() {
		assertEquals(String.class, HibernateTypeToJavaClass.toJavaClass("clob"));
	}

	@Test
	public void testSerializableType() {
		assertEquals(java.io.Serializable.class, HibernateTypeToJavaClass.toJavaClass("serializable"));
	}

	@Test
	public void testNullReturnsObject() {
		assertEquals(Object.class, HibernateTypeToJavaClass.toJavaClass(null));
	}

	@Test
	public void testUnknownTypeReturnsObject() {
		assertEquals(Object.class, HibernateTypeToJavaClass.toJavaClass("unknown_type"));
	}

	@Test
	public void testFullyQualifiedStringType() {
		assertEquals(String.class, HibernateTypeToJavaClass.toJavaClass("java.lang.String"));
	}

	@Test
	public void testBooleanWrapperType() {
		assertEquals(Boolean.class, HibernateTypeToJavaClass.toJavaClass("java.lang.Boolean"));
	}

	// ---- Temporal type tests ----

	@Test
	public void testTemporalDate() {
		assertEquals(TemporalType.DATE, HibernateTypeToJavaClass.toTemporalType("date"));
	}

	@Test
	public void testTemporalTime() {
		assertEquals(TemporalType.TIME, HibernateTypeToJavaClass.toTemporalType("time"));
	}

	@Test
	public void testTemporalTimestamp() {
		assertEquals(TemporalType.TIMESTAMP, HibernateTypeToJavaClass.toTemporalType("timestamp"));
	}

	@Test
	public void testTemporalNonTemporal() {
		assertNull(HibernateTypeToJavaClass.toTemporalType("string"));
	}

	@Test
	public void testTemporalNull() {
		assertNull(HibernateTypeToJavaClass.toTemporalType(null));
	}

	// ---- LOB type tests ----

	@Test
	public void testIsLobBlob() {
		assertTrue(HibernateTypeToJavaClass.isLob("blob"));
	}

	@Test
	public void testIsLobClob() {
		assertTrue(HibernateTypeToJavaClass.isLob("clob"));
	}

	@Test
	public void testIsLobNonLob() {
		assertFalse(HibernateTypeToJavaClass.isLob("string"));
	}

	@Test
	public void testIsLobNull() {
		assertFalse(HibernateTypeToJavaClass.isLob(null));
	}
}