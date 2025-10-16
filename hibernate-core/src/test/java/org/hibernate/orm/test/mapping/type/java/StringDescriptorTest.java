/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import org.hibernate.type.descriptor.java.StringJavaType;

/**
 * @author Steve Ebersole
 */
public class StringDescriptorTest extends AbstractDescriptorTest<String> {
	final String original = "abc";
	final String copy = new String( original.toCharArray() );
	final String different = "xyz";

	public StringDescriptorTest() {
		super( StringJavaType.INSTANCE );
	}

	@Override
	protected Data<String> getTestData() {
		return new Data<String>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}
}
