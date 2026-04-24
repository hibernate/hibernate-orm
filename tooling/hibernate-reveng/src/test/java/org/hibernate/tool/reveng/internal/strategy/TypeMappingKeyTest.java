/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
