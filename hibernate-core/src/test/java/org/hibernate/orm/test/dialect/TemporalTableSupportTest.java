/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.temporal.spi.TemporalRestrictionRequest;
import org.hibernate.dialect.temporal.spi.TemporalTableAuxiliaryObject;
import org.hibernate.dialect.temporal.spi.TemporalTableDdlRequest;
import org.hibernate.dialect.temporal.spi.TemporalTableSupport;
import org.hibernate.dialect.temporal.spi.TemporalTableSupports;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.mapping.Table;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.type.SqlTypes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.dialect.temporal.spi.TemporalTableAuxiliaryObject.Scope.DATABASE;
import static org.hibernate.dialect.temporal.spi.TemporalTableAuxiliaryObject.Scope.TABLE;
import static org.hibernate.temporal.TemporalTableStrategy.HISTORY_TABLE;
import static org.hibernate.temporal.TemporalTableStrategy.NATIVE;
import static org.hibernate.temporal.TemporalTableStrategy.SINGLE_TABLE;

/// Verifies the supported temporal-table contract, stock profiles, and every
/// maintained Dialect supply point.
///
/// @author Steve Ebersole
public class TemporalTableSupportTest {
	private static final Set<Class<?>> FORBIDDEN_SIGNATURE_TYPES = Set.of(
			Table.class,
			Database.class,
			Metadata.class,
			InFlightMetadataCollector.class,
			SqlStringGenerationContext.class,
			LoadQueryInfluencers.class,
			Identifier.class
	);

	@Test
	void requestsValidateAndNormalizeRenderedNames() {
		final TemporalTableDdlRequest request = new TemporalTableDdlRequest(
				HISTORY_TABLE,
				"quoted.orders",
				"valid_from",
				"valid_to",
				false,
				"ignored_current",
				"ignored_history"
		);
		assertThat( request.currentPartitionName() ).isNull();
		assertThat( request.historyPartitionName() ).isNull();

		assertThatThrownBy( () -> ddlRequest( null, false ) ).isInstanceOf( IllegalArgumentException.class );
		assertThatThrownBy( () -> new TemporalTableDdlRequest(
				HISTORY_TABLE, " ", "valid_from", "valid_to", false, null, null
		) ).isInstanceOf( IllegalArgumentException.class );
		assertThatThrownBy( () -> new TemporalTableDdlRequest(
				HISTORY_TABLE, "orders", "valid_from", "valid_to", true, null, "orders_history"
		) ).isInstanceOf( IllegalArgumentException.class );
		assertThatThrownBy( () -> new TemporalRestrictionRequest( null, false, false ) )
				.isInstanceOf( IllegalArgumentException.class );
	}

	@Test
	void auxiliaryDescriptorsValidateAndSnapshotTheirCommands() {
		final List<String> creates = new ArrayList<>( List.of( "create one" ) );
		final List<String> drops = new ArrayList<>( List.of( "drop one" ) );
		final TemporalTableAuxiliaryObject object = new TemporalTableAuxiliaryObject(
				"temporal-audit", TABLE, false, creates, drops
		);
		creates.add( "create two" );
		drops.clear();
		assertThat( object.createCommands() ).containsExactly( "create one" );
		assertThat( object.dropCommands() ).containsExactly( "drop one" );
		assertThatThrownBy( () -> object.createCommands().add( "create two" ) )
				.isInstanceOf( UnsupportedOperationException.class );
		assertThatThrownBy( () -> new TemporalTableAuxiliaryObject(
				" ", TABLE, false, List.of( "create" ), List.of()
		) ).isInstanceOf( IllegalArgumentException.class );
		assertThatThrownBy( () -> new TemporalTableAuxiliaryObject(
				"empty", DATABASE, false, List.of(), List.of()
		) ).isInstanceOf( IllegalArgumentException.class );
	}

