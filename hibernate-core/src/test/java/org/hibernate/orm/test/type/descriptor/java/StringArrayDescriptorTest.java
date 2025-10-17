/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.descriptor.java;

import org.hibernate.orm.test.mapping.type.java.AbstractDescriptorTest;
import org.hibernate.type.descriptor.java.ArrayJavaType;
import org.hibernate.type.descriptor.java.StringJavaType;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * @author Jordan Gigov
 */
public class StringArrayDescriptorTest extends AbstractDescriptorTest<String[]> {

	final String[] original = new String[]{null, "double-quote at end\"", "\' single quote at start", "escape at end\\", "escape and quote at end\\\""};
	final String[] copy = new String[]{null, "double-quote at end\"", "\' single quote at start", "escape at end\\", "escape and quote at end\\\""};
	final String[] different = new String[]{"null", "double-quote at end\"", "\' single quote at start", "escape at end\\", "escape and quote at end\\\""};

	public StringArrayDescriptorTest() {
		super( new ArrayJavaType<>( StringJavaType.INSTANCE ) );
	}

	@Override
	protected Data<String[]> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return true;
	}

	@Test
	public void testEmptyArrayExternalization() {
		// ensure the symmetry of toString/fromString
		String[] emptyArray = new String[]{};
		String externalized = typeDescriptor().toString( emptyArray );
		String[] consumed = typeDescriptor().fromString( externalized );
		assertTrue( typeDescriptor().areEqual( emptyArray, consumed ) );
	}

}
