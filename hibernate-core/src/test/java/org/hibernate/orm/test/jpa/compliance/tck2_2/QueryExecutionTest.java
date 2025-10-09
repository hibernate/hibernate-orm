/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.tck2_2;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.transform.ResultTransformer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hamcrest.CoreMatchers;

import static jakarta.persistence.CascadeType.*;
import static jakarta.persistence.CascadeType.PERSIST;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {QueryExecutionTest.Customer.class, QueryExecutionTest.Order.class})
@SessionFactory
public class QueryExecutionTest {
	@Test
	public void testCollectionFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final List nonDistinctResult = session.createQuery( "select c from Customer c join fetch c.orders" ).list();
					// note: this was historically `2` because Hibernate would return a root result for each of its fetched rows.
					//		- that was confusing behavior for users.  we have changed this in 6 to be more logical
					assertThat( nonDistinctResult.size(), CoreMatchers.is( 1 ) );

					final List distinctResult = session.createQuery( "select distinct c from Customer c join fetch c.orders" ).list();
					assertThat( distinctResult.size(), CoreMatchers.is( 1 ) );

					final List distinctViaTransformerResult = session.createQuery( "select c from Customer c join fetch c.orders" )
							.setResultTransformer(new ResultTransformer() {
								@Override
								public Object transformTuple(Object[] tuple, String[] aliases) {
									return tuple[0];
								}
							}).list();
					assertThat( distinctResult.size(), CoreMatchers.is( 1 ) );
				}
		);
	}

	@Entity( name = "Customer" )
	@Table( name = "customers" )
	public static class Customer {
		@Id
		public Integer id;
		public String name;
		@OneToMany( mappedBy = "customer", cascade = {PERSIST, REMOVE} )
		public List<Order> orders = new ArrayList<>();

		public Customer() {
		}

		public Customer(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity( name = "Order" )
	@Table( name = "orders" )
	public static class Order {
		@Id
		public Integer id;
		@JoinColumn
		@ManyToOne
		public Customer customer;
		public String receivableLocator;

		public Order() {
		}

		public Order(Integer id, Customer customer, String receivableLocator) {
			this.id = id;
			this.customer = customer;
			this.receivableLocator = receivableLocator;

			customer.orders.add( this );
		}
	}

	@BeforeEach
	public void createData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Customer cust = new Customer( 1, "Acme Corp");
					final Order order1 = new Order( 1, cust, "123" );
					final Order order2 = new Order( 2, cust, "456" );
					session.persist( cust );
				}
		);
	}

	@AfterEach
	public void dropData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Customer customer = session.find( Customer.class, 1 );
					if ( customer != null ) {
						session.remove( customer );
					}
				}
		);
	}
}
