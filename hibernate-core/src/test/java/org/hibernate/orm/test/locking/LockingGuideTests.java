package org.hibernate.orm.test.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Version;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies that a stale update reports an optimistic locking failure.
///
/// @author Steve Ebersole
@Jpa(annotatedClasses = LockingGuideTests.Book.class)
class LockingGuideTests {
	@BeforeEach
	void createData(EntityManagerFactoryScope scope) {
		scope.inTransaction( manager -> manager.persist( new Book( 1L, "Original" ) ) );
	}

	@AfterEach
	void cleanup(EntityManagerFactoryScope scope) {
		scope.dropData();
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
		scope.inTransaction( manager -> {
			Book book = manager.find( Book.class, 1L );
			assertEquals( "Committed", book.title );
			assertEquals( 1, book.version );
		} );
	}

	@Entity(name = "GuideBook")
	public static class Book {
		@Id
		private Long id;
		private String title;
		@Version
		private long version;

		public Book() {
		}

		Book(Long id, String title) {
			this.id = id;
			this.title = title;
		}
	}
}
