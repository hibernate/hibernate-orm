/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;

/**
 * @author Jordan Gigov
 */
public class OffsetDateTimeDescriptorTest extends AbstractDescriptorTest<OffsetDateTime> {
	final OffsetDateTime original = OffsetDateTime.of(LocalDateTime.of( 2016, 10, 8, 15, 13 ), ZoneOffset.ofHoursMinutes( 2, 0));
	final OffsetDateTime copy = OffsetDateTime.of(LocalDateTime.of( 2016, 10, 8, 15, 13 ), ZoneOffset.ofHoursMinutes( 2, 0));
	final OffsetDateTime different = OffsetDateTime.of(LocalDateTime.of( 2016, 10, 8, 15, 13 ), ZoneOffset.ofHoursMinutes( 4, 30));

	public OffsetDateTimeDescriptorTest() {
		super( OffsetDateTimeJavaType.INSTANCE);
	}

	@Override
	protected Data<OffsetDateTime> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}

}
