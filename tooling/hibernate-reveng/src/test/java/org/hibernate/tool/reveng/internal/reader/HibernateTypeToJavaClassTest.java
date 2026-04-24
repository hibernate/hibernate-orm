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
package org.hibernate.tool.reveng.internal.reader;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import jakarta.persistence.TemporalType;

import org.hibernate.tool.reveng.internal.util.TypeHelper;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TypeHelper} Hibernate type name to Java class mappings.
 *
 * @author Koen Aers
 */
public class HibernateTypeToJavaClassTest {

	@Test
	public void testStringType() {
		assertEquals(String.class, TypeHelper.toJavaClass("string"));
	}

	@Test
	public void testIntType() {
		assertEquals(int.class, TypeHelper.toJavaClass("int"));
	}

	@Test
	public void testIntegerWrapperType() {
		assertEquals(Integer.class, TypeHelper.toJavaClass("java.lang.Integer"));
	}

	@Test
	public void testLongType() {
		assertEquals(long.class, TypeHelper.toJavaClass("long"));
	}

	@Test
	public void testLongWrapperType() {
		assertEquals(Long.class, TypeHelper.toJavaClass("java.lang.Long"));
	}

	@Test
	public void testShortType() {
		assertEquals(short.class, TypeHelper.toJavaClass("short"));
	}

	@Test
	public void testByteType() {
		assertEquals(byte.class, TypeHelper.toJavaClass("byte"));
	}

	@Test
	public void testFloatType() {
		assertEquals(float.class, TypeHelper.toJavaClass("float"));
	}

	@Test
	public void testDoubleType() {
		assertEquals(double.class, TypeHelper.toJavaClass("double"));
	}

	@Test
	public void testBooleanType() {
		assertEquals(boolean.class, TypeHelper.toJavaClass("boolean"));
	}

	@Test
	public void testCharType() {
		assertEquals(char.class, TypeHelper.toJavaClass("char"));
	}

	@Test
	public void testCharacterType() {
		assertEquals(char.class, TypeHelper.toJavaClass("character"));
	}

	@Test
	public void testBigDecimalType() {
		assertEquals(BigDecimal.class, TypeHelper.toJavaClass("big_decimal"));
	}

	@Test
	public void testBigIntegerType() {
		assertEquals(BigInteger.class, TypeHelper.toJavaClass("big_integer"));
	}

	@Test
	public void testDateType() {
		assertEquals(Date.class, TypeHelper.toJavaClass("date"));
	}

	@Test
	public void testTimeType() {
		assertEquals(Date.class, TypeHelper.toJavaClass("time"));
	}

	@Test
	public void testTimestampType() {
		assertEquals(Date.class, TypeHelper.toJavaClass("timestamp"));
	}

	@Test
	public void testBinaryType() {
		assertEquals(byte[].class, TypeHelper.toJavaClass("binary"));
	}

	@Test
	public void testBlobType() {
		assertEquals(byte[].class, TypeHelper.toJavaClass("blob"));
	}

	@Test
	public void testClobType() {
		assertEquals(String.class, TypeHelper.toJavaClass("clob"));
	}

	@Test
	public void testSerializableType() {
		assertEquals(java.io.Serializable.class, TypeHelper.toJavaClass("serializable"));
	}

	@Test
	public void testNullReturnsObject() {
		assertEquals(Object.class, TypeHelper.toJavaClass(null));
	}

	@Test
	public void testUnknownTypeReturnsObject() {
		assertEquals(Object.class, TypeHelper.toJavaClass("unknown_type"));
	}

	@Test
	public void testFullyQualifiedStringType() {
		assertEquals(String.class, TypeHelper.toJavaClass("java.lang.String"));
	}

	@Test
	public void testBooleanWrapperType() {
		assertEquals(Boolean.class, TypeHelper.toJavaClass("java.lang.Boolean"));
	}

	// ---- Temporal type tests ----

	@Test
	public void testTemporalDate() {
		assertEquals(TemporalType.DATE, TypeHelper.toTemporalType("date"));
	}

	@Test
	public void testTemporalTime() {
		assertEquals(TemporalType.TIME, TypeHelper.toTemporalType("time"));
	}

	@Test
	public void testTemporalTimestamp() {
		assertEquals(TemporalType.TIMESTAMP, TypeHelper.toTemporalType("timestamp"));
	}

	@Test
	public void testTemporalNonTemporal() {
		assertNull(TypeHelper.toTemporalType("string"));
	}

	@Test
	public void testTemporalNull() {
		assertNull(TypeHelper.toTemporalType(null));
	}

	// ---- LOB type tests ----

	@Test
	public void testIsLobBlob() {
		assertTrue(TypeHelper.isLob("blob"));
	}

	@Test
	public void testIsLobClob() {
		assertTrue(TypeHelper.isLob("clob"));
	}

	@Test
	public void testIsLobNonLob() {
		assertFalse(TypeHelper.isLob("string"));
	}

	@Test
	public void testIsLobNull() {
		assertFalse(TypeHelper.isLob(null));
	}
}