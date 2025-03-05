/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc.internal;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class SessionJdbcBatchTest
		extends BaseNonConfigCoreFunctionalTestCase {

	private PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Event.class };
	}

	@Override
	protected void addSettings(Map<String,Object> settings) {
		settings.put( AvailableSettings.STATEMENT_BATCH_SIZE, 2 );
		settings.put( AvailableSettings.DIALECT_NATIVE_PARAM_MARKERS, Boolean.FALSE );
		if ( settings.containsKey( AvailableSettings.CONNECTION_PROVIDER ) ) {
			connectionProvider.setConnectionProvider( (ConnectionProvider) settings.get( AvailableSettings.CONNECTION_PROVIDER ) );
		}
		settings.put(
				AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
	}

	@Override
	protected void releaseResources() {
		super.releaseResources();
		connectionProvider.stop();
	}

	@Override
	protected boolean rebuildSessionFactoryOnError() {
		return false;
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Override
	protected void cleanupTestData() throws Exception {
		inTransaction(
				(session) -> session.createQuery( "delete Event" ).executeUpdate()
		);
	}

	private long id;

	@Test
	public void testSessionFactorySetting() throws Throwable {
		Session session = sessionFactory().openSession();
		session.beginTransaction();
		try {
			addEvents( session );
		}
		finally {
			connectionProvider.clear();
			session.getTransaction().commit();
			session.close();
		}
		PreparedStatement preparedStatement = connectionProvider.getPreparedStatement(
				"insert into Event (name,id) values (?,?)" );
		List<Object[]> addBatchCalls = connectionProvider.spyContext.getCalls(
				PreparedStatement.class.getMethod( "addBatch" ),
				preparedStatement
		);
		List<Object[]> executeBatchCalls = connectionProvider.spyContext.getCalls(
				PreparedStatement.class.getMethod( "executeBatch" ),
				preparedStatement
		);
		assertEquals( 5, addBatchCalls.size() );
		assertEquals( 3, executeBatchCalls.size() );
	}

	@Test
	public void testSessionSettingOverridesSessionFactorySetting()
			throws Throwable {
		Session session = sessionFactory().openSession();
		session.setJdbcBatchSize( 3 );
		session.beginTransaction();
		try {
			addEvents( session );
		}
		finally {
			connectionProvider.clear();
			session.getTransaction().commit();
			session.close();
		}

		PreparedStatement preparedStatement = connectionProvider.getPreparedStatement( "insert into Event (name,id) values (?,?)" );
		List<Object[]> addBatchCalls = connectionProvider.spyContext.getCalls(
				PreparedStatement.class.getMethod( "addBatch" ),
				preparedStatement
		);
		List<Object[]> executeBatchCalls = connectionProvider.spyContext.getCalls(
				PreparedStatement.class.getMethod( "executeBatch" ),
				preparedStatement
		);
		assertEquals( 5, addBatchCalls.size() );
		assertEquals( 2, executeBatchCalls.size() );

		session = sessionFactory().openSession();
		session.setJdbcBatchSize( null );
		session.beginTransaction();
		try {
			addEvents( session );
		}
		finally {
			connectionProvider.clear();
			session.getTransaction().commit();
			session.close();
		}
		List<PreparedStatement> preparedStatements = connectionProvider.getPreparedStatements();
		assertEquals(1, preparedStatements.size());
		preparedStatement = preparedStatements.get( 0 );
		addBatchCalls = connectionProvider.spyContext.getCalls(
				PreparedStatement.class.getMethod( "addBatch" ),
				preparedStatement
		);
		executeBatchCalls = connectionProvider.spyContext.getCalls(
				PreparedStatement.class.getMethod( "executeBatch" ),
				preparedStatement
		);
		assertEquals( 5, addBatchCalls.size() );
		assertEquals( 3, executeBatchCalls.size() );
	}

	private void addEvents(Session session) {
		for ( long i = 0; i < 5; i++ ) {
			Event event = new Event();
			event.id = id++;
			event.name = "Event " + i;
			session.persist( event );
		}
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		private Long id;

		private String name;
	}
}
