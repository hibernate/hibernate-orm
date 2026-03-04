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

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;

/**
 * Tests for {@link OneToManyMetadata}.
 *
 * @author Koen Aers
 */
public class OneToManyMetadataTest {

	@Test
	public void testConstructorAndDefaults() {
		OneToManyMetadata otm = new OneToManyMetadata(
			"employees", "department", "Employee", "com.example");

		assertEquals("employees", otm.getFieldName());
		assertEquals("department", otm.getMappedBy());
		assertEquals("Employee", otm.getElementEntityClassName());
		assertEquals("com.example", otm.getElementEntityPackage());
		assertNull(otm.getFetchType());
		assertNull(otm.getCascadeTypes());
		assertFalse(otm.isOrphanRemoval());
	}

	@Test
	public void testFetchType() {
		OneToManyMetadata otm = new OneToManyMetadata(
			"employees", "department", "Employee", "com.example")
			.fetchType(FetchType.EAGER);

		assertEquals(FetchType.EAGER, otm.getFetchType());
	}

	@Test
	public void testCascadeTypes() {
		OneToManyMetadata otm = new OneToManyMetadata(
			"employees", "department", "Employee", "com.example")
			.cascade(CascadeType.ALL);

		assertArrayEquals(new CascadeType[]{ CascadeType.ALL }, otm.getCascadeTypes());
	}

	@Test
	public void testMultipleCascadeTypes() {
		OneToManyMetadata otm = new OneToManyMetadata(
			"employees", "department", "Employee", "com.example")
			.cascade(CascadeType.PERSIST, CascadeType.MERGE);

		assertArrayEquals(
			new CascadeType[]{ CascadeType.PERSIST, CascadeType.MERGE },
			otm.getCascadeTypes());
	}

	@Test
	public void testOrphanRemoval() {
		OneToManyMetadata otm = new OneToManyMetadata(
			"employees", "department", "Employee", "com.example")
			.orphanRemoval(true);

		assertTrue(otm.isOrphanRemoval());
	}

	@Test
	public void testFluentChaining() {
		OneToManyMetadata otm = new OneToManyMetadata(
			"employees", "department", "Employee", "com.example")
			.fetchType(FetchType.LAZY)
			.cascade(CascadeType.ALL)
			.orphanRemoval(true);

		assertEquals(FetchType.LAZY, otm.getFetchType());
		assertArrayEquals(new CascadeType[]{ CascadeType.ALL }, otm.getCascadeTypes());
		assertTrue(otm.isOrphanRemoval());
	}
}
