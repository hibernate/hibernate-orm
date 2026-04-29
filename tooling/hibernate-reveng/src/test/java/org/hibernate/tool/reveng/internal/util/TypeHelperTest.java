/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.util;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
import java.util.Date;

import jakarta.persistence.TemporalType;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TypeHelper}.
 *
 * @author Koen Aers
 */
public class TypeHelperTest {

	// ================================================================
	// getPreferredHibernateType
	// ================================================================

	@Test
	public void testPreferredTypeForInteger() {
		assertEquals("int", TypeHelper.getPreferredHibernateType(
				Types.INTEGER, 0, 0, 0, false, false));
		assertEquals(Integer.class.getName(), TypeHelper.getPreferredHibernateType(
				Types.INTEGER, 0, 0, 0, true, false));
	}

	@Test
	public void testPreferredTypeForBigint() {
		assertEquals("long", TypeHelper.getPreferredHibernateType(
				Types.BIGINT, 0, 0, 0, false, false));
		assertEquals(Long.class.getName(), TypeHelper.getPreferredHibernateType(
				Types.BIGINT, 0, 0, 0, true, false));
	}

	@Test
	public void testPreferredTypeForSmallint() {
		assertEquals("short", TypeHelper.getPreferredHibernateType(
				Types.SMALLINT, 0, 0, 0, false, false));
		assertEquals(Short.class.getName(), TypeHelper.getPreferredHibernateType(
				Types.SMALLINT, 0, 0, 0, true, false));
	}

	@Test
	public void testPreferredTypeForTinyint() {
		assertEquals("byte", TypeHelper.getPreferredHibernateType(
				Types.TINYINT, 0, 0, 0, false, false));
		assertEquals(Byte.class.getName(), TypeHelper.getPreferredHibernateType(
				Types.TINYINT, 0, 0, 0, true, false));
	}

	@Test
	public void testPreferredTypeForReal() {
		assertEquals("float", TypeHelper.getPreferredHibernateType(
				Types.REAL, 0, 0, 0, false, false));
		assertEquals(Float.class.getName(), TypeHelper.getPreferredHibernateType(
				Types.REAL, 0, 0, 0, true, false));
	}

	@Test
	public void testPreferredTypeForFloat() {
		assertEquals("double", TypeHelper.getPreferredHibernateType(
				Types.FLOAT, 0, 0, 0, false, false));
		assertEquals(Double.class.getName(), TypeHelper.getPreferredHibernateType(
				Types.FLOAT, 0, 0, 0, true, false));
	}

	@Test
	public void testPreferredTypeForDouble() {
		assertEquals("double", TypeHelper.getPreferredHibernateType(
				Types.DOUBLE, 0, 0, 0, false, false));
		assertEquals(Double.class.getName(), TypeHelper.getPreferredHibernateType(
				Types.DOUBLE, 0, 0, 0, true, false));
	}

	@Test
	public void testPreferredTypeForVarchar() {
		assertEquals("string", TypeHelper.getPreferredHibernateType(
				Types.VARCHAR, 0, 0, 0, false, false));
		assertEquals("string", TypeHelper.getPreferredHibernateType(
				Types.VARCHAR, 0, 0, 0, true, false));
	}

	@Test
	public void testPreferredTypeForLongvarchar() {
		assertEquals("string", TypeHelper.getPreferredHibernateType(
				Types.LONGVARCHAR, 0, 0, 0, false, false));
	}

	@Test
	public void testPreferredTypeForChar() {
		assertEquals("char", TypeHelper.getPreferredHibernateType(
				Types.CHAR, 1, 0, 0, false, false));
		assertEquals(Character.class.getName(), TypeHelper.getPreferredHibernateType(
				Types.CHAR, 1, 0, 0, true, false));
	}

	@Test
	public void testPreferredTypeForCharWithSizeGreaterThanOne() {
		assertEquals("string", TypeHelper.getPreferredHibernateType(
				Types.CHAR, 10, 0, 0, false, false));
		assertEquals("string", TypeHelper.getPreferredHibernateType(
				Types.CHAR, 10, 0, 0, true, false));
	}

