/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.valuehandlingmode.inline;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.CockroachDialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		annotatedClasses = {
				PredicateTest.Customer.class,
				PredicateTest.Order.class
		}
		, properties = @Setting(name = AvailableSettings.CRITERIA_VALUE_HANDLING_MODE, value = "inline")
)
public class PredicateTest {

	@Test
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "https://github.com/cockroachdb/cockroach/issues/41943")
	public void testQuotientConversion(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Order> orderCriteria = builder.createQuery( Order.class );
					Root<Order> orderRoot = orderCriteria.from( Order.class );

					Long longValue = 999999999L;
					Path<Double> doublePath = orderRoot.get( "totalPrice" );
					Path<Integer> integerPath = orderRoot.get( "customer" ).get( "age" );

					orderCriteria.select( orderRoot );
					Predicate p = builder.ge(
							builder.quot( integerPath, doublePath ),
							longValue
					);
					orderCriteria.where( p );

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
		@JoinColumn(name = "FK4_FOR_CUSTOMER_TABLE")
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
