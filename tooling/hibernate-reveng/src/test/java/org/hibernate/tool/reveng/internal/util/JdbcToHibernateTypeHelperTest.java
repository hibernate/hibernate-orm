/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.util;

import org.hibernate.MappingException;
import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JdbcToHibernateTypeHelperTest {

	// --- getPreferredHibernateType ---

	@Test
	public void testGetPreferredHibernateTypeVarchar() {
		assertEquals("string",
				JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.VARCHAR, 255, 0, 0, false, false));
	}

	@Test
	public void testGetPreferredHibernateTypeVarcharNullable() {
		assertEquals("string",
				JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.VARCHAR, 255, 0, 0, true, false));
	}

	@Test
	public void testGetPreferredHibernateTypeIntegerNotNullable() {
		assertEquals("int",
				JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.INTEGER, 0, 0, 0, false, false));
	}

	@Test
	public void testGetPreferredHibernateTypeIntegerNullable() {
		assertEquals(Integer.class.getName(),
				JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.INTEGER, 0, 0, 0, true, false));
	}

	@Test
	public void testGetPreferredHibernateTypeBooleanNotNullable() {
		assertEquals("boolean",
				JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.BOOLEAN, 0, 0, 0, false, false));
	}

	@Test
	public void testGetPreferredHibernateTypeNumericPrecision1() {
		assertEquals("boolean",
				JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.NUMERIC, 0, 1, 0, false, false));
	}

	@Test
	public void testGetPreferredHibernateTypeNumericPrecision1Nullable() {
		assertEquals(Boolean.class.getName(),
				JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.NUMERIC, 0, 1, 0, true, false));
	}

	@Test
	public void testGetPreferredHibernateTypeNumericPrecision2() {
		assertEquals("byte",
				JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.NUMERIC, 0, 2, 0, false, false));
	}

	@Test
	public void testGetPreferredHibernateTypeNumericPrecision4() {
		assertEquals("short",
				JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.NUMERIC, 0, 4, 0, false, false));
	}

	@Test
	public void testGetPreferredHibernateTypeNumericPrecision9() {
		assertEquals("int",
				JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.NUMERIC, 0, 9, 0, false, false));
	}

	@Test
	public void testGetPreferredHibernateTypeNumericPrecision18() {
		assertEquals("long",
				JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.NUMERIC, 0, 18, 0, false, false));
	}

	@Test
	public void testGetPreferredHibernateTypeNumericPrecision19() {
		assertEquals("big_integer",
				JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.NUMERIC, 0, 19, 0, false, false));
	}

	@Test
	public void testGetPreferredHibernateTypeNumericWithScale() {
		assertEquals("big_decimal",
				JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.DECIMAL, 0, 10, 2, false, false));
	}

	@Test
	public void testGetPreferredHibernateTypeCharSize1() {
		assertEquals("char",
				JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.CHAR, 1, 0, 0, false, false));
	}

	@Test
	public void testGetPreferredHibernateTypeCharSizeGreaterThan1() {
		assertEquals("string",
				JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.CHAR, 50, 0, 0, false, false));
	}

	@Test
	public void testGetPreferredHibernateTypeBlob() {
		assertEquals("blob",
				JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.BLOB, 0, 0, 0, false, false));
	}

	@Test
	public void testGetPreferredHibernateTypeClob() {
		assertEquals("clob",
				JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.CLOB, 0, 0, 0, false, false));
	}

	@Test
	public void testGetPreferredHibernateTypeTimestamp() {
		assertEquals("timestamp",
				JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.TIMESTAMP, 0, 0, 0, false, false));
	}

	@Test
	public void testGetPreferredHibernateTypeUnknown() {
		assertNull(JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.ARRAY, 0, 0, 0, false, false));
	}

	@Test
	public void testGetPreferredHibernateTypeGeneratedIdentifier() {
		assertEquals(Integer.class.getName(),
				JdbcToHibernateTypeHelper.getPreferredHibernateType(Types.INTEGER, 0, 0, 0, false, true));
	}

	// --- getJDBCTypes ---

	@Test
	public void testGetJDBCTypesNotEmpty() {
		String[] types = JdbcToHibernateTypeHelper.getJDBCTypes();
		assertNotNull(types);
		assertTrue(types.length > 0);
	}

	@Test
	public void testGetJDBCTypesContainsVarchar() {
		String[] types = JdbcToHibernateTypeHelper.getJDBCTypes();
		boolean found = false;
		for (String type : types) {
			if ("VARCHAR".equals(type)) {
				found = true;
				break;
			}
		}
		assertTrue(found);
	}

	// --- getJDBCType ---

	@Test
	public void testGetJDBCTypeByName() {
		assertEquals(Types.VARCHAR, JdbcToHibernateTypeHelper.getJDBCType("VARCHAR"));
	}

	@Test
	public void testGetJDBCTypeByNumber() {
		assertEquals(12, JdbcToHibernateTypeHelper.getJDBCType("12"));
	}

	@Test
	public void testGetJDBCTypeInvalidThrows() {
		assertThrows(MappingException.class,
				() -> JdbcToHibernateTypeHelper.getJDBCType("NOT_A_TYPE"));
	}

	// --- getJDBCTypeName ---

	@Test
	public void testGetJDBCTypeNameKnown() {
		assertEquals("VARCHAR", JdbcToHibernateTypeHelper.getJDBCTypeName(Types.VARCHAR));
	}

	@Test
	public void testGetJDBCTypeNameUnknown() {
		assertEquals("99999", JdbcToHibernateTypeHelper.getJDBCTypeName(99999));
	}

	// --- typeHasScale / typeHasPrecision / typeHasLength ---

	@Test
	public void testTypeHasScale() {
		assertTrue(JdbcToHibernateTypeHelper.typeHasScale(Types.DECIMAL));
		assertTrue(JdbcToHibernateTypeHelper.typeHasScale(Types.NUMERIC));
		assertFalse(JdbcToHibernateTypeHelper.typeHasScale(Types.VARCHAR));
		assertFalse(JdbcToHibernateTypeHelper.typeHasScale(Types.INTEGER));
	}

	@Test
	public void testTypeHasPrecision() {
		assertTrue(JdbcToHibernateTypeHelper.typeHasPrecision(Types.DECIMAL));
		assertTrue(JdbcToHibernateTypeHelper.typeHasPrecision(Types.NUMERIC));
		assertTrue(JdbcToHibernateTypeHelper.typeHasPrecision(Types.REAL));
		assertTrue(JdbcToHibernateTypeHelper.typeHasPrecision(Types.FLOAT));
		assertTrue(JdbcToHibernateTypeHelper.typeHasPrecision(Types.DOUBLE));
		assertFalse(JdbcToHibernateTypeHelper.typeHasPrecision(Types.VARCHAR));
	}

	@Test
	public void testTypeHasScaleAndPrecision() {
		assertTrue(JdbcToHibernateTypeHelper.typeHasScaleAndPrecision(Types.DECIMAL));
		assertFalse(JdbcToHibernateTypeHelper.typeHasScaleAndPrecision(Types.REAL));
		assertFalse(JdbcToHibernateTypeHelper.typeHasScaleAndPrecision(Types.VARCHAR));
	}

	@Test
	public void testTypeHasLength() {
		assertTrue(JdbcToHibernateTypeHelper.typeHasLength(Types.VARCHAR));
		assertTrue(JdbcToHibernateTypeHelper.typeHasLength(Types.CHAR));
		assertTrue(JdbcToHibernateTypeHelper.typeHasLength(Types.LONGVARCHAR));
		assertTrue(JdbcToHibernateTypeHelper.typeHasLength(Types.DATE));
		assertTrue(JdbcToHibernateTypeHelper.typeHasLength(Types.TIME));
		assertTrue(JdbcToHibernateTypeHelper.typeHasLength(Types.TIMESTAMP));
		assertFalse(JdbcToHibernateTypeHelper.typeHasLength(Types.INTEGER));
		assertFalse(JdbcToHibernateTypeHelper.typeHasLength(Types.BLOB));
	}
}
