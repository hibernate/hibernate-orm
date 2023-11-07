package org.hibernate.orm.test.locking;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
					final ReferencedEntity e1 = new ReferencedEntity( 0, "lazy" );
					final ReferencedEntity e2 = new ReferencedEntity( 1, "eager" );
					entityManager.persist( e1 );
					entityManager.persist( e2 );
					final MainEntity e3 = new MainEntity( 0, e1, e2 );
					entityManager.persist( e3 );
				}
		);
	}

	@Test
	public void testRefreshBeforeRead(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					MainEntity m = entityManager.find( MainEntity.class, 0 );
					assertNotNull( m );
					ReferencedEntity lazyReference = m.referencedLazy();
					ReferencedEntity eagerReference = m.referencedEager();
					assertNotNull( lazyReference );
					assertNotNull( eagerReference );

					// First refresh, then access
					entityManager.refresh( eagerReference, LockModeType.PESSIMISTIC_WRITE );
					entityManager.refresh( lazyReference, LockModeType.PESSIMISTIC_WRITE );

					assertEquals( "lazy", lazyReference.status() );
					assertEquals( "eager", eagerReference.status() );
					assertEquals( LockModeType.PESSIMISTIC_WRITE, entityManager.getLockMode( lazyReference ) );
					assertEquals( LockModeType.PESSIMISTIC_WRITE, entityManager.getLockMode( eagerReference ) );
				} );
	}

	@Test
	public void testRefreshAfterRead(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					MainEntity m = entityManager.find( MainEntity.class, 0 );
					assertNotNull( m );
					ReferencedEntity lazyReference = m.referencedLazy();
					ReferencedEntity eagerReference = m.referencedEager();
					assertNotNull( lazyReference );
					assertNotNull( eagerReference );

					// First access, the refresh
					assertEquals( "lazy", lazyReference.status() );
					assertEquals( "eager", eagerReference.status() );

					entityManager.refresh( lazyReference, LockModeType.PESSIMISTIC_WRITE );
					entityManager.refresh( eagerReference, LockModeType.PESSIMISTIC_WRITE );

					assertEquals( LockModeType.PESSIMISTIC_WRITE, entityManager.getLockMode( lazyReference ) );
					assertEquals( LockModeType.PESSIMISTIC_WRITE, entityManager.getLockMode( eagerReference ) );
				} );
	}


	@Entity
	public static class MainEntity {
		@Id
		private long id;

		private String name;

		@OneToOne(targetEntity = ReferencedEntity.class, fetch = FetchType.LAZY)
		@JoinColumn(name = "LAZY_COLUMN")
		private ReferencedEntity referencedLazy;

		@OneToOne(targetEntity = ReferencedEntity.class, fetch = FetchType.EAGER)
		@JoinColumn(name = "EAGER_COLUMN")
		private ReferencedEntity referencedEager;

		protected MainEntity() {
		}

		public MainEntity(long id, ReferencedEntity lazy, ReferencedEntity eager) {
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

	@Entity
	public static class ReferencedEntity {

		@Id
		private long id;

		private String status;

		protected ReferencedEntity() {
		}

		public ReferencedEntity(long id, String status) {
			this.id = id;
			this.status = status;
		}

		public String status() {
			return status;
		}
	}

}
