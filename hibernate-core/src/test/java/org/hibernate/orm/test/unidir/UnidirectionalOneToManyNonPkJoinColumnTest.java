/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unidir;

import java.io.Serializable;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class UnidirectionalOneToManyNonPkJoinColumnTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Customer.class,
			Order.class
		};
	}

	@Test
	@JiraKey( value = "HHH-12064" )
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			// Save the entity on the One side
			Customer customer = new Customer();
			customer.idCode = "ABC";
			customer.translationId = 1L;

			entityManager.persist(customer);
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			// Attempt to load the entity saved in the previous session
			entityManager.find(Customer.class, "ABC");
		} );
	}

	@Entity(name = "Customer")
	@Table(name = "tbl_customer")
	public static class Customer implements Serializable {

		@Id
		public String idCode;

		public Long translationId;

		@Fetch(FetchMode.JOIN)
		@OneToMany(fetch = FetchType.LAZY, orphanRemoval = true)
		@JoinColumn(name = "translationId", referencedColumnName = "translationId")
		public List<Order> translations;
	}

	@Entity(name = "Order")
	@Table(name = "tbl_order")
	public static class Order {

		@Id
		public long id;

		public long translationId;
	}
}
