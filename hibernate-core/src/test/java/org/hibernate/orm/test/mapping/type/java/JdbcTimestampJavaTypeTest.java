/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import java.sql.Timestamp;
import java.util.Date;

import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;

/**
 * @author Owen Farrell
 */
public class JdbcTimestampJavaTypeTest extends AbstractDescriptorTest<Timestamp> {
	final Timestamp original = new Timestamp( new Date().getTime() );
	final Timestamp copy = new Timestamp( original.getTime() );
	final Timestamp different = new Timestamp( original.getTime() + 500L);

	public JdbcTimestampJavaTypeTest() {
		super( JdbcTimestampJavaType.INSTANCE );
	}

	@Override
	protected Data<Timestamp> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return true;
	}
}
