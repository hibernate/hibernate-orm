/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jdbc.internal;

import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.JdbcStatisticsConnectionProvider;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class SessionJdbcBatchTest
		extends BaseNonConfigCoreFunctionalTestCase {

	private JdbcStatisticsConnectionProvider connectionProvider = new JdbcStatisticsConnectionProvider();

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
	protected boolean rebuildSessionFactoryOnError() {
		return false;
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	private long id;

	@Test
	public void testSessionFactorySetting() {
		Session session = sessionFactory().openSession();
		session.beginTransaction();
		try {
			addEvents( session );
		}
		finally {
			connectionProvider.getPreparedStatementStatistics().clear();
			session.getTransaction().commit();
			session.close();
		}
		JdbcStatisticsConnectionProvider.PreparedStatementStatistics statementStatistics =
				connectionProvider.getPreparedStatementStatistics().get(
						"insert into Event (name, id) values (?, ?)" );
		assertEquals( 5, statementStatistics.getAddBatchCount() );
		assertEquals( 3, statementStatistics.getExecuteBatchCount() );
	}

	@Test
	public void testSessionSettingOverridesSessionFactorySetting() {
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
		JdbcStatisticsConnectionProvider.PreparedStatementStatistics statementStatistics =
				connectionProvider.getPreparedStatementStatistics().get(
						"insert into Event (name, id) values (?, ?)" );
		assertEquals( 5, statementStatistics.getAddBatchCount() );
		assertEquals( 2, statementStatistics.getExecuteBatchCount() );

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
		statementStatistics =
				connectionProvider.getPreparedStatementStatistics().get(
						"insert into Event (name, id) values (?, ?)" );
		assertEquals( 5, statementStatistics.getAddBatchCount() );
		assertEquals( 3, statementStatistics.getExecuteBatchCount() );
	}

	private void addEvents(Session session) {
		for ( long i = 0; i < 5; i++ ) {
			Event event = new Event();
			event.id = id++;
			event.name = "Event " + i;
			session.persist( event );
		}
	}

	@Test
	public void testSessionJdbcBatchOverridesSessionFactorySetting() {

		Session session = sessionFactory().openSession();
		session.beginTransaction();
		try {

		}
		finally {
			session.getTransaction().commit();
			session.close();
		}
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		private Long id;

		private String name;
	}
}
