/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.util.Set;

import jakarta.persistence.TemporalType;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;
import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;
import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupports;
import org.hibernate.dialect.temporaltype.spi.DelegatingCurrentTemporalSupport;
import org.hibernate.dialect.temporaltype.spi.DelegatingTemporalFormatSupport;
import org.hibernate.dialect.temporaltype.spi.DelegatingTemporalOperationSupport;
import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupports;
import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupports;
import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.sql.spi.StringBuilderSqlAppender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the temporal-type strategy defaults and representative maintained
/// Dialect supplies.
///
/// @author Steve Ebersole
public class TemporalTypeSupportTests {
	@Test
	void standardSuppliesAreStable() {
		final var dialect = new Dialect( DatabaseVersion.make( 1 ) ) {};
		assertSame( dialect.getCurrentTemporalSupport(), dialect.getCurrentTemporalSupport() );
		assertSame( dialect.getTemporalFormatSupport(), dialect.getTemporalFormatSupport() );
		assertSame( dialect.getTemporalOperationSupport(), dialect.getTemporalOperationSupport() );
		assertSame( TemporalValueSemantics.STANDARD, dialect.getTemporalValueSemantics() );
		assertNull( dialect.getCurrentTemporalSupport().getCurrentTimestampSelection() );
		assertEquals( "current_date", dialect.getCurrentTemporalSupport().currentDate() );
		assertEquals( "current_time", dialect.getCurrentTemporalSupport().currentTime() );
		assertEquals( "current_timestamp", dialect.getCurrentTemporalSupport().currentTimestamp() );
		assertEquals( "current_time", dialect.getCurrentTemporalSupport().currentLocalTime() );
		assertEquals( "current_timestamp", dialect.getCurrentTemporalSupport().currentLocalTimestamp() );
		assertEquals( "current_timestamp", dialect.getCurrentTemporalSupport().currentTimestampWithTimeZone() );
		assertFalse( dialect.getCurrentTemporalSupport().isCurrentTimestampStable() );
		assertTrue( dialect.getTemporalValueSemantics().roundsOnOverflow() );
		assertFalse( dialect.getTemporalValueSemantics().supportsLiteralOffset() );
	}

	@Test
	void representativeMaintainedSuppliesPreserveSql() {
		final var h2 = new H2Dialect();
		assertSame( h2, h2.getCurrentTemporalSupport() );
		assertSame( h2, h2.getTemporalFormatSupport() );
		assertSame( h2, h2.getTemporalOperationSupport() );
		assertEquals(
				CurrentTimestampSelection.prepared( "call current_timestamp()" ),
				h2.getCurrentTemporalSupport().getCurrentTimestampSelection()
		);
		assertTrue( h2.getCurrentTemporalSupport().isCurrentTimestampStable() );
		assertSame( TemporalValueSemantics.OFFSET_LITERALS, h2.getTemporalValueSemantics() );

		final var db2 = new DB2Dialect();
		assertEquals(
				CurrentTimestampSelection.prepared( "values current timestamp" ),
				db2.getCurrentTemporalSupport().getCurrentTimestampSelection()
		);
		assertSame( TemporalValueSemantics.TRUNCATING, db2.getTemporalValueSemantics() );

		final var postgres = new PostgreSQLDialect();
		assertEquals( "select now()", postgres.getCurrentTemporalSupport().getCurrentTimestampSelection().command() );
		assertEquals( "cast(?3+(?2)*interval '1 day' as timestamp)", postgres.getTemporalOperationSupport()
				.timestampaddPattern( TemporalUnit.DAY, jakarta.persistence.TemporalType.TIMESTAMP, null ) );
	}

	@Test
	void supportValueObjectsRejectInvalidState() {
		assertEquals( CurrentTimestampSelection.prepared( " select now() " ).command(), " select now() " );
		assertFalse( CurrentTimestampSelection.prepared( "select now()" ).callable() );
		assertTrue( CurrentTimestampSelection.callable( "{?=call now()}" ).callable() );
		assertThrows( IllegalArgumentException.class, () -> CurrentTimestampSelection.prepared( null ) );
		assertThrows( IllegalArgumentException.class, () -> CurrentTimestampSelection.prepared( " " ) );
		assertThrows(
				IllegalArgumentException.class,
				() -> new TemporalValueSemantics( null, TemporalValueSemantics.OffsetLiteralSupport.UNSUPPORTED )
		);
		assertThrows(
				IllegalArgumentException.class,
				() -> new TemporalValueSemantics( TemporalValueSemantics.PrecisionAdjustment.ROUND, null )
		);
		assertTrue( TemporalValueSemantics.STANDARD.roundsOnOverflow() );
		assertFalse( TemporalValueSemantics.STANDARD.supportsLiteralOffset() );
		assertFalse( TemporalValueSemantics.TRUNCATING.roundsOnOverflow() );
		assertFalse( TemporalValueSemantics.TRUNCATING.supportsLiteralOffset() );
		assertTrue( TemporalValueSemantics.OFFSET_LITERALS.roundsOnOverflow() );
		assertTrue( TemporalValueSemantics.OFFSET_LITERALS.supportsLiteralOffset() );
		assertFalse( TemporalValueSemantics.TRUNCATING_WITH_OFFSET_LITERALS.roundsOnOverflow() );
		assertTrue( TemporalValueSemantics.TRUNCATING_WITH_OFFSET_LITERALS.supportsLiteralOffset() );
	}

