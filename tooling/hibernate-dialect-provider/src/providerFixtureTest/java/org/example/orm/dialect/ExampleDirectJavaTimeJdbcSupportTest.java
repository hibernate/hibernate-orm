/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies direct Java Time JDBC support supplied by the standalone provider.
///
/// @author Steve Ebersole
public class ExampleDirectJavaTimeJdbcSupportTest {
	private final ExampleDialect dialect = new ExampleDialect();

	@Test
	void suppliesStableSupportForEachJdbcAccessPath() {
		final var support = dialect.getDirectJavaTimeJdbcSupport();
		assertSame( support, dialect.getDirectJavaTimeJdbcSupport() );

		assertTrue( support.supports( LocalDate.class ) );
		assertTrue( support.supports( LocalTime.class ) );
		assertTrue( support.supports( LocalDateTime.class ) );
		assertTrue( support.supports( OffsetTime.class ) );
		assertTrue( support.supports( OffsetDateTime.class ) );
		assertFalse( support.supports( ZonedDateTime.class ) );
		assertFalse( support.supports( Instant.class ) );

		assertTrue( support.supportsInStruct( LocalDate.class ) );
		assertTrue( support.supportsInStruct( OffsetTime.class ) );
		assertTrue( support.supportsInStruct( OffsetDateTime.class ) );
		assertFalse( support.supportsInStruct( LocalDateTime.class ) );

		assertTrue( support.supportsInArray( LocalDateTime.class ) );
		assertFalse( support.supportsInArray( LocalDate.class ) );
		assertFalse( support.supportsInArray( OffsetTime.class ) );
		assertFalse( support.supportsInArray( OffsetDateTime.class ) );
	}
}
