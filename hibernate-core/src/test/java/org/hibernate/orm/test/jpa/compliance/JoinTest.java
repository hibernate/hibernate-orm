/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CollectionJoin;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.EntityType;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = {
				JoinTest.Order.class,
				JoinTest.Customer.class,
				JoinTest.LineItem.class
		}
		,
		properties = {
				@Setting(name = AvailableSettings.CRITERIA_COPY_TREE, value = "true"),
		}
)
public class JoinTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {

					Customer customer1 = new Customer( 1, "Andrea" );
					Customer customer2 = new Customer( 2, "Christian" );
					Customer customer3 = new Customer( 3, "Steve" );

					LineItem lineItem1 = new LineItem( 1, 1 );
					LineItem lineItem2 = new LineItem( 2, 2 );
					LineItem lineItem3 = new LineItem( 3, 3 );

					Order order1 = new Order( 1, customer1 );
					Order order2 = new Order( 2, customer2 );
					Order order3 = new Order( 3, customer3 );

					order1.addLineItem( lineItem1 );
					order2.addLineItem( lineItem2 );
					order3.addLineItem( lineItem3 );

					entityManager.persist( customer1 );
					entityManager.persist( customer2 );
					entityManager.persist( customer3 );

					entityManager.persist( lineItem1 );
					entityManager.persist( lineItem2 );
					entityManager.persist( lineItem3 );

					entityManager.persist( order1 );
					entityManager.persist( order2 );
					entityManager.persist( order3 );

				}
		);
	}

	@Test
	public void setJoinOnExpressionTest2(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final EntityType<Customer> customerEntity = entityManager.getMetamodel()
							.entity( Customer.class );

					final EntityType<Order> orderEntity = entityManager.getMetamodel().entity( Order.class );

					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaQuery<Integer> criteriaQuery = criteriaBuilder.createQuery( Integer.class );

					final Root<Customer> customer = criteriaQuery.from( Customer.class );
					criteriaQuery.select( customer.get( "id" ) );


					final CollectionAttribute<? super Customer, Order> orders = customerEntity.getCollection(
							"orders",
							Order.class
					);

					final CollectionJoin<Order, LineItem> lineItem = customer.join( orders )
							.join( orderEntity.getCollection( "lineItems", LineItem.class ), JoinType.INNER );

					lineItem.on( criteriaBuilder.equal( lineItem.get( "id" ), "1" ) );

					final List<Integer> ids = entityManager.createQuery( criteriaQuery ).getResultList();

					assertEquals( 1, ids.size() );
					assertEquals( 1, ids.get( 0 ) );

				}
		);

	}

	@Entity
	@Table(name = "ORDER_TABLE")
	public static class Order {

		@Id
		private Integer id;

		private String description;

		@ManyToOne
		private Customer customer;

		@OneToMany
		private Collection<LineItem> lineItems = new ArrayList<>();

		public Order() {
		}

		public Order(Integer id, Customer customer) {
			this.id = id;
			this.customer = customer;
			customer.addOrder( this );
		}

		public Integer getId() {
			return id;
		}

		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}

		public void addLineItem(LineItem lineItem) {
			lineItems.add( lineItem );
		}
	}

	@Entity
	@Table(name = "CUSTOMER_TABLE")
	public static class Customer {

		@Id
		private Integer id;

		private String name;

		@OneToMany(mappedBy = "customer")
		private Collection<Order> orders = new ArrayList<>();

		public Customer() {
		}

		public Customer(Integer id, String name) {
			this.id = id;
			this.name = name;
		}


		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Collection<Order> getOrders() {
			return orders;
		}

		public void setOrders(Collection<Order> orders) {
			this.orders = orders;
		}

		public void addOrder(Order order) {
			orders.add( order );
		}
	}

	@Entity
	@Table(name = "LINEITEM_TABLE")
	public static class LineItem {

		@Id
		private Integer id;

		private Integer quantity;

		public LineItem() {
		}

		public LineItem(Integer id, Integer quantity) {
			this.id = id;
			this.quantity = quantity;
		}

		public Integer getId() {
			return id;
		}

		public int getQuantity() {
			return quantity;
		}
	}

}
