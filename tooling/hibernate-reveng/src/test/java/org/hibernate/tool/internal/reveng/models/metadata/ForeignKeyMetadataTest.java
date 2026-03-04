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

/**
 * Tests for {@link ForeignKeyMetadata}.
 *
 * @author Koen Aers
 */
public class ForeignKeyMetadataTest {

	@Test
	public void testConstructorAndDefaults() {
		ForeignKeyMetadata fk = new ForeignKeyMetadata(
			"department", "DEPARTMENT_ID", "Department", "com.example");

		assertEquals("department", fk.getFieldName());
		assertEquals("DEPARTMENT_ID", fk.getForeignKeyColumnName());
		assertEquals("Department", fk.getTargetEntityClassName());
		assertEquals("com.example", fk.getTargetEntityPackage());
		assertTrue(fk.isOptional(), "Should default to optional");
		assertNull(fk.getFetchType());
		assertNull(fk.getReferencedColumnName());
	}

	@Test
	public void testReferencedColumnName() {
		ForeignKeyMetadata fk = new ForeignKeyMetadata(
			"department", "DEPT_ID", "Department", "com.example")
			.referencedColumnName("ID");

		assertEquals("ID", fk.getReferencedColumnName());
	}

	@Test
	public void testFetchType() {
		ForeignKeyMetadata fk = new ForeignKeyMetadata(
			"department", "DEPARTMENT_ID", "Department", "com.example")
			.fetchType(FetchType.LAZY);

		assertEquals(FetchType.LAZY, fk.getFetchType());
	}

	@Test
	public void testOptional() {
		ForeignKeyMetadata fk = new ForeignKeyMetadata(
			"department", "DEPARTMENT_ID", "Department", "com.example")
			.optional(false);

		assertFalse(fk.isOptional());
	}

	@Test
	public void testFluentChaining() {
		ForeignKeyMetadata fk = new ForeignKeyMetadata(
			"department", "DEPARTMENT_ID", "Department", "com.example")
			.referencedColumnName("DEPT_PK")
			.fetchType(FetchType.EAGER)
			.optional(false);

		assertEquals("DEPT_PK", fk.getReferencedColumnName());
		assertEquals(FetchType.EAGER, fk.getFetchType());
		assertFalse(fk.isOptional());
	}
}