	@Test
	void standardProfileDefinesEveryOperation() {
		final TemporalTableSupport support = TemporalTableSupports.standard( SqlTypes.TIMESTAMP, 9, true );
		final TemporalTableDdlRequest request = ddlRequest( HISTORY_TABLE, false );

		assertThat( support.supportsNativeTemporalTables() ).isFalse();
		assertThat( support.getTemporalColumnType() ).isEqualTo( SqlTypes.TIMESTAMP );
		assertThat( support.getTemporalColumnPrecision() ).isEqualTo( 9 );
		assertThat( support.getTemporalTableOptions( request ) ).isNull();
		assertThat( support.suppressesTemporalTablePrimaryKeys( false ) ).isFalse();
		assertThat( support.suppressesTemporalTablePrimaryKeys( true ) ).isFalse();
		assertThat( support.supportsTemporalTablePartitioning() ).isFalse();
		assertThat( support.getTemporalTableAuxiliaryObjects( request ) ).isEmpty();
		assertThat( support.getExtraTemporalTableDeclarations( request ) ).isNull();
		assertThat( support.createTemporalTableCheckConstraint( HISTORY_TABLE ) ).isTrue();
		assertThat( support.createTemporalTableCheckConstraint( NATIVE ) ).isFalse();
		assertThat( support.getAsOfOperator( NATIVE ) ).isEqualTo( "for system_time as of" );
		assertThat( support.useAsOfOperator( NATIVE ) ).isTrue();
		assertThat( support.useAsOfOperator( HISTORY_TABLE ) ).isFalse();
		assertThat( support.useAsOfOperatorForCurrent( NATIVE ) ).isFalse();
		assertThat( support.useTemporalRestriction( restriction( SINGLE_TABLE, false, false ) ) ).isTrue();
		assertThat( support.useTemporalRestriction( restriction( HISTORY_TABLE, false, false ) ) ).isFalse();
		assertThat( support.useTemporalRestriction( restriction( HISTORY_TABLE, true, false ) ) ).isTrue();
		assertThat( support.useTemporalRestriction( restriction( NATIVE, true, true ) ) ).isFalse();
		assertThatThrownBy( support::getTemporalExclusionColumnOption )
				.isInstanceOf( MappingException.class );
		assertThat( support.getDefaultTemporalTableStrategy() ).isEqualTo( HISTORY_TABLE );

		assertThat( TemporalTableSupports.standard( SqlTypes.TIMESTAMP, 9, false )
				.createTemporalTableCheckConstraint( HISTORY_TABLE ) ).isFalse();
	}

	@Test
	void mysqlProfilePreservesPartitionGrammar() {
		final TemporalTableSupport support = TemporalTableSupports.mysql( 6, true );
		final TemporalTableDdlRequest request = ddlRequest( HISTORY_TABLE, true );

		assertThat( support.getTemporalColumnType() ).isEqualTo( SqlTypes.TIMESTAMP_UTC );
		assertThat( support.getTemporalColumnPrecision() ).isEqualTo( 6 );
		assertThat( support.supportsTemporalTablePartitioning() ).isTrue();
		assertThat( support.suppressesTemporalTablePrimaryKeys( true ) ).isTrue();
		assertThat( support.getTemporalTableOptions( request ) ).isEqualTo(
				"partition by list (valid_to_null) (partition orders_history values in (0),"
						+ " partition orders_current values in (1))"
		);
		assertThat( support.getExtraTemporalTableDeclarations( request ) ).isEqualTo(
				"valid_to_null tinyint as (valid_to is null) virtual invisible"
		);
	}

