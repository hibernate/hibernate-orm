/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.strategy;

import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SQLTypeMappingTest {

	@Test
	public void testConstructorWithJdbcType() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.VARCHAR);
		assertEquals(Types.VARCHAR, mapping.getJDBCType());
		assertEquals(SQLTypeMapping.UNKNOWN_LENGTH, mapping.getLength());
		assertEquals(SQLTypeMapping.UNKNOWN_PRECISION, mapping.getPrecision());
		assertEquals(SQLTypeMapping.UNKNOWN_SCALE, mapping.getScale());
		assertNull(mapping.getNullable());
		assertNull(mapping.getHibernateType());
	}

	@Test
	public void testConstructorWithAllParams() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.DECIMAL, 10, 5, 2, Boolean.TRUE);
		assertEquals(Types.DECIMAL, mapping.getJDBCType());
		assertEquals(10, mapping.getLength());
		assertEquals(5, mapping.getPrecision());
		assertEquals(2, mapping.getScale());
		assertEquals(Boolean.TRUE, mapping.getNullable());
	}

	@Test
	public void testSetters() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.VARCHAR);
		mapping.setLength(255);
		mapping.setPrecision(10);
		mapping.setScale(2);
		mapping.setNullable(Boolean.FALSE);
		mapping.setHibernateType("string");

		assertEquals(255, mapping.getLength());
		assertEquals(10, mapping.getPrecision());
		assertEquals(2, mapping.getScale());
		assertEquals(Boolean.FALSE, mapping.getNullable());
		assertEquals("string", mapping.getHibernateType());
	}

	@Test
	public void testMatchExactMatch() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.VARCHAR, 255, 0, 0, Boolean.TRUE);
		assertTrue(mapping.match(Types.VARCHAR, 255, 0, 0, true));
	}

	@Test
	public void testMatchDifferentJdbcType() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.VARCHAR, 255, 0, 0, Boolean.TRUE);
		assertFalse(mapping.match(Types.INTEGER, 255, 0, 0, true));
	}

	@Test
	public void testMatchUnknownLengthMatchesAny() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.VARCHAR);
		assertTrue(mapping.match(Types.VARCHAR, 255, 10, 2, true));
	}

	@Test
	public void testMatchDifferentLength() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.VARCHAR, 100, SQLTypeMapping.UNKNOWN_PRECISION, SQLTypeMapping.UNKNOWN_SCALE, null);
		assertFalse(mapping.match(Types.VARCHAR, 255, 0, 0, true));
	}

	@Test
	public void testMatchNullableNull() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.VARCHAR, 255, 0, 0, null);
		// null nullable means "don't care" — should match both
		assertTrue(mapping.match(Types.VARCHAR, 255, 0, 0, true));
		assertTrue(mapping.match(Types.VARCHAR, 255, 0, 0, false));
	}

	@Test
	public void testMatchNullableMismatch() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.VARCHAR, 255, 0, 0, Boolean.TRUE);
		assertFalse(mapping.match(Types.VARCHAR, 255, 0, 0, false));
	}

	@Test
	public void testCompareToEqual() {
		SQLTypeMapping a = new SQLTypeMapping(Types.VARCHAR, 255, 10, 2, Boolean.TRUE);
		SQLTypeMapping b = new SQLTypeMapping(Types.VARCHAR, 255, 10, 2, Boolean.TRUE);
		assertEquals(0, a.compareTo(b));
	}

	@Test
	public void testCompareToDifferentJdbcType() {
		SQLTypeMapping a = new SQLTypeMapping(Types.VARCHAR);
		SQLTypeMapping b = new SQLTypeMapping(Types.INTEGER);
		assertNotEquals(0, a.compareTo(b));
	}

	@Test
	public void testCompareToDifferentLength() {
		SQLTypeMapping a = new SQLTypeMapping(Types.VARCHAR, 100, 0, 0, null);
		SQLTypeMapping b = new SQLTypeMapping(Types.VARCHAR, 200, 0, 0, null);
		assertTrue(a.compareTo(b) < 0);
		assertTrue(b.compareTo(a) > 0);
	}

	@Test
	public void testCompareToDifferentPrecision() {
		SQLTypeMapping a = new SQLTypeMapping(Types.VARCHAR, 100, 5, 0, null);
		SQLTypeMapping b = new SQLTypeMapping(Types.VARCHAR, 100, 10, 0, null);
		assertTrue(a.compareTo(b) < 0);
	}

	@Test
	public void testCompareToDifferentScale() {
		SQLTypeMapping a = new SQLTypeMapping(Types.VARCHAR, 100, 10, 1, null);
		SQLTypeMapping b = new SQLTypeMapping(Types.VARCHAR, 100, 10, 2, null);
		assertTrue(a.compareTo(b) < 0);
	}

	@Test
	public void testCompareToDifferentNullable() {
		SQLTypeMapping falseNull = new SQLTypeMapping(Types.VARCHAR, 100, 10, 2, Boolean.FALSE);
		SQLTypeMapping trueNull = new SQLTypeMapping(Types.VARCHAR, 100, 10, 2, Boolean.TRUE);
		SQLTypeMapping unknownNull = new SQLTypeMapping(Types.VARCHAR, 100, 10, 2, null);

		assertTrue(falseNull.compareTo(trueNull) < 0);
		assertTrue(trueNull.compareTo(unknownNull) < 0);
		assertTrue(falseNull.compareTo(unknownNull) < 0);
	}

	@Test
	public void testEqualsAndHashCode() {
		SQLTypeMapping a = new SQLTypeMapping(Types.VARCHAR, 255, 10, 2, Boolean.TRUE);
		SQLTypeMapping b = new SQLTypeMapping(Types.VARCHAR, 255, 10, 2, Boolean.TRUE);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void testNotEquals() {
		SQLTypeMapping a = new SQLTypeMapping(Types.VARCHAR, 255, 10, 2, Boolean.TRUE);
		SQLTypeMapping b = new SQLTypeMapping(Types.INTEGER, 255, 10, 2, Boolean.TRUE);
		assertNotEquals(a, b);
	}

	@Test
	public void testEqualsDifferentType() {
		SQLTypeMapping a = new SQLTypeMapping(Types.VARCHAR);
		assertNotEquals(a, "string");
	}

	@Test
	public void testToString() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.VARCHAR, 255, 10, 2, Boolean.TRUE);
		mapping.setHibernateType("string");
		String str = mapping.toString();
		assertTrue(str.contains("string"));
		assertTrue(str.contains("255"));
	}
}
