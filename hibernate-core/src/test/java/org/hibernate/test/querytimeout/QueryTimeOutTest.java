/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.querytimeout;

import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.jdbc.BasicPreparedStatementObserver;
import org.hibernate.testing.jdbc.PreparedStatementProxyConnectionProvider;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Gail Badner
 */
public class QueryTimeOutTest extends BaseNonConfigCoreFunctionalTestCase {

	private static final TimeoutPreparedStatementObserver preparedStatementObserver = new TimeoutPreparedStatementObserver();
	private static final PreparedStatementProxyConnectionProvider connectionProvider = new PreparedStatementProxyConnectionProvider(
			preparedStatementObserver
	);

	private static final String QUERY = "update AnEntity set name='abc'";

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { AnEntity.class };
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( AvailableSettings.CONNECTION_PROVIDER, connectionProvider );
	}

	@Before
	public void before() {
		preparedStatementObserver.clear();
	}

	@Override
	public void releaseResources() {
		super.releaseResources();
		connectionProvider.stop();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12075")
	public void testCreateQuerySetTimeout() {
		Session session = openSession();
		session.getTransaction().begin();
		{
					Query query = session.createQuery( QUERY );
					query.setTimeout( 123 );
					query.executeUpdate();

					PreparedStatement preparedStatement = preparedStatementObserver.getPreparedStatement( QUERY );
					assertEquals( 123, preparedStatementObserver.getTimeOut( preparedStatement ) );
		}
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12075")
	public void testCreateSQLQuerySetTimeout() {
		Session session = openSession();
		session.getTransaction().begin();
		{
			SQLQuery query = session.createSQLQuery( QUERY );
			query.setTimeout( 123 );
			query.executeUpdate();

			PreparedStatement preparedStatement = preparedStatementObserver.getPreparedStatement( QUERY );
			assertEquals( 123, preparedStatementObserver.getTimeOut( preparedStatement ) );
		}
		session.getTransaction().commit();
		session.close();
	}

	@Entity(name = "AnEntity" )
	@Table(name = "AnEntity" )
	public static class AnEntity {
		@Id
		private int id;

		private String name;
	}

	private static class TimeoutPreparedStatementObserver extends BasicPreparedStatementObserver {
		private final Map<PreparedStatement, Integer> timeoutByPreparedStatement =
				new HashMap<PreparedStatement, Integer>();

		@Override
		public void preparedStatementMethodInvoked(
				PreparedStatement preparedStatement,
				Method method,
				Object[] args,
				Object invocationReturnValue) {
			super.preparedStatementMethodInvoked( preparedStatement, method, args, invocationReturnValue );
			if ( "setQueryTimeout".equals( method.getName() ) ) {
				// ugh, when ResourceRegistryStandardImpl closes the PreparedStatement, it calls
				// PreparedStatement#setQueryTimeout( 0 ). Ignore this call if the PreparedStatement
				// is already in timeoutByPreparedStatement
				Integer timeout = (Integer) args[0];
				Integer existingTimeout = timeoutByPreparedStatement.get( preparedStatement );
				if ( timeout == 0 && existingTimeout != null && existingTimeout != 0  ) {
					// ignore;
					return;
				}
				timeoutByPreparedStatement.put( preparedStatement, timeout );
			}
		}

		public int getTimeOut(PreparedStatement preparedStatement) {
			return timeoutByPreparedStatement.get( preparedStatement );
		}

		@Override
		public void connectionProviderStopped() {
			super.connectionProviderStopped();
			timeoutByPreparedStatement.clear();
		}

		@Override
		public void clear() {
			super.clear();
			timeoutByPreparedStatement.clear();
		}
	}
}
