/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Marco Belladelli
 * @author Tomas Cerskus
 */
@JiraKey(value = "HHH-16136")
public class OneToManyLazyAndEagerProxyTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				User.class,
				Order.class,
				OrderItem.class
		};
	}

	@Before
	public void prepare() {
		doInJPA( this::entityManagerFactory, em -> {
			final User user = new User( "User 1", "Marco" );
			final User targetUser = new User( "User 2", "Andrea" );
			final Order order = new Order( "Order 1", user, targetUser );
			final OrderItem orderItem = new OrderItem( "Order Item 1", user, order );
			order.getOrderItems().add( orderItem );
			em.persist( user );
			em.persist( targetUser );
			em.persist( order );
			em.persist( orderItem );
		} );
	}

	@After
	public void tearDown() {
		doInJPA( this::entityManagerFactory, em -> {
			em.createQuery( "delete from OrderItem" ).executeUpdate();
			em.createQuery( "delete from Order" ).executeUpdate();
			em.createQuery( "delete from User" ).executeUpdate();
		});
	}

	@Test
	public void testHibernateProxy() {
		doInJPA( this::entityManagerFactory, em -> {
			// get reference to get a lazy proxy instance
			final User userRef = em.getReference( User.class, "User 1" );
			assertTrue( userRef instanceof HibernateProxy );
			assertFalse( "Proxy should not be initialized", Hibernate.isInitialized( userRef ) );
			// query eager order.user and check that same instance was initialized
			final Order order = em.createQuery( "select o from Order o", Order.class )
					.getResultList()
					.get( 0 );
			final User user = order.getUser();
			assertEquals( "Proxy instance should be the same", userRef, user );
			assertTrue( "Proxy should be initialized", Hibernate.isInitialized( user ) );
			assertEquals( "Marco", user.getName() );
		} );
	}

	@Entity(name = "Order")
	@Table(name = "Orders")
	public static class Order {
		@Id
		private String id;

		@OneToMany(targetEntity = OrderItem.class, mappedBy = "order", fetch = FetchType.EAGER)
		private final Set<OrderItem> orderItems = new HashSet<>();

		@ManyToOne(fetch = FetchType.EAGER)
		private User user;

		@ManyToOne(fetch = FetchType.EAGER)
		private User targetUser;

		public Order() {
		}

		public Order(String id, User user, User targetUser) {
			this.id = id;
			this.user = user;
			this.targetUser = targetUser;
		}

		public String getId() {
			return id;
		}

		public Set<OrderItem> getOrderItems() {
			return orderItems;
		}

		public User getUser() {
			return user;
		}

		public User getTargetUser() {
			return targetUser;
		}
	}

	@Entity(name = "User")
	@Table(name = "Users")
	public static class User {
		@Id
		private String id;

		private String name;

		public User() {
		}

		public User(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "OrderItem")
	@Table(name = "OrderItems")
	public static class OrderItem {
		@Id
		private String id;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		private User user;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		private Order order;

		public OrderItem() {
		}

		public OrderItem(String id, User user, Order order) {
			this.id = id;
			this.user = user;
			this.order = order;
		}

		public String getId() {
			return id;
		}

		public User getUser() {
			return user;
		}

		public Order getOrder() {
			return order;
		}
	}
}
