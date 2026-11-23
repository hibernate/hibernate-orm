package org.hibernate.temporal.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Removing an audited entity by an uninitialized proxy records its state in the audit log.
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditDeleteUninitializedProxyTest.Book.class,
		AuditDeleteUninitializedProxyTest.Writer.class
})
@ServiceRegistry(settings = @Setting(name = CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditDeleteUninitializedProxyTest$ChangesetIds"))
class AuditDeleteUninitializedProxyTest {

	@Test
	void removeUninitializedProxy(SessionFactoryScope scope) {
		// changeset 1
		scope.inTransaction( session -> {
			final var writer = new Writer();
			writer.id = 1;
			writer.name = "Writer";
			session.persist( writer );
			final var book = new Book();
			book.id = 1;
			book.writer = writer;
			session.persist( book );
		} );
		// changeset 2
		scope.inTransaction( session -> {
			final var book = session.find( Book.class, 1 );
			final var writer = book.writer;
			assertFalse( Hibernate.isInitialized( writer ) );
			book.writer = null;
			session.remove( writer );
		} );
		scope.inSession( session -> assertNull( session.find( Writer.class, 1 ) ) );
		try (var session = scope.getSessionFactory().withOptions().atChangeset( 1 ).openSession()) {
			assertEquals( "Writer", session.find( Writer.class, 1 ).name );
		}
		try (var session = scope.getSessionFactory().withOptions().atChangeset( 2 ).openSession()) {
			assertNull( session.find( Writer.class, 1 ) );
		}
	}

	@Audited
	@Entity(name = "Book")
	static class Book {
		@Id
		int id;
		@ManyToOne(fetch = LAZY)
		Writer writer;
	}

	@Audited
	@Entity(name = "Writer")
	static class Writer {
		@Id
		int id;
		String name;
	}

	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}
}
