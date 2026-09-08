/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.type;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Set;

import org.hibernate.dialect.type.spi.DirectJavaTimeJdbcSupport;
import org.hibernate.dialect.type.spi.DirectJavaTimeJdbcSupports;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.community.dialect.DerbyLegacyDialect;
import org.hibernate.community.dialect.DB2LegacyDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Tests for the standard direct Java Time JDBC support profiles.
///
/// @author Steve Ebersole
@BaseUnitTest
class DirectJavaTimeJdbcSupportTests {
	@Test
	void noneSupportsNothing() {
		final DirectJavaTimeJdbcSupport support = DirectJavaTimeJdbcSupports.none();

		assertThat( support.supports( LocalDate.class ) ).isFalse();
		assertThat( support.supports( OffsetDateTime.class ) ).isFalse();
		assertThat( support.supports( ZonedDateTime.class ) ).isFalse();
		assertThat( support.supports( Instant.class ) ).isFalse();
	}

	@Test
	void jdbc42SupportsStandardTypes() {
		final DirectJavaTimeJdbcSupport support = DirectJavaTimeJdbcSupports.jdbc42();

		assertThat( support.supports( LocalDate.class ) ).isTrue();
		assertThat( support.supports( LocalTime.class ) ).isTrue();
		assertThat( support.supports( LocalDateTime.class ) ).isTrue();
		assertThat( support.supports( OffsetTime.class ) ).isTrue();
		assertThat( support.supports( OffsetDateTime.class ) ).isTrue();
		assertThat( support.supports( ZonedDateTime.class ) ).isFalse();
		assertThat( support.supports( Instant.class ) ).isFalse();
	}

	@Test
	void allSupportsEveryRecognizedType() {
		final DirectJavaTimeJdbcSupport support = DirectJavaTimeJdbcSupports.all();

		assertThat( support.supports( LocalDate.class ) ).isTrue();
		assertThat( support.supports( LocalTime.class ) ).isTrue();
		assertThat( support.supports( LocalDateTime.class ) ).isTrue();
		assertThat( support.supports( OffsetTime.class ) ).isTrue();
		assertThat( support.supports( OffsetDateTime.class ) ).isTrue();
		assertThat( support.supports( ZonedDateTime.class ) ).isTrue();
		assertThat( support.supports( Instant.class ) ).isTrue();
		assertThat( support.supports( String.class ) ).isFalse();
	}

	@Test
	void containerSupportIsConservativeByDefault() {
		final DirectJavaTimeJdbcSupport support = DirectJavaTimeJdbcSupports.all();

		assertThat( support.supportsInStruct( LocalDate.class ) ).isFalse();
		assertThat( support.supportsInArray( LocalDate.class ) ).isFalse();
	}

	@Test
	void containerCapabilitiesAreIndependent() {
		final DirectJavaTimeJdbcSupport support = DirectJavaTimeJdbcSupports.of(
				Set.of( LocalDate.class ),
				Set.of( LocalDateTime.class ),
				Set.of( Instant.class )
		);

		assertThat( support.supports( LocalDate.class ) ).isTrue();
		assertThat( support.supportsInStruct( LocalDate.class ) ).isFalse();
		assertThat( support.supportsInArray( LocalDate.class ) ).isFalse();

		assertThat( support.supports( LocalDateTime.class ) ).isFalse();
		assertThat( support.supportsInStruct( LocalDateTime.class ) ).isTrue();
		assertThat( support.supportsInArray( LocalDateTime.class ) ).isFalse();

		assertThat( support.supports( Instant.class ) ).isFalse();
		assertThat( support.supportsInStruct( Instant.class ) ).isFalse();
		assertThat( support.supportsInArray( Instant.class ) ).isTrue();
	}

	@Test
	void offsetTypesAreAMatchedPair() {
		final DirectJavaTimeJdbcSupport onlyOffsetTime = DirectJavaTimeJdbcSupports.of( OffsetTime.class );
		final DirectJavaTimeJdbcSupport onlyOffsetDateTime =
				DirectJavaTimeJdbcSupports.of( OffsetDateTime.class );
		final DirectJavaTimeJdbcSupport both =
				DirectJavaTimeJdbcSupports.of( OffsetTime.class, OffsetDateTime.class );

		assertThat( onlyOffsetTime.supports( OffsetTime.class ) ).isFalse();
		assertThat( onlyOffsetTime.supports( OffsetDateTime.class ) ).isFalse();
		assertThat( onlyOffsetDateTime.supports( OffsetTime.class ) ).isFalse();
		assertThat( onlyOffsetDateTime.supports( OffsetDateTime.class ) ).isFalse();
		assertThat( both.supports( OffsetTime.class ) ).isTrue();
		assertThat( both.supports( OffsetDateTime.class ) ).isTrue();
	}

