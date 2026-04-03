/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.strategy;

import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
	public void testConstructorFull() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.DECIMAL, 10, 5, 2, true);
		assertEquals(Types.DECIMAL, mapping.getJDBCType());
		assertEquals(10, mapping.getLength());
		assertEquals(5, mapping.getPrecision());
		assertEquals(2, mapping.getScale());
		assertTrue(mapping.getNullable());
	}

	@Test
	public void testSettersAndGetters() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.INTEGER);
		mapping.setLength(100);
		mapping.setPrecision(10);
		mapping.setScale(2);
		mapping.setNullable(false);
		mapping.setHibernateType("int");

		assertEquals(100, mapping.getLength());
		assertEquals(10, mapping.getPrecision());
		assertEquals(2, mapping.getScale());
		assertFalse(mapping.getNullable());
		assertEquals("int", mapping.getHibernateType());
	}

	@Test
	public void testMatchExact() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.VARCHAR, 255, 0, 0, false);
		assertTrue(mapping.match(Types.VARCHAR, 255, 0, 0, false));
	}

	@Test
	public void testMatchDifferentJdbcType() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.VARCHAR, 255, 0, 0, false);
		assertFalse(mapping.match(Types.INTEGER, 255, 0, 0, false));
	}

	@Test
	public void testMatchUnknownLength() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.VARCHAR);
		assertTrue(mapping.match(Types.VARCHAR, 100, 5, 3, true));
	}

	@Test
	public void testMatchDifferentLength() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.VARCHAR, 255, SQLTypeMapping.UNKNOWN_PRECISION, SQLTypeMapping.UNKNOWN_SCALE, null);
		assertFalse(mapping.match(Types.VARCHAR, 100, 0, 0, false));
	}

	@Test
	public void testMatchNullableNull() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.VARCHAR, 255, 0, 0, null);
		assertTrue(mapping.match(Types.VARCHAR, 255, 0, 0, true));
		assertTrue(mapping.match(Types.VARCHAR, 255, 0, 0, false));
	}

	@Test
	public void testMatchNullableMismatch() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.VARCHAR, 255, 0, 0, true);
		assertFalse(mapping.match(Types.VARCHAR, 255, 0, 0, false));
	}

	@Test
	public void testCompareToEqual() {
		SQLTypeMapping m1 = new SQLTypeMapping(Types.VARCHAR, 255, 0, 0, true);
		SQLTypeMapping m2 = new SQLTypeMapping(Types.VARCHAR, 255, 0, 0, true);
		assertEquals(0, m1.compareTo(m2));
	}

	@Test
	public void testCompareToDifferentJdbcType() {
		SQLTypeMapping m1 = new SQLTypeMapping(Types.INTEGER);
		SQLTypeMapping m2 = new SQLTypeMapping(Types.VARCHAR);
		assertTrue(m1.compareTo(m2) != 0);
	}

	@Test
	public void testCompareToDifferentLength() {
		SQLTypeMapping m1 = new SQLTypeMapping(Types.VARCHAR, 100, 0, 0, null);
		SQLTypeMapping m2 = new SQLTypeMapping(Types.VARCHAR, 200, 0, 0, null);
		assertTrue(m1.compareTo(m2) < 0);
	}

	@Test
	public void testCompareToDifferentNullable() {
		SQLTypeMapping m1 = new SQLTypeMapping(Types.VARCHAR, 255, 0, 0, false);
		SQLTypeMapping m2 = new SQLTypeMapping(Types.VARCHAR, 255, 0, 0, true);
		assertTrue(m1.compareTo(m2) < 0);
	}

	@Test
	public void testCompareToNullVsNonNull() {
		SQLTypeMapping m1 = new SQLTypeMapping(Types.VARCHAR, 255, 0, 0, null);
		SQLTypeMapping m2 = new SQLTypeMapping(Types.VARCHAR, 255, 0, 0, true);
		assertTrue(m1.compareTo(m2) > 0);
	}

	@Test
	public void testEqualsTrue() {
		SQLTypeMapping m1 = new SQLTypeMapping(Types.VARCHAR, 255, 0, 0, true);
		SQLTypeMapping m2 = new SQLTypeMapping(Types.VARCHAR, 255, 0, 0, true);
		assertEquals(m1, m2);
	}

	@Test
	public void testEqualsFalse() {
		SQLTypeMapping m1 = new SQLTypeMapping(Types.VARCHAR, 255, 0, 0, true);
		SQLTypeMapping m2 = new SQLTypeMapping(Types.INTEGER, 255, 0, 0, true);
		assertFalse(m1.equals(m2));
	}

	@Test
	public void testEqualsNotSameClass() {
		SQLTypeMapping m = new SQLTypeMapping(Types.VARCHAR);
		assertFalse(m.equals("not a mapping"));
	}

	@Test
	public void testHashCodeConsistent() {
		SQLTypeMapping m1 = new SQLTypeMapping(Types.VARCHAR, 255, 0, 0, true);
		SQLTypeMapping m2 = new SQLTypeMapping(Types.VARCHAR, 255, 0, 0, true);
		assertEquals(m1.hashCode(), m2.hashCode());
	}

	@Test
	public void testToString() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.VARCHAR);
		mapping.setHibernateType("string");
		String s = mapping.toString();
		assertTrue(s.contains("string"));
		assertTrue(s.contains("" + Types.VARCHAR));
	}
}
