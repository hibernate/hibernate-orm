/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.descriptor.java;

import org.hibernate.orm.test.mapping.type.java.AbstractDescriptorTest;
import org.hibernate.type.descriptor.java.ArrayJavaType;
import org.hibernate.type.descriptor.java.LongJavaType;

/**
 * @author Jordan Gigov
 */
public class LongArrayDescriptorTest extends AbstractDescriptorTest<Long[]> {
	final Long[] original = new Long[]{ 13L, -2L, 666L };
	final Long[] copy = new Long[]{ 13L, -2L, 666L };
	final Long[] different = new Long[]{ -2L, 666L, 13L };

	public LongArrayDescriptorTest() {
		super( new ArrayJavaType<>( LongJavaType.INSTANCE ) );
	}

	@Override
	protected Data<Long[]> getTestData() {
		return new Data<Long[]>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return true;
	}

}
