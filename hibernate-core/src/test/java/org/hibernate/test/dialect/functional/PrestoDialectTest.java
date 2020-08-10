/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.functional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.dialect.PrestoDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author findepi
 */
@RequiresDialect(value = PrestoDialect.class)
public class PrestoDialectTest extends BaseCoreFunctionalTestCase {
	@Override
	protected boolean createSchema() {
		return false;
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return false;
	}

	@Override
	protected void prepareTest() throws Exception {
		doInHibernate( this::sessionFactory, s -> {
			s.createNativeQuery( "DROP TABLE IF EXISTS keyholder" ).executeUpdate();
			s.createNativeQuery( "CREATE TABLE keyholder(key integer)" ).executeUpdate();

			s.createNativeQuery( "DROP TABLE IF EXISTS category" ).executeUpdate();
			s.createNativeQuery( "CREATE TABLE category(id integer, name varchar)" ).executeUpdate();

			s.createNativeQuery( "DROP TABLE IF EXISTS product2" ).executeUpdate();
			s.createNativeQuery( "CREATE TABLE product2(id integer, category_id integer, description varchar)" )
					.executeUpdate();
		} );
	}

	@Test
	public void testPaginationWithCTEQueryNoOffset() {
		doInHibernate( this::sessionFactory, session -> {
			for ( int i = 0; i < 20; ++i ) {
				session.persist( new Product2( i, "Product" + i ) );
			}
			session.flush();
			session.clear();

			List<?> results = session
					.createNativeQuery( "WITH a AS (SELECT description FROM Product2) SELECT description FROM a" )
					.setMaxResults( 10 )
					.getResultList();

			assertEquals( 10, results.size() );
			assertEquals( String.class, results.get( 0 ).getClass() );
		} );
	}

	@Test
	public void testPaginationWithCTEQueryNoOffsetNewLine() {
		doInHibernate( this::sessionFactory, session -> {
			for ( int i = 0; i < 20; ++i ) {
				session.persist( new Product2( i, "Product" + i ) );
			}
			session.flush();
			session.clear();

			List<?> results = session
					.createNativeQuery(
							"WITH a AS (\n" +
									"\tSELECT description \n" +
									"\tFROM Product2\n" +
									") \n" +
									"SELECT description FROM a" )
					.setMaxResults( 10 )
					.getResultList();

			assertEquals( 10, results.size() );
			assertEquals( String.class, results.get( 0 ).getClass() );
		} );
	}

	@Test
	public void testPaginationWithCTEQueryWithOffsetAndOrderBy() {
		doInHibernate( this::sessionFactory, session -> {
			for ( int i = 0; i < 20; ++i ) {
				session.persist( new Product2( i, "Product" + i ) );
			}
			session.flush();
			session.clear();

			List<?> results = session
					.createNativeQuery(
							"WITH a AS (SELECT id, description FROM Product2) SELECT id, description FROM a ORDER BY id DESC" )
					.setFirstResult( 5 )
					.setMaxResults( 10 )
					.getResultList();
			assertEquals( 10, results.size() );

			final Object[] row = (Object[]) results.get( 0 );
			assertEquals( 2, row.length );
			assertEquals( Integer.class, row[0].getClass() );
			assertEquals( String.class, row[1].getClass() );
			assertEquals( 14, row[0] );
			assertEquals( "Product14", row[1] );
		} );
	}

	@Test
	public void testPaginationWithCTEQueryWithOffset() {
		doInHibernate( this::sessionFactory, session -> {
			for ( int i = 0; i < 20; ++i ) {
				session.persist( new Product2( i, "Product" + i ) );
			}
			session.flush();
			session.clear();

			List<?> results = session
					.createNativeQuery( "WITH a AS (SELECT id, description FROM Product2) SELECT id, description FROM a" )
					.setFirstResult( 5 )
					.setMaxResults( 10 )
					.getResultList();

			assertEquals( 10, results.size() );

			final Object[] row = (Object[]) results.get( 0 );
			assertEquals( 2, row.length );
			assertEquals( Integer.class, row[0].getClass() );
			assertEquals( String.class, row[1].getClass() );
		} );
	}

