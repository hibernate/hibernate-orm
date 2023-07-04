package org.hibernate.orm.test.bytecode.enhancement.batch;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey("HHH-16890")
@RunWith( BytecodeEnhancerRunner.class )
public class BatchEntityWithSelectFetchTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Order.class,
				Product.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
	}

	@Before
	public void setupData() {
		Product cheese1 = new Product( 1l, "Cheese 1" );
		Product cheese2 = new Product( 2l, "Cheese 2" );
		Product cheese3 = new Product( 3l, "Cheese 3" );

		Order order = new Order( 1l, "Hibernate" );
		Order order2 = new Order( 2l, "Hibernate 2" );

		order.setProduct( cheese2 );
		order2.setProduct( cheese1 );

		inTransaction( s -> {
			s.persist( cheese1 );
			s.persist( cheese2 );
			s.persist( cheese3 );
			s.persist( order );
			s.persist( order2 );
		} );
	}

	@After
	public void tearDown(){
		inTransaction(
				session -> {
					session.createMutationQuery( "delete from Order" ).executeUpdate();
					session.createMutationQuery( "delete from Product" ).executeUpdate();
				}
		);
	}

	@Test
	public void testGetOrder() {
		inSession( s -> {
			s.getSessionFactory().getCache().evictAllRegions();

			Product product1 = s.getReference( Product.class, 1l );
			Product product2 = s.getReference( Product.class, 2l );

			Order o = s.get( Order.class, 1 );
			assertTrue( Hibernate.isInitialized( product1 ) );
			assertTrue( Hibernate.isInitialized( product2 ) );

		} );
	}

	@Test
	public void testGetOrder2() {
		inSession( s -> {
			s.getSessionFactory().getCache().evictAllRegions();

			Product product = s.getReference( Product.class, 2l );

			Order o = s.get( Order.class, 1 );
			assertTrue( Hibernate.isInitialized( product ) );
			assertTrue( Hibernate.isInitialized( o.getProduct() ) );

		} );
	}

	@Test
	public void testGetProduct() {
		inSession( s -> {
			s.getSessionFactory().getCache().evictAllRegions();

			Product product3 = s.getReference( Product.class, 3l );
			assertFalse( Hibernate.isInitialized( product3 ) );

			Product product1 = s.get( Product.class, 1l );

			assertThat( product1 ).isNotNull();
			assertTrue( Hibernate.isInitialized( product3 ) );
		} );
	}

	@Test
	public void testCriteriaQuery() {
		inSession( s -> {
			s.getSessionFactory().getCache().evictAllRegions();

			Product product1 = s.getReference( Product.class, 1l );
			Product product2 = s.getReference( Product.class, 2l );
			Product product3 = s.getReference( Product.class, 3l );

			CriteriaBuilder cb = s.getCriteriaBuilder();
			CriteriaQuery<Order> cr = cb.createQuery( Order.class );
			Root<Order> root = cr.from( Order.class );
			CriteriaQuery<Order> query = cr.select( root );

			List<Order> orders = s.createQuery( query ).getResultList();

			assertEquals( 2, orders.size() );

			assertTrue( Hibernate.isInitialized( product1 ) );
			assertTrue( Hibernate.isInitialized( product2 ) );
			assertTrue( Hibernate.isInitialized( product3 ) );

		} );
	}

	@Entity(name = "Order")
	@Table(name = "ORDER_TABLE")
	public static class Order {
		@Id
		Long id;

		String name;

		public Order() {
		}

		public Order(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		@ManyToOne(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		Product product;

		public Product getProduct() {
			return product;
		}

		public void setProduct(Product product) {
			this.product = product;
		}
	}

	@Entity(name = "Product")
	@BatchSize(size = 512)
	@Cacheable
	public static class Product {
		@Id
		Long id;

		String name;


		public Product() {
		}

		public Product(Long id, String name) {
			this.id = id;
			this.name = name;
		}

	}
}
