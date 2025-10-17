/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.descriptor.java;

import java.time.LocalDate;

import org.hibernate.orm.test.mapping.type.java.AbstractDescriptorTest;
import org.hibernate.type.descriptor.java.ArrayJavaType;
import org.hibernate.type.descriptor.java.LocalDateJavaType;

/**
 * @author Jordan Gigov
 */
public class LocalDateArrayDescriptorTest extends AbstractDescriptorTest<LocalDate[]> {

	final LocalDate[] original = new LocalDate[]{
		LocalDate.of( 2016, 10, 8 ),
		LocalDate.of( 2016, 9, 3 ),
		LocalDate.of( 2013, 8, 8 )
	};
	final LocalDate[] copy = new LocalDate[]{
		LocalDate.of( 2016, 10, 8 ),
		LocalDate.of( 2016, 9, 3 ),
		LocalDate.of( 2013, 8, 8 )
	};
	final LocalDate[] different = new LocalDate[]{
		LocalDate.of( 2016, 10, 8 ),
		LocalDate.of( 2016, 9, 7 ),
		LocalDate.of( 2013, 8, 8 )
	};

	public LocalDateArrayDescriptorTest() {
		super( new ArrayJavaType<>( LocalDateJavaType.INSTANCE ) );
	}

	@Override
	protected Data<LocalDate[]> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return true;
	}

}
