/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.bytecode.internal.BytecodeProviderInitiator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;

import org.hibernate.jpa.HibernateHints;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@JiraKey("HHH-14549")
@DomainModel(
		annotatedClasses = {
				LoadingLazyCollectionAfterQueryExecutionWithFlushModeAlwaysTest.Customer.class,
				LoadingLazyCollectionAfterQueryExecutionWithFlushModeAlwaysTest.ProductOrder.class,
				LoadingLazyCollectionAfterQueryExecutionWithFlushModeAlwaysTest.Product.class,
				LoadingLazyCollectionAfterQueryExecutionWithFlushModeAlwaysTest.City.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = HibernateHints.HINT_FLUSH_MODE, value = "ALWAYS" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext({ DirtyCheckEnhancementContext.class, NoDirtyCheckEnhancementContext.class })
public class LoadingLazyCollectionAfterQueryExecutionWithFlushModeAlwaysTest {

	@BeforeAll
	static void beforeAll() {
		String byteCodeProvider = Environment.getProperties().getProperty( AvailableSettings.BYTECODE_PROVIDER );
		assumeFalse( byteCodeProvider != null && !BytecodeProviderInitiator.BYTECODE_PROVIDER_NAME_BYTEBUDDY.equals(
				byteCodeProvider ) );
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					ProductOrder order = new ProductOrder();
					order.setOrderNumber( "12345" );

					Customer customer = new Customer();
					customer.setProductOrder( order );
					customer.setName( "Fab" );

					Product product = new Product();
					product.setName( "gloves" );

					order.addProduct( product );

					entityManager.persist( order );
					entityManager.persist( product );
					entityManager.persist( customer );
				}
		);
	}

	@Test
	public void reproducer_Case1(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					List<Customer> customers = entityManager.createQuery( "select c from Customer c" ).getResultList();
					assertEquals( 1, customers.size() );

					Customer customer = customers.get( 0 );
					ProductOrder order = customer.getProductOrder();
					assertNotNull( order );

					entityManager.createQuery( "select c from City c" ).getResultList();

					assertEquals( 1, order.getProducts().size() );
				} );
	}

	@MappedSuperclass
	public static abstract class Base {
		@Id
		@GeneratedValue
		public Long id;

		public Long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}

	@Entity(name = "Customer")
	public static class Customer extends Base {

		private String name;

		@OneToOne(fetch = FetchType.LAZY)
		ProductOrder order;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public ProductOrder getProductOrder() {
			return order;
		}

		public void setProductOrder(ProductOrder order) {
			this.order = order;
		}
	}

	@Entity(name = "ProductOrder")
	public static class ProductOrder extends Base {

		private String orderNumber;

		@OneToMany
		private List<Product> products = new ArrayList<>();

		public String getOrderNumber() {
			return orderNumber;
		}

		public void setOrderNumber(String orderNumber) {
			this.orderNumber = orderNumber;
		}

		public List<Product> getProducts() {
			return products;
		}

		public void addProduct(Product product) {
			products.add( product );
		}

	}

	@Entity(name = "Product")
	public static class Product extends Base {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "City")
	public static class City extends Base {
		private String name;
	}
}
