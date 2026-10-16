package org.hibernate.orm.test.locking;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ExcludedFromVersioning;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Version;

import org.hibernate.Locking;
import org.hibernate.Timeouts;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.HibernateHints;
import org.hibernate.jpa.SpecHints;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.VersionMatchMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Executable application locking examples for the User Guide.
///
/// @author Steve Ebersole
@Jpa(annotatedClasses = { LockingGuideTests.Book.class, LockingGuideTests.Counter.class }, useCollectingStatementInspector = true)
class LockingGuideTests {
	@BeforeEach
	void createData(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.persist( new Book( 1L, "Original" ) );
			entityManager.persist( new Book( 2L, "Second" ) );
			entityManager.persist( new Counter( 1L, "Original" ) );
		} );
	}

	@AfterEach
	void cleanup(EntityManagerFactoryScope scope) {
		scope.dropData();
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSelectLocking.class)
	void standardEntryPoints(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::locking-api-example[]
			Book book = entityManager.find( Book.class, 1L, LockModeType.PESSIMISTIC_READ );
			entityManager.lock( book, LockModeType.PESSIMISTIC_WRITE );
			entityManager.refresh( book, LockModeType.PESSIMISTIC_WRITE );
			List<Book> books = entityManager.createQuery( "from GuideBook order by id", Book.class )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.getResultList();
			//end::locking-api-example[]
			assertEquals( LockModeType.PESSIMISTIC_WRITE, entityManager.getLockMode( book ) );
			assertEquals( 2, books.size() );
			assertTrue( books.stream().anyMatch( result -> result == book ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Concurrent changes can instead fail at SERIALIZABLE isolation")
	void staleWriteFails(EntityManagerFactoryScope scope) {
		try (EntityManager stale = scope.getEntityManagerFactory().createEntityManager()) {
			stale.getTransaction().begin();
			try {
				Book book = stale.find( Book.class, 1L );
				scope.inTransaction( winner -> winner.find( Book.class, 1L ).title = "Committed" );
				book.title = "Stale";
				assertThrows( OptimisticLockException.class, stale::flush );
			}
			finally {
				stale.getTransaction().rollback();
			}
		}
		scope.inTransaction( entityManager -> {
			Book book = entityManager.find( Book.class, 1L );
			assertEquals( "Committed", book.title );
			assertEquals( 1, book.version );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Concurrent changes can instead fail at SERIALIZABLE isolation")
	void explicitOptimisticValidation(EntityManagerFactoryScope scope) {
		PersistenceException failure = assertThrows( PersistenceException.class, () ->
				scope.inTransaction( entityManager -> {
					//tag::locking-explicit-optimistic-example[]
					Book book = entityManager.find( Book.class, 1L, LockModeType.OPTIMISTIC );
					// No change to book is needed to request version validation.
					//end::locking-explicit-optimistic-example[]
					assertEquals( 0, book.version );
					scope.inTransaction( writer -> writer.find( Book.class, 1L ).title = "Concurrent" );
				} ) );
		assertTrue( causedByOptimisticFailure( failure ), failure::toString );
	}

	@Test
	void optimisticForceIncrement(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager ->
				entityManager.find( Book.class, 1L, LockModeType.OPTIMISTIC_FORCE_INCREMENT ) );
		scope.inTransaction( entityManager -> {
			Book book = entityManager.find( Book.class, 1L );
			assertEquals( "Original", book.title );
			assertEquals( 1, book.version );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSelectLocking.class)
	void pessimisticForceIncrement(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Book book = entityManager.find( Book.class, 1L, LockModeType.PESSIMISTIC_FORCE_INCREMENT );
			assertEquals( 1, book.version );
		} );
		scope.inTransaction( entityManager -> assertEquals( 1, entityManager.find( Book.class, 1L ).version ) );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Concurrent changes can instead fail at SERIALIZABLE isolation")
	@SkipForDialect(dialectClass = MariaDBDialect.class, majorVersion = 11, minorVersion = 6, microVersion = 2,
			versionMatchMode = VersionMatchMode.SAME_OR_NEWER,
			reason = "MariaDB reports DB_RECORD_CHANGED when acquiring protection for a concurrently changed row")
	void excludedAttributeCanBeOverwritten(EntityManagerFactoryScope scope) {
		//tag::locking-guide-exclusion-example[]
		scope.inTransaction( alice -> {
			Counter counter = alice.find( Counter.class, 1L );
			counter.title = "Changed title";
			scope.inTransaction( bob -> {
				Counter other = bob.find( Counter.class, 1L );
				other.readCount++;
			} );
			scope.inTransaction( reader -> {
				Counter current = reader.find( Counter.class, 1L );
				assertEquals( 1, current.readCount );
				assertEquals( 0, current.version );
			} );
		} );
		scope.inTransaction( entityManager -> {
			Counter counter = entityManager.find( Counter.class, 1L );
			assertEquals( "Changed title", counter.title );
			assertEquals( 1, counter.version );
			assertEquals( 0, counter.readCount );
		} );
		//end::locking-guide-exclusion-example[]
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSelectLocking.class)
	void followOnPolicies(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			scope.getCollectingStatementInspector().clear();
			//tag::locking-guide-follow-on-example[]
			List<Book> books = entityManager.createQuery( "from GuideBook order by id", Book.class )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.setHint( HibernateHints.HINT_FOLLOW_ON_STRATEGY, Locking.FollowOn.FORCE )
					.getResultList();
			//end::locking-guide-follow-on-example[]
			assertEquals( 2, books.size() );
			books.forEach( book -> assertEquals( LockModeType.PESSIMISTIC_WRITE, entityManager.getLockMode( book ) ) );
			var lockingStyle = scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class )
					.getJdbcServices().getDialect().getLockingSupport().getMetadata().getPessimisticLockStyle();
			assertEquals( lockingStyle == PessimisticLockStyle.TABLE_HINT ? 1 : 2,
					scope.getCollectingStatementInspector().getSqlQueries().size() );
		} );
		scope.inTransaction( entityManager -> {
			//tag::locking-guide-follow-on-disallow-example[]
			Book book = entityManager.find( Book.class, 1L, LockModeType.PESSIMISTIC_WRITE,
					Locking.FollowOn.DISALLOW );
			//end::locking-guide-follow-on-disallow-example[]
			assertNotNull( book );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSkipLocked.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	void skipLockedSelection(EntityManagerFactoryScope scope) {
		scope.inTransaction( holder -> {
			holder.find( Book.class, 1L, LockModeType.PESSIMISTIC_WRITE ).title = "In progress";
			holder.flush();
			scope.inTransaction( entityManager -> {
				//tag::locking-guide-skip-locked-example[]
				List<Book> available = entityManager.createQuery( "from GuideBook order by id", Book.class )
						.setLockMode( LockModeType.PESSIMISTIC_WRITE )
						.setHint( SpecHints.HINT_SPEC_LOCK_TIMEOUT, Timeouts.SKIP_LOCKED )
						.getResultList();
				//end::locking-guide-skip-locked-example[]
				assertEquals( List.of( 2L ), available.stream().map( book -> book.id ).toList() );
			} );
		} );
	}

	private boolean causedByOptimisticFailure(Throwable failure) {
		for ( Throwable cause = failure; cause != null; cause = cause.getCause() ) {
			if ( cause instanceof OptimisticLockException ) {
				return true;
			}
		}
		return false;
	}

	//tag::locking-guide-version-mapping[]
	@Entity(name = "GuideBook")
	public static class Book {
		@Id
		private Long id;
		private String title;
		@Version
		private long version;
		//end::locking-guide-version-mapping[]

		public Book() {
		}

		Book(Long id, String title) {
			this.id = id;
			this.title = title;
		}
	//tag::locking-guide-version-mapping[]
	}
	//end::locking-guide-version-mapping[]

	//tag::locking-guide-exclusion-mapping[]
	@Entity(name = "GuideCounter")
	public static class Counter {
		@Id
		private Long id;
		private String title;
		@Version
		private long version;
		@ExcludedFromVersioning
		private long readCount;
		//end::locking-guide-exclusion-mapping[]

		public Counter() {
		}

		Counter(Long id, String title) {
			this.id = id;
			this.title = title;
		}
	//tag::locking-guide-exclusion-mapping[]
	}
	//end::locking-guide-exclusion-mapping[]

}
