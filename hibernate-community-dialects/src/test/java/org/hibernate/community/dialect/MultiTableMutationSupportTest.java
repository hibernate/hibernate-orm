/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.mutation.spi.MultiTableMutationStrategyKind;
import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.orm.test.dialect.resolver.TestingDialectResolutionInfo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.mutation.spi.MultiTableMutationStrategyKind.CTE;
import static org.hibernate.dialect.mutation.spi.MultiTableMutationStrategyKind.GLOBAL_TEMPORARY_TABLE;
import static org.hibernate.dialect.mutation.spi.MultiTableMutationStrategyKind.LOCAL_TEMPORARY_TABLE;
import static org.hibernate.dialect.mutation.spi.MultiTableMutationStrategyKind.PERSISTENT_TABLE;

/// Verifies the community Dialect multi-table mutation fallback-family matrix.
///
/// @author Steve Ebersole
public class MultiTableMutationSupportTest {
	@Test
	void directCommunityOwnersPreserveTheirSelections() {
		assertProfile( new DB2LegacyDialect(), CTE );
		assertProfile( new PostgreSQLLegacyDialect( DatabaseVersion.make( 9, 1 ) ), CTE );
		assertProfile( new GaussDBDialect(), CTE );

		assertProfile( new InterSystemsIRISDialect(), GLOBAL_TEMPORARY_TABLE );
		assertProfile( new IngresDialect(), GLOBAL_TEMPORARY_TABLE );
		assertProfile( new TeradataDialect(), GLOBAL_TEMPORARY_TABLE );
		assertProfile( new TimesTenDialect(), GLOBAL_TEMPORARY_TABLE );
		assertProfile( new HANALegacyDialect(), GLOBAL_TEMPORARY_TABLE );
		assertProfile( new OracleLegacyDialect(), GLOBAL_TEMPORARY_TABLE );

		assertProfile( new DerbyDialect(), LOCAL_TEMPORARY_TABLE );
		assertProfile( new DerbyLegacyDialect(), LOCAL_TEMPORARY_TABLE );
		assertProfile( new InformixDialect(), LOCAL_TEMPORARY_TABLE );
		assertProfile( new MaxDBDialect(), LOCAL_TEMPORARY_TABLE );
		assertProfile( new H2LegacyDialect(), LOCAL_TEMPORARY_TABLE );
		assertProfile( new MySQLLegacyDialect(), LOCAL_TEMPORARY_TABLE );
		assertProfile( new SingleStoreDialect(), LOCAL_TEMPORARY_TABLE );
	}

	@Test
	void inheritedCommunityFamiliesPreserveTheirSelections() {
		assertProfile( new DB2iLegacyDialect(), CTE );
		assertProfile( new DB2zLegacyDialect(), CTE );
		assertProfile( new PostgresPlusLegacyDialect( DatabaseVersion.make( 9, 1 ) ), CTE );

		assertProfile( new MariaDBLegacyDialect(), LOCAL_TEMPORARY_TABLE );
		assertProfile( new SQLServerLegacyDialect(), LOCAL_TEMPORARY_TABLE );
		assertProfile( new SybaseLegacyDialect(), LOCAL_TEMPORARY_TABLE );
		assertProfile( new SybaseASELegacyDialect(), LOCAL_TEMPORARY_TABLE );

		assertProfile( new CockroachLegacyDialect(), PERSISTENT_TABLE );
	}

	@Test
	void versionedCommunityProfilesPreserveBothBranches() {
		assertProfile(
				new HSQLLegacyDialect(
						TestingDialectResolutionInfo.forDatabaseInfo( "HSQL Database Engine", 1, 8 )
				),
				GLOBAL_TEMPORARY_TABLE
		);
		assertProfile( new HSQLLegacyDialect( DatabaseVersion.make( 2 ) ), LOCAL_TEMPORARY_TABLE );

		assertProfile( new FirebirdDialect( DatabaseVersion.make( 2, 0 ) ), PERSISTENT_TABLE );
		assertProfile( new FirebirdDialect( DatabaseVersion.make( 2, 1 ) ), GLOBAL_TEMPORARY_TABLE );
	}

	private static void assertProfile(Dialect dialect, MultiTableMutationStrategyKind expected) {
		final MultiTableMutationSupport expectedProfile = MultiTableMutationSupport.forBoth( expected );
		assertThat( dialect.getMultiTableMutationSupport() )
				.as( dialect.getClass().getName() )
				.isSameAs( expectedProfile );
		switch ( expected ) {
			case CTE -> assertThat( dialect.getCteSupport().supports( CteSupport.MutationFeature.NON_QUERY ) )
					.as( dialect.getClass().getName() )
					.isTrue();
			case LOCAL_TEMPORARY_TABLE -> assertThat( dialect.getLocalTemporaryTableStrategy() )
					.as( dialect.getClass().getName() )
					.isNotNull();
			case GLOBAL_TEMPORARY_TABLE -> assertThat( dialect.getGlobalTemporaryTableStrategy() )
					.as( dialect.getClass().getName() )
					.isNotNull();
			case PERSISTENT_TABLE -> assertThat( dialect.getPersistentTemporaryTableStrategy() )
					.as( dialect.getClass().getName() )
					.isNotNull();
		}
	}
}
