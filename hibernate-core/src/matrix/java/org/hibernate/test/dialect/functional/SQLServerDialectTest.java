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
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.SQLServer2005Dialect;
import org.hibernate.exception.LockTimeoutException;
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

	@TestForIssue(jiraKey = "HHH-7198")
	@Test
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
			Product2 kit = new Product2();
			kit.id = i;
			kit.description = "Kit" + i;
			s.persist( kit );
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
	
	@TestForIssue(jiraKey = "HHH-7369")
	@Test
	public void testPaginationOnScalarQuery() throws Exception {

		Session s = openSession();
		Transaction tx = s.beginTransaction();

		for ( int i = 1; i <= 20; i++ ) {
			Product2 kit = new Product2();
			kit.id = i;
			kit.description = "Kit" + i;
			s.persist( kit );
		}
		s.flush();
		s.clear();
		
		List list = s.createSQLQuery( "select id from Product2 where description like :pattern order by id" ).setString("pattern", "Kit%").list();
		assertEquals(Integer.class, list.get(0).getClass()); // scalar result is an Integer

		list = s.createSQLQuery( "select id from Product2 where description like  :pattern  order by id" ).setString("pattern", "Kit%").setFirstResult( 2 ).setMaxResults( 2 ).list();
		assertEquals(Integer.class, list.get(0).getClass()); // this fails without patch, as result suddenly has become an array
		
		// same once again with alias (explicit AS keyword)
		list = s.createSQLQuery( "select id as myint from Product2 where description like :pattern order by id asc" ).setString("pattern", "Kit%").setFirstResult( 2 ).setMaxResults( 2 ).list();
		assertEquals(Integer.class, list.get(0).getClass()); 
		
		// same once again with alias (without optional AS keyword)
		list = s.createSQLQuery( "select id myint from Product2 where description like :pattern order by id asc" ).setString("pattern", "Kit%").setFirstResult( 2 ).setMaxResults( 2 ).list();
		assertEquals(Integer.class, list.get(0).getClass()); 
		
		// with subquery
		list = s.createSQLQuery( "select id, description as descr, (select max(id) from Product2) maximum from Product2 where description like :pattern order by id" ).setString("pattern", "Kit%").setFirstResult( 0 ).setMaxResults( 2 ).list();
		assertEquals(2, list.size());
		assertEquals(3, ((Object[])list.get(0)).length );
		
		// with distinct clause & aliases without AS keyword
		list = s.createSQLQuery( "select distinct id ranking, description descr from Product2 where description like :pattern order by id" ).setString("pattern", "Kit%").setFirstResult( 0 ).setMaxResults( 2 ).list();
		assertEquals(2, list.size());
		assertEquals(2, ((Object[])list.get(0)).length );
		
		// with distinct clause & aliases with AS keyword
		list = s.createSQLQuery( "select distinct id as ranking, description as descr from Product2 where description like :pattern order by id" ).setString("pattern", "Kit%").setFirstResult( 0 ).setMaxResults( 2 ).list();
		assertEquals(2, list.size());
		assertEquals(2, ((Object[])list.get(0)).length );
		
		tx.rollback();
		s.close();
	}
	
	@TestForIssue(jiraKey = "HHH-7368")
	@Test
	public void testPaginationCorrectness() throws Exception {

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		for ( int i = 0; i < 20; i++ ) {
			Product2 kit = new Product2();
			kit.id = i;
			kit.description = "Kit" + i;
			s.persist( kit );
		}
		s.flush();
		s.clear();

		List list = s.createQuery( "from Product2 where description like :pattern order by id" ).setString("pattern", "Kit%").setFirstResult( 2 ).setMaxResults( 2 ).list();
		assertEquals(2, list.size());
		assertEquals(2, ((Product2)list.get(0)).id.intValue());
		assertEquals(3, ((Product2)list.get(1)).id.intValue());
		
		list = s.createQuery( "from Product2 where description like :pattern order by id" ).setString("pattern", "Kit%").setFirstResult( 0 ).setMaxResults( 2 ).list();
		assertEquals(2, list.size());
		assertEquals(0, ((Product2)list.get(0)).id.intValue());
		assertEquals(1, ((Product2)list.get(1)).id.intValue());

		tx.rollback();
		s.close();
	}
	
	@TestForIssue(jiraKey = "HHH-7368")
	@Test
	public void testPaginationWithTrailingSemicolon() throws Exception {
		Session s = openSession();
		List list = s.createSQLQuery( "select id from Product2 where description like 'Kit%' order by id;" ).setFirstResult( 2 ).setMaxResults( 2 ).list();
	    // caused:      SQLServerException: Incorrect syntax near ';'.
		//                                                         |
		//                                                         V
		//WITH query AS (select id, ROW_NUMBER() OVER ( order by id;) - 1 as __hibernate_row_nr__  from Product2 where description like 'Kit%' ) SELECT id FROM query 
		//WHERE __hibernate_row_nr__ >= 2  AND __hibernate_row_nr__ < 4  

		s.close();
	}


	@TestForIssue(jiraKey = "HHH-3961")
	@Test
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