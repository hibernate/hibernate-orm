/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.refresh;

import org.hibernate.Hibernate;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A load plan built while a cascading fetch profile is enabled contains extra (cascade-driven)
 * fetches, so it must never be stored in - or served from - the regular per-lock-mode plan cache,
 * which is not keyed by the cascading fetch profile.
 * <p>
 * This used to happen for exclusive locks: {@code resolveLoadPlan} only routed to the internal
 * cascade cache for non-exclusive locks, so a {@code refresh} under a pessimistic lock fell through
 * to the regular cache and poisoned it for subsequent plain loads at the same lock mode.
 */
@DomainModel(
		annotatedClasses = {
				RefreshPessimisticLockPlanCacheTest.Owner.class,
				RefreshPessimisticLockPlanCacheTest.Target.class
		}
)
@SessionFactory
@BytecodeEnhanced(runNotEnhancedAsWell = true)
public class RefreshPessimisticLockPlanCacheTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Target t1 = new Target( 1L, "target 1" );
			final Target t2 = new Target( 2L, "target 2" );
			session.persist( t1 );
			session.persist( t2 );
			session.persist( new Owner( 1L, t1 ) );
			session.persist( new Owner( 2L, t2 ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void cascadePlanUnderPessimisticLockDoesNotPoisonTheRegularCache(SessionFactoryScope scope) {
		// Refreshing under an exclusive lock builds a plan that immediately fetches the
		// cascade=REFRESH association.
		scope.inTransaction( session -> {
			final Owner owner = session.find( Owner.class, 1L );
			Hibernate.initialize( owner.getTarget() );
			session.refresh( owner, LockModeType.PESSIMISTIC_WRITE );
			// just a sanity check to ensure the relation was indeed fetched and is in the plan cache
			assertThat( Hibernate.isInitialized( owner.getTarget() ) ).isTrue();
		} );

		// A plain load at the same lock mode must not reuse that cascade plan: the association
		// is mapped LAZY, so it must still come back uninitialized.
		scope.inTransaction( session -> {
			final Owner owner = session.find( Owner.class, 2L, LockModeType.PESSIMISTIC_WRITE );
			assertThat( Hibernate.isInitialized( owner.getTarget() ) ).isFalse();
		} );
	}

	@Entity(name = "PlanCacheOwner")
	@Table(name = "plan_cache_owner")
	public static class Owner {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
		private Target target;

		public Owner() {
		}

		public Owner(Long id, Target target) {
			this.id = id;
			this.target = target;
		}

		public Target getTarget() {
			return target;
		}
	}

	@Entity(name = "PlanCacheTarget")
	@Table(name = "plan_cache_target")
	public static class Target {
		@Id
		private Long id;

		private String name;

		public Target() {
		}

		public Target(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
