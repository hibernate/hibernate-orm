/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.QueryHint;
import jakarta.persistence.Table;
import org.hibernate.Locking;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.jpa.AvailableHints;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.LockMode.PESSIMISTIC_WRITE;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(OracleDialect.class)
@JiraKey(value = "HHH-9486")
@DomainModel(annotatedClasses = {
		OracleFollowOnLockingTest.Product.class,
		OracleFollowOnLockingTest.Vehicle.class,
		OracleFollowOnLockingTest.SportsCar.class,
		OracleFollowOnLockingTest.Truck.class,
		OracleFollowOnLockingTest.Customer.class,
		OracleFollowOnLockingTest.Purchase.class
})
@SessionFactory(useCollectingStatementInspector = true)
public class OracleFollowOnLockingTest {
	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
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

			final Customer billyBob = new Customer( 1, "Billy Bob" );
			final Purchase purchase1 = new Purchase( 1, Instant.now(), 123.00, billyBob );
			final Purchase purchase2 = new Purchase( 2, Instant.now(), 789.00, billyBob );
			session.persist( billyBob );
			session.persist( purchase1 );
			session.persist( purchase2 );
		} );

	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	void testLockAcrossJoin(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			final List<Customer> customers = session.createSelectionQuery(
							"select c from Customer c outer join fetch c.purchases", Customer.class )
					.setHibernateLockMode( PESSIMISTIC_WRITE )
					.setLockScope( Locking.Scope.ROOT_ONLY )
					.getResultList();

			assertThat( customers ).hasSize( 1 );
			// See the note on `OracleLockingSupport#getOuterJoinLockingType`.
			// As of 23 at least Oracle does support locking across joins - I've verified this locally.
			// Need CI to tell us about previous versions...
			// there should be no follow-on locking (again as of 23 at least)
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " for update of " );
		} );
	}

	@Test
	public void testPessimisticLockWithMaxResultsThenNoFollowOnLocking(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			List<Product> products = session.createQuery( "select p from Product p", Product.class )
					.setHibernateLockMode( PESSIMISTIC_WRITE )
					.setMaxResults( 10 )
					.getResultList();

			assertThat( products ).hasSize( 10 );
			// there should be no follow-on locking - so just 1
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		} );
	}

	@Test
	public void testPessimisticLockWithFirstResultThenFollowOnLocking(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			List<Product> products = session.createQuery( "select p from Product p", Product.class )
					.setHibernateLockMode( PESSIMISTIC_WRITE )
					.setFirstResult( 40 )
					.setMaxResults( 10 )
					.getResultList();

			assertThat( products ).hasSize( 10 );
			// there should be no follow-on locking - so just 1
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		} );
	}

	@Test
	public void testPessimisticLockWithFirstResultAndJoinThenFollowOnLocking(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			List<Product> products = session.createQuery( "select p from Product p left join p.vehicle v on v.id is null", Product.class )
					.setHibernateLockMode( PESSIMISTIC_WRITE )
					.setFirstResult( 40 )
					.setMaxResults( 10 )
					.getResultList();

			assertThat( products ).hasSize( 10 );
			// this should trigger follow-on locking - so 2 (initial query, lock)
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " join " );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).doesNotContain( " for update of " );
			assertThat( sqlCollector.getSqlQueries().get( 1 ) ).doesNotContain( " join " );
			assertThat( sqlCollector.getSqlQueries().get( 1 ) ).contains( " for update of " );
		} );
	}

	@Test
	public void testPessimisticLockWithNamedQueryExplicitlyEnablingFollowOnLockingThenFollowOnLocking(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			List<Product> products = session.createNamedQuery( "product_by_name", Product.class )
					.getResultList();

			assertThat( products ).hasSize( 50 );
			// this should trigger follow-on locking - so 2 (initial query, lock)
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
		} );
	}

	@Test
	public void testPessimisticLockWithCountDistinctThenFollowOnLocking(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			List<Product> products = session.createQuery("select p from Product p where ( select count(distinct p1.id) from Product p1 ) > 0 ", Product.class )
					.setHibernateLockMode( PESSIMISTIC_WRITE )
					.setFollowOnStrategy( Locking.FollowOn.DISALLOW )
					.getResultList();

			assertThat( products ).hasSize( 50 );
			// we disallow follow-on locking - so 1 (initial query)
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		} );
	}

	@Test
	public void testPessimisticLockWithFirstResultWhileExplicitlyDisablingFollowOnLockingThenFails(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			List<Product> products = session.createQuery( "select p from Product p", Product.class )
					.setHibernateLockMode( PESSIMISTIC_WRITE )
					.setFollowOnStrategy( Locking.FollowOn.DISALLOW )
					.setFirstResult( 40 )
					.setMaxResults( 10 )
					.getResultList();

			assertThat( products ).hasSize( 10 );
			// we disallow follow-on locking - so 1 (initial query)
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		} );
	}

	@Test
	public void testPessimisticLockWithFirstResultAndJoinWhileExplicitlyDisablingFollowOnLockingThenFails(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			try {
				List<Product> products = session.createQuery( "select p from Product p left join p.vehicle v on v.id is null", Product.class )
						.setHibernateLockMode( PESSIMISTIC_WRITE )
						.setFollowOnStrategy( Locking.FollowOn.DISALLOW )
						.setFirstResult( 40 )
						.setMaxResults( 10 )
						.getResultList();
				fail(
						"Should throw exception since Oracle does not support LIMIT if follow on locking is disabled" );
			}
			catch ( IllegalStateException expected ) {
				Assertions.assertEquals( IllegalQueryOperationException.class, expected.getCause().getClass() );
				assertThat( expected.getCause().getMessage() )
						.contains( "Locking with OFFSET/FETCH is not supported" );
			}
		} );
	}

	@Test
	public void testPessimisticLockWithFirstResultsWhileExplicitlyEnablingFollowOnLockingThenFollowOnLocking(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			List<Product> products = session.createQuery( "select p from Product p", Product.class )
					.setHibernateLockMode( PESSIMISTIC_WRITE )
					.setFollowOnStrategy( Locking.FollowOn.FORCE )
					.setFirstResult( 40 )
					.setMaxResults( 10 )
					.getResultList();

			assertThat( products ).hasSize( 10 );
			// this should trigger follow-on locking - so 2 (initial query, lock)
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
		} );
	}


	@Test
	@JiraKey(value = "HHH-16433")
	public void testPessimisticLockWithOrderByThenNoFollowOnLocking(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			List<Product> products = session.createQuery( "select p from Product p order by p.id", Product.class )
					.setHibernateLockMode( PESSIMISTIC_WRITE )
					.getResultList();

			assertThat( products ).hasSize( 50 );
			//assertEquals( 1, sqlStatementInterceptor.getSqlQueries().size() );
			// this should not trigger follow-on locking apparently - so 1 (initial query)
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		} );
	}

	@Test
	public void testPessimisticLockWithMaxResultsAndOrderByThenNoFollowOnLocking(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			List<Product> products = session.createQuery( "select p from Product p order by p.id", Product.class )
					.setHibernateLockMode( PESSIMISTIC_WRITE )
					.setMaxResults( 10 )
					.getResultList();

			assertThat( products ).hasSize( 10 );
			// this should not trigger follow-on locking
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		} );
	}

	@Test
	public void testPessimisticLockWithMaxResultsAndOrderByWhileExplicitlyDisablingFollowOnLocking(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			List<Product> products = session.createQuery( "select p from Product p order by p.id", Product.class )
					.setHibernateLockMode( PESSIMISTIC_WRITE )
					.setFollowOnStrategy( Locking.FollowOn.DISALLOW )
					.setMaxResults( 10 )
					.getResultList();

			assertThat( products ).hasSize( 10 );
			//assertEquals( 1, sqlStatementInterceptor.getSqlQueries().size() );
			// this should not trigger follow-on locking apparently - so 1 (initial query)
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		} );
	}

	@Test
	public void testPessimisticLockWithMaxResultsAndOrderByWhileExplicitlyEnablingFollowOnLockingThenFollowOnLocking(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			List<Product> products = session.createQuery( "select p from Product p order by p.id", Product.class )
					.setHibernateLockMode( PESSIMISTIC_WRITE )
					.setFollowOnStrategy( Locking.FollowOn.FORCE )
					.setMaxResults( 10 )
					.getResultList();

			assertThat( products ).hasSize( 10 );
			// this should trigger follow-on locking - so 2 (initial query, locking)
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );

		} );
	}

	@Test
	public void testPessimisticLockWithDistinctThenFollowOnLocking(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			List<Product> products = session.createQuery( "select distinct p from Product p", Product.class )
					.setHibernateLockMode( PESSIMISTIC_WRITE )
					.getResultList();

			assertThat( products ).hasSize( 50 );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).doesNotContain( " for update " );
			assertThat( sqlCollector.getSqlQueries().get( 1 ) ).contains( " where tbl.id in (?," );
			assertThat( sqlCollector.getSqlQueries().get( 1 ) ).contains( " for update " );
		} );
	}

	@Test
	public void testPessimisticLockWithDistinctWhileExplicitlyDisablingFollowOnLockingThenFails(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			try {
				session.createQuery( "select distinct p from Product p where p.id > 40", Product.class )
						.setHibernateLockMode( PESSIMISTIC_WRITE )
						.setFollowOnStrategy( Locking.FollowOn.DISALLOW )
						.getResultList();
				fail( "Should throw exception since Oracle does not support DISTINCT if follow on locking is disabled" );
			}
			catch ( IllegalStateException expected ) {
				assertThat( expected.getCause() ).isInstanceOf( IllegalQueryOperationException.class );
				assertThat( expected.getCause().getMessage() ).contains( "Locking with DISTINCT is not supported" );
			}

		} );
	}

	@Test
	public void testPessimisticLockWithDistinctWhileExplicitlyEnablingFollowOnLockingThenFollowOnLocking(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			List<Product> products = session.createQuery( "select distinct p from Product p where p.id > 40" )
					.setHibernateLockMode( PESSIMISTIC_WRITE )
					.setFollowOnStrategy( Locking.FollowOn.FORCE )
					.setMaxResults( 10 )
					.getResultList();

			assertThat( products ).hasSize( 10 );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).doesNotContain( " for update " );
			assertThat( sqlCollector.getSqlQueries().get( 1 ) ).contains( " where tbl.id in (?," );
			assertThat( sqlCollector.getSqlQueries().get( 1 ) ).contains( " for update " );
		} );
	}

	@Test
	public void testPessimisticLockWithGroupByThenFollowOnLocking(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			final String qry = """
					select count(p), p \
					from Product p \
					group by p.id, p.name, p.vehicle.id""";
			List<Object[]> products = session.createQuery( qry, Object[].class )
					.setHibernateLockMode( PESSIMISTIC_WRITE )
					.getResultList();

			assertThat( products ).hasSize( 50 );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).doesNotContain( " for update " );
			assertThat( sqlCollector.getSqlQueries().get( 1 ) ).contains( " where tbl.id in (?," );
			assertThat( sqlCollector.getSqlQueries().get( 1 ) ).contains( " for update " );
		} );
	}

	@Test
	public void testPessimisticLockWithGroupByWhileExplicitlyDisablingFollowOnLockingThenFails(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			try {
				final String qry = """
					select count(p), p \
					from Product p \
					group by p.id, p.name, p.vehicle.id""";
				List<Object[]> products = session.createQuery( qry, Object[].class )
						.setHibernateLockMode( PESSIMISTIC_WRITE )
						.setFollowOnStrategy( Locking.FollowOn.DISALLOW )
						.getResultList();
				fail( "Should throw exception since Oracle does not support GROUP BY if follow on locking is disabled" );
			}
			catch ( IllegalStateException expected ) {
				assertThat( expected.getCause() ).isInstanceOf( IllegalQueryOperationException.class );
				assertThat( expected.getCause().getMessage() ).contains( "Locking with GROUP BY is not supported" );
			}
		} );
	}

	@Test
	public void testPessimisticLockWithGroupByWhileExplicitlyEnablingFollowOnLockingThenFollowOnLocking(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			final String qry = """
					select count(p), p \
					from Product p \
					group by p.id, p.name, p.vehicle.id""";
			List<Object[]> products = session.createQuery( qry, Object[].class )
					.setHibernateLockMode( PESSIMISTIC_WRITE )
					.setFollowOnStrategy( Locking.FollowOn.FORCE )
					.getResultList();

			assertThat( products ).hasSize( 50 );
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).doesNotContain( " for update " );
			assertThat( sqlCollector.getSqlQueries().get( 1 ) ).contains( " where tbl.id in (?," );
			assertThat( sqlCollector.getSqlQueries().get( 1 ) ).contains( " for update " );
		} );
	}

	@Test
	public void testPessimisticLockWithUnionThenFollowOnLocking(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			List<Vehicle> vehicles = session.createQuery( "select v from Vehicle v", Vehicle.class )
					.setHibernateLockMode( PESSIMISTIC_WRITE )
					.getResultList();

			assertThat( vehicles ).hasSize( 3 );
			vehicles.forEach( (vehicle) -> {
				assertThat( session.getCurrentLockMode( vehicle ) ).isEqualTo( PESSIMISTIC_WRITE );
			} );
			// follow on locking due to the UNION - initial query, lock trucks, lock sports cars
			assertThat( sqlCollector.getSqlQueries() ).hasSize( 3 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " union all " );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).doesNotContain( " for update " );
			assertThat( sqlCollector.getSqlQueries().get( 1 ) ).contains( " where tbl.id in (?" );
			assertThat( sqlCollector.getSqlQueries().get( 1 ) ).contains( " for update " );
			assertThat( sqlCollector.getSqlQueries().get( 2 ) ).contains( " where tbl.id in (?" );
			assertThat( sqlCollector.getSqlQueries().get( 2 ) ).contains( " for update " );
		} );
	}

	@Test
	public void testPessimisticLockWithUnionWhileExplicitlyDisablingFollowOnLockingThenFails(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();

			try {
				List<Vehicle> vehicles = session.createQuery( "select v from Vehicle v", Vehicle.class )
						.setHibernateLockMode( PESSIMISTIC_WRITE )
						.setFollowOnStrategy( Locking.FollowOn.DISALLOW )
						.getResultList();
				fail( "Should throw exception since Oracle does not support UNION if follow on locking is disabled" );
			}
			catch ( IllegalStateException expected ) {
				assertThat( expected.getCause() ).isInstanceOf( IllegalQueryOperationException.class );
				assertThat( expected.getCause().getMessage() ).contains( "Locking with set operators is not supported" );
			}
		} );
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

	@Entity(name="Customer")
	@Table(name="customers")
	public static class Customer {
		@Id
		private Integer id;
		private String name;
		@OneToMany(mappedBy = "customer")
		private Set<Purchase> purchases;

		public Customer() {
		}

		public Customer(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public void addPurchase(Purchase purchase) {
			if ( purchases == null ) {
				purchases = new HashSet<>();
			}
			purchases.add( purchase );
		}
	}

	@Entity(name="Purchase")
	@Table(name="purchases")
	public static class Purchase {
		@Id
		private Integer id;
		@Column(name = "ts")
		private Instant timestamp;
		private double amount;
		@ManyToOne(optional = false)
		private Customer customer;

		public Purchase() {
		}

		public Purchase(Integer id, Instant timestamp, double amount, Customer customer) {
			this.id = id;
			this.timestamp = timestamp;
			this.amount = amount;
			this.customer = customer;

			customer.addPurchase( this );
		}
	}
}