	@Test
	void maintainedStrategiesPreserveVendorGrammarAndDescriptorScope() {
		final TemporalTableDdlRequest nativeRequest = ddlRequest( NATIVE, false );
		final TemporalTableDdlRequest partitionedRequest = ddlRequest( HISTORY_TABLE, true );

		final TemporalTableSupport db2 = new DB2Dialect().getTemporalTableSupport();
		assertThat( db2.supportsNativeTemporalTables() ).isTrue();
		assertThat( db2.getTemporalColumnPrecision() ).isEqualTo( 12 );
		assertThat( db2.getDefaultTemporalTableStrategy() ).isEqualTo( NATIVE );
		assertThat( db2.getExtraTemporalTableDeclarations( nativeRequest ) ).isEqualTo(
				"transaction_start_id timestamp(12) not null generated always as transaction start id implicitly hidden,"
						+ " period system_time (valid_from, valid_to)"
		);
		assertThat( db2.getTemporalTableAuxiliaryObjects( nativeRequest ) ).singleElement().satisfies( object -> {
			assertThat( object.exportIdentifier() ).isEqualTo( "orders_history" );
			assertThat( object.scope() ).isEqualTo( TABLE );
			assertThat( object.createCommands() ).containsExactly(
					"create table orders_history like orders",
					"alter table orders add versioning use history table orders_history"
			);
			assertThat( object.dropCommands() ).containsExactly( "drop table orders_history" );
		} );

		final TemporalTableSupport mariaDB = new MariaDBDialect().getTemporalTableSupport();
		assertThat( mariaDB.supportsNativeTemporalTables() ).isTrue();
		assertThat( mariaDB.supportsTemporalTablePartitioning() ).isFalse();
		assertThat( mariaDB.getTemporalColumnType() ).isEqualTo( SqlTypes.TIMESTAMP_WITH_TIMEZONE );
		assertThat( mariaDB.getTemporalTableOptions( nativeRequest ) ).isEqualTo( "with system versioning" );
		assertThat( mariaDB.getExtraTemporalTableDeclarations( nativeRequest ) )
				.isEqualTo( "period for system_time (valid_from, valid_to)" );
		assertThat( mariaDB.getTemporalExclusionColumnOption() ).isEqualTo( "without system versioning" );
		assertThat( mariaDB.getDefaultTemporalTableStrategy() ).isEqualTo( NATIVE );

		final TemporalTableSupport postgreSQL = new PostgreSQLDialect().getTemporalTableSupport();
		assertThat( postgreSQL.getTemporalColumnType() ).isEqualTo( SqlTypes.TIMESTAMP_UTC );
		assertThat( postgreSQL.getTemporalTableOptions( partitionedRequest ) )
				.isEqualTo( "partition by list (valid_to)" );
		assertThat( postgreSQL.getTemporalTableAuxiliaryObjects( partitionedRequest ) )
				.extracting( TemporalTableAuxiliaryObject::exportIdentifier )
				.containsExactly( "orders_current", "orders_history" );
		assertThat( postgreSQL.getTemporalTableAuxiliaryObjects( partitionedRequest ) )
				.allSatisfy( object -> assertThat( object.scope() ).isEqualTo( TABLE ) );

		final TemporalTableSupport sqlServer = new SQLServerDialect( DatabaseVersion.make( 16 ) )
				.getTemporalTableSupport();
		assertThat( sqlServer.supportsNativeTemporalTables() ).isTrue();
		assertThat( sqlServer.getExtraTemporalTableDeclarations( nativeRequest ) ).isEqualTo(
				"transaction_start_id bigint generated always as transaction_id start hidden not null, "
						+ "period for system_time (valid_from, valid_to)"
		);
		assertThat( sqlServer.getTemporalTableOptions( nativeRequest ) )
				.isEqualTo( "with (system_versioning = on)" );
		assertThat( sqlServer.getTemporalTableAuxiliaryObjects( nativeRequest ) ).singleElement().satisfies( object -> {
			assertThat( object.scope() ).isEqualTo( TABLE );
			assertThat( object.dropCommands() )
					.containsExactly( "alter table orders set (system_versioning = off)" );
		} );
	}

