/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.sql.autodiscovery;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.loader.custom.NonUniqueDiscoveredSqlAliasException;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class AutoDiscoveryTest extends BaseCoreFunctionalTestCase {
	private static final String QUERY_STRING =
			"select u.name as username, g.name as groupname, m.joindate " +
					"from t_membership m " +
					"        inner join t_user u on m.member_id = u.id " +
					"        inner join t_group g on m.group_id = g.id";

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Group.class, User.class, Membership.class };
	}

	@Test( expected = NonUniqueDiscoveredSqlAliasException.class )
	public void testAutoDiscoveryWithDuplicateColumnLabels() {
		Session session = openSession();
		session.beginTransaction();
		session.save( new User( "steve" ) );
		session.save( new User( "stliu" ) );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		List results = session.createSQLQuery( "select u.name, u2.name from t_user u, t_user u2 where u.name='steve'" ).list();
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
		session.save( u );
		session.save( g );
		session.save( m );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		List result = session.createSQLQuery( QUERY_STRING ).list();
		Object[] row = (Object[]) result.get( 0 );
		Assert.assertEquals( "steve", row[0] );
		Assert.assertEquals( "developer", row[1] );
		session.delete( m );
		session.delete( u );
		session.delete( g );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testDialectGetColumnAliasExtractor() throws Exception {
		Session session = openSession();
		final SessionImplementor sessionImplementor = (SessionImplementor) session;
		session.beginTransaction();
		session.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						PreparedStatement ps = sessionImplementor.getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().prepareStatement( QUERY_STRING );
						ResultSet rs = sessionImplementor.getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().extract( ps );
						try {
							ResultSetMetaData metadata = rs.getMetaData();
							String column1Alias = getDialect().getColumnAliasExtractor().extractColumnAlias( metadata, 1 );
							String column2Alias = getDialect().getColumnAliasExtractor().extractColumnAlias( metadata, 2 );
							Assert.assertFalse( "bad dialect.getColumnAliasExtractor impl", column1Alias.equals( column2Alias ) );
						}
						finally {
							sessionImplementor.getTransactionCoordinator().getJdbcCoordinator().release( rs, ps );
							sessionImplementor.getTransactionCoordinator().getJdbcCoordinator().release( ps );
						}
					}
				}
		);
		session.getTransaction().commit();
		session.close();
	}
}
