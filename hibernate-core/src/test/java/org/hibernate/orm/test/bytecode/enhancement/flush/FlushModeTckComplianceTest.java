package org.hibernate.orm.test.bytecode.enhancement.flush;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(BytecodeEnhancerRunner.class)
@JiraKey("HHH-16572")
public class FlushModeTckComplianceTest extends BaseCoreFunctionalTestCase {

	public static final int CITY_ID = 2;
	public static final int ORDER_ID = 3;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Customer.class,
				Spouse.class,
				Order.class,
		};
	}

	@Before
	public void setUp() {
		inTransaction(
				session -> {
					Customer customer = new  Customer( 1, "Alan E. Frechette" );
					session.persist( customer );

					Spouse spouse = new Spouse( CITY_ID, "Thomas", "Mullen" );
					customer.setSpouse( spouse );

					Order order = new Order( ORDER_ID, customer );
					session.persist( order );
				}
		);
	}

	@Test
	public void testQueryWithFlushModeAuto() {
		inTransaction(
				session -> {
					Spouse spouse = session.find( Spouse.class, CITY_ID );
					spouse.setLastName( "Milano" );

					List<Order> orders = session.createQuery(
									"SELECT o FROM Order o WHERE o.customer.spouse.lastName = 'Milano'" )
							.setFlushMode( FlushModeType.AUTO ).getResultList();

					assertThat( orders.size() ).isEqualTo( 1 );
					assertThat( orders.get( 0 ).getId() ).isEqualTo( ORDER_ID );
				}
		);
	}


	@Entity(name = "Order")
	@Table(name = "ORDER_TABLE")
	public static class Order {

		private Integer id;

		private double totalPrice;

		private Customer customer;

		public Order() {
		}

		public Order(Integer id, double totalPrice) {
			this.id = id;
			this.totalPrice = totalPrice;
		}

		public Order(Integer id, Customer customer) {
			this.id = id;
			this.customer = customer;
		}

		public Order(Integer id) {
			this.id = id;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public double getTotalPrice() {
			return totalPrice;
		}

		public void setTotalPrice(double price) {
			this.totalPrice = price;
		}

		@ManyToOne
		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}

	}

	@Entity(name = "Spouse")
	@Table(name = "SPOUSE_TABLE")
	public static class Spouse {

		private Integer id;

		private String first;

		private String last;

		public Spouse() {
		}

		public Spouse(Integer id, String firstName, String lastName) {
			this.id = id;
			this.first = firstName;
			this.last = lastName;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getFirst() {
			return first;
		}

		public void setFirst(String firstName) {
			this.first = firstName;
		}

		public String getLastName() {
			return last;
		}

		public void setLastName(String lastName) {
			this.last = lastName;
		}
	}


	@Entity(name = "Customer")
	@Table(name = "CUSTOMER_TABLE")
	public static class Customer {

		private Integer id;

		private String name;

		private Spouse spouse;

		private Collection<Order> orders = new ArrayList<>();

		public Customer() {
		}

		public Customer(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@OneToOne(cascade = CascadeType.ALL)
		public Spouse getSpouse() {
			return spouse;
		}

		public void setSpouse(Spouse spouse) {
			this.spouse = spouse;
		}

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "customer")
		public Collection<Order> getOrders() {
			return orders;
		}

		public void setOrders(Collection<Order> orders) {
			this.orders = orders;
		}

	}
}