	@Test
	public void testPaginationWithScalarQuery() {
		doInHibernate( this::sessionFactory, session -> {
			for ( int i = 0; i < 10; i++ ) {
				session.persist( new Product2( i, "Kit" + i ) );
			}
			session.flush();
			session.clear();

			List<?> list = session.createNativeQuery(
					"select id from Product2 where description like 'Kit%' order by id" )
					.list();
			assertEquals( Integer.class, list.get( 0 ).getClass() ); // scalar result is an Integer

			list = session.createNativeQuery( "select id from Product2 where description like 'Kit%' order by id" )
					.setFirstResult( 2 )
					.setMaxResults( 2 )
					.list();
			assertEquals( Integer.class, list.get( 0 ).getClass() );

			// same once again with alias
			list = session.createNativeQuery(
					"select id as myint from Product2 where description like 'Kit%' order by id asc" )
					.setFirstResult( 2 )
					.setMaxResults( 2 )
					.list();
			assertEquals( Integer.class, list.get( 0 ).getClass() );
		} );
	}

	@Test
	public void testPaginationWithHQLProjection() {
		doInHibernate( this::sessionFactory, session -> {
			for ( int i = 10; i < 20; i++ ) {
				session.persist( new Product2( i, "Kit" + i ) );
			}
			session.flush();
			session.clear();

			List<?> list = session.createQuery(
					"select id, description as descr, (select max(id) from Product2) as maximum from Product2"
			).setFirstResult( 2 ).setMaxResults( 2 ).list();
			assertEquals( 19, ( (Object[]) list.get( 1 ) )[2] );

			list = session.createQuery(
					"select id, description, (select max(id) from Product2) from Product2 order by id" )
					.setFirstResult( 2 ).setMaxResults( 2 ).list();
			assertEquals( 2, list.size() );
			assertArrayEquals( new Object[] { 12, "Kit12", 19 }, (Object[]) list.get( 0 ) );
			assertArrayEquals( new Object[] { 13, "Kit13", 19 }, (Object[]) list.get( 1 ) );
		} );
	}

	@Test
	public void testPaginationWithHQL() {
		doInHibernate( this::sessionFactory, session -> {
			for ( int i = 20; i < 30; i++ ) {
				session.persist( new Product2( i, "Kit" + i ) );
			}
			session.flush();
			session.clear();

			List<?> list = session.createQuery( "from Product2 order by id" )
					.setFirstResult( 3 )
					.setMaxResults( 2 )
					.list();
			assertEquals( Arrays.asList( new Product2( 23, "Kit23" ), new Product2( 24, "Kit24" ) ), list );
		} );
	}

	@Test
	public void testPaginationWithMaxOnly() {
		doInHibernate( this::sessionFactory, session -> {
			for ( int i = 30; i < 40; i++ ) {
				session.persist( new Product2( i, "Kit" + i ) );
			}
			session.flush();
			session.clear();

			List<?> list = session.createQuery( "from Product2 order by id" )
					.setFirstResult( 0 )
					.setMaxResults( 2 )
					.list();
			assertEquals( Arrays.asList( new Product2( 30, "Kit30" ), new Product2( 31, "Kit31" ) ), list );

			list = session.createQuery( "select distinct p from Product2 p order by p.id" ).setMaxResults( 1 ).list();
			assertEquals( Collections.singletonList( new Product2( 30, "Kit30" ) ), list );
		} );
	}

	@Test
	public void testPaginationWithAggregation() {
		doInHibernate( this::sessionFactory, session -> {
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
			@SuppressWarnings("unchecked")
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
		} );
	}

	@Test
	public void testPaginationWithCastOperator() {
		doInHibernate( this::sessionFactory, session -> {
			for ( int i = 40; i < 50; i++ ) {
				session.persist( new Product2( i, "Kit" + i ) );
			}
			session.flush();
			session.clear();

			@SuppressWarnings("unchecked")
			List<Object[]> list = session.createQuery(
					"select p.id, cast(p.id as string) as string_id from Product2 p order by p.id" )
					.setFirstResult( 1 ).setMaxResults( 2 ).list();
			assertEquals( 2, list.size() );
			assertArrayEquals( new Object[] { 41, "41" }, list.get( 0 ) );
			assertArrayEquals( new Object[] { 42, "42" }, list.get( 1 ) );
		} );
	}

	@Override
	protected java.lang.Class<?>[] getAnnotatedClasses() {
		return new java.lang.Class[] {
				Product2.class, Category.class
		};
	}
}
