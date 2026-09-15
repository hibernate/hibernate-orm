/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tenantid;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.stream.Stream;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.TenantId;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.sql.spi.mutation.SelfExecutingUpdateOperation;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hibernate.cfg.BatchSettings.STATEMENT_BATCH_SIZE;
import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;
import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.JdbcSettings.DIALECT_NATIVE_PARAM_MARKERS;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;
import static org.hibernate.orm.test.tenantid.TenantIdMutationMappingTest.inTenant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ServiceRegistryFunctionalTesting
@DomainModel(annotatedClasses = { TenantIdSecondaryTableBatchingTest.Plain.class, TenantIdSecondaryTableBatchingTest.Tenant.class })
@SessionFactory(useCollectingStatementInspector = true)
class TenantIdSecondaryTableBatchingTest implements ServiceRegistryProducer {
	private final PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		final var configuredProvider = builder.getSettings().get( CONNECTION_PROVIDER );
		if ( configuredProvider != null ) {
			connectionProvider.setConnectionProvider( (ConnectionProvider) configuredProvider );
		}
		return builder.applySetting( CONNECTION_PROVIDER, connectionProvider )
				.applySetting( FLUSH_QUEUE_TYPE, "legacy" )
				.applySetting( STATEMENT_BATCH_SIZE, 5 )
				.applySetting( DIALECT_NATIVE_PARAM_MARKERS, false )
				.applySetting( MULTI_TENANT_IDENTIFIER_RESOLVER, new TenantIdMutationTest.Resolver() )
				.applySetting( MULTI_TENANT_RLS_ENABLED, false )
				.build();
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		connectionProvider.clear();
	}

	static Stream<Arguments> rowChanges() {
		return Stream.of( Arguments.of( null, "changed" ), Arguments.of( "original", "changed" ), Arguments.of( "original", null ) );
	}

	static Stream<Arguments> mutations() {
		return Stream.of( Plain.class, Tenant.class ).flatMap( type ->
				(type == Plain.class ? Stream.of( "mine" ) : Stream.of( "mine", "root" )).flatMap( tenant ->
						Stream.of( false, true ).flatMap( stateless -> rowChanges().map( change ->
								Arguments.of( type, tenant, stateless, change.get()[0], change.get()[1] ) ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("mutations")
	void batchesUnlessUpdateMustCheckOwner(
			Class<? extends Item> type, String tenant, boolean stateless, String initial, String changed,
			SessionFactoryScope scope) throws Exception {
		final var persister = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( type );
		// Dynamic insert selects the self-executing optional-table fallback on every dialect.
		assertInstanceOf( SelfExecutingUpdateOperation.class, persister.getUpdateCoordinator().getStaticMutationOperationGroup()
				.getOperation( type.getAnnotation( SecondaryTable.class ).name() ) );
		final var items = new ArrayList<Item>();
		inTenant( scope, "mine", session -> {
			for ( long id = 1; id <= 3; id++ ) {
				final Item item = type == Plain.class ? new Plain() : new Tenant();
				item.id = id;
				item.detail( initial );
				items.add( item );
				session.persist( item );
			}
		} );
		if ( stateless ) {
			try ( var session = scope.getSessionFactory().withStatelessOptions().tenantIdentifier( tenant ).openStatelessSession() ) {
				final var transaction = session.beginTransaction();
				connectionProvider.clear();
				items.forEach( item -> item.detail( changed ) );
				session.updateMultiple( items );
				transaction.commit();
			}
		}
		else {
			inTenant( scope, tenant, session -> {
				final var managed = items.stream().map( item -> session.find( type, item.id ) ).toList();
				connectionProvider.clear();
				final var inspector = scope.getCollectingStatementInspector();
				inspector.clear();
				managed.forEach( item -> item.detail( changed ) );
				session.flush();
				assertTrue( inspector.getSqlQueries().stream().noneMatch( sql -> sql.startsWith( "select " ) ) );
			} );
		}
		final boolean checksOwner = type == Tenant.class && tenant.equals( "mine" ) && !stateless;
		int additions = 0;
		int batches = 0;
		int updates = 0;
		for ( var entry : connectionProvider.getPreparedStatementsAndSql().entrySet() ) {
			if ( entry.getValue().startsWith( "update " + type.getAnnotation( Table.class ).name() + " set " ) ) {
				additions += connectionProvider.spyContext.getCalls( PreparedStatement.class.getMethod( "addBatch" ), entry.getKey() ).size();
				batches += connectionProvider.spyContext.getCalls( PreparedStatement.class.getMethod( "executeBatch" ), entry.getKey() ).size();
				updates += connectionProvider.spyContext.getCalls( PreparedStatement.class.getMethod( "executeUpdate" ), entry.getKey() ).size();
			}
		}
		assertEquals( checksOwner ? 0 : 3, additions );
		int expectedBatches = checksOwner ? 0 : 1;
		if ( stateless && type == Tenant.class && tenant.equals( "mine" ) ) {
			// Each existing stateless ownership select flushes the preceding batch.
			expectedBatches = 3;
		}
		assertEquals( expectedBatches, batches );
		assertEquals( checksOwner ? 3 : 0, updates );
		inTenant( scope, "mine", session -> {
			for ( var item : items ) {
				final var stored = session.find( type, item.id );
				assertEquals( changed, stored.detail() );
				assertEquals( 1, stored.version );
			}
		} );
	}

	@ParameterizedTest
	@MethodSource("rowChanges")
	void rejectsForeignOwnerBeforeOptionalMutation(String initial, String changed, SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> {
			final var item = new Tenant();
			item.id = 1L;
			item.detail( initial );
			session.persist( item );
		} );
		try ( var session = scope.getSessionFactory().withOptions().tenantIdentifier( "mine" ).openSession() ) {
			session.beginTransaction();
			final var item = session.find( Tenant.class, 1L );
			session.getTransaction().commit();
			inTenant( scope, "root", other -> other.createNativeMutationQuery(
					"update batching_tenant set tenant='yours' where id=1" ).executeUpdate() );
			final var transaction = session.beginTransaction();
			try {
				item.detail( changed );
				assertThrows( PersistenceException.class, session::flush );
				// Check before rollback so it cannot hide an unauthorized optional-table mutation.
				session.doWork( connection -> {
					try ( var statement = connection.createStatement();
						var rows = statement.executeQuery( "select detail from batching_tenant_detail where id=1" ) ) {
						if ( initial == null ) {
							assertFalse( rows.next() );
						}
						else {
							assertTrue( rows.next() );
							assertEquals( initial, rows.getString( 1 ) );
						}
					}
				} );
			}
			finally {
				transaction.rollback();
			}
		}
	}

	@MappedSuperclass
	abstract static class Item {
		@Id Long id;
		@Version int version;
		abstract String detail();
		abstract void detail(String detail);
	}

	@Entity(name = "BatchingPlain")
	@Table(name = "batching_plain")
	@SecondaryTable(name = "batching_plain_detail")
	@DynamicInsert
	static class Plain extends Item {
		@Column(table = "batching_plain_detail") String detail;
		@Override String detail() { return detail; }
		@Override void detail(String detail) { this.detail = detail; }
	}

	@Entity(name = "BatchingTenant")
	@Table(name = "batching_tenant")
	@SecondaryTable(name = "batching_tenant_detail")
	@DynamicInsert
	static class Tenant extends Item {
		@TenantId String tenant;
		@Column(table = "batching_tenant_detail") String detail;
		@Override String detail() { return detail; }
		@Override void detail(String detail) { this.detail = detail; }
	}
}
