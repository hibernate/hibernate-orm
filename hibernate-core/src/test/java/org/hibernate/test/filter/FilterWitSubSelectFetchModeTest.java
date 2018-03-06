/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.filter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-7119")
public class FilterWitSubSelectFetchModeTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Customer.class, CustomerOrder.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Before
	public void setUp() {
		doInHibernate( this::sessionFactory, session -> {
			Customer firstCustomer = new Customer( 1L, "First" );
			Customer secondCustomer = new Customer( 2L, "Second" );
			Customer thirdCustomer = new Customer( 3L, "Third" );
			Customer fourthCustomer = new Customer( 4L, "Fourth" );
			Customer fifthCustomer = new Customer( 5L, "Fifth" );

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
	public void testFiltersAreApplied() {

		doInHibernate( this::sessionFactory, session -> {
			session.enableFilter( "ID" ).setParameter( "id", 3L );
			List result = session.createQuery( "from Customer order by id" ).list();

			assertFalse( result.isEmpty() );
			Customer customer = (Customer) result.get( 0 );
			assertSame( customer.getCustomerId(), 3L );

			assertSame( customer.getOrders().size(), 2 );
			SessionStatistics statistics = session.getStatistics();
			assertSame( statistics.getEntityCount(), 9 );

			Statistics sfStatistics = session.getSessionFactory().getStatistics();

			assertSame( sfStatistics.getCollectionFetchCount(), 1L );
			assertSame( sfStatistics.getQueries().length, 1 );
		} );
	}

	@Entity(name = "Customer")
	@FilterDef(defaultCondition = "customerId >= :id", name = "ID",
			parameters = { @ParamDef(type = "long", name = "id") }
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
