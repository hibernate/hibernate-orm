package org.hibernate.orm.test.bytecode.enhancement.batch;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
@DomainModel(
		annotatedClasses = {
				BatchEntityWithSelectFetchWithDisableProxyTest.Order.class,
				BatchEntityWithSelectFetchWithDisableProxyTest.Product.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
public class BatchEntityWithSelectFetchWithDisableProxyTest {

	@BeforeEach
	public void setupData(SessionFactoryScope scope) {
		Product cheese1 = new Product( 1l, "Cheese 1" );
		Product cheese2 = new Product( 2l, "Cheese 2" );
		Product cheese3 = new Product( 3l, "Cheese 3" );

		Order order = new Order( 1l, "Hibernate" );
		Order order2 = new Order( 2l, "Hibernate 2" );

		order.setProduct( cheese2 );
		order2.setProduct( cheese1 );

		scope.inTransaction( s -> {
			s.persist( cheese1 );
			s.persist( cheese2 );
			s.persist( cheese3 );
			s.persist( order );
			s.persist( order2 );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from Order" ).executeUpdate();
					session.createMutationQuery( "delete from Product" ).executeUpdate();
				}
		);
	}

	@Test
	public void testGetOrder(SessionFactoryScope scope) {
		scope.inSession( s -> {
			s.getSessionFactory().getCache().evictAllRegions();

			Product product1 = s.getReference( Product.class, 1l );
			Product product2 = s.getReference( Product.class, 2l );

			Order o = s.get( Order.class, 1 );
			assertTrue( Hibernate.isInitialized( product1 ) );
			assertTrue( Hibernate.isInitialized( product2 ) );

		} );
	}

	@Test
	public void testGetOrder2(SessionFactoryScope scope) {
		scope.inSession( s -> {
			s.getSessionFactory().getCache().evictAllRegions();

			Product product = s.getReference( Product.class, 2l );

			Order o = s.get( Order.class, 1 );
			assertTrue( Hibernate.isInitialized( product ) );
			assertTrue( Hibernate.isInitialized( o.getProduct() ) );

		} );
	}

	@Test
	public void testGetProduct(SessionFactoryScope scope) {
		scope.inSession( s -> {
			s.getSessionFactory().getCache().evictAllRegions();

			Product product3 = s.getReference( Product.class, 3l );
			assertFalse( Hibernate.isInitialized( product3 ) );

			Product product1 = s.get( Product.class, 1l );

			assertThat( product1 ).isNotNull();
			assertTrue( Hibernate.isInitialized( product3 ) );
		} );
	}

	@Test
	public void testCriteriaQuery(SessionFactoryScope scope) {
		scope.inSession( s -> {
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
	@Proxy(lazy = false)
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
