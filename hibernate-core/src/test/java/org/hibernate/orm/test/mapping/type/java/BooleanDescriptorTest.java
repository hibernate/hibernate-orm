/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;


import org.hibernate.type.descriptor.java.BooleanJavaType;

/**
 * @author Steve Ebersole
 */
public class BooleanDescriptorTest extends AbstractDescriptorTest<Boolean> {
	final Boolean original = Boolean.TRUE;
	final Boolean copy = new Boolean( true );
	final Boolean different = Boolean.FALSE;

	public BooleanDescriptorTest() {
		super( BooleanJavaType.INSTANCE );
	}

	@Override
	protected Data<Boolean> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}
}
