package org.hibernate.orm.test.bytecode.enhancement.batch;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey("HHH-16890")
@RunWith(BytecodeEnhancerRunner.class)
public class BatchEntityOneToManyWithDisabledProxyTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Order.class, Product.class };
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

		order.addProduct( cheese1 );
		order.addProduct( cheese2 );

		inTransaction( s -> {
			s.persist( cheese1 );
			s.persist( cheese2 );
			s.persist( cheese3 );
			s.persist( order );
		} );
	}

	@After
	public void tearDown() {
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

			Order o = s.get( Order.class, 1 );

			List<Product> products = o.getProducts();
			assertEquals( 2, products.size() );
		} );
	}


	@Test
	public void testGetProduct() {
		inSession( s -> {
			s.getSessionFactory().getCache().evictAllRegions();
			Product product = s.getReference( Product.class, 3l );

			s.getReference( Product.class, 1l );

			s.get( Product.class, 2l );
			assertTrue( Hibernate.isInitialized( product ) );
		} );
	}

	@Test
	public void testCriteriaQuery() {
		inSession( s -> {
			s.getSessionFactory().getCache().evictAllRegions();

			CriteriaBuilder cb = s.getCriteriaBuilder();
			CriteriaQuery<Product> cr = cb.createQuery( Product.class );
			Root<Product> root = cr.from( Product.class );
			CriteriaQuery<Product> query = cr.select( root );

			List<Product> products = s.createQuery( query ).getResultList();

			assertEquals( 3, products.size() );
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

		@OneToMany(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		List<Product> products = new ArrayList<>();

		public List<Product> getProducts() {
			return products;
		}

		public void addProduct(Product product) {
			this.products.add( product );
		}
	}

	@Entity(name = "Product")
	@BatchSize(size = 512)
	@Proxy(lazy = false)
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

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
