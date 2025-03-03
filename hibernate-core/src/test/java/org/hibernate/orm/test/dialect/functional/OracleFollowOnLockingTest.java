/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.jpa.AvailableHints;
import org.hibernate.query.IllegalQueryOperationException;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(OracleDialect.class)
@JiraKey(value = "HHH-9486")
public class OracleFollowOnLockingTest extends
		BaseNonConfigCoreFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		sqlStatementInterceptor = new SQLStatementInterceptor( sfb );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			Product.class,
			Vehicle.class,
			SportsCar.class,
			Truck.class
		};
	}

	@Before
	public void init() {
		final Session session = openSession();
		session.beginTransaction();

		for ( int i = 0; i < 50; i++ ) {
			Product product = new Product();
			product.name = "Product " + i % 10;
			session.persist( product );
		}

		Truck truck1 = new Truck();
		Truck truck2 = new Truck();
		SportsCar sportsCar1 = new SportsCar();
		session.persist( truck1 );
		session.persist( truck2 );
		session.persist( sportsCar1 );

		session.getTransaction().commit();
		session.close();
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	public void testPessimisticLockWithMaxResultsThenNoFollowOnLocking() {

		final Session session = openSession();
		session.beginTransaction();

		sqlStatementInterceptor.getSqlQueries().clear();

		List<Product> products =
				session.createQuery(
						"select p from Product p", Product.class )
						.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ) )
						.setMaxResults( 10 )
						.getResultList();

		assertEquals( 10, products.size() );
		assertEquals( 1, sqlStatementInterceptor.getSqlQueries().size() );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPessimisticLockWithFirstResultThenFollowOnLocking() {

		final Session session = openSession();
		session.beginTransaction();

		sqlStatementInterceptor.getSqlQueries().clear();

		List<Product> products =
				session.createQuery(
						"select p from Product p", Product.class )
						.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ) )
						.setFirstResult( 40 )
						.setMaxResults( 10 )
						.getResultList();

		assertEquals( 10, products.size() );
		assertEquals( 1, sqlStatementInterceptor.getSqlQueries().size() );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPessimisticLockWithFirstResultAndJoinThenFollowOnLocking() {

		final Session session = openSession();
		session.beginTransaction();

		sqlStatementInterceptor.getSqlQueries().clear();

		List<Product> products =
				session.createQuery(
								"select p from Product p left join p.vehicle v on v.id is null", Product.class )
						.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ) )
						.setFirstResult( 40 )
						.setMaxResults( 10 )
						.getResultList();

		assertEquals( 10, products.size() );
		assertEquals( 11, sqlStatementInterceptor.getSqlQueries().size() );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPessimisticLockWithNamedQueryExplicitlyEnablingFollowOnLockingThenFollowOnLocking() {

		final Session session = openSession();
		session.beginTransaction();

		sqlStatementInterceptor.getSqlQueries().clear();

		List<Product> products = session.createNamedQuery(
			"product_by_name", Product.class )
		.getResultList();

		assertEquals( 50, products.size() );
		assertEquals( 51, sqlStatementInterceptor.getSqlQueries().size() );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPessimisticLockWithCountDistinctThenFollowOnLocking() {

		final Session session = openSession();
		session.beginTransaction();

		sqlStatementInterceptor.getSqlQueries().clear();

		List<Product> products = session.createQuery(
			"select p from Product p where ( select count(distinct p1.id) from Product p1 ) > 0 ", Product.class )
		.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ).setFollowOnLocking( false ) )
		.getResultList();

		assertEquals( 50, products.size() );
		assertEquals( 1, sqlStatementInterceptor.getSqlQueries().size() );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPessimisticLockWithFirstResultWhileExplicitlyDisablingFollowOnLockingThenFails() {

		final Session session = openSession();
		session.beginTransaction();

		sqlStatementInterceptor.getSqlQueries().clear();

		List<Product> products = session.createQuery( "select p from Product p", Product.class )
				.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE )
										.setFollowOnLocking( false ) )
				.setFirstResult( 40 )
				.setMaxResults( 10 )
				.getResultList();

		assertEquals( 10, products.size() );
		assertEquals( 1, sqlStatementInterceptor.getSqlQueries().size() );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPessimisticLockWithFirstResultAndJoinWhileExplicitlyDisablingFollowOnLockingThenFails() {

		final Session session = openSession();
		session.beginTransaction();

		sqlStatementInterceptor.getSqlQueries().clear();

		try {
			List<Product> products =
					session.createQuery(
									"select p from Product p left join p.vehicle v on v.id is null", Product.class )
							.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE )
													.setFollowOnLocking( false ) )
							.setFirstResult( 40 )
							.setMaxResults( 10 )
							.getResultList();
			fail( "Should throw exception since Oracle does not support ORDER BY if follow on locking is disabled" );
		}
		catch ( IllegalStateException expected ) {
			assertEquals(
					IllegalQueryOperationException.class,
					expected.getCause().getClass()
			);
			assertThat(
					expected.getCause().getMessage(),
					containsString( "Locking with OFFSET/FETCH is not supported" )
			);
		}
	}

	@Test
	public void testPessimisticLockWithFirstResultsWhileExplicitlyEnablingFollowOnLockingThenFollowOnLocking() {

		final Session session = openSession();
		session.beginTransaction();

		sqlStatementInterceptor.getSqlQueries().clear();

		List<Product> products =
				session.createQuery(
						"select p from Product p", Product.class )
						.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE )
												.setFollowOnLocking( true ) )
						.setFirstResult( 40 )
						.setMaxResults( 10 )
						.getResultList();

		assertEquals( 10, products.size() );
		assertEquals( 11, sqlStatementInterceptor.getSqlQueries().size() );

		session.getTransaction().commit();
		session.close();
	}


	@Test
	@JiraKey(value = "HHH-16433")
	public void testPessimisticLockWithOrderByThenNoFollowOnLocking() {

		final Session session = openSession();
		session.beginTransaction();

		sqlStatementInterceptor.getSqlQueries().clear();

		List<Product> products =
				session.createQuery(
						"select p from Product p order by p.id", Product.class )
						.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ) )
						.getResultList();

		assertTrue( products.size() > 1 );
		assertEquals( 1, sqlStatementInterceptor.getSqlQueries().size() );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPessimisticLockWithMaxResultsAndOrderByThenNoFollowOnLocking() {

		final Session session = openSession();
		session.beginTransaction();

		sqlStatementInterceptor.getSqlQueries().clear();

		List<Product> products =
				session.createQuery(
						"select p from Product p order by p.id", Product.class )
						.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ) )
						.setMaxResults( 10 )
						.getResultList();

		assertEquals( 10, products.size() );
		assertEquals( 1, sqlStatementInterceptor.getSqlQueries().size() );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPessimisticLockWithMaxResultsAndOrderByWhileExplicitlyDisablingFollowOnLocking() {

		final Session session = openSession();
		session.beginTransaction();

		sqlStatementInterceptor.getSqlQueries().clear();

		List<Product> products =
				session.createQuery(
						"select p from Product p order by p.id",
						Product.class
				)
						.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE )
												.setFollowOnLocking( false ) )
						.setMaxResults( 10 )
						.getResultList();
		assertEquals( 10, products.size() );
		assertEquals( 1, sqlStatementInterceptor.getSqlQueries().size() );
	}

	@Test
	public void testPessimisticLockWithMaxResultsAndOrderByWhileExplicitlyEnablingFollowOnLockingThenFollowOnLocking() {

		final Session session = openSession();
		session.beginTransaction();

		sqlStatementInterceptor.getSqlQueries().clear();

		List<Product> products =
				session.createQuery(
						"select p from Product p order by p.id", Product.class )
						.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE )
												.setFollowOnLocking( true ) )
						.setMaxResults( 10 )
						.getResultList();

		assertEquals( 10, products.size() );
		assertEquals( 11, sqlStatementInterceptor.getSqlQueries().size() );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPessimisticLockWithDistinctThenFollowOnLocking() {

		final Session session = openSession();
		session.beginTransaction();

		sqlStatementInterceptor.getSqlQueries().clear();

		List<Product> products =
				session.createQuery(
						"select distinct p from Product p",
						Product.class
				)
						.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ) )
						.getResultList();

		assertEquals( 50, products.size() );
		assertEquals( 51, sqlStatementInterceptor.getSqlQueries().size() );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPessimisticLockWithDistinctWhileExplicitlyDisablingFollowOnLockingThenFails() {

		final Session session = openSession();
		session.beginTransaction();

		sqlStatementInterceptor.getSqlQueries().clear();

		try {
			List<Product> products =
					session.createQuery(
							"select distinct p from Product p where p.id > 40",
							Product.class
					)
							.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE )
													.setFollowOnLocking( false ) )
							.getResultList();
			fail( "Should throw exception since Oracle does not support DISTINCT if follow on locking is disabled" );
		}
		catch ( IllegalStateException expected ) {
			assertEquals(
					IllegalQueryOperationException.class,
					expected.getCause().getClass()
			);
			assertTrue(
					expected.getCause().getMessage().contains(
							"Locking with DISTINCT is not supported"
					)
			);
		}
	}

	@Test
	public void testPessimisticLockWithDistinctWhileExplicitlyEnablingFollowOnLockingThenFollowOnLocking() {

		final Session session = openSession();
		session.beginTransaction();

		sqlStatementInterceptor.getSqlQueries().clear();

		List<Product> products =
				session.createQuery(
						"select distinct p from Product p where p.id > 40" )
						.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE )
												.setFollowOnLocking( true ) )
						.setMaxResults( 10 )
						.getResultList();

		assertEquals( 10, products.size() );
		assertEquals( 11, sqlStatementInterceptor.getSqlQueries().size() );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPessimisticLockWithGroupByThenFollowOnLocking() {

		final Session session = openSession();
		session.beginTransaction();

		sqlStatementInterceptor.getSqlQueries().clear();

		List<Object[]> products =
				session.createQuery(
						"select count(p), p " +
								"from Product p " +
								"group by p.id, p.name, p.vehicle.id " )
						.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ) )
						.getResultList();

		assertEquals( 50, products.size() );
		assertEquals( 51, sqlStatementInterceptor.getSqlQueries().size() );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPessimisticLockWithGroupByWhileExplicitlyDisablingFollowOnLockingThenFails() {

		final Session session = openSession();
		session.beginTransaction();

		sqlStatementInterceptor.getSqlQueries().clear();

		try {
			List<Object[]> products =
					session.createQuery(
							"select count(p), p " +
									"from Product p " +
									"group by p.id, p.name, p.vehicle.id " )
							.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE )
													.setFollowOnLocking( false ) )
							.getResultList();
			fail( "Should throw exception since Oracle does not support GROUP BY if follow on locking is disabled" );
		}
		catch ( IllegalStateException expected ) {
			assertEquals(
					IllegalQueryOperationException.class,
					expected.getCause().getClass()
			);
			assertTrue(
					expected.getCause().getMessage().contains(
							"Locking with GROUP BY is not supported"
					)
			);
		}
	}

	@Test
	public void testPessimisticLockWithGroupByWhileExplicitlyEnablingFollowOnLockingThenFollowOnLocking() {

		final Session session = openSession();
		session.beginTransaction();

		sqlStatementInterceptor.getSqlQueries().clear();

		List<Object[]> products =
				session.createQuery(
						"select count(p), p " +
								"from Product p " +
								"group by p.id, p.name, p.vehicle.id " )
						.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE )
												.setFollowOnLocking( true ) )
						.getResultList();

		assertEquals( 50, products.size() );
		assertEquals( 51, sqlStatementInterceptor.getSqlQueries().size() );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPessimisticLockWithUnionThenFollowOnLocking() {

		final Session session = openSession();
		session.beginTransaction();


		sqlStatementInterceptor.getSqlQueries().clear();

		List<Vehicle> vehicles = session.createQuery( "select v from Vehicle v" )
			.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ) )
			.getResultList();

		assertEquals( 3, vehicles.size() );
		assertEquals( 4, sqlStatementInterceptor.getSqlQueries().size() );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPessimisticLockWithUnionWhileExplicitlyDisablingFollowOnLockingThenFails() {

		final Session session = openSession();
		session.beginTransaction();

		sqlStatementInterceptor.getSqlQueries().clear();

		try {
			List<Vehicle> vehicles = session.createQuery( "select v from Vehicle v" )
					.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ).setFollowOnLocking( false ) )
					.getResultList();
			fail( "Should throw exception since Oracle does not support UNION if follow on locking is disabled" );
		}
		catch ( IllegalStateException expected ) {
			assertEquals(
					IllegalQueryOperationException.class,
					expected.getCause().getClass()
			);
			assertTrue(
					expected.getCause().getMessage().contains(
							"Locking with set operators is not supported"
					)
			);
		}
	}

	@NamedQuery(
			name = "product_by_name",
			query = "select p from Product p where p.name is not null",
			lockMode = LockModeType.PESSIMISTIC_WRITE,
			hints = @QueryHint(name = AvailableHints.HINT_FOLLOW_ON_LOCKING, value = "true")
	)
	@Entity(name = "Product")
	public static class Product {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private Vehicle vehicle;
	}

	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@Entity(name = "Vehicle")
	public static class Vehicle {

		@Id
		@GeneratedValue
		private Long id;

		private String name;
	}

	@Entity(name = "SportsCar")
	public static class SportsCar extends Vehicle {

		private double speed;
	}

	@Entity(name = "Truck")
	public static class Truck extends Vehicle {

		private double torque;
	}
}
