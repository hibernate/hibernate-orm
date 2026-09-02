/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identifier.spi.KeywordRegistration;
import org.hibernate.dialect.queryhint.spi.QueryHintPlacement;
import org.hibernate.dialect.queryhint.spi.QueryHints;
import org.hibernate.query.spi.QueryOptionsAdapter;
import org.hibernate.sql.Alias;
import org.hibernate.sql.exec.internal.QuerySqlDecorator;
import org.hibernate.sql.spi.SqlComments;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the focused identifier, keyword, literal, query-decoration, and
/// default-property contracts.
///
/// @author Steve Ebersole
public class DialectGeneralSupportTests {
	@Test
	void quotesIdentifiersAndRecognizesHistoricalMarkerPairs() {
		final var dialect = new TestingDialect();
		assertNull( dialect.getIdentifierSupport().toQuotedIdentifier( null ) );
		assertEquals( "[name]", dialect.getIdentifierSupport().toQuotedIdentifier( "name" ) );
		assertEquals( "[name]", dialect.getIdentifierSupport().quote( "`name`" ) );
		assertEquals( "`name", dialect.getIdentifierSupport().quote( "`name" ) );
		assertEquals( "name`", dialect.getIdentifierSupport().quote( "name`" ) );
		assertEquals( "[alias_x]", new Alias( "_x" ).toAliasString( "[alias]" ) );
		assertEquals( "\"alias_x\"", new Alias( "_x" ).toAliasString( "\"alias\"" ) );
		assertEquals( "`alias_x`", new Alias( "_x" ).toAliasString( "`alias`" ) );
		assertEquals( "[abcdef_x]", new Alias( 8, "_x" ).toAliasString( "[abcdefghij]" ) );
	}

	@Test
	void materializesImmutableKeywordsOnce() {
		final var dialect = new TestingDialect();
		assertTrue( dialect.getKeywords().contains( "select" ) );
		assertTrue( dialect.getKeywords().contains( "fixture_keyword" ) );
		assertEquals( 1, dialect.keywordContributions.get() );
		assertThrows( UnsupportedOperationException.class, () -> dialect.getKeywords().add( "later" ) );
		assertEquals( 1, dialect.keywordContributions.get() );
	}

	@Test
	void rendersEveryStandardLiteralFamilyAndRejectsUnknownAmounts() {
		final var literals = new Dialect( DatabaseVersion.make( 1 ) ) {
		}.getLiteralSupport();
		assertEquals( "'it''s'", render( appender -> literals.appendLiteral( appender, "it's" ) ) );
		assertEquals( "X'010f'", render( appender -> literals.appendBinaryLiteral( appender, new byte[] { 1, 15 } ) ) );
		assertEquals( "1", literals.toBooleanValueString( true ) );
		assertEquals( "0", literals.toBooleanValueString( false ) );
		assertEquals( "interval '2' day", render( appender -> literals.appendIntervalLiteral( appender, Duration.ofDays( 2 ) ) ) );
		assertEquals(
				"interval '3661.500000000' second",
				render( appender -> literals.appendIntervalLiteral( appender, Duration.ofSeconds( 3661, 500_000_000 ) ) )
		);
		assertEquals(
				"(interval '1' year+interval '2' month+interval '3' day)",
				render( appender -> literals.appendIntervalLiteral( appender, Period.of( 1, 2, 3 ) ) )
		);
		final TemporalAmount unsupported = new TemporalAmount() {
			@Override public long get(TemporalUnit unit) { return 0; }
			@Override public List<TemporalUnit> getUnits() { return List.of(); }
			@Override public java.time.temporal.Temporal addTo(java.time.temporal.Temporal temporal) { return temporal; }
			@Override public java.time.temporal.Temporal subtractFrom(java.time.temporal.Temporal temporal) { return temporal; }
		};
		assertThrows(
				IllegalArgumentException.class,
				() -> literals.appendIntervalLiteral( new StringBuilderSqlAppender( new StringBuilder() ), unsupported )
		);
	}

	@Test
	void ordersHintsAndCommentsByFinalLeadingPosition() {
		final var options = new QueryOptionsAdapter() {
			@Override public String getComment() { return "outer /* nested */"; }
			@Override public List<String> getDatabaseHints() { return List.of( "first", "second" ); }
		};
		final Dialect after = hintDialect( QueryHintPlacement.AFTER_COMMENT );
		final Dialect before = hintDialect( QueryHintPlacement.BEFORE_COMMENT );
		assertEquals(
				"/* outer /\\* nested *\\/ */ /*+ first,second */ select 1",
				QuerySqlDecorator.decorate( "select 1", options, true, after )
		);
		assertEquals(
				"/*+ first,second */ /* outer /\\* nested *\\/ */ select 1",
				QuerySqlDecorator.decorate( "select 1", options, true, before )
		);
		assertEquals( "/*+ first,second */ select 1", QuerySqlDecorator.decorate( "select 1", options, false, before ) );
		assertEquals( "plain", SqlComments.escape( "plain" ) );
		assertNull( SqlComments.escape( null ) );
		assertEquals(
				"select * from things use index (things_idx) where id=1",
				QueryHints.addUseIndexHint( "select * from things where id=1", "things_idx" )
		);
		assertEquals( "update things set id=1", QueryHints.addUseIndexHint( "update things set id=1", "idx" ) );
	}

	@Test
	void contributesDefaultsOnceAndReturnsDefensiveCopies() {
		final var dialect = new TestingDialect();
		final Properties first = dialect.getDefaultProperties();
		final Properties second = dialect.getDefaultProperties();
		assertNotSame( first, second );
		assertEquals( "9", first.getProperty( "fixture.default" ) );
		first.setProperty( "fixture.default", "changed" );
		assertEquals( "9", dialect.getDefaultProperties().getProperty( "fixture.default" ) );
		assertEquals( 1, dialect.propertyContributions.get() );
	}

	private static Dialect hintDialect(QueryHintPlacement placement) {
		return new Dialect( DatabaseVersion.make( 1 ) ) {
			@Override public QueryHintPlacement getQueryHintPlacement() { return placement; }
			@Override public String getQueryHintString(String sql, List<String> hints) {
				assertThrows( UnsupportedOperationException.class, () -> hints.add( "third" ) );
				return "/*+ " + String.join( ",", hints ) + " */ " + sql;
			}
		};
	}

	private static String render(java.util.function.Consumer<StringBuilderSqlAppender> renderer) {
		final var result = new StringBuilder();
		renderer.accept( new StringBuilderSqlAppender( result ) );
		return result.toString();
	}

	private static class TestingDialect extends Dialect {
		private final AtomicInteger keywordContributions = new AtomicInteger();
		private final AtomicInteger propertyContributions = new AtomicInteger();

		private TestingDialect() {
			super( DatabaseVersion.make( 1 ) );
		}

		@Override public char openQuote() { return '['; }
		@Override public char closeQuote() { return ']'; }

		@Override
		protected void contributeKeywords(KeywordRegistration registration) {
			super.contributeKeywords( registration );
			keywordContributions.incrementAndGet();
			registration.registerKeyword( " Fixture_Keyword " );
		}

		@Override
		protected void contributeDefaultProperties(Properties properties) {
			super.contributeDefaultProperties( properties );
			propertyContributions.incrementAndGet();
			properties.setProperty( "fixture.default", "9" );
		}
	}
}
