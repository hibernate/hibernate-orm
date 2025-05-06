/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import java.time.LocalDateTime;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;

/**
 * @author Jordan Gigov
 */
public class LocalDateTimeDescriptorTest extends AbstractDescriptorTest<LocalDateTime> {
	final LocalDateTime original = LocalDateTime.of( 2016, 10, 8, 10, 15, 0 );
	final LocalDateTime copy = LocalDateTime.of( 2016, 10, 8, 10, 15, 0 );
	final LocalDateTime different = LocalDateTime.of( 2013,  8, 8, 15, 12 );

	public LocalDateTimeDescriptorTest() {
		super( LocalDateTimeJavaType.INSTANCE);
	}

	@Override
	protected Data<LocalDateTime> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}

}
