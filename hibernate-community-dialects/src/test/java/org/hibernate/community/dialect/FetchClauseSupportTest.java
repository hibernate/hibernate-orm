/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.sql.ast.spi.FetchClauseSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies the legacy and community fetch-clause version profiles.
///
/// @author Steve Ebersole
public class FetchClauseSupportTest {
	@Test
	void legacyVersionBoundariesArePreserved() {
		assertThat( new FirebirdDialect( DatabaseVersion.make( 2, 5 ) ).getFetchClauseSupport() )
				.isSameAs( FetchClauseSupport.NONE );
		assertThat( new FirebirdDialect( DatabaseVersion.make( 3 ) ).getFetchClauseSupport() )
				.isSameAs( FetchClauseSupport.ROWS_ONLY );
		assertThat( new H2LegacyDialect( DatabaseVersion.make( 1, 4, 197 ) ).getFetchClauseSupport() )
				.isSameAs( FetchClauseSupport.NONE );
		assertThat( new H2LegacyDialect( DatabaseVersion.make( 1, 4, 198 ) ).getFetchClauseSupport() )
				.isSameAs( FetchClauseSupport.ALL );
		assertThat( new IngresDialect( DatabaseVersion.make( 9, 2 ) ).getFetchClauseSupport() )
				.isSameAs( FetchClauseSupport.NONE );
		assertThat( new IngresDialect( DatabaseVersion.make( 9, 3 ) ).getFetchClauseSupport() )
				.isSameAs( FetchClauseSupport.ALL );
		assertThat( new OracleLegacyDialect( DatabaseVersion.make( 12, 1 ) ).getFetchClauseSupport() )
				.isSameAs( FetchClauseSupport.NONE );
		assertThat( new OracleLegacyDialect( DatabaseVersion.make( 12, 2 ) ).getFetchClauseSupport() )
				.isSameAs( FetchClauseSupport.ALL );
		assertThat( new PostgreSQLLegacyDialect( DatabaseVersion.make( 8, 3 ) ).getFetchClauseSupport() )
				.isSameAs( FetchClauseSupport.NONE );
		assertThat( new PostgreSQLLegacyDialect( DatabaseVersion.make( 8, 4 ) ).getFetchClauseSupport() )
				.isSameAs( FetchClauseSupport.ROWS_ONLY );
		assertThat( new PostgreSQLLegacyDialect( DatabaseVersion.make( 13 ) ).getFetchClauseSupport() )
				.isSameAs( FetchClauseSupport.ROWS );
		assertThat( new SQLServerLegacyDialect( DatabaseVersion.make( 10 ) ).getFetchClauseSupport() )
				.isSameAs( FetchClauseSupport.NONE );
		assertThat( new SQLServerLegacyDialect( DatabaseVersion.make( 11 ) ).getFetchClauseSupport() )
				.isSameAs( FetchClauseSupport.ALL );
	}

	@Test
	void explicitPostgreSqlDerivedProfilesRemainRestricted() {
		assertThat( new GaussDBDialect().getFetchClauseSupport() ).isSameAs( FetchClauseSupport.NONE );
		assertThat( new InterSystemsIRISDialect().getFetchClauseSupport() )
				.isSameAs( FetchClauseSupport.ROWS_ONLY );
	}
}
