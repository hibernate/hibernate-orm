/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import java.time.LocalDate;
import org.hibernate.type.descriptor.java.LocalDateJavaType;

/**
 * @author Jordan Gigov
 */
public class LocalDateDescriptorTest extends AbstractDescriptorTest<LocalDate> {
	final LocalDate original = LocalDate.of( 2016, 10, 8 );
	final LocalDate copy = LocalDate.of( 2016, 10, 8 );
	final LocalDate different = LocalDate.of( 2013,  8, 8 );

	public LocalDateDescriptorTest() {
		super( LocalDateJavaType.INSTANCE);
	}

	@Override
	protected Data<LocalDate> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}

}
