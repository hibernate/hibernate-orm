/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.java;

import org.hibernate.type.descriptor.java.PrimitiveByteArrayTypeDescriptor;

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
		super( PrimitiveByteArrayTypeDescriptor.INSTANCE );
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
		assertEquals("null", PrimitiveByteArrayTypeDescriptor.INSTANCE.extractLoggableRepresentation(null));
		assertEquals("[]", PrimitiveByteArrayTypeDescriptor.INSTANCE.extractLoggableRepresentation(new byte[] {} ));
		assertEquals("[1, 2, 3]", PrimitiveByteArrayTypeDescriptor.INSTANCE.extractLoggableRepresentation(original));
	}
}