	@Test
	void offsetTypesAreAMatchedPairInEachContainer() {
		final DirectJavaTimeJdbcSupport support = DirectJavaTimeJdbcSupports.of(
				Set.of(),
				Set.of( OffsetTime.class, OffsetDateTime.class ),
				Set.of( OffsetTime.class )
		);

		assertThat( support.supports( OffsetTime.class ) ).isFalse();
		assertThat( support.supports( OffsetDateTime.class ) ).isFalse();
		assertThat( support.supportsInStruct( OffsetTime.class ) ).isTrue();
		assertThat( support.supportsInStruct( OffsetDateTime.class ) ).isTrue();
		assertThat( support.supportsInArray( OffsetTime.class ) ).isFalse();
		assertThat( support.supportsInArray( OffsetDateTime.class ) ).isFalse();
	}

	@Test
	void customProfilesRejectUnknownTypes() {
		assertThatIllegalArgumentException()
				.isThrownBy( () -> DirectJavaTimeJdbcSupports.of( String.class ) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> DirectJavaTimeJdbcSupports.of(
						Set.of(),
						Set.of( String.class ),
						Set.of()
				) );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> DirectJavaTimeJdbcSupports.of(
						Set.of(),
						Set.of(),
						Set.of( String.class )
				) );
	}

	@Test
	void maintainedDialectProfilesReflectKnownDriverLimitations() {
		assertThat( new DB2Dialect().getDirectJavaTimeJdbcSupport().supports( LocalDate.class ) ).isFalse();
		assertThat( new DB2LegacyDialect().getDirectJavaTimeJdbcSupport().supports( LocalDate.class ) ).isFalse();
		assertThat( new DerbyDialect().getDirectJavaTimeJdbcSupport().supports( LocalDate.class ) ).isFalse();
		assertThat( new DerbyLegacyDialect().getDirectJavaTimeJdbcSupport().supports( LocalDate.class ) ).isFalse();

		assertOnlyLocalDateSupported( new SpannerDialect().getDirectJavaTimeJdbcSupport() );
		assertOnlyLocalDateSupported( new SpannerPostgreSQLDialect().getDirectJavaTimeJdbcSupport() );
		assertOnlyLocalTypesSupported( new PostgreSQLDialect().getDirectJavaTimeJdbcSupport() );
		assertOnlyLocalTypesSupported( new MySQLDialect().getDirectJavaTimeJdbcSupport() );

		final var jtds = new SybaseDialect( sybaseInfo(
				"jTDS Type 4 JDBC Driver for MS SQL Server and Sybase"
		) );
		final var jconnect = new SybaseDialect( sybaseInfo( "jConnect (TM) for JDBC (TM)" ) );
		assertThat( jtds.getDirectJavaTimeJdbcSupport().supports( LocalDate.class ) ).isFalse();
		assertThat( jconnect.getDirectJavaTimeJdbcSupport().supports( LocalDate.class ) ).isTrue();
	}

	private void assertOnlyLocalTypesSupported(DirectJavaTimeJdbcSupport support) {
		assertThat( support.supports( LocalDate.class ) ).isTrue();
		assertThat( support.supports( LocalTime.class ) ).isTrue();
		assertThat( support.supports( LocalDateTime.class ) ).isTrue();
		assertThat( support.supports( OffsetTime.class ) ).isFalse();
		assertThat( support.supports( OffsetDateTime.class ) ).isFalse();
		assertThat( support.supports( ZonedDateTime.class ) ).isFalse();
		assertThat( support.supports( Instant.class ) ).isFalse();
	}

	private void assertOnlyLocalDateSupported(DirectJavaTimeJdbcSupport support) {
		assertThat( support.supports( LocalDate.class ) ).isTrue();
		assertThat( support.supports( LocalTime.class ) ).isFalse();
		assertThat( support.supports( LocalDateTime.class ) ).isFalse();
		assertThat( support.supports( OffsetTime.class ) ).isFalse();
		assertThat( support.supports( OffsetDateTime.class ) ).isFalse();
		assertThat( support.supports( ZonedDateTime.class ) ).isFalse();
		assertThat( support.supports( Instant.class ) ).isFalse();
	}

	private DialectResolutionInfo sybaseInfo(String driverName) {
		final DialectResolutionInfo info = mock( DialectResolutionInfo.class );
		when( info.getDriverName() ).thenReturn( driverName );
		when( info.getDatabaseVersion() ).thenReturn( "16.0" );
		when( info.getDatabaseMajorVersion() ).thenReturn( 16 );
		return info;
	}
}
