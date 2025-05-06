/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;

/**
 * @author Jordan Gigov
 */
public class OffsetTimeDescriptorTest extends AbstractDescriptorTest<OffsetTime> {
	final OffsetTime original = OffsetTime.of(LocalTime.of( 15, 13 ), ZoneOffset.ofHoursMinutes( -6, 0));
	final OffsetTime copy = OffsetTime.of(LocalTime.of( 15, 13 ), ZoneOffset.ofHoursMinutes( -6, 0));
	final OffsetTime different = OffsetTime.of(LocalTime.of( 15, 13 ), ZoneOffset.ofHoursMinutes( 4, 30));

	public OffsetTimeDescriptorTest() {
		super( OffsetTimeJavaType.INSTANCE);
	}

	@Override
	protected Data<OffsetTime> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}

}
