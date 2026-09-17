/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tenantid;

import java.sql.PreparedStatement;
import java.util.ArrayList;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import org.hibernate.annotations.SecondaryRow;
import org.hibernate.annotations.TenantId;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.BatchSettings.STATEMENT_BATCH_SIZE;
import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.JdbcSettings.DIALECT_NATIVE_PARAM_MARKERS;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;
import static org.hibernate.orm.test.tenantid.TenantIdMutationMappingTest.inTenant;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ServiceRegistryFunctionalTesting
@DomainModel(annotatedClasses = TenantIdOwnershipBatchingTest.Item.class)
@SessionFactory(useCollectingStatementInspector = true)
class TenantIdOwnershipBatchingTest implements ServiceRegistryProducer {
	private final PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		final var configuredProvider = builder.getSettings().get( CONNECTION_PROVIDER );
		if ( configuredProvider != null ) {
			connectionProvider.setConnectionProvider( (ConnectionProvider) configuredProvider );
		}
		return builder.applySetting( CONNECTION_PROVIDER, connectionProvider )
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

	@Test
	void completedOwnershipChecksDoNotBreakLaterBatches(SessionFactoryScope scope) throws Exception {
		inTenant( scope, "mine", session -> {
			for ( long id = 1; id <= 3; id++ ) {
				final var item = new Item();
				item.id = id;
				session.persist( item );
			}
		} );
		inTenant( scope, "mine", session -> {
			final var items = new ArrayList<Item>();
			for ( long id = 1; id <= 3; id++ ) {
				items.add( session.find( Item.class, id ) );
			}
			connectionProvider.clear();
			scope.getCollectingStatementInspector().clear();
			items.forEach( item -> {
				item.first = "changed";
				item.second = "changed";
			} );
			session.flush();
		} );
		assertEquals( 3, scope.getCollectingStatementInspector().getSqlQueries().stream()
				.filter( sql -> sql.startsWith( "select " ) ).count() );
		int additions = 0;
		int batches = 0;
		for ( var entry : connectionProvider.getPreparedStatementsAndSql().entrySet() ) {
			if ( entry.getValue().startsWith( "update ownership_batch_" ) ) {
				additions += connectionProvider.spyContext.getCalls( PreparedStatement.class.getMethod( "addBatch" ), entry.getKey() ).size();
				batches += connectionProvider.spyContext.getCalls( PreparedStatement.class.getMethod( "executeBatch" ), entry.getKey() ).size();
			}
		}
		assertEquals( 6, additions );
		// First table: three batches separated by ownership queries. Second table: one batch.
		assertEquals( 4, batches );
		inTenant( scope, "mine", session -> {
			for ( long id = 1; id <= 3; id++ ) {
				final var item = session.find( Item.class, id );
				assertEquals( "changed", item.first );
				assertEquals( "changed", item.second );
			}
		} );
	}

	@Entity(name = "OwnershipBatchItem")
	@Table(name = "ownership_batch_item")
	@SecondaryTable(name = "ownership_batch_first")
	@SecondaryTable(name = "ownership_batch_second")
	@SecondaryRow(table = "ownership_batch_first", optional = false)
	@SecondaryRow(table = "ownership_batch_second", optional = false)
	static class Item {
		@Id Long id;
		@TenantId String tenant;
		@Column(name = "first_payload", table = "ownership_batch_first") String first = "original";
		@Column(name = "second_value", table = "ownership_batch_second") String second = "original";
	}
}
