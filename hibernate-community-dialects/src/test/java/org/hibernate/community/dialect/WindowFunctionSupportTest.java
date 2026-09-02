/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.function.spi.WindowFunctionSupport.Feature.PARTITION_BY;
import static org.hibernate.dialect.function.spi.WindowFunctionSupport.Feature.RANGE_FRAME;
import static org.hibernate.dialect.function.spi.WindowFunctionSupport.Feature.ROWS_FRAME;
import static org.hibernate.dialect.function.spi.WindowFunctionSupport.Feature.WINDOW_FUNCTIONS;

/// Verifies every community window-function profile and version boundary.
///
/// @author Steve Ebersole
public class WindowFunctionSupportTest {
	@Test
	void baselineAndAsymmetricProfiles() {
		assertFeatures( new DerbyDialect(), WINDOW_FUNCTIONS );
		assertFeatures( new CUBRIDDialect(), WINDOW_FUNCTIONS, PARTITION_BY );
		assertFeatures( new AltibaseDialect(), WINDOW_FUNCTIONS, PARTITION_BY );
		assertFeatures( new InterSystemsIRISDialect(), WINDOW_FUNCTIONS, PARTITION_BY );
		assertRowsAndRange( new DB2LegacyDialect() );
		assertRowsAndRange( new HANALegacyDialect() );
		assertRowsAndRange( new OracleLegacyDialect() );
		assertRowsAndRange( new SQLServerLegacyDialect() );
		assertRowsAndRange( new SingleStoreDialect() );
		assertFull( new CockroachLegacyDialect() );
		assertFull( new GaussDBDialect() );
		assertFeatures( new SybaseASELegacyDialect() );
	}

	@Test
	void documentedVersionBoundaries() {
		assertFeatures( new DerbyLegacyDialect( DatabaseVersion.make( 10, 3 ) ) );
		assertFeatures( new DerbyLegacyDialect( DatabaseVersion.make( 10, 4 ) ), WINDOW_FUNCTIONS );

		assertFeatures( new FirebirdDialect( DatabaseVersion.make( 2, 5 ) ) );
		assertFeatures( new FirebirdDialect( DatabaseVersion.make( 3 ) ), WINDOW_FUNCTIONS, PARTITION_BY );
		assertRowsAndRange( new FirebirdDialect( DatabaseVersion.make( 4 ) ) );

		assertFeatures( new InformixDialect( DatabaseVersion.make( 12, 9 ) ) );
		assertRowsAndRange( new InformixDialect( DatabaseVersion.make( 12, 10 ) ) );
		assertFeatures( new IngresDialect( DatabaseVersion.make( 10, 1 ) ) );
		assertFeatures( new IngresDialect( DatabaseVersion.make( 10, 2 ) ), WINDOW_FUNCTIONS );

		assertFeatures( new MariaDBLegacyDialect( DatabaseVersion.make( 10, 1 ) ) );
		assertRowsAndRange( new MariaDBLegacyDialect( DatabaseVersion.make( 10, 2 ) ) );
		assertFeatures( new MySQLLegacyDialect( DatabaseVersion.make( 8, 0, 1 ) ) );
		assertRowsAndRange( new MySQLLegacyDialect( DatabaseVersion.make( 8, 0, 2 ) ) );

		assertFeatures( new H2LegacyDialect( DatabaseVersion.make( 1, 4, 199 ) ) );
		assertFull( new H2LegacyDialect( DatabaseVersion.make( 1, 4, 200 ) ) );

		assertFeatures( new PostgreSQLLegacyDialect( DatabaseVersion.make( 8, 3 ) ) );
		assertRowsAndRange( new PostgreSQLLegacyDialect( DatabaseVersion.make( 8, 4 ) ) );
		assertFull( new PostgreSQLLegacyDialect( DatabaseVersion.make( 11 ) ) );

		assertFeatures( new SQLiteDialect( DatabaseVersion.make( 3, 24 ) ) );
		assertRowsAndRange( new SQLiteDialect( DatabaseVersion.make( 3, 25 ) ) );
		assertFull( new SQLiteDialect( DatabaseVersion.make( 3, 28 ) ) );

		assertFeatures( new SybaseAnywhereDialect( DatabaseVersion.make( 8 ) ) );
		assertRowsAndRange( new SybaseAnywhereDialect( DatabaseVersion.make( 9 ) ) );

		assertFeatures( new TeradataDialect( DatabaseVersion.make( 16, 9 ) ) );
		assertFeatures(
				new TeradataDialect( DatabaseVersion.make( 16, 10 ) ),
				WINDOW_FUNCTIONS,
				PARTITION_BY,
				ROWS_FRAME
		);
	}

	@Test
	void focusedFilterAndOrdinalitySupplyPointsPreserveCommunityValues() {
		assertThat( new H2LegacyDialect( DatabaseVersion.make( 1, 4, 196 ) ).supportsFilterClause() ).isFalse();
		assertThat( new H2LegacyDialect( DatabaseVersion.make( 1, 4, 197 ) ).supportsFilterClause() ).isTrue();
		assertThat( new HSQLLegacyDialect().supportsFilterClause() ).isTrue();
		assertThat( new PostgreSQLLegacyDialect( DatabaseVersion.make( 9, 3 ) ).supportsFilterClause() ).isFalse();
		assertThat( new PostgreSQLLegacyDialect( DatabaseVersion.make( 9, 4 ) ).supportsFilterClause() ).isTrue();
		assertThat( new GaussDBDialect().supportsFilterClause() ).isFalse();
		assertThat( new SQLiteDialect( DatabaseVersion.make( 3, 3 ) ).supportsFilterClause() ).isTrue();

		assertThat( new H2LegacyDialect().getDefaultOrdinalityColumnName() ).isEqualTo( "nord" );
		assertThat( new HSQLLegacyDialect().getDefaultOrdinalityColumnName() ).isEqualTo( "c2" );
		assertThat( new PostgreSQLLegacyDialect().getDefaultOrdinalityColumnName() ).isEqualTo( "ordinality" );
		assertThat( new GaussDBDialect().getDefaultOrdinalityColumnName() ).isEqualTo( "ordinality" );
		assertThat( new CockroachLegacyDialect().getDefaultOrdinalityColumnName() ).isEqualTo( "ordinality" );
	}

	private static void assertRowsAndRange(Dialect dialect) {
		assertFeatures( dialect, WINDOW_FUNCTIONS, PARTITION_BY, ROWS_FRAME, RANGE_FRAME );
	}

	private static void assertFull(Dialect dialect) {
		assertFeatures( dialect, WindowFunctionSupport.Feature.values() );
	}

	private static void assertFeatures(Dialect dialect, WindowFunctionSupport.Feature... features) {
		assertThat( dialect.getWindowFunctionSupport().getFeatures() )
				.as( dialect.getClass().getSimpleName() + " " + dialect.getVersion() )
				.containsExactlyInAnyOrder( features );
	}
}
