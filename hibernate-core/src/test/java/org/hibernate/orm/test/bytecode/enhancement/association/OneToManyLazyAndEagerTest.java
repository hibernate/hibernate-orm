/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.association;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Marco Belladelli
 * @author Tomas Cerskus
 */
@RunWith(BytecodeEnhancerRunner.class)
@TestForIssue(jiraKey = "HHH-16136")
public class OneToManyLazyAndEagerTest extends BaseEntityManagerFunctionalTestCase {
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
		} );
	}

	@Test
	public void testQuery() {
		doInJPA( this::entityManagerFactory, em -> {
			final Order order = em.createQuery( "select o from Order o", Order.class )
					.getResultList()
					.get( 0 );
			final User user = order.getUser();
			assertTrue( "Proxy should be initialized", Hibernate.isInitialized( user ) );
			assertEquals( "Marco", order.getUser().getName() );
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