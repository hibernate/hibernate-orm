/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.descriptor.java;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;


import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

public class DateTimeToSqlDateUnwrapTest {

	@Test
	@JiraKey("HHH-19204")
	public void testLocalDateTimeUnwrap() {
		LocalDateTimeJavaType descriptor = new LocalDateTimeJavaType();
		LocalDateTime value = LocalDateTime.now();

		checkUnwrap(descriptor, value, java.sql.Date.class);
		checkUnwrap(descriptor, value, java.sql.Time.class);
	}

	@Test
	@JiraKey("HHH-19204")
	public void testOffsetDateTimeUnwrap() {
		OffsetDateTimeJavaType descriptor = new OffsetDateTimeJavaType();
		OffsetDateTime value = OffsetDateTime.now();

		checkUnwrap(descriptor, value, java.sql.Date.class);
		checkUnwrap(descriptor, value, java.sql.Time.class);
	}

	@Test
	@JiraKey("HHH-19204")
	public void testZonedDateTimeUnwrap() {
		ZonedDateTimeJavaType descriptor = new ZonedDateTimeJavaType();
		ZonedDateTime value = ZonedDateTime.now();

		checkUnwrap(descriptor, value, java.sql.Date.class);
		checkUnwrap(descriptor, value, java.sql.Time.class);
	}

	private <T> void checkUnwrap(JavaType<T> descriptor, T value, Class<?> targetType) {
		try {
			descriptor.unwrap(value, (Class) targetType, null);
		} catch (ClassCastException e) {
			// before fix
			fail("HHH-19204: ClassCastException occurred while unwrapping to " + targetType.getSimpleName());
		} catch (Exception e) {
			// after fix
		}
	}
}
