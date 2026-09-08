/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.descriptor.jdbc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.type.descriptor.java.LocalDateJavaType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.jdbc.JavaTimeJdbcType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests database-rendered Java Time forms encountered in aggregate values.
///
/// @author Steve Ebersole
class JavaTimeAggregateParsingTest {
	@Test
	void decodesDatabaseLocalDateForms() {
		assertEquals(
				LocalDate.of( 2000, 1, 1 ),
				JavaTimeJdbcType.fromEncodedString(
						LocalDateJavaType.INSTANCE,
						"2000-01-01 00:00:00",
						0,
						19
				)
		);
		assertEquals(
				LocalDate.of( 2000, 1, 1 ),
				JavaTimeJdbcType.fromEncodedString(
						LocalDateJavaType.INSTANCE,
						"2000-01-01T00:00:00",
						0,
						19
				)
		);
	}

	@Test
	void decodesDatabaseLocalDateTimeForm() {
		assertEquals(
				LocalDateTime.of( 2000, 1, 1, 12, 34, 56 ),
				JavaTimeJdbcType.fromEncodedString(
						LocalDateTimeJavaType.INSTANCE,
						"2000-01-01 12:34:56",
						0,
						19
				)
		);
	}
}
