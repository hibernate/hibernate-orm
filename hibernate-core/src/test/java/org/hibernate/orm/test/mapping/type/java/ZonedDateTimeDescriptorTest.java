/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaType;

/**
 * @author Jordan Gigov
 */
public class ZonedDateTimeDescriptorTest extends AbstractDescriptorTest<ZonedDateTime> {
	final ZonedDateTime original = ZonedDateTime.of( LocalDateTime.of( 2016, 10, 8, 15, 13 ), ZoneId.of( "UTC" ));
	final ZonedDateTime copy = ZonedDateTime.of( LocalDateTime.of( 2016, 10, 8, 15, 13 ), ZoneId.of( "UTC" ) );
	final ZonedDateTime different = ZonedDateTime.of( LocalDateTime.of( 2016, 10, 8, 15, 13 ), ZoneId.of( "EET" ) );

	public ZonedDateTimeDescriptorTest() {
		super( ZonedDateTimeJavaType.INSTANCE);
	}

	@Override
	protected Data<ZonedDateTime> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}

}