	@Test
	public void testPreferredTypeForBit() {
		assertEquals("boolean", TypeHelper.getPreferredHibernateType(
				Types.BIT, 0, 0, 0, false, false));
		assertEquals(Boolean.class.getName(), TypeHelper.getPreferredHibernateType(
				Types.BIT, 0, 0, 0, true, false));
	}

	@Test
	public void testPreferredTypeForBoolean() {
		assertEquals("boolean", TypeHelper.getPreferredHibernateType(
				Types.BOOLEAN, 0, 0, 0, false, false));
	}

	@Test
	public void testPreferredTypeForDate() {
		assertEquals("date", TypeHelper.getPreferredHibernateType(
				Types.DATE, 0, 0, 0, false, false));
	}

	@Test
	public void testPreferredTypeForTime() {
		assertEquals("time", TypeHelper.getPreferredHibernateType(
				Types.TIME, 0, 0, 0, false, false));
	}

	@Test
	public void testPreferredTypeForTimestamp() {
		assertEquals("timestamp", TypeHelper.getPreferredHibernateType(
				Types.TIMESTAMP, 0, 0, 0, false, false));
	}

	@Test
	public void testPreferredTypeForBlob() {
		assertEquals("blob", TypeHelper.getPreferredHibernateType(
				Types.BLOB, 0, 0, 0, false, false));
	}

	@Test
	public void testPreferredTypeForClob() {
		assertEquals("clob", TypeHelper.getPreferredHibernateType(
				Types.CLOB, 0, 0, 0, false, false));
	}

	@Test
	public void testPreferredTypeForBinary() {
		assertEquals("binary", TypeHelper.getPreferredHibernateType(
				Types.BINARY, 0, 0, 0, false, false));
	}

	@Test
	public void testPreferredTypeForVarbinary() {
		assertEquals("binary", TypeHelper.getPreferredHibernateType(
				Types.VARBINARY, 0, 0, 0, false, false));
	}

	@Test
	public void testPreferredTypeForLongvarbinary() {
		assertEquals("binary", TypeHelper.getPreferredHibernateType(
				Types.LONGVARBINARY, 0, 0, 0, false, false));
	}

	@Test
	public void testPreferredTypeForDecimalNoScale() {
		assertEquals("big_decimal", TypeHelper.getPreferredHibernateType(
				Types.DECIMAL, 0, 0, 1, false, false));
	}

	@Test
	public void testPreferredTypeForDecimalWithPrecision1() {
		assertEquals("boolean", TypeHelper.getPreferredHibernateType(
				Types.DECIMAL, 0, 1, 0, false, false));
		assertEquals(Boolean.class.getName(), TypeHelper.getPreferredHibernateType(
				Types.DECIMAL, 0, 1, 0, true, false));
	}

	@Test
	public void testPreferredTypeForDecimalWithPrecision2() {
		assertEquals("byte", TypeHelper.getPreferredHibernateType(
				Types.DECIMAL, 0, 2, 0, false, false));
		assertEquals(Byte.class.getName(), TypeHelper.getPreferredHibernateType(
				Types.DECIMAL, 0, 2, 0, true, false));
	}

	@Test
	public void testPreferredTypeForDecimalWithPrecision4() {
		assertEquals("short", TypeHelper.getPreferredHibernateType(
				Types.DECIMAL, 0, 4, 0, false, false));
		assertEquals(Short.class.getName(), TypeHelper.getPreferredHibernateType(
				Types.DECIMAL, 0, 4, 0, true, false));
	}

	@Test
	public void testPreferredTypeForDecimalWithPrecision9() {
		assertEquals("int", TypeHelper.getPreferredHibernateType(
				Types.DECIMAL, 0, 9, 0, false, false));
		assertEquals(Integer.class.getName(), TypeHelper.getPreferredHibernateType(
				Types.DECIMAL, 0, 9, 0, true, false));
	}

	@Test
	public void testPreferredTypeForDecimalWithPrecision18() {
		assertEquals("long", TypeHelper.getPreferredHibernateType(
				Types.DECIMAL, 0, 18, 0, false, false));
		assertEquals(Long.class.getName(), TypeHelper.getPreferredHibernateType(
				Types.DECIMAL, 0, 18, 0, true, false));
	}

	@Test
	public void testPreferredTypeForDecimalWithPrecision19() {
		assertEquals("big_integer", TypeHelper.getPreferredHibernateType(
				Types.DECIMAL, 0, 19, 0, false, false));
	}