	@Test
	void oraclePreservesNativeAndHistoryQueryChoicesAndDescriptorOrdering() {
		final TemporalTableSupport oracle = new OracleDialect().getTemporalTableSupport();
		final TemporalTableDdlRequest nativeRequest = ddlRequest( NATIVE, false );

		assertThat( oracle.supportsNativeTemporalTables() ).isTrue();
		assertThat( oracle.supportsTemporalTablePartitioning() ).isTrue();
		assertThat( oracle.suppressesTemporalTablePrimaryKeys( true ) ).isFalse();
		assertThat( oracle.createTemporalTableCheckConstraint( HISTORY_TABLE ) ).isFalse();
		assertThat( oracle.getExtraTemporalTableDeclarations( nativeRequest ) )
				.isEqualTo( "period for system_time (valid_from, valid_to)" );
		assertThat( oracle.getTemporalTableOptions( nativeRequest ) ).isEqualTo( "flashback archive fba_history" );
		assertThat( oracle.getAsOfOperator( NATIVE ) ).isEqualTo( "as of timestamp" );
		assertThat( oracle.getAsOfOperator( SINGLE_TABLE ) ).isEqualTo( "as of period for system_time" );
		assertThat( oracle.useAsOfOperator( HISTORY_TABLE ) ).isFalse();
		assertThat( oracle.useAsOfOperator( NATIVE ) ).isTrue();
		assertThat( oracle.useAsOfOperatorForCurrent( SINGLE_TABLE ) ).isTrue();
		assertThat( oracle.useTemporalRestriction( restriction( SINGLE_TABLE, false, true ) ) ).isFalse();
		assertThat( oracle.useTemporalRestriction( restriction( HISTORY_TABLE, true, true ) ) ).isTrue();
		assertThat( oracle.useTemporalRestriction( restriction( HISTORY_TABLE, false, true ) ) ).isFalse();
		assertThat( oracle.useTemporalRestriction( restriction( SINGLE_TABLE, false, false ) ) ).isTrue();

		assertThat( oracle.getTemporalTableAuxiliaryObjects( nativeRequest ) )
				.extracting(
						TemporalTableAuxiliaryObject::exportIdentifier,
						TemporalTableAuxiliaryObject::scope,
						TemporalTableAuxiliaryObject::beforeTables
				)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple( "disable-flashback-archive", TABLE, false ),
						org.assertj.core.groups.Tuple.tuple( "fba_history", DATABASE, true )
				);
	}

	@Test
	void supportedSignaturesExcludeInternalImplementationTypes() {
		for ( Class<?> type : List.of(
				TemporalTableSupport.class,
				TemporalTableDdlRequest.class,
				TemporalRestrictionRequest.class,
				TemporalTableAuxiliaryObject.class
		) ) {
			for ( Method method : type.getDeclaredMethods() ) {
				assertSupportedType( method.getAnnotatedReturnType(), type, method );
				for ( AnnotatedType parameterType : method.getAnnotatedParameterTypes() ) {
					assertSupportedType( parameterType, type, method );
				}
			}
		}
	}
	private static void assertSupportedType(AnnotatedType type, Class<?> owner, Method method) {
		assertThat( FORBIDDEN_SIGNATURE_TYPES )
				.as( "%s#%s must not expose %s", owner.getName(), method.getName(), type.getType().getTypeName() )
				.doesNotContain( rawType( type ) );
		if ( type instanceof AnnotatedParameterizedType parameterizedType ) {
			for ( AnnotatedType argument : parameterizedType.getAnnotatedActualTypeArguments() ) {
				assertSupportedType( argument, owner, method );
			}
		}
	}

	private static Class<?> rawType(AnnotatedType type) {
		return type.getType() instanceof Class<?> javaType ? javaType : null;
	}

	private static TemporalTableDdlRequest ddlRequest(TemporalTableStrategy strategy, boolean partitioned) {
		return new TemporalTableDdlRequest(
				strategy,
				"orders",
				"valid_from",
				"valid_to",
				partitioned,
				partitioned ? "orders_current" : null,
				partitioned ? "orders_history" : null
		);
	}

	private static TemporalRestrictionRequest restriction(
			TemporalTableStrategy strategy,
			boolean temporalIdentifierPresent,
			boolean instantChangesetIdentifier) {
		return new TemporalRestrictionRequest(
				strategy,
				temporalIdentifierPresent,
				instantChangesetIdentifier
		);
	}
}
