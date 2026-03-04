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
 * Tests for {@link OneToOneMetadata}.
 *
 * @author Koen Aers
 */
public class OneToOneMetadataTest {

	@Test
	public void testConstructorAndDefaults() {
		OneToOneMetadata oto = new OneToOneMetadata(
			"address", "Address", "com.example");

		assertEquals("address", oto.getFieldName());
		assertEquals("Address", oto.getTargetEntityClassName());
		assertEquals("com.example", oto.getTargetEntityPackage());
		assertTrue(oto.isOptional(), "Should default to optional");
		assertFalse(oto.isOrphanRemoval());
		assertNull(oto.getForeignKeyColumnName());
		assertNull(oto.getReferencedColumnName());
		assertNull(oto.getMappedBy());
		assertNull(oto.getFetchType());
		assertNull(oto.getCascadeTypes());
	}

	@Test
	public void testOwningSide() {
		OneToOneMetadata oto = new OneToOneMetadata(
			"address", "Address", "com.example")
			.foreignKeyColumnName("ADDRESS_ID")
			.fetchType(FetchType.LAZY)
			.cascade(CascadeType.ALL);

		assertEquals("ADDRESS_ID", oto.getForeignKeyColumnName());
		assertEquals(FetchType.LAZY, oto.getFetchType());
		assertArrayEquals(new CascadeType[]{ CascadeType.ALL }, oto.getCascadeTypes());
	}

	@Test
	public void testInverseSide() {
		OneToOneMetadata oto = new OneToOneMetadata(
			"user", "User", "com.example")
			.mappedBy("address")
			.fetchType(FetchType.LAZY);

		assertEquals("address", oto.getMappedBy());
		assertEquals(FetchType.LAZY, oto.getFetchType());
	}

	@Test
	public void testReferencedColumnName() {
		OneToOneMetadata oto = new OneToOneMetadata(
			"address", "Address", "com.example")
			.foreignKeyColumnName("ADDR_ID")
			.referencedColumnName("ADDRESS_PK");

		assertEquals("ADDRESS_PK", oto.getReferencedColumnName());
	}

	@Test
	public void testOptional() {
		OneToOneMetadata oto = new OneToOneMetadata(
			"address", "Address", "com.example")
			.optional(false);

		assertFalse(oto.isOptional());
	}

	@Test
	public void testOrphanRemoval() {
		OneToOneMetadata oto = new OneToOneMetadata(
			"address", "Address", "com.example")
			.orphanRemoval(true);

		assertTrue(oto.isOrphanRemoval());
	}

	@Test
	public void testFluentChaining() {
		OneToOneMetadata oto = new OneToOneMetadata(
			"address", "Address", "com.example")
			.foreignKeyColumnName("ADDRESS_ID")
			.referencedColumnName("ID")
			.fetchType(FetchType.LAZY)
			.cascade(CascadeType.ALL)
			.optional(false)
			.orphanRemoval(true);

		assertEquals("ADDRESS_ID", oto.getForeignKeyColumnName());
		assertEquals("ID", oto.getReferencedColumnName());
		assertEquals(FetchType.LAZY, oto.getFetchType());
		assertArrayEquals(new CascadeType[]{ CascadeType.ALL }, oto.getCascadeTypes());
		assertFalse(oto.isOptional());
		assertTrue(oto.isOrphanRemoval());
	}
}
