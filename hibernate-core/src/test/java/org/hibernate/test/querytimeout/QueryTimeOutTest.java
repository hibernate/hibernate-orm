/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.querytimeout;

import java.sql.SQLException;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.QueryHint;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.QueryHints;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Gail Badner
 */
@RequiresDialectFeature(DialectChecks.SupportsJdbcDriverProxying.class)
public class QueryTimeOutTest extends BaseNonConfigCoreFunctionalTestCase {

	private static final PreparedStatementSpyConnectionProvider CONNECTION_PROVIDER = new PreparedStatementSpyConnectionProvider( true, false );
	private static final String QUERY = "update AnEntity set name='abc'";

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { AnEntity.class };
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( AvailableSettings.CONNECTION_PROVIDER, CONNECTION_PROVIDER );
	}

	@Before
	public void before() {
		CONNECTION_PROVIDER.clear();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12075")
	public void testCreateQuerySetTimeout() {
		doInHibernate(
				this::sessionFactory, session -> {
					Query query = session.createQuery( QUERY );
					query.setTimeout( 123 );
					query.executeUpdate();

					try {
						verify( CONNECTION_PROVIDER.getPreparedStatement( QUERY ), times( 1 ) ).setQueryTimeout( 123 );
					}
					catch (SQLException ex) {
						fail( "should not have thrown exception" );
					}
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12075")
	public void testCreateQuerySetTimeoutHint() {
		doInHibernate(
				this::sessionFactory, session -> {
					Query query = session.createQuery( QUERY );
					query.setHint( QueryHints.SPEC_HINT_TIMEOUT, 123000 );
					query.executeUpdate();

					try {
						verify( CONNECTION_PROVIDER.getPreparedStatement( QUERY ), times( 1 ) ).setQueryTimeout( 123 );
					}
					catch (SQLException ex) {
						fail( "should not have thrown exception" );
					}
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12075")
	public void testCreateNativeQuerySetTimeout() {
		doInHibernate(
				this::sessionFactory, session -> {
					NativeQuery query = session.createNativeQuery( QUERY );
					query.setTimeout( 123 );
					query.executeUpdate();

					try {
						verify( CONNECTION_PROVIDER.getPreparedStatement( QUERY ), times( 1 ) ).setQueryTimeout( 123 );
					}
					catch (SQLException ex) {
						fail( "should not have thrown exception" );
					}
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12075")
	public void testCreateNativeQuerySetTimeoutHint() {
		doInHibernate(
				this::sessionFactory, session -> {
					NativeQuery query = session.createNativeQuery( QUERY );
					query.setHint( QueryHints.SPEC_HINT_TIMEOUT, 123000 );
					query.executeUpdate();

					try {
						verify( CONNECTION_PROVIDER.getPreparedStatement( QUERY ), times( 1 ) ).setQueryTimeout( 123 );
					}
					catch (SQLException ex) {
						fail( "should not have thrown exception" );
					}
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12075")
	public void testCreateSQLQuerySetTimeout() {
		doInHibernate(
				this::sessionFactory, session -> {
					NativeQuery query = session.createSQLQuery( QUERY );
					query.setTimeout( 123 );
					query.executeUpdate();

					try {
						verify( CONNECTION_PROVIDER.getPreparedStatement( QUERY ), times( 1 ) ).setQueryTimeout( 123 );
					}
					catch (SQLException ex) {
						fail( "should not have thrown exception" );
					}
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12075")
	public void testCreateSQLQuerySetTimeoutHint() {
		doInHibernate(
				this::sessionFactory, session -> {
					NativeQuery query = session.createSQLQuery( QUERY );
					query.setHint( QueryHints.SPEC_HINT_TIMEOUT, 123000 );
					query.executeUpdate();

					try {
						verify( CONNECTION_PROVIDER.getPreparedStatement( QUERY ), times( 1 ) ).setQueryTimeout( 123 );
					}
					catch (SQLException ex) {
						fail( "should not have thrown exception" );
					}
				}
		);
	}

	@Entity(name = "AnEntity" )
	@Table(name = "AnEntity" )
	public static class AnEntity {
		@Id
		private int id;

		private String name;
	}
}
