/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.util.Arrays;
import java.util.List;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.lob.spi.LobSupports;
import org.hibernate.dialect.rowid.spi.RowIdSupports;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.type.spi.EnumSupports;
import org.hibernate.dialect.type.spi.StringValueSemantics;
import org.hibernate.dialect.type.spi.UserDefinedTypeDdlSupport;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.UserDefinedObjectType;
import org.hibernate.tool.schema.spi.StandardUserDefinedTypeExporter;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Verifies the stock type, row-id, and LOB provider contracts.
///
/// @author Steve Ebersole
public class DialectTypeSupportTests {
	@Test
	void enumProfilesPreserveDeclarationsLifecycleAndRelationalValues() {
		final var inline = EnumSupports.inline();
		assertThat( inline ).isSameAs( EnumSupports.inline() );
		assertThat( inline.getTypeDeclaration( "priority", new String[] { "low", "h'igh" } ) )
				.isEqualTo( "enum ('low','h''igh')" );

		final var postgresql = EnumSupports.postgresql();
		assertThat( postgresql.getCreateTypeCommands( "priority", new String[] { "low", "high" } ) )
				.containsExactly(
						"create type priority as enum ('low','high')",
						"create cast (varchar as priority) with inout as implicit",
						"create cast (priority as varchar) with inout as implicit"
				);
		assertThat( postgresql.getDropTypeCommands( "priority" ) )
				.containsExactly( "drop type if exists priority cascade" );

		final var standard = EnumSupports.standard();
		assertThat( standard.getCheckCondition(
				"priority",
				Arrays.asList( "low", null, "low", "h'igh" ),
				VarcharJdbcType.INSTANCE
		) ).isEqualTo( "priority in ('low','low','h''igh') or priority is null" );
		assertThat( standard.getCheckCondition( "rank", List.of( 1, 3, 3 ), IntegerJdbcType.INSTANCE ) )
				.isEqualTo( "rank in (1,3,3)" );
		assertThat( standard.getCheckCondition( "rank", 1, 4 ) ).isEqualTo( "rank between 1 and 4" );

		final var oracle23 = EnumSupports.oracle( DatabaseVersion.make( 23 ) );
		assertThat( oracle23 ).isSameAs( EnumSupports.oracle( DatabaseVersion.make( 23 ) ) );
		assertThat( oracle23.getTypeDeclaration( "priority", new String[] { "low" } ) ).isEqualTo( "priority" );
		assertThat( oracle23.getCreateTypeCommands( "priority", new String[] { "low", "high" } ) )
				.containsExactly( "create domain priority as enum (low='low', high='high')" );
		assertThat( oracle23.getCreateOrdinalTypeCommands( "priority", new String[] { "low", "high" } ) )
				.containsExactly( "create domain priority as enum (low, high)" );
		assertThat( EnumSupports.oracle( DatabaseVersion.make( 22 ) )
				.getTypeDeclaration( "priority", new String[] { "low" } ) ).isNull();
	}

	@Test
	void rowIdProfilesKeepCapabilityAndResolutionCoherent() {
		final var none = RowIdSupports.none();
		assertThat( none.isSupported() ).isFalse();
		assertThat( none.resolveExpression( null ) ).isNull();
		assertThatThrownBy( none::sqlTypeCode ).isInstanceOf( UnsupportedOperationException.class );

		final var fixed = RowIdSupports.fixed( "rowid", 17 );
		assertThat( fixed ).isSameAs( RowIdSupports.fixed( "rowid", 17 ) );
		assertThat( fixed.resolveExpression( "ignored" ) ).isEqualTo( "rowid" );
		assertThat( fixed.columnDefinition( "ignored" ) ).isNull();

		final var requested = RowIdSupports.requestedName( "rowid_", 18, " rowid generated always" );
		assertThat( requested.resolveExpression( "" ) ).isEqualTo( "rowid_" );
		assertThat( requested.resolveExpression( "mapped_id" ) ).isEqualTo( "mapped_id" );
		assertThat( requested.columnDefinition( "mapped_id" ) )
				.isEqualTo( "mapped_id rowid generated always" );
		assertThat( RowIdSupports.requestedName( null, 18, " suffix" ).columnDefinition( null ) ).isNull();
		assertThatIllegalArgumentException().isThrownBy( () -> RowIdSupports.fixed( "", 17 ) );
		assertThat( new SpannerPostgreSQLDialect().getRowIdSupport().isSupported() ).isFalse();
	}

