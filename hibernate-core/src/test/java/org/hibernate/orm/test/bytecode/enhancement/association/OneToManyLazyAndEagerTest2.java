/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.association;

import jakarta.persistence.*;

import org.hibernate.Hibernate;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Marco Belladelli
 * @author Tomas Cerskus
 */
@RunWith(BytecodeEnhancerRunner.class)
@TestForIssue(jiraKey = "HHH-16477")
public class OneToManyLazyAndEagerTest2 extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				User.class,
				Coupon.class,
				Order.class
		};
	}

	@Before
	public void prepare() {
		doInJPA( this::entityManagerFactory, em -> {
			final User user = new User( "User 1", "Marco" );
			final User targetUser = new User( "User 2", "Andrea" );
			final Coupon coupon = new Coupon( "Coupon 1", targetUser );
			final Order order = new Order( "Order 1", user, targetUser, coupon );
			em.persist( user );
			em.persist( targetUser );
			em.persist( coupon );
			em.persist( order );
		} );
	}

	@After
	public void tearDown() {
		doInJPA( this::entityManagerFactory, em -> {
			em.createQuery( "delete from Order" ).executeUpdate();
			em.createQuery( "delete from Coupon" ).executeUpdate();
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
			assertEquals( "Marco", user.getName() );

			final User targetUser = order.getTargetUser();
			assertTrue( "Proxy should be initialized", Hibernate.isInitialized( targetUser ) );
			assertEquals( "Andrea", targetUser.getName() );

			final Coupon coupon = order.getCoupon();
			assertTrue( "Proxy should be initialized", Hibernate.isInitialized( coupon ) );
			assertThat( coupon.getTargetUser() ).isSameAs( targetUser );


		} );
	}

	@Entity(name = "Order")
	@Table(name = "Orders")
	public static class Order {
		@Id
		private String id;

		@ManyToOne(fetch = FetchType.EAGER)
		private User user;

		@ManyToOne(fetch = FetchType.EAGER)
		private User targetUser;

		@ManyToOne(fetch = FetchType.EAGER)
		private Coupon coupon;

		public Order() {
		}

		public Order(String id, User user, User targetUser, Coupon coupon) {
			this.id = id;
			this.user = user;
			this.targetUser = targetUser;
			this.coupon = coupon;
		}

		public String getId() {
			return id;
		}

		public User getUser() {
			return user;
		}

		public User getTargetUser() {
			return targetUser;
		}

		public Coupon getCoupon() {
			return coupon;
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

	@Entity(name = "Coupon")
	@Table(name = "Coupons")
	public static class Coupon {
		@Id
		private String id;

		@ManyToOne(fetch = FetchType.LAZY)
		private User targetUser;

		public Coupon() {
		}

		public Coupon(String id, User targetUser) {
			this.id = id;
			this.targetUser = targetUser;
		}

		public String getId() {
			return id;
		}

		public User getTargetUser() {
			return targetUser;
		}

	}
}