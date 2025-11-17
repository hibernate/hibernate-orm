/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Marco Belladelli
 * @author Tomas Cerskus
 */
@JiraKey(value = "HHH-16136")
@Jpa(
		annotatedClasses = {
				OneToManyLazyAndEagerProxyTest.User.class,
				OneToManyLazyAndEagerProxyTest.Order.class,
				OneToManyLazyAndEagerProxyTest.OrderItem.class
		}
)
public class OneToManyLazyAndEagerProxyTest {

	@BeforeEach
	public void prepare(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
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

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().unwrap( SessionFactory.class ).getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testHibernateProxy(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// get reference to get a lazy proxy instance
			final User userRef = em.getReference( User.class, "User 1" );
			assertThat( userRef ).isInstanceOf( HibernateProxy.class );
			assertThat( Hibernate.isInitialized( userRef ) )
					.describedAs( "Proxy should not be initialized" )
					.isFalse();
			// query eager order.user and check that same instance was initialized
			final Order order = em.createQuery( "select o from Order o", Order.class )
					.getResultList()
					.get( 0 );
			final User user = order.getUser();
			assertThat( user )
					.describedAs( "Proxy instance should be the same" )
					.isEqualTo( userRef );
			assertThat( Hibernate.isInitialized( user ) )
					.describedAs( "Proxy should be initialized" )
					.isTrue();
			assertThat( user.getName() ).isEqualTo( "Marco" );
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
