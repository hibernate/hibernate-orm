/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.descriptor;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.InheritanceType;

/**
 * Tests for {@link InheritanceDescriptor}.
 *
 * @author Koen Aers
 */
public class InheritanceDescriptorTest {

	@Test
	public void testConstructorAndDefaults() {
		InheritanceDescriptor inheritance = new InheritanceDescriptor(InheritanceType.SINGLE_TABLE);

		assertEquals(InheritanceType.SINGLE_TABLE, inheritance.getStrategy());
		assertNull(inheritance.getDiscriminatorColumnName());
		assertNull(inheritance.getDiscriminatorType());
		assertEquals(0, inheritance.getDiscriminatorColumnLength());
	}

	@Test
	public void testJoinedStrategy() {
		InheritanceDescriptor inheritance = new InheritanceDescriptor(InheritanceType.JOINED);

		assertEquals(InheritanceType.JOINED, inheritance.getStrategy());
	}

	@Test
	public void testDiscriminatorColumn() {
		InheritanceDescriptor inheritance = new InheritanceDescriptor(InheritanceType.SINGLE_TABLE)
			.discriminatorColumn("VEHICLE_TYPE");

		assertEquals("VEHICLE_TYPE", inheritance.getDiscriminatorColumnName());
	}

	@Test
	public void testDiscriminatorType() {
		InheritanceDescriptor inheritance = new InheritanceDescriptor(InheritanceType.SINGLE_TABLE)
			.discriminatorType(DiscriminatorType.STRING);

		assertEquals(DiscriminatorType.STRING, inheritance.getDiscriminatorType());
	}

	@Test
	public void testDiscriminatorColumnLength() {
		InheritanceDescriptor inheritance = new InheritanceDescriptor(InheritanceType.SINGLE_TABLE)
			.discriminatorColumnLength(50);

		assertEquals(50, inheritance.getDiscriminatorColumnLength());
	}

	@Test
	public void testFluentChaining() {
		InheritanceDescriptor inheritance = new InheritanceDescriptor(InheritanceType.SINGLE_TABLE)
			.discriminatorColumn("TYPE")
			.discriminatorType(DiscriminatorType.INTEGER)
			.discriminatorColumnLength(10);

		assertEquals(InheritanceType.SINGLE_TABLE, inheritance.getStrategy());
		assertEquals("TYPE", inheritance.getDiscriminatorColumnName());
		assertEquals(DiscriminatorType.INTEGER, inheritance.getDiscriminatorType());
		assertEquals(10, inheritance.getDiscriminatorColumnLength());
	}
}
