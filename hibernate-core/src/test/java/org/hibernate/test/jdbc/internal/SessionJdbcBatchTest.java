/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jdbc.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
	protected void addSettings(Map settings) {
		settings.put( AvailableSettings.STATEMENT_BATCH_SIZE, 2 );
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

	private long id;

	@Test
	public void testSessionFactorySetting() throws SQLException {
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
				"insert into Event (name, id) values (?, ?)" );
		verify( preparedStatement, times( 5 ) ).addBatch();
		verify( preparedStatement, times( 3 ) ).executeBatch();
	}

	@Test
	public void testSessionSettingOverridesSessionFactorySetting()
			throws SQLException {
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

		PreparedStatement preparedStatement = connectionProvider.getPreparedStatement( "insert into Event (name, id) values (?, ?)" );
		verify(preparedStatement, times( 5 )).addBatch();
		verify(preparedStatement, times( 2 )).executeBatch();

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
		verify(preparedStatement, times( 5 )).addBatch();
		verify(preparedStatement, times( 3 )).executeBatch();
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
