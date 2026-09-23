/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import org.hibernate.testing.util.MappingTableHelper;

import org.hibernate.boot.mapping.internal.relational.ColumnNameCorrespondence;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.BaseUnitTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Exercises distinct logical keys and database-aware physical correspondence.
///
/// @author Steve Ebersole
@BaseUnitTest
class ColumnNameCorrespondenceTest {
	private static final org.hibernate.relational.naming.spi.PhysicalName.Factory COLUMN_NAMES =
			new org.hibernate.relational.naming.spi.PhysicalName.Factory(
					(text, quoted) -> quoted ? text : text.toUpperCase( java.util.Locale.ROOT ) );

	@Test
	void quotedAndUnquotedLogicalNamesDoNotOverwriteEachOther() {
		final var fixture = fixture();
		final var table = MappingTableHelper.table( "orm", "owner", fixture.factory );
		final var unquoted = column( table, "p_unquoted" );
		final var quoted = column( table, "p_quoted" );
		fixture.names.register( table, new LogicalName( "ssn", false, true ), unquoted );
		fixture.names.register( table, new LogicalName( "ssn", true, true ), quoted );
		assertThat( fixture.names.findPhysicalColumn( table, new LogicalName( "SSN", false, false ) ) ).isSameAs( unquoted );
		assertThat( fixture.names.findPhysicalColumn( table, new LogicalName( "ssn", true, false ) ) ).isSameAs( quoted );
		assertThat( fixture.names.findPhysicalColumn( table, new LogicalName( "SSN", true, true ) ) ).isNull();
		assertThat( fixture.names.matches( quoted, new LogicalName( "ssn", false, true ) ) ).isFalse();
	}

	@Test
	void reverseLookupUsesPhysicalPolicyInsteadOfRenderedString() {
		final var fixture = fixture();
		final var table = MappingTableHelper.table( "orm", "owner", fixture.factory );
		final var logical = new LogicalName( "socialSecurityNumber", false, true );
		fixture.names.register( table, logical, column( table, "p_ssn" ) );
		assertThat( fixture.names.findLogicalName( table, fixture.factory.create( "P_SSN", true ) ) ).isSameAs( logical );
		assertThat( fixture.names.findLogicalName( table, fixture.factory.create( "p_ssn", true ) ) ).isNull();
	}

	@Test
	void aliasesAndInheritedTablesResolveWithoutAcceptingPhysicalReferences() {
		final var fixture = fixture();
		final var parent = MappingTableHelper.table( "orm", "owner", fixture.factory );
		final var child = mock( DenormalizedTable.class );
		when( child.getIncludedTable() ).thenReturn( parent );
		final var original = column( parent, "p_ssn" );
		fixture.names.register( parent, new LogicalName( "ssn", false, true ), original );
		fixture.names.register( parent, new LogicalName( "socialSecurityNumber", false, false ), original );
		final var inherited = column( child, "P_SSN" );
		assertThat( fixture.names.matches( inherited, new LogicalName( "SSN", false, true ) ) ).isTrue();
		assertThat( fixture.names.matches( inherited, new LogicalName( "socialSecurityNumber", false, true ) ) ).isTrue();
		assertThat( fixture.names.matches( inherited, new LogicalName( "p_ssn", false, true ) ) ).isFalse();
		assertThat( fixture.names.findPhysicalColumn( child, new LogicalName( "ssn", false, false ) ) ).isSameAs( original );
		assertThat( fixture.names.findLogicalName( child, inherited ).getText() ).isEqualTo( "socialSecurityNumber" );
	}

	@Test
	void syntheticSourceNameFallbackKeepsLogicalQuotingSemantics() {
		final var fixture = fixture();
		final var synthetic = column( MappingTableHelper.table( "orm", "owner", fixture.factory ), "`Exact`" );
		assertThat( fixture.names.matches( synthetic, new LogicalName( "Exact", true, true ) ) ).isTrue();
		assertThat( fixture.names.matches( synthetic, new LogicalName( "Exact", false, true ) ) ).isFalse();
		assertThat( fixture.names.matches( synthetic, new LogicalName( "exact", true, true ) ) ).isFalse();
	}

	private static Column column(Table table, String name) {
		final var column = new Column( MappingTableHelper.columnName( name, COLUMN_NAMES ) );
		final var value = mock( Value.class );
		when( value.getColumnContainer() ).thenReturn( table );
		column.setValue( value );
		return column;
	}

	private static Fixture fixture() {
		final var environment = mock( JdbcEnvironment.class );
		final var helper = IdentifierHelperBuilder.from( environment ).build();
		when( environment.getIdentifierHelper() ).thenReturn( helper );
		final var database = mock( Database.class );
		when( database.getJdbcEnvironment() ).thenReturn( environment );
		when( database.toIdentifier( anyString() ) ).thenAnswer( call -> Identifier.toIdentifier( call.getArgument( 0 ) ) );
		return new Fixture( new ColumnNameCorrespondence( database ), helper.getPhysicalNameFactory() );
	}

	private record Fixture(ColumnNameCorrespondence names, PhysicalName.Factory factory) {
	}
}