	@Test
	public void testPreferredTypeForNumericNoScale() {
		assertEquals("int", TypeHelper.getPreferredHibernateType(
				Types.NUMERIC, 0, 5, 0, false, false));
	}

	@Test
	public void testPreferredTypeForGeneratedIdentifier() {
		assertEquals(Integer.class.getName(), TypeHelper.getPreferredHibernateType(
				Types.INTEGER, 0, 0, 0, false, true));
	}

	@Test
	public void testPreferredTypeForUnknownType() {
		assertNull(TypeHelper.getPreferredHibernateType(
				Types.OTHER, 0, 0, 0, false, false));
	}

	// ================================================================
	// toJavaClass
	// ================================================================

	@Test
	public void testToJavaClassNull() {
		assertEquals(Object.class, TypeHelper.toJavaClass(null));
	}

	@Test
	public void testToJavaClassPrimitives() {
		assertEquals(int.class, TypeHelper.toJavaClass("int"));
		assertEquals(long.class, TypeHelper.toJavaClass("long"));
		assertEquals(short.class, TypeHelper.toJavaClass("short"));
		assertEquals(byte.class, TypeHelper.toJavaClass("byte"));
		assertEquals(float.class, TypeHelper.toJavaClass("float"));
		assertEquals(double.class, TypeHelper.toJavaClass("double"));
		assertEquals(boolean.class, TypeHelper.toJavaClass("boolean"));
		assertEquals(char.class, TypeHelper.toJavaClass("char"));
		assertEquals(char.class, TypeHelper.toJavaClass("character"));
	}

	@Test
	public void testToJavaClassWrappers() {
		assertEquals(Integer.class, TypeHelper.toJavaClass("java.lang.Integer"));
		assertEquals(Long.class, TypeHelper.toJavaClass("java.lang.Long"));
		assertEquals(Short.class, TypeHelper.toJavaClass("java.lang.Short"));
		assertEquals(Byte.class, TypeHelper.toJavaClass("java.lang.Byte"));
		assertEquals(Float.class, TypeHelper.toJavaClass("java.lang.Float"));
		assertEquals(Double.class, TypeHelper.toJavaClass("java.lang.Double"));
		assertEquals(Boolean.class, TypeHelper.toJavaClass("java.lang.Boolean"));
		assertEquals(Character.class, TypeHelper.toJavaClass("java.lang.Character"));
	}

	@Test
	public void testToJavaClassString() {
		assertEquals(String.class, TypeHelper.toJavaClass("string"));
		assertEquals(String.class, TypeHelper.toJavaClass("java.lang.String"));
		assertEquals(String.class, TypeHelper.toJavaClass("clob"));
	}

	@Test
	public void testToJavaClassNumeric() {
		assertEquals(BigDecimal.class, TypeHelper.toJavaClass("big_decimal"));
		assertEquals(BigDecimal.class, TypeHelper.toJavaClass("java.math.BigDecimal"));
		assertEquals(BigInteger.class, TypeHelper.toJavaClass("big_integer"));
		assertEquals(BigInteger.class, TypeHelper.toJavaClass("java.math.BigInteger"));
	}

	@Test
	public void testToJavaClassDate() {
		assertEquals(Date.class, TypeHelper.toJavaClass("date"));
		assertEquals(Date.class, TypeHelper.toJavaClass("time"));
		assertEquals(Date.class, TypeHelper.toJavaClass("timestamp"));
		assertEquals(Date.class, TypeHelper.toJavaClass("java.util.Date"));
	}

	@Test
	public void testToJavaClassBooleanShorthand() {
		assertEquals(Boolean.class, TypeHelper.toJavaClass("yes_no"));
		assertEquals(Boolean.class, TypeHelper.toJavaClass("true_false"));
		assertEquals(Boolean.class, TypeHelper.toJavaClass("numeric_boolean"));
	}

	@Test
	public void testToJavaClassBinary() {
		assertEquals(byte[].class, TypeHelper.toJavaClass("binary"));
		assertEquals(byte[].class, TypeHelper.toJavaClass("blob"));
	}

	@Test
	public void testToJavaClassSerializable() {
		assertEquals(java.io.Serializable.class, TypeHelper.toJavaClass("serializable"));
	}

