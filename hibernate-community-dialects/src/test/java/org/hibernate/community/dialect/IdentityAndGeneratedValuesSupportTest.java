/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.Set;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.generated.spi.GeneratedValuesSupport;
import org.hibernate.dialect.identity.spi.IdentityValueRetrieval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.dialect.generated.spi.GeneratedValuesSupport.Capability.ARBITRARY_GENERATED_KEYS;
import static org.hibernate.dialect.generated.spi.GeneratedValuesSupport.Capability.INSERT_RETURNING;
import static org.hibernate.dialect.generated.spi.GeneratedValuesSupport.Capability.INSERT_RETURNING_ROW_ID;
import static org.hibernate.dialect.generated.spi.GeneratedValuesSupport.Capability.UPDATE_RETURNING;

/// Verifies provider-owned identity implementations and generated-values
/// profiles in the community Dialect module.
///
/// @author Steve Ebersole
public class IdentityAndGeneratedValuesSupportTest {
	@Test
	void communityGeneratedValuesProfilesPreserveInheritedAndVersionedAnswers() {
		assertProfile( new DB2LegacyDialect().getGeneratedValuesSupport(), INSERT_RETURNING, UPDATE_RETURNING );
		assertProfile( new DB2iLegacyDialect( DatabaseVersion.make( 7, 5 ) ).getGeneratedValuesSupport(), INSERT_RETURNING, UPDATE_RETURNING );
		assertProfile(
				new GaussDBDialect().getGeneratedValuesSupport(),
				INSERT_RETURNING,
				UPDATE_RETURNING,
				INSERT_RETURNING_ROW_ID
		);
		assertProfile( new OracleLegacyDialect( DatabaseVersion.make( 11 ) ).getGeneratedValuesSupport() );
		assertProfile(
				new OracleLegacyDialect( DatabaseVersion.make( 12 ) ).getGeneratedValuesSupport(),
				ARBITRARY_GENERATED_KEYS
		);
		assertProfile( new H2LegacyDialect().getGeneratedValuesSupport() );
		assertProfile( new MySQLLegacyDialect().getGeneratedValuesSupport() );
		assertProfile( new MariaDBLegacyDialect().getGeneratedValuesSupport() );
		assertProfile( new PostgreSQLLegacyDialect().getGeneratedValuesSupport() );
		assertProfile( new PostgresPlusLegacyDialect().getGeneratedValuesSupport() );
	}

	@Test
	void communityIdentityProfilesOwnTheirImplementationsAndPreserveBranches() {
		assertThat( new DB2LegacyDialect().getIdentityColumnSupport().getClass().getPackageName() )
				.startsWith( "org.hibernate.community.dialect.identity.internal" );
		assertThat( new PostgreSQLLegacyDialect().getIdentityColumnSupport().getClass().getPackageName() )
				.startsWith( "org.hibernate.community.dialect.identity.internal" );
		assertThat( new SQLServerLegacyDialect().getIdentityColumnSupport().getClass().getPackageName() )
				.startsWith( "org.hibernate.community.dialect.identity.internal" );

		assertThat( new OracleLegacyDialect( DatabaseVersion.make( 11 ) )
				.getIdentityColumnSupport()
				.getIdentityValueRetrieval() )
				.isEqualTo( IdentityValueRetrieval.INFERRED_GENERATED_KEYS );
		assertThat( new OracleLegacyDialect( DatabaseVersion.make( 12 ) )
				.getIdentityColumnSupport()
				.getIdentityValueRetrieval() )
				.isEqualTo( IdentityValueRetrieval.NAMED_GENERATED_KEYS );
		assertThat( new SybaseAnywhereDialect().getIdentityColumnSupport().getIdentityValueRetrieval() )
				.isEqualTo( IdentityValueRetrieval.INFERRED_GENERATED_KEYS );

		final var hsql1 = new org.hibernate.community.dialect.identity.internal.HSQLIdentityColumnSupport(
				DatabaseVersion.make( 1, 8 )
		);
		assertThat( hsql1.hasIdentityInsertKeyword() ).isTrue();
		assertThat( hsql1.getIdentityInsertString() ).isEqualTo( "null" );
		final var hsql2 = new HSQLLegacyDialect( DatabaseVersion.make( 2 ) ).getIdentityColumnSupport();
		assertThat( hsql2.hasIdentityInsertKeyword() ).isTrue();
		assertThat( hsql2.getIdentityInsertString() ).isEqualTo( "default" );
	}
	private static void assertProfile(
			GeneratedValuesSupport support,
			GeneratedValuesSupport.Capability... capabilities) {
		final Set<GeneratedValuesSupport.Capability> expected = Set.of( capabilities );
		assertThat( support.getCapabilities() ).isEqualTo( expected );
		assertThat( support.unquoteGeneratedKeyColumnNames() ).isFalse();
		for ( GeneratedValuesSupport.Capability capability : GeneratedValuesSupport.Capability.values() ) {
			assertThat( support.supports( capability ) )
					.as( capability.name() )
					.isEqualTo( expected.contains( capability ) );
		}
	}
}
