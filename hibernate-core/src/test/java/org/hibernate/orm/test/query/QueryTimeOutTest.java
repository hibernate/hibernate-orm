/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.AbstractTransactSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.DialectContext;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hibernate.jpa.SpecHints.HINT_SPEC_QUERY_TIMEOUT;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gail Badner
 */
@RequiresDialectFeature(DialectChecks.SupportsJdbcDriverProxying.class)
public class QueryTimeOutTest extends BaseNonConfigCoreFunctionalTestCase {

	private static final PreparedStatementSpyConnectionProvider CONNECTION_PROVIDER = new PreparedStatementSpyConnectionProvider(
	);
	private static final String QUERY = "update AnEntity set name='abc'";

	private String expectedSqlQuery;

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { AnEntity.class };
	}

	@Override
	protected void addSettings(Map<String,Object> settings) {
		if ( settings.containsKey( AvailableSettings.CONNECTION_PROVIDER ) ) {
			CONNECTION_PROVIDER.setConnectionProvider( (ConnectionProvider) settings.get( AvailableSettings.CONNECTION_PROVIDER ) );
		}
		settings.put( AvailableSettings.CONNECTION_PROVIDER, CONNECTION_PROVIDER );
	}

	@Before
	public void before() {
		CONNECTION_PROVIDER.clear();
		final JdbcType jdbcType = sessionFactory().getTypeConfiguration().getJdbcTypeRegistry().getDescriptor(
				Types.VARCHAR
		);
		final String baseQuery;
		if ( DialectContext.getDialect() instanceof OracleDialect ) {
			baseQuery = "update AnEntity ae1_0 set ae1_0.name=?";
		}
		else if ( DialectContext.getDialect() instanceof SybaseDialect ) {
			baseQuery = "update AnEntity set name=? from AnEntity ae1_0";
		}
		else if ( DialectContext.getDialect() instanceof AbstractTransactSQLDialect ) {
			baseQuery = "update ae1_0 set name=? from AnEntity ae1_0";
		}
		else if (DialectContext.getDialect() instanceof InformixDialect ) {
			baseQuery = "update AnEntity set name=?";
		}
		else {
			baseQuery = "update AnEntity ae1_0 set name=?";
		}
		expectedSqlQuery = baseQuery.replace(
				"?",
				jdbcType.getJdbcLiteralFormatter( StringJavaType.INSTANCE )
						.toJdbcLiteral(
								"abc",
								sessionFactory().getJdbcServices().getDialect(),
								sessionFactory().getWrapperOptions()
						)
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12075")
	public void testCreateQuerySetTimeout() {
		doInHibernate(
				this::sessionFactory, session -> {
					Query query = session.createQuery( QUERY );
					query.setTimeout( 123 );
					query.executeUpdate();

					try {
						List<Object[]> setQueryTimeoutCalls = CONNECTION_PROVIDER.spyContext.getCalls(
								Statement.class.getMethod( "setQueryTimeout", int.class ),
								CONNECTION_PROVIDER.getPreparedStatement( expectedSqlQuery )
						);
						assertEquals( 2, setQueryTimeoutCalls.size() );
						assertEquals( 123, setQueryTimeoutCalls.get( 0 )[0] );
						assertEquals( 0, setQueryTimeoutCalls.get( 1 )[0] );
					}
					catch (Exception ex) {
						fail( "should not have thrown exception" );
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12075")
	public void testCreateQuerySetTimeoutHint() {
		doInHibernate(
				this::sessionFactory, session -> {
					Query query = session.createQuery( QUERY );
					query.setHint( HINT_SPEC_QUERY_TIMEOUT, 123000 );
					query.executeUpdate();

					try {
						List<Object[]> setQueryTimeoutCalls = CONNECTION_PROVIDER.spyContext.getCalls(
								Statement.class.getMethod( "setQueryTimeout", int.class ),
								CONNECTION_PROVIDER.getPreparedStatement( expectedSqlQuery )
						);
						assertEquals( 2, setQueryTimeoutCalls.size() );
						assertEquals( 123, setQueryTimeoutCalls.get( 0 )[0] );
						assertEquals( 0, setQueryTimeoutCalls.get( 1 )[0] );
					}
					catch (Exception ex) {
						fail( "should not have thrown exception" );
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12075")
	public void testCreateNativeQuerySetTimeout() {
		doInHibernate(
				this::sessionFactory, session -> {
					NativeQuery query = session.createNativeQuery( QUERY );
					query.setTimeout( 123 );
					query.executeUpdate();

					try {
						List<Object[]> setQueryTimeoutCalls = CONNECTION_PROVIDER.spyContext.getCalls(
								Statement.class.getMethod( "setQueryTimeout", int.class ),
								CONNECTION_PROVIDER.getPreparedStatement( QUERY )
						);
						assertEquals( 2, setQueryTimeoutCalls.size() );
						assertEquals( 123, setQueryTimeoutCalls.get( 0 )[0] );
						assertEquals( 0, setQueryTimeoutCalls.get( 1 )[0] );
					}
					catch (Exception ex) {
						fail( "should not have thrown exception" );
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12075")
	public void testCreateNativeQuerySetTimeoutHint() {
		doInHibernate(
				this::sessionFactory, session -> {
					NativeQuery query = session.createNativeQuery( QUERY );
					query.setHint( HINT_SPEC_QUERY_TIMEOUT, 123000 );
					query.executeUpdate();

					try {
						List<Object[]> setQueryTimeoutCalls = CONNECTION_PROVIDER.spyContext.getCalls(
								Statement.class.getMethod( "setQueryTimeout", int.class ),
								CONNECTION_PROVIDER.getPreparedStatement( QUERY )
						);
						assertEquals( 2, setQueryTimeoutCalls.size() );
						assertEquals( 123, setQueryTimeoutCalls.get( 0 )[0] );
						assertEquals( 0, setQueryTimeoutCalls.get( 1 )[0] );
					}
					catch (Exception ex) {
						fail( "should not have thrown exception" );
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12075")
	public void testCreateSQLQuerySetTimeout() {
		doInHibernate(
				this::sessionFactory, session -> {
					NativeQuery query = session.createNativeQuery( QUERY );
					query.setTimeout( 123 );
					query.executeUpdate();

					try {
						List<Object[]> setQueryTimeoutCalls = CONNECTION_PROVIDER.spyContext.getCalls(
								Statement.class.getMethod( "setQueryTimeout", int.class ),
								CONNECTION_PROVIDER.getPreparedStatement( QUERY )
						);
						assertEquals( 2, setQueryTimeoutCalls.size() );
						assertEquals( 123, setQueryTimeoutCalls.get( 0 )[0] );
						assertEquals( 0, setQueryTimeoutCalls.get( 1 )[0] );
					}
					catch (Exception ex) {
						fail( "should not have thrown exception" );
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12075")
	public void testCreateSQLQuerySetTimeoutHint() {
		doInHibernate(
				this::sessionFactory, session -> {
					NativeQuery query = session.createNativeQuery( QUERY );
					query.setHint( HINT_SPEC_QUERY_TIMEOUT, 123000 );
					query.executeUpdate();

					try {
						List<Object[]> setQueryTimeoutCalls = CONNECTION_PROVIDER.spyContext.getCalls(
								Statement.class.getMethod( "setQueryTimeout", int.class ),
								CONNECTION_PROVIDER.getPreparedStatement( QUERY )
						);
						assertEquals( 2, setQueryTimeoutCalls.size() );
						assertEquals( 123, setQueryTimeoutCalls.get( 0 )[0] );
						assertEquals( 0, setQueryTimeoutCalls.get( 1 )[0] );
					}
					catch (Exception ex) {
						fail( "should not have thrown exception" );
					}
				}
		);
	}

	@Entity(name = "AnEntity")
	@Table(name = "AnEntity")
	public static class AnEntity {
		@Id
		private int id;

		private String name;
	}
}
