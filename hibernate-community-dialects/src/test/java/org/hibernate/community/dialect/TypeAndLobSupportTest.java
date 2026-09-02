/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.type.spi.ObjectNullBindingStrategy;
import org.hibernate.dialect.type.spi.StringValueSemantics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies representative community-provider type and LOB profiles.
///
/// @author Steve Ebersole
public class TypeAndLobSupportTest {
	@Test
	void enumAndObjectNullProfilesAreStable() {
		final var mysql = new MySQLLegacyDialect();
		assertThat( mysql.getEnumSupport() ).isSameAs( mysql.getEnumSupport() );
		assertThat( mysql.getEnumSupport().getTypeDeclaration( "priority", new String[] { "low", "high" } ) )
				.isEqualTo( "enum ('low','high')" );
		assertThat( mysql.getObjectNullBindingStrategy() )
				.isEqualTo( ObjectNullBindingStrategy.SET_NULL_WITH_NULL_TYPE );

		final var gauss = new GaussDBDialect();
		assertThat( gauss.getEnumSupport().getCreateTypeCommands( "priority", new String[] { "low" } ) )
				.hasSize( 3 );
		assertThat( gauss.getObjectNullBindingStrategy() )
				.isEqualTo( ObjectNullBindingStrategy.SET_NULL_WITH_NULL_TYPE );
	}

	@Test
	void rowIdAndUdtProfilesRemainProviderSpecific() {
		final var h2 = new H2LegacyDialect();
		assertThat( h2.getRowIdSupport() ).isSameAs( h2.getRowIdSupport() );
		assertThat( h2.getRowIdSupport().resolveExpression( "ignored" ) ).isEqualTo( "_rowid_" );

		final var db2z = new DB2zLegacyDialect();
		assertThat( db2z.getRowIdSupport().resolveExpression( null ) ).isEqualTo( "rowid_" );
		assertThat( db2z.getRowIdSupport().columnDefinition( "mapped_rowid" ) )
				.isEqualTo( "mapped_rowid rowid not null generated always" );

		final var informix = new InformixDialect();
		assertThat( informix.getUserDefinedTypeExporter() ).isSameAs( informix.getUserDefinedTypeExporter() );
		assertThat( informix.getLobSupport().useMaterializedLobWhenCapacityExceeded() ).isFalse();
	}

	@Test
	void lobAndStringProfilesPreserveLegacyFamilyDifferences() {
		final var postgresql = new PostgreSQLLegacyDialect();
		assertThat( postgresql.getLobSupport() ).isSameAs( postgresql.getLobSupport() );
		assertThat( postgresql.getLobSupport().supportsMaterializedLobAccess() ).isFalse();

		final var oracle = new OracleLegacyDialect();
		assertThat( oracle.getLobSupport().forceLobAsLastValue() ).isTrue();
		assertThat( oracle.getLobSupport().getValueLobFragmentForExtraCreateTableInfo( "payload" ) ).isNull();
		assertThat( oracle.getStringValueSemantics() ).isSameAs( StringValueSemantics.EMPTY_STRING_AS_NULL );

		assertThat( new SybaseAnywhereDialect().getStringValueSemantics() )
				.isSameAs( StringValueSemantics.CHAR_TRAILING_SPACES_STRIPPED );
		assertThat( new TimesTenDialect().getStringValueSemantics() )
				.isSameAs( StringValueSemantics.EMPTY_STRING_AS_NULL );
	}
}
