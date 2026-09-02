/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.TimeZone;
import java.util.stream.Stream;

import jakarta.persistence.TemporalType;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies complete datetime literals for every community rendering
/// cohort without requiring a live database.
///
/// @author Steve Ebersole
public class CommunityDateTimeLiteralRenderingTest {
	private static final TimeZone UTC = TimeZone.getTimeZone( "UTC" );
	private static final OffsetDateTime OFFSET_TIMESTAMP = OffsetDateTime.of(
			2026,
			1,
			2,
			3,
			4,
			5,
			123_456_789,
			ZoneOffset.UTC
	);

	@ParameterizedTest(name = "{0}")
	@MethodSource("timestampDialects")
	void rendersTimestampLiteral(String name, Dialect dialect, String expected) {
		assertEquals( expected, render( dialect, OFFSET_TIMESTAMP, TemporalType.TIMESTAMP ) );
	}

	static Stream<Arguments> timestampDialects() {
		return Stream.of(
				arguments( new AltibaseDialect(), "Altibase", "{ts '2026-01-02 03:04:05.123456'}" ),
				arguments(
						new CockroachLegacyDialect(),
						"Cockroach Legacy",
						"timestamp with time zone '2026-01-02 03:04:05.123456Z'"
				),
				arguments( new DB2LegacyDialect(), "DB2 Legacy", "timestamp '2026-01-02 03:04:05.123456789'" ),
				arguments( new FirebirdDialect(), "Firebird Legacy", "timestamp '2026-01-02 03:04:05.123'" ),
				arguments(
						new GaussDBDialect(),
						"GaussDB",
						"timestamp with time zone '2026-01-02 03:04:05.123456Z'"
				),
				arguments(
						new H2LegacyDialect(),
						"H2 Legacy",
						"timestamp with time zone '2026-01-02 03:04:05.123456789Z'"
				),
				arguments( new HANALegacyDialect(), "HANA Legacy", "{ts '2026-01-02 03:04:05.123456'}" ),
				arguments(
						new InformixDialect(),
						"Informix",
						"datetime (2026-01-02 03:04:05.123) year to fraction"
				),
				arguments(
						new InterSystemsIRISDialect(),
						"InterSystems IRIS",
						"'2026-01-02 03:04:05.123456789'"
				),
				arguments(
						new OracleLegacyDialect(),
						"Oracle Legacy",
						"timestamp '2026-01-02 03:04:05.123456789+00:00'"
				),
				arguments(
						new PostgreSQLLegacyDialect(),
						"PostgreSQL Legacy",
						"timestamp with time zone '2026-01-02 03:04:05.123456Z'"
				),
				arguments(
						new SQLServerLegacyDialect(),
						"SQL Server Legacy",
						"cast('2026-01-02 03:04:05.123456' as datetime2)"
				),
				arguments( new SQLiteDialect(), "SQLite", "datetime(2026-01-02 03:04:05.123456789)" ),
				arguments( new SingleStoreDialect(), "SingleStore", "timestamp('2026-01-02 03:04:05.123456')" ),
				arguments(
						new SybaseLegacyDialect(),
						"Sybase Legacy",
						"convert(datetime,'2026-01-02 03:04:05.123',140)"
				)
		);
	}

	@Test
	void rendersFirebirdFourNumericZeroOffset() {
		assertEquals(
				"timestamp '2026-01-02 03:04:05.123+00:00'",
				render( new FirebirdDialect( DatabaseVersion.make( 4 ) ), OFFSET_TIMESTAMP, TemporalType.TIMESTAMP )
		);
	}

	@Test
	void rendersH2TimeBeforeAndAfterOffsetSupport() {
		assertEquals(
				"time '03:04:05'",
				render( new H2LegacyDialect( DatabaseVersion.make( 1, 4, 199 ) ), OFFSET_TIMESTAMP, TemporalType.TIME )
		);
		assertEquals(
				"time with time zone '03:04:05Z'",
				render( new H2LegacyDialect( DatabaseVersion.make( 1, 4, 200 ) ), OFFSET_TIMESTAMP, TemporalType.TIME )
		);
	}

	private static Arguments arguments(Dialect dialect, String name, String expected) {
		return Arguments.of( name, dialect, expected );
	}

	private static String render(Dialect dialect, TemporalAccessor value, TemporalType temporalType) {
		final var result = new StringBuilder();
		dialect.getLiteralSupport().appendDateTimeLiteral(
				new StringBuilderSqlAppender( result ),
				value,
				temporalType,
				UTC
		);
		return result.toString();
	}
}