	@Test
	void lobProfilesChangeOnlyTheirDocumentedAxes() {
		final var standard = LobSupports.standard();
		assertThat( standard.supportsJdbcConnectionLobCreation( null ) ).isTrue();
		assertThat( standard.useInputStreamToInsertBlob() ).isTrue();
		assertThat( standard.useConnectionToCreateLob() ).isFalse();
		assertThat( standard.supportsMaterializedLobAccess() ).isTrue();
		assertThat( standard.useMaterializedLobWhenCapacityExceeded() ).isTrue();

		assertThat( LobSupports.noCapacityPromotion().useMaterializedLobWhenCapacityExceeded() ).isFalse();
		assertThat( LobSupports.noContextualCreation().supportsJdbcConnectionLobCreation( null ) ).isFalse();
		assertThat( LobSupports.nonStreaming().useInputStreamToInsertBlob() ).isFalse();
		assertThat( LobSupports.nonStreaming().useConnectionToCreateLob() ).isFalse();
		assertThat( LobSupports.postgresql().supportsMaterializedLobAccess() ).isFalse();

		final var oracle = LobSupports.oracle( true, true );
		assertThat( oracle ).isSameAs( LobSupports.oracle( true, true ) );
		assertThat( oracle.useInputStreamToInsertBlob() ).isFalse();
		assertThat( oracle.forceLobAsLastValue() ).isTrue();
		assertThat( oracle.getValueLobFragmentForExtraCreateTableInfo( "payload" ) )
				.isEqualTo( " lob(payload) query as value" );
		assertThat( LobSupports.oracle( false, false ).getValueLobFragmentForExtraCreateTableInfo( "payload" ) )
				.isNull();
	}

	@Test
	void immutableConfigurationValuesValidateEveryAxis() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new UserDefinedTypeDdlSupport( null, "", ExistenceCheckPlacement.NONE ) );
		assertThatIllegalArgumentException().isThrownBy(
				() -> new StringValueSemantics( null, StringValueSemantics.CharTrailingSpaceSemantics.PRESERVED ) );
		assertThat( StringValueSemantics.EMPTY_STRING_AS_NULL_AND_CHAR_TRAILING_SPACES_STRIPPED
				.treatsEmptyStringAsNull() ).isTrue();
		assertThat( StringValueSemantics.EMPTY_STRING_AS_NULL_AND_CHAR_TRAILING_SPACES_STRIPPED
				.stripsCharTrailingSpaces() ).isTrue();
	}

	@Test
	void standardUdtExporterPreservesGrammarFragmentsAndExistencePlacement() {
		final UserDefinedObjectType userDefinedType = mock( UserDefinedObjectType.class );
		when( userDefinedType.getNameIdentifier() ).thenReturn( Identifier.toIdentifier( "fixture_type" ) );
		when( userDefinedType.getColumns() ).thenReturn( List.of() );

		final SqlStringGenerationContext context = mock( SqlStringGenerationContext.class );
		when( context.format( any( QualifiedName.class ) ) ).thenReturn( "fixture_type" );
		final Metadata metadata = mock( Metadata.class );
		final Dialect dialect = new Dialect( DatabaseVersion.make( 1 ) ) {
		};

		final var afterName = new StandardUserDefinedTypeExporter(
				dialect,
				new UserDefinedTypeDdlSupport( "object ", " organization heap", ExistenceCheckPlacement.AFTER_NAME )
		);
		assertThat( afterName.getSqlCreateStrings( userDefinedType, metadata, context ) )
				.containsExactly( "create type fixture_type as object () organization heap" );
		assertThat( afterName.getSqlDropStrings( userDefinedType, metadata, context ) )
				.containsExactly( "drop type fixture_type if exists" );

		final var beforeName = new StandardUserDefinedTypeExporter(
				dialect,
				new UserDefinedTypeDdlSupport( "", "", ExistenceCheckPlacement.BEFORE_NAME )
		);
		assertThat( beforeName.getSqlDropStrings( userDefinedType, metadata, context ) )
				.containsExactly( "drop type if exists fixture_type" );
	}
}
