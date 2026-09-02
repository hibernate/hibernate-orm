/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.literal;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAccessor;
import java.util.TimeZone;
import java.util.stream.Stream;

import jakarta.persistence.TemporalType;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies complete datetime literals for every maintained Dialect
/// rendering cohort without requiring a live database.
///
/// @author Steve Ebersole
public class MaintainedDateTimeLiteralRenderingTest {
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
		assertEquals( expected, render( dialect, OFFSET_TIMESTAMP ) );
	}

	static Stream<Arguments> timestampDialects() {
		return Stream.of(
				arguments(
						new Dialect( DatabaseVersion.make( 0 ) ) {},
						"Standard",
						"{ts '2026-01-02 03:04:05.123456789'}"
				),
				arguments(
						new CockroachDialect(),
						"Cockroach",
						"timestamp with time zone '2026-01-02 03:04:05.123456Z'"
				),
				arguments( new DB2Dialect(), "DB2", "timestamp '2026-01-02 03:04:05.123456789'" ),
				arguments(
						new H2Dialect(),
						"H2",
						"timestamp with time zone '2026-01-02 03:04:05.123456789Z'"
				),
				arguments( new HANADialect(), "HANA", "{ts '2026-01-02 03:04:05.123456'}" ),
				arguments( new MySQLDialect(), "MySQL", "timestamp '2026-01-02 03:04:05.123456'" ),
				arguments(
						new OracleDialect(),
						"Oracle",
						"timestamp '2026-01-02 03:04:05.123456789+00:00'"
				),
				arguments(
						new PostgreSQLDialect(),
						"PostgreSQL",
						"timestamp with time zone '2026-01-02 03:04:05.123456Z'"
				),
				arguments(
						new SQLServerDialect(),
						"SQL Server",
						"cast('2026-01-02 03:04:05.123456' as datetime2)"
				),
				arguments( new SpannerDialect(), "Spanner", "TIMESTAMP '2026-01-02 03:04:05.123456789Z'" ),
				arguments( new SybaseDialect(), "Sybase", "convert(datetime,'2026-01-02 03:04:05.123',140)" )
		);
	}

	private static Arguments arguments(Dialect dialect, String name, String expected) {
		return Arguments.of( name, dialect, expected );
	}

	private static String render(Dialect dialect, TemporalAccessor value) {
		final var result = new StringBuilder();
		dialect.getLiteralSupport().appendDateTimeLiteral(
				new StringBuilderSqlAppender( result ),
				value,
				TemporalType.TIMESTAMP,
				UTC
		);
		return result.toString();
	}
}
