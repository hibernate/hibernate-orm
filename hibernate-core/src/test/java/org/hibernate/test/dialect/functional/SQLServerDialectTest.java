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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.dialect.SQLServer2005Dialect;
import org.hibernate.engine.spi.SessionImplementor;
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
	@Test
	@TestForIssue(jiraKey = "HHH-7198")
	public void testMaxResultsSqlServerWithCaseSensitiveCollation() throws Exception {

		final Session s = openSession();
		s.beginTransaction();
		String defaultCollationName = s.doReturningWork( new ReturningWork<String>() {
			@Override
			public String execute(Connection connection) throws SQLException {
				String databaseName = connection.getCatalog();
				Statement st = ((SessionImplementor)s).getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().createStatement();
				ResultSet rs =  ((SessionImplementor)s).getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().extract( st, "SELECT collation_name FROM sys.databases WHERE name = '"+databaseName+ "';" );
				while(rs.next()){
					return rs.getString( "collation_name" );
				}
				throw new AssertionError( "can't get collation name of database "+databaseName );

			}
		} );
		s.getTransaction().commit();
		s.close();

		Session s2 = openSession();
		String databaseName = s2.doReturningWork( new ReturningWork<String>() {
			@Override
			public String execute(Connection connection) throws SQLException {
				return connection.getCatalog();
			}
		} );
		s2.createSQLQuery( "ALTER DATABASE " + databaseName + " set single_user with rollback immediate" )
				.executeUpdate();
		s2.createSQLQuery( "ALTER DATABASE " + databaseName + " COLLATE Latin1_General_CS_AS" ).executeUpdate();
		s2.createSQLQuery( "ALTER DATABASE " + databaseName + " set multi_user" ).executeUpdate();

		Transaction tx = s2.beginTransaction();

		for ( int i = 1; i <= 20; i++ ) {
			s2.persist( new Product2( i, "Kit" + i ) );
		}
		s2.flush();
		s2.clear();

		List list = s2.createQuery( "from Product2 where description like 'Kit%'" )
				.setFirstResult( 2 )
				.setMaxResults( 2 )
				.list();
		assertEquals( 2, list.size() );
		tx.rollback();
		s2.close();

		s2 = openSession();
		s2.createSQLQuery( "ALTER DATABASE " + databaseName + " set single_user with rollback immediate" )
				.executeUpdate();
		s2.createSQLQuery( "ALTER DATABASE " + databaseName + " COLLATE " + defaultCollationName ).executeUpdate();
		s2.createSQLQuery( "ALTER DATABASE " + databaseName + " set multi_user" ).executeUpdate();
		s2.close();
	}
	
	private void doWork(Session s) {
		
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
	@TestForIssue(jiraKey = "HHH-6627")
	public void testPaginationWithAggregation() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		// populating test data
		Category category1 = new Category( 1, "Category1" );
		Category category2 = new Category( 2, "Category2" );
		Category category3 = new Category( 3, "Category3" );
		session.persist( category1 );
		session.persist( category2 );
		session.persist( category3 );
		session.flush();
		session.persist( new Product2( 1, "Kit1", category1 ) );
		session.persist( new Product2( 2, "Kit2", category1 ) );
		session.persist( new Product2( 3, "Kit3", category1 ) );
		session.persist( new Product2( 4, "Kit4", category2 ) );
		session.persist( new Product2( 5, "Kit5", category2 ) );
		session.persist( new Product2( 6, "Kit6", category3 ) );
		session.flush();
		session.clear();

		// count number of products in each category
		List<Object[]> result = session.createCriteria( Category.class, "c" ).createAlias( "products", "p" )
				.setProjection(
						Projections.projectionList()
								.add( Projections.groupProperty( "c.id" ) )
								.add( Projections.countDistinct( "p.id" ) )
				)
				.addOrder( Order.asc( "c.id" ) )
				.setFirstResult( 1 ).setMaxResults( 3 ).list();

		assertEquals( 2, result.size() );
		assertArrayEquals( new Object[] { 2, 2L }, result.get( 0 ) ); // two products of second category
		assertArrayEquals( new Object[] { 3, 1L }, result.get( 1 ) ); // one products of third category

		tx.rollback();
		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7752")
	public void testPaginationWithFormulaSubquery() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		// populating test data
		Folder folder1 = new Folder( 1L, "Folder1" );
		Folder folder2 = new Folder( 2L, "Folder2" );
		Folder folder3 = new Folder( 3L, "Folder3" );
		session.persist( folder1 );
		session.persist( folder2 );
		session.persist( folder3 );
		session.flush();
		session.persist( new Contact( 1L, "Lukasz", "Antoniak", "owner", folder1 ) );
		session.persist( new Contact( 2L, "Kinga", "Mroz", "co-owner", folder2 ) );
		session.flush();
		session.clear();
		session.refresh( folder1 );
		session.refresh( folder2 );
		session.clear();

		List<Long> folderCount = session.createQuery( "select count(distinct f) from Folder f" ).setMaxResults( 1 ).list();
		assertEquals( Arrays.asList( 3L ), folderCount );

		List<Folder> distinctFolders = session.createQuery( "select distinct f from Folder f order by f.id desc" )
				.setFirstResult( 1 ).setMaxResults( 2 ).list();
		assertEquals( Arrays.asList( folder2, folder1 ), distinctFolders );

		tx.rollback();
		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7781")
	public void testPaginationWithCastOperator() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		for ( int i = 40; i < 50; i++ ) {
			session.persist( new Product2( i, "Kit" + i ) );
		}
		session.flush();
		session.clear();

		List<Object[]> list = session.createQuery( "select p.id, cast(p.id as string) as string_id from Product2 p order by p.id" )
				.setFirstResult( 1 ).setMaxResults( 2 ).list();
		assertEquals( 2, list.size() );
		assertArrayEquals( new Object[] { 41, "41" }, list.get( 0 ) );
		assertArrayEquals( new Object[] { 42, "42" }, list.get( 1 ) );

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
				Product2.class, Category.class, Folder.class, Contact.class
		};
	}
}
