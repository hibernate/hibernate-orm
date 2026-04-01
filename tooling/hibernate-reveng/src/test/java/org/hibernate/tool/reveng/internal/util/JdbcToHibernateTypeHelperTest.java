/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.util;

import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JdbcToHibernateTypeHelperTest {

	@Test
	public void testGetPreferredTypeVarchar() {
		assertEquals("string", JdbcToHibernateTypeHelper.getPreferredHibernateType(
				Types.VARCHAR, 255, 0, 0, false, false));
	}

	@Test
	public void testGetPreferredTypeVarcharNullable() {
		assertEquals("string", JdbcToHibernateTypeHelper.getPreferredHibernateType(
				Types.VARCHAR, 255, 0, 0, true, false));
	}

	@Test
	public void testGetPreferredTypeInteger() {
		assertEquals("int", JdbcToHibernateTypeHelper.getPreferredHibernateType(
				Types.INTEGER, 0, 0, 0, false, false));
	}

	@Test
	public void testGetPreferredTypeIntegerNullable() {
		assertEquals(Integer.class.getName(), JdbcToHibernateTypeHelper.getPreferredHibernateType(
				Types.INTEGER, 0, 0, 0, true, false));
	}

	@Test
	public void testGetPreferredTypeNumericPrecision1() {
		assertEquals("boolean", JdbcToHibernateTypeHelper.getPreferredHibernateType(
				Types.NUMERIC, 0, 1, 0, false, false));
	}

	@Test
	public void testGetPreferredTypeNumericPrecision2() {
		assertEquals("byte", JdbcToHibernateTypeHelper.getPreferredHibernateType(
				Types.NUMERIC, 0, 2, 0, false, false));
	}

	@Test
	public void testGetPreferredTypeNumericPrecision4() {
		assertEquals("short", JdbcToHibernateTypeHelper.getPreferredHibernateType(
				Types.NUMERIC, 0, 4, 0, false, false));
	}

	@Test
	public void testGetPreferredTypeNumericPrecision9() {
		assertEquals("int", JdbcToHibernateTypeHelper.getPreferredHibernateType(
				Types.NUMERIC, 0, 9, 0, false, false));
	}

	@Test
	public void testGetPreferredTypeNumericPrecision18() {
		assertEquals("long", JdbcToHibernateTypeHelper.getPreferredHibernateType(
				Types.NUMERIC, 0, 18, 0, false, false));
	}

	@Test
	public void testGetPreferredTypeNumericPrecision20() {
		assertEquals("big_integer", JdbcToHibernateTypeHelper.getPreferredHibernateType(
				Types.NUMERIC, 0, 20, 0, false, false));
	}

	@Test
	public void testGetPreferredTypeNumericWithScale() {
		assertEquals("big_decimal", JdbcToHibernateTypeHelper.getPreferredHibernateType(
				Types.NUMERIC, 0, 10, 2, false, false));
	}

	@Test
	public void testGetPreferredTypeCharSizeGreaterThan1() {
		assertEquals("string", JdbcToHibernateTypeHelper.getPreferredHibernateType(
				Types.CHAR, 10, 0, 0, false, false));
	}

	@Test
	public void testGetPreferredTypeCharSize1() {
		assertEquals("char", JdbcToHibernateTypeHelper.getPreferredHibernateType(
				Types.CHAR, 1, 0, 0, false, false));
	}

	@Test
	public void testGetPreferredTypeGeneratedIdentifier() {
		// generatedIdentifier forces nullable type
		assertEquals(Integer.class.getName(), JdbcToHibernateTypeHelper.getPreferredHibernateType(
				Types.INTEGER, 0, 0, 0, false, true));
	}

	@Test
	public void testGetPreferredTypeUnknown() {
		assertNull(JdbcToHibernateTypeHelper.getPreferredHibernateType(
				Types.OTHER, 0, 0, 0, false, false));
	}

	@Test
	public void testGetJDBCTypes() {
		String[] types = JdbcToHibernateTypeHelper.getJDBCTypes();
		assertNotNull(types);
		assertTrue(types.length > 0);
	}

	@Test
	public void testGetJDBCType() {
		assertEquals(Types.VARCHAR, JdbcToHibernateTypeHelper.getJDBCType("VARCHAR"));
		assertEquals(Types.INTEGER, JdbcToHibernateTypeHelper.getJDBCType("INTEGER"));
	}

	@Test
	public void testGetJDBCTypeName() {
		assertEquals("VARCHAR", JdbcToHibernateTypeHelper.getJDBCTypeName(Types.VARCHAR));
		assertEquals("INTEGER", JdbcToHibernateTypeHelper.getJDBCTypeName(Types.INTEGER));
	}

	@Test
	public void testGetJDBCTypeNameUnknown() {
		String name = JdbcToHibernateTypeHelper.getJDBCTypeName(99999);
		assertEquals("99999", name);
	}

	@Test
	public void testTypeHasScale() {
		assertTrue(JdbcToHibernateTypeHelper.typeHasScale(Types.DECIMAL));
		assertTrue(JdbcToHibernateTypeHelper.typeHasScale(Types.NUMERIC));
		assertFalse(JdbcToHibernateTypeHelper.typeHasScale(Types.VARCHAR));
	}

	@Test
	public void testTypeHasPrecision() {
		assertTrue(JdbcToHibernateTypeHelper.typeHasPrecision(Types.DECIMAL));
		assertTrue(JdbcToHibernateTypeHelper.typeHasPrecision(Types.FLOAT));
		assertFalse(JdbcToHibernateTypeHelper.typeHasPrecision(Types.VARCHAR));
	}

	@Test
	public void testTypeHasScaleAndPrecision() {
		assertTrue(JdbcToHibernateTypeHelper.typeHasScaleAndPrecision(Types.DECIMAL));
		assertFalse(JdbcToHibernateTypeHelper.typeHasScaleAndPrecision(Types.FLOAT));
	}

	@Test
	public void testTypeHasLength() {
		assertTrue(JdbcToHibernateTypeHelper.typeHasLength(Types.VARCHAR));
		assertTrue(JdbcToHibernateTypeHelper.typeHasLength(Types.CHAR));
		assertFalse(JdbcToHibernateTypeHelper.typeHasLength(Types.INTEGER));
	}
}
