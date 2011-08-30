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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

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
		session.beginTransaction();
		session.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						PreparedStatement ps = connection.prepareStatement( QUERY_STRING );
						ResultSet rs = ps.executeQuery();
						try {
							ResultSetMetaData metadata = rs.getMetaData();
							String column1Alias = getDialect().getColumnAliasExtractor().extractColumnAlias( metadata, 1 );
							String column2Alias = getDialect().getColumnAliasExtractor().extractColumnAlias( metadata, 2 );
							Assert.assertFalse( "bad dialect.getColumnAliasExtractor impl", column1Alias.equals( column2Alias ) );
						}
						finally {
							rs.close();
							ps.close();
						}
					}
				}
		);
		session.getTransaction().commit();
		session.close();
	}
}
