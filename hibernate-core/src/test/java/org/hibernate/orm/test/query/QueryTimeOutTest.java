/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.AbstractTransactSQLDialect;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.NativeQuery;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import static org.hibernate.jpa.SpecHints.HINT_SPEC_QUERY_TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gail Badner
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJdbcDriverProxying.class)
public class QueryTimeOutTest extends BaseSessionFactoryFunctionalTest {

	private static final PreparedStatementSpyConnectionProvider CONNECTION_PROVIDER =
			new PreparedStatementSpyConnectionProvider();
	private static final String QUERY = "update AnEntity set name='abc'";
	private String expectedSqlQuery;

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {AnEntity.class};
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builer) {
		ConnectionProvider connectionProvider = (ConnectionProvider) builer.getSettings()
				.get( AvailableSettings.CONNECTION_PROVIDER );
		CONNECTION_PROVIDER.setConnectionProvider( connectionProvider );
		builer.applySetting( AvailableSettings.CONNECTION_PROVIDER, CONNECTION_PROVIDER );
	}

	@BeforeEach
	public void before() {
		CONNECTION_PROVIDER.clear();
		SessionFactoryImplementor sessionFactoryImplementor = sessionFactory();
		final JdbcType jdbcType = sessionFactoryImplementor.getTypeConfiguration().getJdbcTypeRegistry()
				.getDescriptor(
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
		else if ( DialectContext.getDialect()
						.getDmlTargetColumnQualifierSupport() == DmlTargetColumnQualifierSupport.NONE ) {
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
								sessionFactoryImplementor.getJdbcServices().getDialect(),
								sessionFactoryImplementor.getWrapperOptions()
						)
		);
	}

	@Test
	@JiraKey(value = "HHH-12075")
	public void testCreateQuerySetTimeout() {
		inTransaction( session -> {
					MutationQuery query = session.createMutationQuery( QUERY );
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
						fail( "should not have thrown exceptioinTransaction( session -> {n" );
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12075")
	public void testCreateQuerySetTimeoutHint() {
		inTransaction( session -> {
					MutationQuery query = session.createMutationQuery( QUERY );
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
	@JiraKey(value = "HHH-12075")
	public void testCreateNativeQuerySetTimeout() {
		inTransaction( session -> {
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
	@JiraKey(value = "HHH-12075")
	public void testCreateNativeQuerySetTimeoutHint() {
		inTransaction( session -> {
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
	@JiraKey(value = "HHH-12075")
	public void testCreateSQLQuerySetTimeout() {
		inTransaction( session -> {
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
	@JiraKey(value = "HHH-12075")
	public void testCreateSQLQuerySetTimeoutHint() {
		inTransaction( session -> {
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
