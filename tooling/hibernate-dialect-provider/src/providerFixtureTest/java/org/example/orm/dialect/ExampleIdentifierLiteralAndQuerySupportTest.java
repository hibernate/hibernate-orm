/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.TimeZone;

import jakarta.persistence.TemporalType;

import org.hibernate.ScrollMode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.dialect.queryhint.spi.QueryHintPlacement;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hibernate.query.common.FetchClauseType.PERCENT_ONLY;
import static org.hibernate.query.common.FetchClauseType.ROWS_ONLY;
import static org.hibernate.query.common.FetchClauseType.ROWS_WITH_TIES;

/// Verifies the identifier, literal, and query contracts exposed by the
/// standalone provider fixture.
///
/// @author Steve Ebersole
public class ExampleIdentifierLiteralAndQuerySupportTest {
	private final ExampleDialect dialect = new ExampleDialect();

	@Test
	void suppliesIdentifierAndKeywordPoliciesWithoutLiveJdbcMetadata() {
		assertEquals( "[fixture]", dialect.getIdentifierSupport().toQuotedIdentifier( "fixture" ) );
		assertTrue( dialect.getKeywords().contains( "fixture_keyword" ) );
		assertFalse( dialect.acceptsJdbcKeyword( "FIXTURE_DRIVER_WORD" ) );

		final JdbcEnvironment environment = proxy( JdbcEnvironment.class );
		final JdbcMetadata metadata = (JdbcMetadata) Proxy.newProxyInstance(
				JdbcMetadata.class.getClassLoader(),
				new Class<?>[] { JdbcMetadata.class },
				(proxy, method, arguments) -> switch ( method.getName() ) {
					case "isJdbcMetadataAccessible" -> false;
					case "getUnquotedIdentifierCaseStrategy" -> IdentifierCaseStrategy.UPPER;
					case "getQuotedIdentifierCaseStrategy" -> IdentifierCaseStrategy.MIXED;
					case "getSqlKeywords" -> Set.of( "fixture_driver_word", "driver_keyword" );
					default -> null;
				}
		);
		final var helper = dialect.buildIdentifierHelper(
				new IdentifierHelperBuildRequest(
						IdentifierHelperBuilder.from( environment ),
						metadata,
						dialect.getKeywordSupport(),
						NameQualifierSupport.BOTH
				)
		);
		assertTrue( helper.toIdentifier( "_fixture" ).isQuoted() );
		assertTrue( helper.toIdentifier( "$fixture" ).isQuoted() );
		assertTrue( helper.toIdentifier( "provider_name" ).isQuoted() );
		assertFalse( helper.toIdentifier( "fixture_name" ).isQuoted() );
		assertTrue( helper.isReservedWord( "driver_keyword" ) );
		assertFalse( helper.isReservedWord( "fixture_driver_word" ) );
	}

	@Test
	void decoratesLiteralAndSuppliesQueryUtilities() {
		final var result = new StringBuilder();
		dialect.getLiteralSupport().appendLiteral( new StringBuilderSqlAppender( result ), "it's" );
		assertEquals( "fixture('it''s')", result.toString() );
		assertEquals( QueryHintPlacement.BEFORE_COMMENT, dialect.getQueryHintPlacement() );
		assertEquals( "/*+ parallel */ select 1", dialect.getQueryHintString( "select 1", "parallel" ) );
		assertEquals(
				"select * from things use index (things_idx) where id=1",
				dialect.addUseIndexHint( "select * from things where id=1", "things_idx" )
		);

		final var temporal = new StringBuilder();
		dialect.getLiteralSupport().appendDateTimeLiteral(
				new StringBuilderSqlAppender( temporal ),
				LocalDate.of( 2026, 1, 2 ),
				TemporalType.DATE,
				TimeZone.getTimeZone( "UTC" )
		);
		assertEquals( "{d '2026-01-02'}", temporal.toString() );

		final var offsetTimestamp = new StringBuilder();
		dialect.getLiteralSupport().appendDateTimeLiteral(
				new StringBuilderSqlAppender( offsetTimestamp ),
				OffsetDateTime.of( 2026, 1, 2, 3, 4, 5, 123_456_000, ZoneOffset.UTC ),
				TemporalType.TIMESTAMP,
				TimeZone.getTimeZone( "UTC" )
		);
		assertEquals( "fixture timestamp '2026-01-02 03:04:05.123456+00:00'", offsetTimestamp.toString() );

		final var array = new StringBuilder();
		dialect.getLiteralSupport().appendArrayLiteral(
				new StringBuilderSqlAppender( array ),
				new Object[] { 1, null, 2 },
				(appender, value, suppliedDialect, options) -> appender.appendSql( value.toString() ),
				null
		);
		assertEquals( "ARRAY[1,null,2]", array.toString() );
	}

	@Test
	void suppliesIndependentFetchAndNativeMarkerContracts() {
		assertTrue( dialect.getFetchClauseSupport().supports( ROWS_ONLY ) );
		assertTrue( dialect.getFetchClauseSupport().supports( PERCENT_ONLY ) );
		assertFalse( dialect.getFetchClauseSupport().supports( ROWS_WITH_TIES ) );
		assertEquals( "$fixture2", dialect.getNativeParameterMarkerStrategy().createMarker( 2, null ) );
	}

	@Test
	void contributesDefensiveDefaultsAndRetainsFocusedCapabilities() {
		final var first = dialect.getDefaultProperties();
		assertEquals( "7", first.getProperty( AvailableSettings.STATEMENT_BATCH_SIZE ) );
		first.setProperty( AvailableSettings.STATEMENT_BATCH_SIZE, "99" );
		final var second = dialect.getDefaultProperties();
		assertNotSame( first, second );
		assertEquals( "7", second.getProperty( AvailableSettings.STATEMENT_BATCH_SIZE ) );
		assertEquals( ScrollMode.SCROLL_SENSITIVE, dialect.defaultScrollMode() );
		assertFalse( dialect.isJdbcLogWarningsEnabledByDefault() );
	}

	@SuppressWarnings("unchecked")
	private static <T> T proxy(Class<T> contract) {
		return (T) Proxy.newProxyInstance(
				contract.getClassLoader(),
				new Class<?>[] { contract },
				(proxy, method, arguments) -> null
		);
	}
}
