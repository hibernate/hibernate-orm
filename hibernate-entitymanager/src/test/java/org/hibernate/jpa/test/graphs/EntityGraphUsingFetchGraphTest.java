package org.hibernate.jpa.test.graphs;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Subgraph;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.hibernate.Hibernate;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

/**
 * 
 * @author Baris Cubukcuoglu
 * 
 */
public class EntityGraphUsingFetchGraphTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { CustomerOrder.class, Order_Position.class, Product.class, Address.class, Customer.class,
				BankAccount.class };
	}

	@Test
	public void fetchSubGraphFromSubgraph() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Address address = new Address();
		address.city = "TestCity";
		em.persist(address);

		CustomerOrder customerOrder = new CustomerOrder();
		customerOrder.shippingAddress = address;
		em.persist(customerOrder);

		Product product = new Product();
		em.persist(product);

		Order_Position orderPosition = new Order_Position();
		em.persist(orderPosition);

		orderPosition.customerOrder = customerOrder;
		orderPosition.product = product;

		customerOrder.orderPositions.add(orderPosition);
		product.orderProducts.add(orderPosition);
		em.persist(orderPosition);
		em.persist(product);
		em.persist(customerOrder);

		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();

		final EntityGraph<CustomerOrder> entityGraph = em.createEntityGraph(CustomerOrder.class);
		entityGraph.addAttributeNodes("billingAddress", "shippingAddress", "orderDate");
		final Subgraph<BankAccount> bankAccountSubGraph = entityGraph.addSubgraph("bankAccount");
		bankAccountSubGraph.addAttributeNodes("accountNo", "bank", "blz");
		final Subgraph<Customer> customerSubgraph = entityGraph.addSubgraph("customer");
		customerSubgraph.addAttributeNodes("firstName", "lastName");
		final Subgraph<Order_Position> orderProductsSubgraph = entityGraph.addSubgraph("orderPositions");
		orderProductsSubgraph.addAttributeNodes("amount");
		final Subgraph<Product> productSubgraph = orderProductsSubgraph.addSubgraph("product");
		productSubgraph.addAttributeNodes("productName", "productNo");

		TypedQuery<CustomerOrder> query = em.createQuery(
				"SELECT o FROM EntityGraphUsingFetchGraphTest$CustomerOrder o", CustomerOrder.class);
		query.setHint("javax.persistence.loadgraph", entityGraph);
		final List<CustomerOrder> results = query.getResultList();

		assertTrue(Hibernate.isInitialized(results));

		em.getTransaction().commit();
		em.close();
	}

	@Entity
	public static class Customer {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long id;

		public String firstName;

		public String lastName;

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "customer")
		private List<CustomerOrder> customerOrders = new ArrayList<CustomerOrder>();

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "customer")
		private List<BankAccount> bankAccounts = new ArrayList<BankAccount>();
	}

	@Entity
	public static class BankAccount {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long id;

		public Long blz;

		public Long accountNo;

		public String bank;

		@ManyToOne
		@JoinColumn(name = "customer")
		private Customer customer;
	}

	@Entity
	public static class CustomerOrder {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long id;

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "customerOrder")
		public Set<Order_Position> orderPositions = new HashSet<Order_Position>();

		@OneToOne
		public Address billingAddress;

		@Temporal(TemporalType.TIMESTAMP)
		public Date orderDate;

		@OneToOne
		public Address shippingAddress;

		@OneToOne
		@JoinColumn(name = "bankAccount")
		public BankAccount bankAccount;

		@ManyToOne
		@JoinColumn(name = "customer")
		private Customer customer;
	}

	@Entity
	public static class Address {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long id;

		public String city;
	}

	@Entity
	public static class Order_Position {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long id;

		public Integer amount;

		@ManyToOne
		@JoinColumn(name = "customerOrder")
		public CustomerOrder customerOrder;

		@ManyToOne
		@JoinColumn(name = "product")
		public Product product;
	}

	@Entity
	public static class Product {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		public String productName;

		public String productNo;

		@OneToMany(cascade = CascadeType.ALL, mappedBy = "product")
		private List<Order_Position> orderProducts = new ArrayList<Order_Position>();
	}
}
