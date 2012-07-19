/*
  * Hibernate, Relational Persistence for Idiomatic Java
  *
  * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-
  * party contributors as indicated by the @author tags or express
  * copyright attribution statements applied by the authors.
  * All third-party contributions are distributed under license by
  * Red Hat, Inc.
  *
  * This copyrighted material is made available to anyone wishing to
  * use, modify, copy, or redistribute it subject to the terms and
  * conditions of the GNU Lesser General Public License, as published
  * by the Free Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this distribution; if not, write to:
  *
  * Free Software Foundation, Inc.
  * 51 Franklin Street, Fifth Floor
  * Boston, MA  02110-1301  USA
  */
package org.hibernate.test.dialect.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.SQLServer2005Dialect;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.junit.Test;

/**
 * used driver hibernate.connection.driver_class com.microsoft.sqlserver.jdbc.SQLServerDriver
 *
 * @author Guenther Demetz
 */
@RequiresDialect(value = { SQLServer2005Dialect.class })
public class SQLServerDialectTest extends BaseCoreFunctionalTestCase {
	@Test
	@TestForIssue(jiraKey = "HHH-7198")
	public void testMaxResultsSqlServerWithCaseSensitiveCollation() throws Exception {

		Session s = openSession();
		s.beginTransaction();
		String defaultCollationName = s.doReturningWork( new ReturningWork<String>() {
			@Override
			public String execute(Connection connection) throws SQLException {
				String databaseName = connection.getCatalog();
				ResultSet rs =  connection.createStatement().executeQuery( "SELECT collation_name FROM sys.databases WHERE name = '"+databaseName+ "';" );
				while(rs.next()){
					return rs.getString( "collation_name" );
				}
				throw new AssertionError( "can't get collation name of database "+databaseName );

			}
		} );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		String databaseName = s.doReturningWork( new ReturningWork<String>() {
			@Override
			public String execute(Connection connection) throws SQLException {
				return connection.getCatalog();
			}
		} );
		s.createSQLQuery( "ALTER DATABASE " + databaseName + " set single_user with rollback immediate" )
				.executeUpdate();
		s.createSQLQuery( "ALTER DATABASE " + databaseName + " COLLATE Latin1_General_CS_AS" ).executeUpdate();
		s.createSQLQuery( "ALTER DATABASE " + databaseName + " set multi_user" ).executeUpdate();

		Transaction tx = s.beginTransaction();

		for ( int i = 1; i <= 20; i++ ) {
			s.persist( new Product2( i, "Kit" + i ) );
		}
		s.flush();
		s.clear();

		List list = s.createQuery( "from Product2 where description like 'Kit%'" )
				.setFirstResult( 2 )
				.setMaxResults( 2 )
				.list();
		assertEquals( 2, list.size() );
		tx.rollback();
		s.close();

		s = openSession();
		s.createSQLQuery( "ALTER DATABASE " + databaseName + " set single_user with rollback immediate" )
				.executeUpdate();
		s.createSQLQuery( "ALTER DATABASE " + databaseName + " COLLATE " + defaultCollationName ).executeUpdate();
		s.createSQLQuery( "ALTER DATABASE " + databaseName + " set multi_user" ).executeUpdate();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7369")
	public void testPaginationWithScalarQuery() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		for ( int i = 0; i < 10; i++ ) {
			s.persist( new Product2( i, "Kit" + i ) );
		}
		s.flush();
		s.clear();

		List list = s.createSQLQuery( "select id from Product2 where description like 'Kit%' order by id" ).list();
		assertEquals(Integer.class, list.get(0).getClass()); // scalar result is an Integer

		list = s.createSQLQuery( "select id from Product2 where description like 'Kit%' order by id" ).setFirstResult( 2 ).setMaxResults( 2 ).list();
		assertEquals(Integer.class, list.get(0).getClass()); // this fails without patch, as result suddenly has become an array

		// same once again with alias
		list = s.createSQLQuery( "select id as myint from Product2 where description like 'Kit%' order by id asc" ).setFirstResult( 2 ).setMaxResults( 2 ).list();
		assertEquals(Integer.class, list.get(0).getClass());

		tx.rollback();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7368")
	public void testPaginationWithTrailingSemicolon() throws Exception {
		Session s = openSession();
		s.createSQLQuery( "select id from Product2 where description like 'Kit%' order by id;" )
				.setFirstResult( 2 ).setMaxResults( 2 ).list();
		s.close();
	}

	@Test
	public void testPaginationWithHQLProjection() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		for ( int i = 10; i < 20; i++ ) {
			session.persist( new Product2( i, "Kit" + i ) );
		}
		session.flush();
		session.clear();

		List list = session.createQuery(
				"select id, description as descr, (select max(id) from Product2) as maximum from Product2"
		).setFirstResult( 2 ).setMaxResults( 2 ).list();
		assertEquals( 19, ( (Object[]) list.get( 1 ) )[2] );

		list = session.createQuery( "select id, description, (select max(id) from Product2) from Product2 order by id" )
				.setFirstResult( 2 ).setMaxResults( 2 ).list();
		assertEquals( 2, list.size() );
		assertArrayEquals( new Object[] {12, "Kit12", 19}, (Object[]) list.get( 0 ));
		assertArrayEquals( new Object[] {13, "Kit13", 19}, (Object[]) list.get( 1 ));

		tx.rollback();
		session.close();
	}

