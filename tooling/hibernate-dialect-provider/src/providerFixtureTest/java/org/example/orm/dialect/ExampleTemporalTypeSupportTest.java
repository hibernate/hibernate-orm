/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import jakarta.persistence.TemporalType;

import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.sql.spi.StringBuilderSqlAppender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the standalone provider's complete temporal-type strategy supply.
///
/// @author Steve Ebersole
public class ExampleTemporalTypeSupportTest {
	private final ExampleDialect dialect = new ExampleDialect();

	@Test
	void suppliesStableNonstandardStrategies() {
		assertSame( dialect.getCurrentTemporalSupport(), dialect.getCurrentTemporalSupport() );
		assertSame( dialect.getTemporalFormatSupport(), dialect.getTemporalFormatSupport() );
		assertSame( dialect.getTemporalOperationSupport(), dialect.getTemporalOperationSupport() );
		assertEquals( "fixture_current_date()", dialect.getCurrentTemporalSupport().currentDate() );
		assertEquals( "fixture_current_time()", dialect.getCurrentTemporalSupport().currentTime() );
		assertEquals( "fixture_current_timestamp()", dialect.getCurrentTemporalSupport().currentTimestamp() );
		assertEquals( "fixture_current_time()", dialect.getCurrentTemporalSupport().currentLocalTime() );
		assertEquals( "fixture_current_timestamp()", dialect.getCurrentTemporalSupport().currentLocalTimestamp() );
		assertEquals( "fixture_current_timestamp()", dialect.getCurrentTemporalSupport().currentTimestampWithTimeZone() );
		assertTrue( dialect.getCurrentTemporalSupport().isCurrentTimestampStable() );

		final var selection = dialect.getCurrentTemporalSupport().getCurrentTimestampSelection();
		assertEquals( "{?=call fixture_current_timestamp()}", selection.command() );
		assertTrue( selection.callable() );

		final var appender = new StringBuilderSqlAppender();
		dialect.getTemporalFormatSupport().appendFormat( appender, "yyyy" );
		assertEquals( "fixture_format[yyyy]", appender.toString() );
		assertEquals( "fixture_extract(?1,?2)", dialect.getTemporalOperationSupport().extractPattern( TemporalUnit.DAY ) );
		assertEquals( "fixture_extract_day", dialect.getTemporalOperationSupport()
				.translateExtractField( TemporalUnit.DAY ) );
		assertEquals( "fixture_duration_day", dialect.getTemporalOperationSupport()
				.translateDurationField( TemporalUnit.DAY ) );
		assertEquals(
				"fixture_add(?1,?2,?3)",
				dialect.getTemporalOperationSupport().timestampaddPattern( TemporalUnit.DAY, TemporalType.TIMESTAMP, null )
		);
		assertEquals(
				"fixture_add(?1,?2,?3)",
				dialect.getTemporalOperationSupport()
						.timestampaddPattern( TemporalUnit.SECOND, TemporalType.TIMESTAMP, IntervalType.SECOND )
		);
		assertEquals(
				"fixture_diff(?1,?2,?3)",
				dialect.getTemporalOperationSupport().timestampdiffPattern(
						TemporalUnit.DAY,
						TemporalType.TIMESTAMP,
						TemporalType.TIMESTAMP
				)
		);
		assertEquals( 1_000, dialect.getTemporalOperationSupport().fractionalSecondPrecisionInNanos() );
		assertSame( TemporalValueSemantics.TRUNCATING_WITH_OFFSET_LITERALS, dialect.getTemporalValueSemantics() );
		assertFalse( dialect.getTemporalValueSemantics().roundsOnOverflow() );
		assertTrue( dialect.getTemporalValueSemantics().supportsLiteralOffset() );
		assertFalse( dialect.getCurrentTemporalSupport().getClass().getName().contains( ".internal." ) );
		assertFalse( dialect.getTemporalFormatSupport().getClass().getName().contains( ".internal." ) );
		assertFalse( dialect.getTemporalOperationSupport().getClass().getName().contains( ".internal." ) );
	}
}
