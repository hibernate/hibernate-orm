/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import org.hibernate.Hibernate;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		annotatedClasses = {
				LockRefreshReferencedAndCascadingTest.MainEntity.class,
				LockRefreshReferencedAndCascadingTest.ReferencedEntity.class,
				LockRefreshReferencedAndCascadingTest.AnotherReferencedEntity.class,
		}
)
public class LockRefreshReferencedAndCascadingTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final AnotherReferencedEntity anotherReferencedEntity = new AnotherReferencedEntity(
							1L,
							"another lazy"
					);
					final ReferencedEntity e1 = new ReferencedEntity( 0L, "lazy", anotherReferencedEntity );
					final ReferencedEntity e2 = new ReferencedEntity( 1L, "eager", null );
					entityManager.persist( e1 );
					entityManager.persist( e2 );
					final MainEntity e3 = new MainEntity( 0L, e1, e2 );
					entityManager.persist( e3 );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Informix disallows FOR UPDATE with multi-table queries")
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
					assertTrue( Hibernate.isInitialized( lazyReference ) );
					assertTrue( Hibernate.isInitialized( lazyReference.anotherReferencedEntity ) );

					assertEquals( "lazy", lazyReference.status() );
					assertEquals( "eager", eagerReference.status() );
					assertEquals( LockModeType.PESSIMISTIC_WRITE, entityManager.getLockMode( lazyReference ) );
					assertEquals( LockModeType.PESSIMISTIC_WRITE, entityManager.getLockMode( lazyReference.getAnotherReferencedEntity() ) );
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
					// CascadeType.REFRESH  will trigger the initialization
					assertTrue( Hibernate.isInitialized( lazyReference ) );

				} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Informix disallows FOR UPDATE with multi-table queries")
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
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Informix disallows FOR UPDATE with multi-table queries")
	public void testRefreshLockMode(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					MainEntity m = entityManager.find( MainEntity.class, 0L );
					assertNotNull( m );
					ReferencedEntity lazyReference = m.referencedLazy();
					ReferencedEntity eagerReference = m.referencedEager();
					assertNotNull( lazyReference );
					assertNotNull( eagerReference );
					assertFalse( Hibernate.isInitialized( lazyReference ) );

					entityManager.refresh( m, LockModeType.PESSIMISTIC_WRITE );

					assertTrue( Hibernate.isInitialized( lazyReference ) );
					AnotherReferencedEntity anotherReferencedEntity = lazyReference.getAnotherReferencedEntity();
					assertTrue( Hibernate.isInitialized( anotherReferencedEntity ) );

					assertEquals( LockModeType.PESSIMISTIC_WRITE, entityManager.getLockMode( lazyReference ) );
					assertEquals(
							LockModeType.PESSIMISTIC_WRITE,
							entityManager.getLockMode( anotherReferencedEntity )
					);
				} );
	}

	@Test
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Informix disallows FOR UPDATE with multi-table queries")
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

		@OneToOne(cascade = { CascadeType.REFRESH }, fetch = FetchType.LAZY)
		@JoinColumn(name = "LAZY_COLUMN")
		private ReferencedEntity referencedLazy;

		@OneToOne(fetch = FetchType.EAGER)
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

		@OneToOne(cascade = { CascadeType.PERSIST, CascadeType.REFRESH }, fetch = FetchType.LAZY)
		private AnotherReferencedEntity anotherReferencedEntity;

		protected ReferencedEntity() {
		}

		public ReferencedEntity(Long id, String status, AnotherReferencedEntity anotherReferencedEntity) {
			this.id = id;
			this.status = status;
			this.anotherReferencedEntity = anotherReferencedEntity;
		}

		public String status() {
			return status;
		}

		public AnotherReferencedEntity getAnotherReferencedEntity() {
			return anotherReferencedEntity;
		}
	}

	@Entity(name = "AnotherReferencedEntity")
	public static class AnotherReferencedEntity {

		@Id
		private Long id;

		private String status;

		protected AnotherReferencedEntity() {
		}

		public AnotherReferencedEntity(Long id, String status) {
			this.id = id;
			this.status = status;
		}

		public String status() {
			return status;
		}
	}

}