	@Test
	public void testToJavaClassFullyQualifiedKnown() {
		assertEquals(String.class, TypeHelper.toJavaClass("java.lang.String"));
	}

	@Test
	public void testToJavaClassFullyQualifiedUnknown() {
		assertEquals(Object.class, TypeHelper.toJavaClass("com.example.NonExistent"));
	}

	@Test
	public void testToJavaClassUnknownSimple() {
		assertEquals(Object.class, TypeHelper.toJavaClass("unknown_type"));
	}

	// ================================================================
	// toTemporalType
	// ================================================================

	@Test
	public void testToTemporalTypeNull() {
		assertNull(TypeHelper.toTemporalType(null));
	}

	@Test
	public void testToTemporalTypeDate() {
		assertEquals(TemporalType.DATE, TypeHelper.toTemporalType("date"));
	}

	@Test
	public void testToTemporalTypeTime() {
		assertEquals(TemporalType.TIME, TypeHelper.toTemporalType("time"));
	}

	@Test
	public void testToTemporalTypeTimestamp() {
		assertEquals(TemporalType.TIMESTAMP, TypeHelper.toTemporalType("timestamp"));
	}

	@Test
	public void testToTemporalTypeNonTemporal() {
		assertNull(TypeHelper.toTemporalType("string"));
	}

	// ================================================================
	// isLob
	// ================================================================

	@Test
	public void testIsLobNull() {
		assertFalse(TypeHelper.isLob(null));
	}

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

	// ================================================================
	// toHibernateType(Class<?>)
	// ================================================================

	@Test
	public void testToHibernateTypeFromClassNull() {
		assertEquals("serializable", TypeHelper.toHibernateType((Class<?>) null));
	}

	@Test
	public void testToHibernateTypeFromClassPrimitives() {
		assertEquals("int", TypeHelper.toHibernateType(int.class));
		assertEquals("long", TypeHelper.toHibernateType(long.class));
		assertEquals("short", TypeHelper.toHibernateType(short.class));
		assertEquals("byte", TypeHelper.toHibernateType(byte.class));
		assertEquals("float", TypeHelper.toHibernateType(float.class));
		assertEquals("double", TypeHelper.toHibernateType(double.class));
		assertEquals("boolean", TypeHelper.toHibernateType(boolean.class));
		assertEquals("character", TypeHelper.toHibernateType(char.class));
	}

	@Test
	public void testToHibernateTypeFromClassWrappers() {
		assertEquals("java.lang.Integer", TypeHelper.toHibernateType(Integer.class));
		assertEquals("java.lang.Long", TypeHelper.toHibernateType(Long.class));
		assertEquals("java.lang.Short", TypeHelper.toHibernateType(Short.class));
		assertEquals("java.lang.Byte", TypeHelper.toHibernateType(Byte.class));
		assertEquals("java.lang.Float", TypeHelper.toHibernateType(Float.class));
		assertEquals("java.lang.Double", TypeHelper.toHibernateType(Double.class));
		assertEquals("java.lang.Boolean", TypeHelper.toHibernateType(Boolean.class));
		assertEquals("java.lang.Character", TypeHelper.toHibernateType(Character.class));
	}

	@Test
	public void testToHibernateTypeFromClassOther() {
		assertEquals("string", TypeHelper.toHibernateType(String.class));
		assertEquals("big_decimal", TypeHelper.toHibernateType(BigDecimal.class));
		assertEquals("big_integer", TypeHelper.toHibernateType(BigInteger.class));
		assertEquals("timestamp", TypeHelper.toHibernateType(Date.class));
		assertEquals("binary", TypeHelper.toHibernateType(byte[].class));
	}

	@Test
	public void testToHibernateTypeFromClassUnknown() {
		assertEquals(TypeHelperTest.class.getName(), TypeHelper.toHibernateType(TypeHelperTest.class));
	}

	// ================================================================
	// toHibernateType(String)
	// ================================================================

	@Test
	public void testToHibernateTypeFromStringNull() {
		assertEquals("serializable", TypeHelper.toHibernateType((String) null));
	}

	@Test
	public void testToHibernateTypeFromStringKnownClass() {
		assertEquals("string", TypeHelper.toHibernateType("java.lang.String"));
		assertEquals("int", TypeHelper.toHibernateType("int"));
	}

