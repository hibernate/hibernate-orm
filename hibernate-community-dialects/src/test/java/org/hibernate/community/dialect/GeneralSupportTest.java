/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.queryhint.spi.QueryHintPlacement;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies representative community-provider identifier, literal, and query profiles.
///
/// @author Steve Ebersole
public class GeneralSupportTest {
	@Test
	void preservesLegacyIdentifierKeywordAndHintPolicies() {
		final var postgresql = new PostgreSQLLegacyDialect();
		assertEquals( 63, postgresql.getIdentifierSupport().getMaxIdentifierLength() );
		assertEquals( QueryHintPlacement.BEFORE_COMMENT, postgresql.getQueryHintPlacement() );
		assertEquals( "/*+ parallel */ select 1", postgresql.getQueryHintString( "select 1", "parallel" ) );

		final var hsql = new HSQLLegacyDialect();
		assertTrue( hsql.getKeywords().contains( "period" ) );
		assertThrowsImmutable( hsql );
	}

	@Test
	void preservesLegacyLiteralAndDefaultPropertyValues() {
		final var mysql = new MySQLLegacyDialect();
		final var literal = new StringBuilder();
		mysql.getLiteralSupport().appendLiteral( new StringBuilderSqlAppender( literal ), "a\\b" );
		assertEquals( "'a\\\\b'", literal.toString() );
		assertEquals( "2", mysql.getDefaultProperties().getProperty( AvailableSettings.MAX_FETCH_DEPTH ) );

		final var hana = new HANALegacyDialect();
		assertEquals( "true", hana.getDefaultProperties().getProperty( AvailableSettings.NON_CONTEXTUAL_LOB_CREATION ) );
		assertEquals( "false", hana.getDefaultProperties().getProperty( AvailableSettings.USE_GET_GENERATED_KEYS ) );
		assertFalse( new SingleStoreDialect().isJdbcLogWarningsEnabledByDefault() );
	}

	private static void assertThrowsImmutable(HSQLLegacyDialect dialect) {
		try {
			dialect.getKeywords().add( "later" );
			throw new AssertionError( "keyword profile was mutable" );
		}
		catch (UnsupportedOperationException expected) {
			// expected
		}
	}
}
