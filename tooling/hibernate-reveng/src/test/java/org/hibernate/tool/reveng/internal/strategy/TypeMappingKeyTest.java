/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.strategy;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Types;

import org.junit.jupiter.api.Test;

class TypeMappingKeyTest {

	@Test
	void testEqualsSameValues() {
		TypeMappingKey a = new TypeMappingKey(Types.VARCHAR, 255);
		TypeMappingKey b = new TypeMappingKey(Types.VARCHAR, 255);
		assertEquals(a, b);
	}

	@Test
	void testEqualsDifferentType() {
		TypeMappingKey a = new TypeMappingKey(Types.VARCHAR, 255);
		TypeMappingKey b = new TypeMappingKey(Types.INTEGER, 255);
		assertNotEquals(a, b);
	}

	@Test
	void testEqualsDifferentLength() {
		TypeMappingKey a = new TypeMappingKey(Types.VARCHAR, 255);
		TypeMappingKey b = new TypeMappingKey(Types.VARCHAR, 100);
		assertNotEquals(a, b);
	}

	@Test
	void testEqualsNull() {
		TypeMappingKey a = new TypeMappingKey(Types.VARCHAR, 255);
		assertNotEquals(null, a);
	}

	@Test
	void testHashCodeConsistent() {
		TypeMappingKey a = new TypeMappingKey(Types.VARCHAR, 255);
		TypeMappingKey b = new TypeMappingKey(Types.VARCHAR, 255);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void testFromSQLTypeMapping() {
		SQLTypeMapping mapping = new SQLTypeMapping(Types.BIGINT);
		mapping.setLength(0);
		TypeMappingKey key = new TypeMappingKey(mapping);
		assertEquals(Types.BIGINT, key.type);
		assertEquals(0, key.length);
	}

	@Test
	void testToString() {
		TypeMappingKey key = new TypeMappingKey(Types.INTEGER, 10);
		String str = key.toString();
		assertTrue(str.contains("type:" + Types.INTEGER));
		assertTrue(str.contains("length:10"));
	}
}