	@Test
	public void testToHibernateTypeFromStringUnknownClass() {
		assertEquals("com.example.Unknown", TypeHelper.toHibernateType("com.example.Unknown"));
	}

	// ================================================================
	// getJdbcTypeCode
	// ================================================================

	@Test
	public void testGetJdbcTypeCodeNull() {
		assertEquals(Integer.MIN_VALUE, TypeHelper.getJdbcTypeCode(null));
	}

	@Test
	public void testGetJdbcTypeCodeString() {
		assertEquals(Types.VARCHAR, TypeHelper.getJdbcTypeCode(String.class.getName()));
	}

	@Test
	public void testGetJdbcTypeCodeLong() {
		assertEquals(Types.BIGINT, TypeHelper.getJdbcTypeCode(Long.class.getName()));
		assertEquals(Types.BIGINT, TypeHelper.getJdbcTypeCode(long.class.getName()));
	}

	@Test
	public void testGetJdbcTypeCodeInteger() {
		assertEquals(Types.INTEGER, TypeHelper.getJdbcTypeCode(Integer.class.getName()));
		assertEquals(Types.INTEGER, TypeHelper.getJdbcTypeCode(int.class.getName()));
	}

	@Test
	public void testGetJdbcTypeCodeShort() {
		assertEquals(Types.SMALLINT, TypeHelper.getJdbcTypeCode(Short.class.getName()));
	}

	@Test
	public void testGetJdbcTypeCodeByte() {
		assertEquals(Types.TINYINT, TypeHelper.getJdbcTypeCode(Byte.class.getName()));
	}

	@Test
	public void testGetJdbcTypeCodeFloat() {
		assertEquals(Types.FLOAT, TypeHelper.getJdbcTypeCode(Float.class.getName()));
	}

	@Test
	public void testGetJdbcTypeCodeDouble() {
		assertEquals(Types.DOUBLE, TypeHelper.getJdbcTypeCode(Double.class.getName()));
	}

	@Test
	public void testGetJdbcTypeCodeBoolean() {
		assertEquals(Types.BOOLEAN, TypeHelper.getJdbcTypeCode(Boolean.class.getName()));
	}

	@Test
	public void testGetJdbcTypeCodeBigDecimal() {
		assertEquals(Types.NUMERIC, TypeHelper.getJdbcTypeCode(BigDecimal.class.getName()));
	}

	@Test
	public void testGetJdbcTypeCodeDate() {
		assertEquals(Types.DATE, TypeHelper.getJdbcTypeCode(java.sql.Date.class.getName()));
		assertEquals(Types.TIME, TypeHelper.getJdbcTypeCode(java.sql.Time.class.getName()));
		assertEquals(Types.TIMESTAMP, TypeHelper.getJdbcTypeCode(java.sql.Timestamp.class.getName()));
		assertEquals(Types.TIMESTAMP, TypeHelper.getJdbcTypeCode(Date.class.getName()));
	}

	@Test
	public void testGetJdbcTypeCodeBinary() {
		assertEquals(Types.VARBINARY, TypeHelper.getJdbcTypeCode(byte[].class.getName()));
	}

	@Test
	public void testGetJdbcTypeCodeCharacter() {
		assertEquals(Types.CHAR, TypeHelper.getJdbcTypeCode(Character.class.getName()));
		assertEquals(Types.CHAR, TypeHelper.getJdbcTypeCode(char.class.getName()));
	}

	@Test
	public void testGetJdbcTypeCodeUnknown() {
		assertEquals(Integer.MIN_VALUE, TypeHelper.getJdbcTypeCode("com.example.Unknown"));
	}

	// ================================================================
	// JDBC type name <-> integer constant
	// ================================================================

	@Test
	public void testGetJDBCTypes() {
		String[] types = TypeHelper.getJDBCTypes();
		assertNotNull(types);
		assertTrue(types.length > 0);
	}

	@Test
	public void testGetJDBCTypeByName() {
		assertEquals(Types.VARCHAR, TypeHelper.getJDBCType("VARCHAR"));
		assertEquals(Types.INTEGER, TypeHelper.getJDBCType("INTEGER"));
		assertEquals(Types.TIMESTAMP, TypeHelper.getJDBCType("TIMESTAMP"));
	}

