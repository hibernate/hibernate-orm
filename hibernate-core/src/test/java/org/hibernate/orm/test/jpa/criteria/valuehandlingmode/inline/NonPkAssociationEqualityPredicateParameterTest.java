/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.valuehandlingmode.inline;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		annotatedClasses = {
				NonPkAssociationEqualityPredicateParameterTest.Customer.class,
				NonPkAssociationEqualityPredicateParameterTest.Order.class
		}
		, properties = @Setting(name = AvailableSettings.CRITERIA_VALUE_HANDLING_MODE, value = "bind")
)
public class NonPkAssociationEqualityPredicateParameterTest {

	@Test
	public void testEqualityCheck(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
					Root<Order> orderRoot = orderCriteria.from( Order.class );

					orderCriteria.select( orderRoot );
					Customer c = new Customer();
					c.customerNumber = 123L;
					orderCriteria.where(
							builder.equal( orderRoot.get( "customer" ), c )
					);

					List<Order> orders = entityManager.createQuery( orderCriteria ).getResultList();
					assertTrue( orders.size() == 0 );
				}
		);
	}

	@Entity
	@Table(name = "ORDER_TABLE")
	public static class Order {
		private String id;
		private double totalPrice;
		private Customer customer;

		public Order() {
		}

		public Order(String id, double totalPrice) {
			this.id = id;
			this.totalPrice = totalPrice;
		}

		public Order(String id, Customer customer) {
			this.id = id;
			this.customer = customer;
		}

		public Order(String id) {
			this.id = id;
		}

		@Id
		@Column(name = "ID")
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Column(name = "TOTALPRICE")
		public double getTotalPrice() {
			return totalPrice;
		}

		public void setTotalPrice(double price) {
			this.totalPrice = price;
		}

		@ManyToOne
		@JoinColumn(name = "FK4_FOR_CUSTOMER_TABLE", referencedColumnName = "CUSTOMER_NUMBER")
		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}
	}

	@Entity
	@Table(name = "CUSTOMER_TABLE")
	public static class Customer {
		private String id;
		private Long customerNumber;
		private String name;
		private Integer age;

		public Customer() {
		}

		public Customer(String id, String name) {
			this.id = id;
			this.name = name;
		}

		// Used by test case for HHH-8699.
		public Customer(String id, String name, String greeting, Boolean something) {
			this.id = id;
			this.name = name;
		}

		@Id
		@Column(name = "ID")
		public String getId() {
			return id;
		}

		public void setId(String v) {
			this.id = v;
		}

		@Column(name = "CUSTOMER_NUMBER", unique = true)
		public Long getCustomerNumber() {
			return customerNumber;
		}

		public void setCustomerNumber(Long customerNumber) {
			this.customerNumber = customerNumber;
		}

		@Column(name = "NAME")
		public String getName() {
			return name;
		}

		public void setName(String v) {
			this.name = v;
		}

		@Column(name = "AGE")
		public Integer getAge() {
			return age;
		}

		public void setAge(Integer age) {
			this.age = age;
		}
	}
}
