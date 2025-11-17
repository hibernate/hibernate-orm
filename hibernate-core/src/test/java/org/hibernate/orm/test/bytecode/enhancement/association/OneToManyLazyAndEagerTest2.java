/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.association;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marco Belladelli
 * @author Tomas Cerskus
 */
@DomainModel(
		annotatedClasses = {
				OneToManyLazyAndEagerTest2.User.class,
				OneToManyLazyAndEagerTest2.Coupon.class,
				OneToManyLazyAndEagerTest2.Order.class
		}
)
@SessionFactory
@BytecodeEnhanced
@JiraKey("HHH-16477")
public class OneToManyLazyAndEagerTest2 {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
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

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final Order order = em.createQuery( "select o from Order o", Order.class )
					.getResultList()
					.get( 0 );

			final User user = order.getUser();
			assertTrue( Hibernate.isInitialized( user ), "Proxy should be initialized" );
			assertEquals( "Marco", user.getName() );

			final User targetUser = order.getTargetUser();
			assertTrue( Hibernate.isInitialized( targetUser ), "Proxy should be initialized" );
			assertEquals( "Andrea", targetUser.getName() );

			final Coupon coupon = order.getCoupon();
			assertTrue( Hibernate.isInitialized( coupon ), "Proxy should be initialized" );
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
