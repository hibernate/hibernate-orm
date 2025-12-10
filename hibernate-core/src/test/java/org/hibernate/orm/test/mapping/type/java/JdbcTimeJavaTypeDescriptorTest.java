/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import java.sql.Time;
import java.util.Date;

import org.hibernate.type.descriptor.java.JdbcTimeJavaType;

/**
 * @author Owen Farrell
 */
public class JdbcTimeJavaTypeDescriptorTest extends AbstractDescriptorTest<Time> {
	Date now = new Date();
	final Time original = new Time( now.getHours(), now.getMinutes(), now.getSeconds() );
	final Time copy = new Time( original.getTime() );
	final Time different = new Time( now.getHours(), now.getMinutes(), now.getSeconds() + 3 );

	public JdbcTimeJavaTypeDescriptorTest() {
		super( JdbcTimeJavaType.INSTANCE );
	}

	@Override
	protected Data<Time> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return true;
	}
}
