/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.Session;
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

import java.lang.reflect.Method;
import java.sql.PreparedStatement;

import static org.hibernate.cfg.BatchSettings.STATEMENT_BATCH_SIZE;
import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.JdbcSettings.DIALECT_NATIVE_PARAM_MARKERS;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistryFunctionalTesting
@DomainModel(annotatedClasses = SessionJdbcBatchTest.Event.class)
@SessionFactory
public class SessionJdbcBatchTest implements ServiceRegistryProducer {
	private final PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();
	private final Method addBatch;
	private final Method executeBatch;

	public SessionJdbcBatchTest() throws Exception {
		addBatch = PreparedStatement.class.getMethod( "addBatch" );
		executeBatch = PreparedStatement.class.getMethod( "executeBatch" );
	}

	private void addEvents(Session session) {
		for ( long i = 0; i < 5; i++ ) {
			Event event = new Event( Event.MAX++, "Event " + Event.MAX );
			session.persist( event );
		}
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
		connectionProvider.stop();
	}

	@Test
	public void testSessionFactorySetting(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			connectionProvider.clear();
			addEvents( session );
		} );

		var preparedStatement = connectionProvider.getPreparedStatement("insert into Event (name,id) values (?,?)" );
		var addBatchCalls = connectionProvider.spyContext.getCalls( addBatch, preparedStatement );
		var executeBatchCalls = connectionProvider.spyContext.getCalls( executeBatch, preparedStatement );
		assertEquals( 5, addBatchCalls.size() );
		assertEquals( 3, executeBatchCalls.size() );
	}

	@Test
	public void testSessionSettingOverridesSessionFactorySetting(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			connectionProvider.clear();
			session.setJdbcBatchSize( 3 );
			addEvents( session );

		} );

		var preparedStatement = connectionProvider.getPreparedStatement( "insert into Event (name,id) values (?,?)" );
		var addBatchCalls = connectionProvider.spyContext.getCalls( addBatch, preparedStatement );
		var executeBatchCalls = connectionProvider.spyContext.getCalls(  executeBatch, preparedStatement );
		assertEquals( 5, addBatchCalls.size() );
		assertEquals( 2, executeBatchCalls.size() );

		factoryScope.inTransaction( (session) -> {
			connectionProvider.clear();
			session.setJdbcBatchSize( null );
			addEvents( session );
		} );

		var preparedStatements = connectionProvider.getPreparedStatements();
		assertEquals( 1, preparedStatements.size() );
		preparedStatement = preparedStatements.get( 0 );
		addBatchCalls = connectionProvider.spyContext.getCalls( addBatch, preparedStatement );
		executeBatchCalls = connectionProvider.spyContext.getCalls(  executeBatch, preparedStatement );
		assertEquals( 5, addBatchCalls.size() );
		assertEquals( 3, executeBatchCalls.size() );
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder registryBuilder) {
		registryBuilder.applySetting( STATEMENT_BATCH_SIZE, 2 );
		registryBuilder.applySetting( DIALECT_NATIVE_PARAM_MARKERS, Boolean.FALSE );
		final Object configuredConnProvider = registryBuilder.getSettings().get( CONNECTION_PROVIDER );
		if ( configuredConnProvider != null ) {
			connectionProvider.setConnectionProvider( (ConnectionProvider) configuredConnProvider );
		}
		registryBuilder.applySetting( CONNECTION_PROVIDER, connectionProvider );
		return registryBuilder.build();
	}

	@Entity(name = "Event")
	public static class Event {
		static int MAX = 0;

		@Id
		private Integer id;
		private String name;

		public Event() {
		}

		public Event(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
