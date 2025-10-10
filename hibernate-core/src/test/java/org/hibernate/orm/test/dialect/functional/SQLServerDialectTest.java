/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * used driver hibernate.connection.driver_class com.microsoft.sqlserver.jdbc.SQLServerDriver
 *
 * @author Guenther Demetz
 */
@RequiresDialect(SQLServerDialect.class)
@DomainModel(annotatedClasses = {Product2.class, Category.class, Folder.class, Contact.class, SQLServerDialectTest.KeyHolder.class})
@SessionFactory
@ServiceRegistry( settings = {@Setting(name = AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED, value = "true")} )
public class SQLServerDialectTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) throws InterruptedException {
		//SQL Server Driver does not deallocate connections right away
		Thread.sleep( 100 );
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	@JiraKey(value = "HHH-8916")
	public void testPaginationWithCTEQueryNoOffset(SessionFactoryScope scope) {
		// This used to throw SQLServerException: Incorrect syntax near 'SEL'
		scope.inTransaction( session -> {
			for ( int i = 0; i < 20; ++i ) {
				session.persist( new Product2( i, "Product" + i ) );
			}
			session.flush();
			session.clear();

			List results = session
					.createNativeQuery( "WITH a AS (SELECT description FROM Product2) SELECT description FROM a" )
					.setMaxResults( 10 )
					.getResultList();

			assertEquals( 10, results.size() );
			assertEquals( String.class, results.get( 0 ).getClass() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-8916")
	public void testPaginationWithCTEQueryNoOffsetNewLine(SessionFactoryScope scope) {
		// This used to throw SQLServerException: Incorrect syntax near 'SEL'
		scope.inTransaction( session -> {
			for ( int i = 0; i < 20; ++i ) {
				session.persist( new Product2( i, "Product" + i ) );
			}
			session.flush();
			session.clear();

			List results = session
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
	@JiraKey(value = "HHH-8916")
	public void testPaginationWithCTEQueryWithOffsetAndOrderBy(SessionFactoryScope scope) {
		// This used to throw a StringIndexOutOfBoundsException
		scope.inTransaction( session -> {
			for ( int i = 0; i < 20; ++i ) {
				session.persist( new Product2( i, "Product" + i ) );
			}
			session.flush();
			session.clear();

			List results = session
					.createNativeQuery( "WITH a AS (SELECT id, description FROM Product2) SELECT id, description FROM a ORDER BY id DESC" )
					.setFirstResult( 5 )
					.setMaxResults( 10 )
					.getResultList();
			assertEquals( 10, results.size() );

			final Object[] row = (Object[]) results.get( 0 );
			assertEquals( 2, row.length );
			assertEquals( Integer.class, row[ 0 ].getClass() );
			assertEquals( String.class, row[ 1 ].getClass() );
			assertEquals( 14, row[0] );
			assertEquals( "Product14", row[1] );
		} );
	}

	@Test
	@JiraKey(value = "HHH-8916")
	public void testPaginationWithCTEQueryWithOffset(SessionFactoryScope scope) {
		// This used to throw a StringIndexOutOfBoundsException
		scope.inTransaction( session -> {
			for ( int i = 0; i < 20; ++i ) {
				session.persist( new Product2( i, "Product" + i ) );
			}
			session.flush();
			session.clear();

			List results = session
					.createNativeQuery( "WITH a AS (SELECT id, description FROM Product2) SELECT id, description FROM a" )
					.setFirstResult( 5 )
					.setMaxResults( 10 )
					.getResultList();

			assertEquals( 10, results.size() );

			final Object[] row = (Object[]) results.get( 0 );
			assertEquals( 2, row.length );
			assertEquals( Integer.class, row[ 0 ].getClass() );
			assertEquals( String.class, row[ 1 ].getClass() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-7369")
	public void testPaginationWithScalarQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int i = 0; i < 10; i++ ) {
				session.persist( new Product2( i, "Kit" + i ) );
			}
			session.flush();
			session.clear();

			List list = session.createNativeQuery( "select id from Product2 where description like 'Kit%' order by id" ).list();
			assertEquals(Integer.class, list.get(0).getClass()); // scalar result is an Integer

			list = session.createNativeQuery( "select id from Product2 where description like 'Kit%' order by id" ).setFirstResult( 2 ).setMaxResults( 2 ).list();
			assertEquals(Integer.class, list.get(0).getClass()); // this fails without patch, as result suddenly has become an array

			// same once again with alias
			list = session.createNativeQuery( "select id as myint from Product2 where description like 'Kit%' order by id asc" ).setFirstResult( 2 ).setMaxResults( 2 ).list();
			assertEquals(Integer.class, list.get(0).getClass());
		} );
	}

	@Test
	@JiraKey(value = "HHH-7368")
	public void testPaginationWithTrailingSemicolon(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createNativeQuery( "select id from Product2 where description like 'Kit%' order by id;" )
					.setFirstResult( 2 ).setMaxResults( 2 ).list();
		} );
	}

	@Test
	public void testPaginationWithHQLProjection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
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
		} );
	}

	@Test
	public void testPaginationWithHQL(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int i = 20; i < 30; i++ ) {
				session.persist( new Product2( i, "Kit" + i ) );
			}
			session.flush();
			session.clear();

			List list = session.createQuery( "from Product2 order by id" ).setFirstResult( 3 ).setMaxResults( 2 ).list();
			assertEquals( Arrays.asList( new Product2( 23, "Kit23" ), new Product2( 24, "Kit24" ) ), list );
		} );
	}

	@Test
	@JiraKey(value = "HHH-7370")
	public void testPaginationWithMaxOnly(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int i = 30; i < 40; i++ ) {
				session.persist( new Product2( i, "Kit" + i ) );
			}
			session.flush();
			session.clear();

			List list = session.createQuery( "from Product2 order by id" ).setFirstResult( 0 ).setMaxResults( 2 ).list();
			assertEquals( Arrays.asList( new Product2( 30, "Kit30" ), new Product2( 31, "Kit31" ) ), list );

			list = session.createQuery( "select distinct p from Product2 p order by p.id" ).setMaxResults( 1 ).list();
			assertEquals( Collections.singletonList( new Product2( 30, "Kit30" ) ), list );
		} );
	}

	@Test
	@JiraKey(value = "HHH-6627")
	public void testPaginationWithAggregation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
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

			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<Object[]> criteria = criteriaBuilder.createQuery( Object[].class );
			Root<Category> root = criteria.from( Category.class );
			Join<Object, Object> products = root.join( "products", JoinType.INNER );
			criteria.multiselect( root.get( "id" ), criteriaBuilder.countDistinct( products.get( "id" ) ) );
			criteria.groupBy( root.get( "id" ) );
			criteria.orderBy( criteriaBuilder.asc( root.get( "id" ) ) );
			Query<Object[]> query = session.createQuery( criteria );

			List<Object[]> result = query.setFirstResult( 1 ).setMaxResults( 3 ).list();

			assertEquals( 2, result.size() );
			assertArrayEquals( new Object[] { 2, 2L }, result.get( 0 ) ); // two products of second category
			assertArrayEquals( new Object[] { 3, 1L }, result.get( 1 ) ); // one products of third category
		} );
	}

	@Test
	@JiraKey(value = "HHH-7752")
	public void testPaginationWithFormulaSubquery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
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
			session.refresh( folder1 );
			session.refresh( folder2 );
			session.clear();

			List<Long> folderCount = session.createQuery( "select count(distinct f) from Folder f" ).setMaxResults( 1 ).list();
			assertEquals( Arrays.asList( 3L ), folderCount );

			List<Folder> distinctFolders = session.createQuery( "select distinct f from Folder f order by f.id desc" )
					.setFirstResult( 1 ).setMaxResults( 2 ).list();
			assertEquals( Arrays.asList( folder2, folder1 ), distinctFolders );
		} );
	}

	@Test
	@JiraKey(value = "HHH-7781")
	public void testPaginationWithCastOperator(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
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
		} );
	}

	@Test
	@JiraKey(value = "HHH-3961")
	public void testLockNowaitSqlServer(SessionFactoryScope scope) {
		scope.inSession( s -> {
			s.beginTransaction();
			final Product2 kit = new Product2();
			kit.id = 4000;
			kit.description = "m";
			s.persist( kit );
			s.getTransaction().commit();

			final Transaction tx = s.beginTransaction();
			scope.inSession(  s2 -> {
				s2.beginTransaction();

				Product2 kit2 = s2.byId( Product2.class ).load( kit.id );

				kit.description = "change!";

				s.flush(); // creates write lock on kit until we end the transaction

				Thread thread = new Thread(
						() -> {
							try {
								Thread.sleep( TimeUnit.SECONDS.toMillis( 1 ) );
							}
							catch (InterruptedException e) {
								throw new RuntimeException( e );
							}
							tx.commit();
						}
				);

				LockOptions opt = new LockOptions( LockMode.UPGRADE_NOWAIT );
				opt.setTimeOut( 0 ); // seems useless
				long start = System.currentTimeMillis();
				thread.start();
				try {
					s2.lock( kit2, opt );
				}
				catch ( PessimisticEntityLockException e ) {
					assertTrue( e.getCause() instanceof LockTimeoutException );
				}
				long end = System.currentTimeMillis();
				try {
					thread.join();
				}
				catch (InterruptedException e) {
					throw new RuntimeException( e );
				}
				long differenceInMillis = end - start;
				assertTrue(
						differenceInMillis < 2000,
						"Lock NoWait blocked for " + differenceInMillis + " ms, this is definitely too much for Nowait"
								);

				s2.getTransaction().rollback();
			} );

			s.getTransaction().begin();
			s.remove( kit );
			s.getTransaction().commit();
		} );
	}


	@Test
	@JiraKey(value = "HHH-10879")
	public void testKeyReservedKeyword(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final KeyHolder keyHolder = new KeyHolder();
			keyHolder.key = 4000;
			session.persist( keyHolder );
		} );
	}

	@Entity(name = "KeyHolder")
	public static class KeyHolder {
		@Id
		public Integer key;
	}
}