	@Test
	void standardFormatAndOperationBehaviorIsPreserved() {
		final var dialect = new Dialect( DatabaseVersion.make( 1 ) ) {};
		final var appender = new StringBuilderSqlAppender();
		dialect.getTemporalFormatSupport().appendFormat( appender, "yyyy-MM-dd" );
		assertEquals( "YYYY-MM-DD", appender.toString() );
		assertEquals( "extract(?1 from ?2)", dialect.getTemporalOperationSupport().extractPattern( TemporalUnit.DAY ) );
		assertEquals( "dd", dialect.getTemporalOperationSupport().translateExtractField( TemporalUnit.DAY_OF_MONTH ) );
		assertEquals( "dy", dialect.getTemporalOperationSupport().translateExtractField( TemporalUnit.DAY_OF_YEAR ) );
		assertEquals( "dw", dialect.getTemporalOperationSupport().translateExtractField( TemporalUnit.DAY_OF_WEEK ) );
		assertEquals( "nanosecond", dialect.getTemporalOperationSupport().translateDurationField( TemporalUnit.NATIVE ) );
		assertEquals( 1, dialect.getTemporalOperationSupport().fractionalSecondPrecisionInNanos() );
		for ( var unit : Set.of(
				TemporalUnit.OFFSET,
				TemporalUnit.NATIVE,
				TemporalUnit.NANOSECOND,
				TemporalUnit.DATE,
				TemporalUnit.TIME,
				TemporalUnit.WEEK_OF_MONTH,
				TemporalUnit.WEEK_OF_YEAR ) ) {
			assertThrows( IllegalArgumentException.class,
					() -> dialect.getTemporalOperationSupport().translateExtractField( unit ) );
		}
		for ( var unit : Set.of(
				TemporalUnit.DAY_OF_MONTH,
				TemporalUnit.DAY_OF_YEAR,
				TemporalUnit.DAY_OF_WEEK,
				TemporalUnit.WEEK_OF_MONTH,
				TemporalUnit.WEEK_OF_YEAR,
				TemporalUnit.OFFSET,
				TemporalUnit.TIMEZONE_HOUR,
				TemporalUnit.TIMEZONE_MINUTE,
				TemporalUnit.DATE,
				TemporalUnit.TIME ) ) {
			assertThrows( IllegalArgumentException.class,
					() -> dialect.getTemporalOperationSupport().translateDurationField( unit ) );
		}
		assertThrows( UnsupportedOperationException.class, () -> dialect.getTemporalOperationSupport()
				.timestampaddPattern( TemporalUnit.DAY, TemporalType.TIMESTAMP, null ) );
		assertThrows( UnsupportedOperationException.class, () -> dialect.getTemporalOperationSupport()
				.timestampdiffPattern( TemporalUnit.DAY, TemporalType.TIMESTAMP, TemporalType.TIMESTAMP ) );
		assertThrows( NullPointerException.class, () -> dialect.getTemporalOperationSupport().extractPattern( null ) );
		assertThrows( NullPointerException.class, () -> dialect.getTemporalOperationSupport()
				.timestampaddPattern( TemporalUnit.DAY, null, null ) );
	}

	@Test
	void delegatingStrategiesForwardToTheirStableDelegate() {
		final CurrentTemporalSupport currentDelegate = new CurrentTemporalSupport() {
			@Override
			public String currentTimestamp() {
				return "delegate_now";
			}
		};
		final var current = new DelegatingCurrentTemporalSupport( currentDelegate ) {};
		assertEquals( "delegate_now", current.currentTimestamp() );

		final var format = new DelegatingTemporalFormatSupport( TemporalFormatSupports.standard() ) {};
		final var appender = new StringBuilderSqlAppender();
		format.appendFormat( appender, "HH:mm" );
		assertEquals( "HH24:MI", appender.toString() );

		final var operation = new DelegatingTemporalOperationSupport( TemporalOperationSupports.standard() ) {};
		assertEquals( "extract(?1 from ?2)", operation.extractPattern( TemporalUnit.MONTH ) );
		assertEquals( 1, operation.fractionalSecondPrecisionInNanos() );
		assertSame( CurrentTemporalSupports.standard(), CurrentTemporalSupports.standard() );
		assertSame( TemporalFormatSupports.standard(), TemporalFormatSupports.standard() );
		assertSame( TemporalOperationSupports.standard(), TemporalOperationSupports.standard() );
		assertThrows( NullPointerException.class, () -> new DelegatingCurrentTemporalSupport( null ) {} );
		assertThrows( NullPointerException.class, () -> new DelegatingTemporalFormatSupport( null ) {} );
		assertThrows( NullPointerException.class, () -> new DelegatingTemporalOperationSupport( null ) {} );
	}

	@Test
	void nativePrecisionMustBeAPositivePowerOfTenNoGreaterThanOneSecond() {
		for ( long precision : new long[] { 1, 100, 1_000, 1_000_000, 1_000_000_000 } ) {
			assertEquals( precision == 1 ? "" : "*" + precision,
					TemporalUnit.NATIVE.conversionFactorFull( TemporalUnit.NANOSECOND,
					dialectWithNativePrecision( precision ) ) );
		}
		for ( long precision : new long[] { 0, -1, 2, 999, 10_000_000_000L } ) {
			assertThrows( IllegalArgumentException.class, () -> TemporalUnit.NATIVE.conversionFactor(
					TemporalUnit.NANOSECOND,
					dialectWithNativePrecision( precision )
			) );
		}
	}

	private static Dialect dialectWithNativePrecision(long precision) {
		return new Dialect( DatabaseVersion.make( 1 ) ) {
			@Override
			public long fractionalSecondPrecisionInNanos() {
				return precision;
			}
		};
	}
}
