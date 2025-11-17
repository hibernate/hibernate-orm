/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(
		annotatedClasses = {
				FilterWitSubSelectFetchModeTest.Customer.class,
				FilterWitSubSelectFetchModeTest.CustomerOrder.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" )
		}
)
@JiraKey(value = "HHH-7119")
public class FilterWitSubSelectFetchModeTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Customer firstCustomer = new Customer( 1L, "First" );
			final Customer secondCustomer = new Customer( 2L, "Second" );
			final Customer thirdCustomer = new Customer( 3L, "Third" );
			final Customer fourthCustomer = new Customer( 4L, "Fourth" );
			final Customer fifthCustomer = new Customer( 5L, "Fifth" );

			firstCustomer.addOrder( new CustomerOrder( 100L ) );
			firstCustomer.addOrder( new CustomerOrder( 200L ) );

			secondCustomer.addOrder( new CustomerOrder( 300L ) );
			secondCustomer.addOrder( new CustomerOrder( 400L ) );

			thirdCustomer.addOrder( new CustomerOrder( 500L ) );
			thirdCustomer.addOrder( new CustomerOrder( 600L ) );

			fourthCustomer.addOrder( new CustomerOrder( 700L ) );
			fourthCustomer.addOrder( new CustomerOrder( 800L ) );

			fifthCustomer.addOrder( new CustomerOrder( 900L ) );
			fifthCustomer.addOrder( new CustomerOrder( 1000L ) );

			session.persist( fifthCustomer );
			session.persist( secondCustomer );
			session.persist( thirdCustomer );
			session.persist( fourthCustomer );
			session.persist( fifthCustomer );
		} );
	}

	@Test
	void testFiltersAreApplied(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "ID" ).setParameter( "id", 3L );
			//noinspection removal
			var result = session.createQuery( "from Customer order by id", Customer.class ).getResultList();

			assertFalse( result.isEmpty() );
			var customer = result.get( 0 );
			assertThat( customer.getCustomerId(), is( 3L ) );

			assertThat( customer.getOrders().size(), is( 2 ) );
			var statistics = session.getStatistics();
			assertThat( statistics.getEntityCount(), is( 9 ) );

			var sfStatistics = session.getSessionFactory().getStatistics();

			assertThat( sfStatistics.getCollectionFetchCount(), is( 1L ) );
			assertThat( sfStatistics.getQueries().length, is(1 ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@SuppressWarnings("FieldMayBeFinal")
	@Entity(name = "Customer")
	@FilterDef(
			name = "ID",
			defaultCondition = "customerId >= :id",
			parameters = {
					@ParamDef(type = Long.class, name = "id")
			}
	)
	@Filter(name = "ID")
	public static class Customer {

		@Id
		private Long customerId;

		private String name;

		public Customer() {
		}

		public Customer(Long customerId, String name) {
			this.customerId = customerId;
			this.name = name;
		}

		@OneToMany(mappedBy = "customer", fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
		@Fetch(FetchMode.SUBSELECT)
		private Set<CustomerOrder> orders = new HashSet<>();

		public Long getCustomerId() {
			return customerId;
		}

		public void setCustomerId(Long customerId) {
			this.customerId = customerId;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<CustomerOrder> getOrders() {
			return orders;
		}

		public void addOrder(CustomerOrder order) {
			order.setCustomer( this );
			this.orders.add( order );
		}
	}

	@Entity(name = "CustomerOrder")
	public static class CustomerOrder {

		@Id
		@GeneratedValue
		private Long orderId;

		private Long total;

		public CustomerOrder() {
		}

		public CustomerOrder(Long total) {
			this.total = total;
		}

		@ManyToOne
		private Customer customer;

		public Long getOrderId() {
			return orderId;
		}

		public void setOrderId(Long orderId) {
			this.orderId = orderId;
		}

		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}

		public Long getTotal() {
			return total;
		}

		public void setTotal(Long total) {
			this.total = total;
		}
	}
}
