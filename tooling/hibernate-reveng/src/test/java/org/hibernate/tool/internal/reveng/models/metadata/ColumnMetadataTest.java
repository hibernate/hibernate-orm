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
package org.hibernate.tool.internal.reveng.models.metadata;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import jakarta.persistence.FetchType;
import jakarta.persistence.TemporalType;

/**
 * Tests for {@link ColumnMetadata}.
 *
 * @author Koen Aers
 */
public class ColumnMetadataTest {

	@Test
	public void testConstructorAndDefaults() {
		ColumnMetadata column = new ColumnMetadata("USER_NAME", "userName", String.class);

		assertEquals("USER_NAME", column.getColumnName());
		assertEquals("userName", column.getFieldName());
		assertEquals(String.class, column.getJavaType());
		assertTrue(column.isNullable(), "Should default to nullable");
		assertFalse(column.isPrimaryKey());
		assertFalse(column.isAutoIncrement());
		assertFalse(column.isVersion());
		assertFalse(column.isLob());
		assertEquals(0, column.getLength());
		assertEquals(0, column.getPrecision());
		assertEquals(0, column.getScale());
		assertNull(column.getBasicFetchType());
		assertNull(column.getTemporalType());
		assertFalse(column.isBasicOptionalSet());
		assertTrue(column.isBasicOptional(), "Should default to true when not set");
	}

	@Test
	public void testPrimaryKeySetsNullableToFalse() {
		ColumnMetadata column = new ColumnMetadata("ID", "id", Long.class)
			.primaryKey(true);

		assertTrue(column.isPrimaryKey());
		assertFalse(column.isNullable(), "primaryKey(true) should set nullable to false");
	}

	@Test
	public void testNullableCanBeOverriddenAfterPrimaryKey() {
		ColumnMetadata column = new ColumnMetadata("ID", "id", Long.class)
			.primaryKey(true)
			.nullable(true);

		assertTrue(column.isNullable());
	}

	@Test
	public void testAutoIncrement() {
		ColumnMetadata column = new ColumnMetadata("ID", "id", Long.class)
			.autoIncrement(true);

		assertTrue(column.isAutoIncrement());
	}

	@Test
	public void testLengthPrecisionScale() {
		ColumnMetadata column = new ColumnMetadata("AMOUNT", "amount", java.math.BigDecimal.class)
			.length(255)
			.precision(10)
			.scale(2);

		assertEquals(255, column.getLength());
		assertEquals(10, column.getPrecision());
		assertEquals(2, column.getScale());
	}

	@Test
	public void testVersion() {
		ColumnMetadata column = new ColumnMetadata("VERSION", "version", Long.class)
			.version(true);

		assertTrue(column.isVersion());
	}

	@Test
	public void testBasicFetchType() {
		ColumnMetadata column = new ColumnMetadata("DATA", "data", String.class)
			.basicFetch(FetchType.LAZY);

		assertEquals(FetchType.LAZY, column.getBasicFetchType());
	}

	@Test
	public void testBasicOptionalExplicitlySet() {
		ColumnMetadata column = new ColumnMetadata("STATUS", "status", String.class)
			.basicOptional(false);

		assertTrue(column.isBasicOptionalSet());
		assertFalse(column.isBasicOptional());
	}

	@Test
	public void testBasicOptionalNotSet() {
		ColumnMetadata column = new ColumnMetadata("STATUS", "status", String.class);

		assertFalse(column.isBasicOptionalSet());
		assertTrue(column.isBasicOptional(), "Should default to true when not explicitly set");
	}

	@Test
	public void testTemporalType() {
		ColumnMetadata column = new ColumnMetadata("EVENT_DATE", "eventDate", java.util.Date.class)
			.temporal(TemporalType.TIMESTAMP);

		assertEquals(TemporalType.TIMESTAMP, column.getTemporalType());
	}

	@Test
	public void testLob() {
		ColumnMetadata column = new ColumnMetadata("CONTENT", "content", byte[].class)
			.lob(true);

		assertTrue(column.isLob());
	}

	@Test
	public void testFluentChaining() {
		ColumnMetadata column = new ColumnMetadata("ID", "id", Long.class)
			.primaryKey(true)
			.autoIncrement(true)
			.length(10)
			.precision(5)
			.scale(2);

		assertTrue(column.isPrimaryKey());
		assertTrue(column.isAutoIncrement());
		assertEquals(10, column.getLength());
		assertEquals(5, column.getPrecision());
		assertEquals(2, column.getScale());
	}
}
