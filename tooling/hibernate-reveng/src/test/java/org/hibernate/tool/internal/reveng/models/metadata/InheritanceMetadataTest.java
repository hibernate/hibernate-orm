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

import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.InheritanceType;

/**
 * Tests for {@link InheritanceMetadata}.
 *
 * @author Koen Aers
 */
public class InheritanceMetadataTest {

	@Test
	public void testConstructorAndDefaults() {
		InheritanceMetadata inheritance = new InheritanceMetadata(InheritanceType.SINGLE_TABLE);

		assertEquals(InheritanceType.SINGLE_TABLE, inheritance.getStrategy());
		assertNull(inheritance.getDiscriminatorColumnName());
		assertNull(inheritance.getDiscriminatorType());
		assertEquals(0, inheritance.getDiscriminatorColumnLength());
	}

	@Test
	public void testJoinedStrategy() {
		InheritanceMetadata inheritance = new InheritanceMetadata(InheritanceType.JOINED);

		assertEquals(InheritanceType.JOINED, inheritance.getStrategy());
	}

	@Test
	public void testDiscriminatorColumn() {
		InheritanceMetadata inheritance = new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
			.discriminatorColumn("VEHICLE_TYPE");

		assertEquals("VEHICLE_TYPE", inheritance.getDiscriminatorColumnName());
	}

	@Test
	public void testDiscriminatorType() {
		InheritanceMetadata inheritance = new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
			.discriminatorType(DiscriminatorType.STRING);

		assertEquals(DiscriminatorType.STRING, inheritance.getDiscriminatorType());
	}

	@Test
	public void testDiscriminatorColumnLength() {
		InheritanceMetadata inheritance = new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
			.discriminatorColumnLength(50);

		assertEquals(50, inheritance.getDiscriminatorColumnLength());
	}

	@Test
	public void testFluentChaining() {
		InheritanceMetadata inheritance = new InheritanceMetadata(InheritanceType.SINGLE_TABLE)
			.discriminatorColumn("TYPE")
			.discriminatorType(DiscriminatorType.INTEGER)
			.discriminatorColumnLength(10);

		assertEquals(InheritanceType.SINGLE_TABLE, inheritance.getStrategy());
		assertEquals("TYPE", inheritance.getDiscriminatorColumnName());
		assertEquals(DiscriminatorType.INTEGER, inheritance.getDiscriminatorType());
		assertEquals(10, inheritance.getDiscriminatorColumnLength());
	}
}
