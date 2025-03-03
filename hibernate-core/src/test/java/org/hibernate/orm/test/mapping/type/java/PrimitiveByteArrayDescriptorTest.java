/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PrimitiveByteArrayDescriptorTest extends AbstractDescriptorTest<byte[]> {

	private final byte[] original = new byte[] {1, 2, 3};

	private final byte[] copy = new byte[] {1, 2, 3};

	private final byte[] different = new byte[] {3, 2, 1};

	public PrimitiveByteArrayDescriptorTest() {
		super( PrimitiveByteArrayJavaType.INSTANCE );
	}

	@Override
	protected Data<byte[]> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return true;
	}

	@Test
	public void testExtractLoggableRepresentation() {
		assertEquals( "null", PrimitiveByteArrayJavaType.INSTANCE.extractLoggableRepresentation( null));
		assertEquals( "[]", PrimitiveByteArrayJavaType.INSTANCE.extractLoggableRepresentation( new byte[] {} ));
		assertEquals( "[1, 2, 3]", PrimitiveByteArrayJavaType.INSTANCE.extractLoggableRepresentation( original));
	}
}