	@Test
	public void testGetJDBCTypeByNumericString() {
		assertEquals(Types.VARCHAR, TypeHelper.getJDBCType(String.valueOf(Types.VARCHAR)));
	}

	@Test
	public void testGetJDBCTypeByInvalidString() {
		assertThrows(org.hibernate.MappingException.class,
				() -> TypeHelper.getJDBCType("NOT_A_TYPE"));
	}

	@Test
	public void testGetJDBCTypeName() {
		assertEquals("VARCHAR", TypeHelper.getJDBCTypeName(Types.VARCHAR));
		assertEquals("INTEGER", TypeHelper.getJDBCTypeName(Types.INTEGER));
		assertEquals("TIMESTAMP", TypeHelper.getJDBCTypeName(Types.TIMESTAMP));
	}

	@Test
	public void testGetJDBCTypeNameUnknown() {
		String name = TypeHelper.getJDBCTypeName(99999);
		assertEquals("99999", name);
	}

	// ================================================================
	// SQL type metadata
	// ================================================================

	@Test
	public void testTypeHasScale() {
		assertTrue(TypeHelper.typeHasScale(Types.DECIMAL));
		assertTrue(TypeHelper.typeHasScale(Types.NUMERIC));
		assertFalse(TypeHelper.typeHasScale(Types.VARCHAR));
		assertFalse(TypeHelper.typeHasScale(Types.INTEGER));
	}

	@Test
	public void testTypeHasPrecision() {
		assertTrue(TypeHelper.typeHasPrecision(Types.DECIMAL));
		assertTrue(TypeHelper.typeHasPrecision(Types.NUMERIC));
		assertTrue(TypeHelper.typeHasPrecision(Types.REAL));
		assertTrue(TypeHelper.typeHasPrecision(Types.FLOAT));
		assertTrue(TypeHelper.typeHasPrecision(Types.DOUBLE));
		assertFalse(TypeHelper.typeHasPrecision(Types.VARCHAR));
	}

	@Test
	public void testTypeHasScaleAndPrecision() {
		assertTrue(TypeHelper.typeHasScaleAndPrecision(Types.DECIMAL));
		assertTrue(TypeHelper.typeHasScaleAndPrecision(Types.NUMERIC));
		assertFalse(TypeHelper.typeHasScaleAndPrecision(Types.REAL));
		assertFalse(TypeHelper.typeHasScaleAndPrecision(Types.VARCHAR));
	}

	@Test
	public void testTypeHasLength() {
		assertTrue(TypeHelper.typeHasLength(Types.CHAR));
		assertTrue(TypeHelper.typeHasLength(Types.VARCHAR));
		assertTrue(TypeHelper.typeHasLength(Types.LONGVARCHAR));
		assertTrue(TypeHelper.typeHasLength(Types.DATE));
		assertTrue(TypeHelper.typeHasLength(Types.TIME));
		assertTrue(TypeHelper.typeHasLength(Types.TIMESTAMP));
		assertFalse(TypeHelper.typeHasLength(Types.INTEGER));
		assertFalse(TypeHelper.typeHasLength(Types.BLOB));
	}

	// ================================================================
	// isPrimitiveType
	// ================================================================

	@Test
	public void testIsPrimitiveTypeNull() {
		assertFalse(TypeHelper.isPrimitiveType(null));
	}

	@Test
	public void testIsPrimitiveTypePrimitives() {
		assertTrue(TypeHelper.isPrimitiveType("boolean"));
		assertTrue(TypeHelper.isPrimitiveType("byte"));
		assertTrue(TypeHelper.isPrimitiveType("char"));
		assertTrue(TypeHelper.isPrimitiveType("short"));
		assertTrue(TypeHelper.isPrimitiveType("int"));
		assertTrue(TypeHelper.isPrimitiveType("long"));
		assertTrue(TypeHelper.isPrimitiveType("float"));
		assertTrue(TypeHelper.isPrimitiveType("double"));
	}

	@Test
	public void testIsPrimitiveTypeNonPrimitive() {
		assertFalse(TypeHelper.isPrimitiveType("string"));
		assertFalse(TypeHelper.isPrimitiveType("java.lang.Integer"));
		assertFalse(TypeHelper.isPrimitiveType("big_decimal"));
	}
}
