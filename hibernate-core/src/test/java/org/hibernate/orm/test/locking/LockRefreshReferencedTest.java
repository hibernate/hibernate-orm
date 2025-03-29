/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;


import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@JiraKey("HHH-17395")
@Jpa(
		annotatedClasses = {
				LockRefreshReferencedTest.MainEntity.class,
				LockRefreshReferencedTest.ReferencedEntity.class
		}
)
public class LockRefreshReferencedTest {

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final ReferencedEntity e1 = new ReferencedEntity( 0L, "lazy" );
					final ReferencedEntity e2 = new ReferencedEntity( 1L, "eager" );
					entityManager.persist( e1 );
					entityManager.persist( e2 );
					final MainEntity e3 = new MainEntity( 0L, e1, e2 );
					entityManager.persist( e3 );
				}
		);
	}

	@Test
	public void testRefreshBeforeRead(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					MainEntity m = entityManager.find( MainEntity.class, 0L );
					assertNotNull( m );
					ReferencedEntity lazyReference = m.referencedLazy();
					ReferencedEntity eagerReference = m.referencedEager();
					assertNotNull( lazyReference );
					assertNotNull( eagerReference );
					assertFalse( Hibernate.isInitialized( lazyReference ) );

					// First refresh, then access
					entityManager.refresh( eagerReference, LockModeType.PESSIMISTIC_WRITE );
					assertFalse( Hibernate.isInitialized( lazyReference ) );

					entityManager.refresh( lazyReference, LockModeType.PESSIMISTIC_WRITE );

					assertEquals( "lazy", lazyReference.status() );
					assertEquals( "eager", eagerReference.status() );
					assertEquals( LockModeType.PESSIMISTIC_WRITE, entityManager.getLockMode( lazyReference ) );
					assertEquals( LockModeType.PESSIMISTIC_WRITE, entityManager.getLockMode( eagerReference ) );
				} );
	}

	@Test
	public void testRefresh(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					MainEntity m = entityManager.find( MainEntity.class, 0L );
					assertNotNull( m );
					ReferencedEntity lazyReference = m.referencedLazy();
					ReferencedEntity eagerReference = m.referencedEager();
					assertNotNull( lazyReference );
					assertNotNull( eagerReference );
					assertFalse( Hibernate.isInitialized( lazyReference ) );

					entityManager.refresh( m );
					assertFalse( Hibernate.isInitialized( lazyReference ) );

				} );
	}

	@Test
	public void testRefreshAfterRead(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					MainEntity m = entityManager.find( MainEntity.class, 0L );
					assertNotNull( m );
					ReferencedEntity lazyReference = m.referencedLazy();
					ReferencedEntity eagerReference = m.referencedEager();
					assertNotNull( lazyReference );
					assertNotNull( eagerReference );
					assertFalse( Hibernate.isInitialized( lazyReference ) );

					// First access, the refresh
					assertEquals( "lazy", lazyReference.status() );
					assertEquals( "eager", eagerReference.status() );

					entityManager.refresh( lazyReference, LockModeType.PESSIMISTIC_WRITE );
					entityManager.refresh( eagerReference, LockModeType.PESSIMISTIC_WRITE );

					assertEquals( LockModeType.PESSIMISTIC_WRITE, entityManager.getLockMode( lazyReference ) );
					assertEquals( LockModeType.PESSIMISTIC_WRITE, entityManager.getLockMode( eagerReference ) );
				} );
	}


	@Test
	public void testFindWithLockMode(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				session -> {
					MainEntity mainEntity = session.find( MainEntity.class, 0L, LockModeType.PESSIMISTIC_WRITE );
					assertThat( session.getLockMode( mainEntity.referencedEager() ) ).isEqualTo( LockModeType.PESSIMISTIC_WRITE );
				}
		);
	}

	@Entity(name = "MainEntity")
	public static class MainEntity {
		@Id
		private Long id;

		private String name;

		@OneToOne(targetEntity = ReferencedEntity.class, fetch = FetchType.LAZY)
		@JoinColumn(name = "LAZY_COLUMN")
		private ReferencedEntity referencedLazy;

		@OneToOne(targetEntity = ReferencedEntity.class, fetch = FetchType.EAGER)
		@JoinColumn(name = "EAGER_COLUMN")
		private ReferencedEntity referencedEager;

		protected MainEntity() {
		}

		public MainEntity(Long id, ReferencedEntity lazy, ReferencedEntity eager) {
			this.id = id;
			this.referencedLazy = lazy;
			this.referencedEager = eager;
		}

		public ReferencedEntity referencedLazy() {
			return referencedLazy;
		}

		public ReferencedEntity referencedEager() {
			return referencedEager;
		}
	}

	@Entity(name = "ReferencedEntity")
	public static class ReferencedEntity {

		@Id
		private Long id;

		private String status;

		protected ReferencedEntity() {
		}

		public ReferencedEntity(Long id, String status) {
			this.id = id;
			this.status = status;
		}

		public String status() {
			return status;
		}
	}

}
