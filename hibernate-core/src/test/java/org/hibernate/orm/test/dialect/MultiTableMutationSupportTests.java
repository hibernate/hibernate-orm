/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DB2iDialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.mutation.spi.MultiTableMutationStrategyKind;
import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.sql.ast.spi.CteSupport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hibernate.dialect.mutation.spi.MultiTableMutationStrategyKind.CTE;
import static org.hibernate.dialect.mutation.spi.MultiTableMutationStrategyKind.GLOBAL_TEMPORARY_TABLE;
import static org.hibernate.dialect.mutation.spi.MultiTableMutationStrategyKind.LOCAL_TEMPORARY_TABLE;
import static org.hibernate.dialect.mutation.spi.MultiTableMutationStrategyKind.PERSISTENT_TABLE;

/// Verifies the provider profile and maintained Dialect fallback-family matrix.
///
/// @author Steve Ebersole
public class MultiTableMutationSupportTests {
	@Test
	void stockProfilesAndSameKindFactoryAreCanonical() {
		for ( MultiTableMutationStrategyKind kind : MultiTableMutationStrategyKind.values() ) {
			final MultiTableMutationSupport support = MultiTableMutationSupport.forBoth( kind );
			assertThat( support.mutationStrategyKind() ).isEqualTo( kind );
			assertThat( support.insertStrategyKind() ).isEqualTo( kind );
			assertThat( support ).isSameAs( stockProfile( kind ) );
		}
	}

	@Test
	void asymmetricProfilesKeepTheSelectionsIndependentAndUseValueEquality() {
		final MultiTableMutationSupport support = new MultiTableMutationSupport( CTE, LOCAL_TEMPORARY_TABLE );
		assertThat( support.mutationStrategyKind() ).isEqualTo( CTE );
		assertThat( support.insertStrategyKind() ).isEqualTo( LOCAL_TEMPORARY_TABLE );
		assertThat( support ).isEqualTo( new MultiTableMutationSupport( CTE, LOCAL_TEMPORARY_TABLE ) );
		assertThat( support ).isNotEqualTo( MultiTableMutationSupport.CTE );
	}

	@Test
	void nullSelectionsAreRejected() {
		assertThatIllegalArgumentException()
				.isThrownBy( () -> new MultiTableMutationSupport( null, CTE ) )
				.withMessageContaining( "mutationStrategyKind" );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> new MultiTableMutationSupport( CTE, null ) )
				.withMessageContaining( "insertStrategyKind" );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> MultiTableMutationSupport.forBoth( null ) )
				.withMessageContaining( "strategyKind" );
	}

	@Test
	void maintainedDialectProfilesPreserveEffectiveSelections() {
		assertProfile( new Dialect( DatabaseVersion.make( 1 ) ) {}, PERSISTENT_TABLE );
		assertProfile( new SpannerPostgreSQLDialect(), PERSISTENT_TABLE );
		assertProfile( new CockroachDialect(), PERSISTENT_TABLE );

		assertProfile( new DB2Dialect(), CTE );
		assertProfile( new DB2iDialect(), CTE );
		assertProfile( new DB2zDialect(), CTE );
		assertProfile( new PostgreSQLDialect(), CTE );
		assertProfile( new PostgresPlusDialect(), CTE );

		assertProfile( new H2Dialect(), GLOBAL_TEMPORARY_TABLE );
		assertProfile( new HANADialect(), GLOBAL_TEMPORARY_TABLE );
		assertProfile( new OracleDialect(), GLOBAL_TEMPORARY_TABLE );

		assertProfile( new HSQLDialect(), LOCAL_TEMPORARY_TABLE );
		assertProfile( new MySQLDialect(), LOCAL_TEMPORARY_TABLE );
		assertProfile( new MariaDBDialect(), LOCAL_TEMPORARY_TABLE );
		assertProfile( new SQLServerDialect(), LOCAL_TEMPORARY_TABLE );
		assertProfile( new SybaseDialect(), LOCAL_TEMPORARY_TABLE );
	}

	private static MultiTableMutationSupport stockProfile(MultiTableMutationStrategyKind kind) {
		return switch ( kind ) {
			case CTE -> MultiTableMutationSupport.CTE;
			case LOCAL_TEMPORARY_TABLE -> MultiTableMutationSupport.LOCAL_TEMPORARY_TABLE;
			case GLOBAL_TEMPORARY_TABLE -> MultiTableMutationSupport.GLOBAL_TEMPORARY_TABLE;
			case PERSISTENT_TABLE -> MultiTableMutationSupport.PERSISTENT_TABLE;
		};
	}

	private static void assertProfile(Dialect dialect, MultiTableMutationStrategyKind expected) {
		assertThat( dialect.getMultiTableMutationSupport() )
				.as( dialect.getClass().getName() )
				.isSameAs( stockProfile( expected ) );
		assertPrerequisite( dialect, expected );
	}

	private static void assertPrerequisite(Dialect dialect, MultiTableMutationStrategyKind kind) {
		switch ( kind ) {
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
