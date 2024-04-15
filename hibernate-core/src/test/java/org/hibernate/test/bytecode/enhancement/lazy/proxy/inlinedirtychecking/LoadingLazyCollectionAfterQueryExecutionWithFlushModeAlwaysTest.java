package org.hibernate.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.FlushMode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@TestForIssue( jiraKey = "HHH-14549")
@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({ DirtyCheckEnhancementContext.class, NoDirtyCheckEnhancementContext.class })
public class LoadingLazyCollectionAfterQueryExecutionWithFlushModeAlwaysTest
		extends BaseEntityManagerFunctionalTestCase {

	boolean skipTest;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Customer.class,
				ProductOrder.class,
				Product.class,
				City.class
		};
	}

	@Override
	protected void addMappings(Map settings) {
		String byteCodeProvider = Environment.getProperties().getProperty( AvailableSettings.BYTECODE_PROVIDER );
		settings.put( AvailableSettings.FLUSH_MODE, FlushMode.ALWAYS );
		if ( byteCodeProvider != null && !Environment.BYTECODE_PROVIDER_NAME_BYTEBUDDY.equals( byteCodeProvider ) ) {
			// skip the test if the bytecode provider is Javassist
			skipTest = true;
		}
	}

	@Before
	public void setUp() {
		if ( skipTest ) {
			return;
		}
		doInJPA(
				this::entityManagerFactory, entityManager -> {
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
	public void reproducer_Case1() {
		if ( skipTest ) {
			return;
		}
		doInJPA(
				this::entityManagerFactory, entityManager -> {
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
