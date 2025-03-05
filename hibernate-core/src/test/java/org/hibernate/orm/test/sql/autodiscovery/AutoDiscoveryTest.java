/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.autodiscovery;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class AutoDiscoveryTest extends BaseCoreFunctionalTestCase {
	private static final String QUERY_STRING =
			"select u.name as username, g.name as groupname, m.joinDate " +
					"from t_membership m " +
					"        inner join t_user u on m.member_id = u.id " +
					"        inner join t_group g on m.group_id = g.id";

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setImplicitNamingStrategy( ImplicitNamingStrategyJpaCompliantImpl.INSTANCE );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Group.class, User.class, Membership.class };
	}

	@Test
	public void testAutoDiscoveryWithDuplicateColumnLabels() {
		Session session = openSession();
		session.beginTransaction();
		session.persist( new User( "steve" ) );
		session.persist( new User( "stliu" ) );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		List results = session.createNativeQuery(
				"select u.name, u2.name from t_user u, t_user u2 where u.name='steve'" ).list();
		// this should result in a result set like:
		//   [0] steve, steve
		//   [1] steve, stliu
		// although the rows could be reversed
		assertEquals( 2, results.size() );
		final Object[] row1 = (Object[]) results.get( 0 );
		final Object[] row2 = (Object[]) results.get( 1 );
		assertEquals( "steve", row1[0] );
		assertEquals( "steve", row2[0] );
		if ( "steve".equals( row1[1] ) ) {
			assertEquals( "stliu", row2[1] );
		}
		else {
			assertEquals( "stliu", row1[1] );
		}
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.createQuery( "delete from User" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testSqlQueryAutoDiscovery() throws Exception {
		Session session = openSession();
		session.beginTransaction();
		User u = new User( "steve" );
		Group g = new Group( "developer" );
		Membership m = new Membership( u, g );
		session.persist( u );
		session.persist( g );
		session.persist( m );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		List result = session.createNativeQuery( QUERY_STRING ).list();
		Object[] row = (Object[]) result.get( 0 );
		Assert.assertEquals( "steve", row[0] );
		Assert.assertEquals( "developer", row[1] );
		session.remove( m );
		session.remove( u );
		session.remove( g );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testDialectGetColumnAliasExtractor() {
		Session session = openSession();
		final SessionImplementor sessionImplementor = (SessionImplementor) session;
		session.beginTransaction();
		session.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						PreparedStatement ps = sessionImplementor.getJdbcCoordinator().getStatementPreparer().prepareStatement( QUERY_STRING );
						ResultSet rs = sessionImplementor.getJdbcCoordinator().getResultSetReturn().extract( ps, QUERY_STRING );
						try {
							ResultSetMetaData metadata = rs.getMetaData();
							String column1Alias = getDialect().getColumnAliasExtractor().extractColumnAlias( metadata, 1 );
							String column2Alias = getDialect().getColumnAliasExtractor().extractColumnAlias( metadata, 2 );
							Assert.assertFalse( "bad dialect.getColumnAliasExtractor impl", column1Alias.equals( column2Alias ) );
						}
						finally {
							sessionImplementor.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( rs, ps );
							sessionImplementor.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( ps );
						}
					}
				}
		);
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@JiraKey( "HHH-16697" )
	@SkipForDialect( dialectClass = AltibaseDialect.class, reason = "Altibase sum(39.74) returns Float" )
	public void testAggregateQueryAutoDiscovery() {
		Session session = openSession();
		session.beginTransaction();
		User u = new User( "steve" );
		session.persist( u );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		List<Object> result = session.createNativeQuery( "select sum(39.74) from t_user u" ).list();
		Assert.assertEquals( new BigDecimal( "39.74" ), result.get( 0 ) );
		session.remove( u );
		session.getTransaction().commit();
		session.close();
	}
}