	@Test
	public void testPaginationWithHQL() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		for ( int i = 20; i < 30; i++ ) {
			session.persist( new Product2( i, "Kit" + i ) );
		}
		session.flush();
		session.clear();

		List list = session.createQuery( "from Product2 order by id" ).setFirstResult( 3 ).setMaxResults( 2 ).list();
		assertEquals( Arrays.asList( new Product2( 23, "Kit23" ), new Product2( 24, "Kit24" ) ), list );

		tx.rollback();
		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7370")
	public void testPaginationWithMaxOnly() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		for ( int i = 30; i < 40; i++ ) {
			session.persist( new Product2( i, "Kit" + i ) );
		}
		session.flush();
		session.clear();

		List list = session.createQuery( "from Product2 order by id" ).setFirstResult( 0 ).setMaxResults( 2 ).list();
		assertEquals( Arrays.asList( new Product2( 30, "Kit30" ), new Product2( 31, "Kit31" ) ), list );

		list = session.createQuery( "select distinct p from Product2 p order by p.id" ).setMaxResults( 1 ).list();
		assertEquals( Arrays.asList( new Product2( 30, "Kit30" ) ), list );

		tx.rollback();
		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-3961")
	public void testLockNowaitSqlServer() throws Exception {
		Session s = openSession();
		s.beginTransaction();

		final Product2 kit = new Product2();
		kit.id = 4000;
		kit.description = "m";
		s.persist( kit );
		s.getTransaction().commit();
		final Transaction tx = s.beginTransaction();


		Session s2 = openSession();

		Transaction tx2 = s2.beginTransaction();

		Product2 kit2 = (Product2) s2.byId( Product2.class ).load( kit.id );

		kit.description = "change!";
		s.flush(); // creates write lock on kit until we end the transaction

		Thread thread = new Thread(
				new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep( 3000 );
						}
						catch ( InterruptedException e ) {
							e.printStackTrace();
						}
						tx.commit();
					}
				}
		);

		LockOptions opt = new LockOptions( LockMode.UPGRADE_NOWAIT );
		opt.setTimeOut( 0 ); // seems useless
		long start = System.currentTimeMillis();
		thread.start();
		try {
			s2.buildLockRequest( opt ).lock( kit2 );
		}
		catch ( LockTimeoutException e ) {
			// OK
		}
		long end = System.currentTimeMillis();
		thread.join();
		long differenceInMillisecs = end - start;
		assertTrue(
				"Lock NoWait blocked for " + differenceInMillisecs + " ms, this is definitely to much for Nowait",
				differenceInMillisecs < 2000
		);

		s2.getTransaction().rollback();
		s.getTransaction().begin();
		s.delete( kit );
		s.getTransaction().commit();


	}

	@Override
	protected java.lang.Class<?>[] getAnnotatedClasses() {
		return new java.lang.Class[] {
				Product2.class
		};
	}
}
