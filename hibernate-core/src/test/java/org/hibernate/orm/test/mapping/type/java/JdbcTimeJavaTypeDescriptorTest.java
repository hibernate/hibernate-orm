/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import java.util.Date;

import org.hibernate.type.descriptor.java.JdbcTimeJavaType;

/**
 * @author Owen Farrell
 */
public class JdbcTimeJavaTypeDescriptorTest extends AbstractDescriptorTest<Date> {
	final Date original = new Date();
	final Date copy = new Date( original.getTime() );
	final Date different = new Date( original.getTime() + 500L);

	public JdbcTimeJavaTypeDescriptorTest() {
		super( JdbcTimeJavaType.INSTANCE );
	}

	@Override
	protected Data<Date> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return true;
	}
}
