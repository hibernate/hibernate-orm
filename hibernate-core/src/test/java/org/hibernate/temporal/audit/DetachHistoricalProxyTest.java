package org.hibernate.temporal.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.audit.AuditLog.ALL_CHANGESETS;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey("HHH-21231")
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		DetachHistoricalProxyTest.Book.class,
		DetachHistoricalProxyTest.Loan.class,
		DetachHistoricalProxyTest.Shelf.class
})
@ServiceRegistry(settings = @Setting(name = CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.DetachHistoricalProxyTest$Ids"))
class DetachHistoricalProxyTest {
	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inTransaction( session -> {
			var book = new Book();
			book.title = "old";
			session.persist( book );
			var loan = new Loan();
			loan.book = book;
			loan.note = "first";
			session.persist( loan );
			var shelf = new Shelf();
			shelf.book = book;
			session.persist( shelf );
		} );
		scope.inTransaction( session -> {
			session.find( Book.class, 1 ).title = "current";
			session.find( Loan.class, 1 ).note = "second";
		} );
	}

	@Test
	void detachInitializedHistoricalProxy(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			var current = findCurrentBook( session );
			var oldProxy = findHistoricalBook( session );
			assertFalse( Hibernate.isInitialized( oldProxy ) );
			assertEquals( "old", oldProxy.getTitle() );
			assertEquals( "current", current.getTitle() );
			var oldBook = Hibernate.unproxy( oldProxy, Book.class );
			assertNotSame( current, oldBook );
			assertTrue( session.contains( oldProxy ) );
			assertTrue( session.contains( current ) );

			session.detach( oldProxy );

			assertTrue( session.contains( current ), "Detaching the historical proxy also detached the current book" );
			assertFalse( session.contains( oldBook ) );
			assertFalse( session.contains( oldProxy ) );
			assertSame( current, findCurrentBook( session ) );
		}
	}

	@Test
	void detachUninitializedHistoricalProxy(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			var current = findCurrentBook( session );
			var oldProxy = findHistoricalBook( session );
			assertNotSame( current, oldProxy );
			assertFalse( Hibernate.isInitialized( oldProxy ) );
			assertTrue( session.contains( oldProxy ) );
			assertSame( current, session.getReference( Book.class, 1 ) );

			session.detach( oldProxy );

			assertFalse( Hibernate.isInitialized( oldProxy ) );
			assertFalse( session.contains( oldProxy ) );
			assertTrue( session.contains( current ) );
			assertSame( current, session.getReference( Book.class, 1 ) );
			assertSame( current, findCurrentBook( session ) );
		}
	}

	@Test
	void detachOrdinaryProxy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var proxy = session.getReference( Book.class, 1 );
			assertTrue( session.contains( proxy ) );
			assertFalse( Hibernate.isInitialized( proxy ) );

			session.detach( proxy );

			assertFalse( Hibernate.isInitialized( proxy ) );
			assertFalse( session.contains( proxy ) );
			assertNotSame( proxy, session.getReference( Book.class, 1 ) );
		} );
	}

	@Test
	void detachOrdinaryEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var book = session.find( Book.class, 1 );
			assertTrue( session.contains( book ) );

			session.detach( book );

			assertFalse( session.contains( book ) );
			assertNotSame( book, session.find( Book.class, 1 ) );
		} );
	}

	private static Book findCurrentBook(Session session) {
		return session.createSelectionQuery(
				"from Shelf s join fetch s.book b where b.title = 'current'", Shelf.class
		).getSingleResult().book;
	}

	private static Book findHistoricalBook(Session session) {
		return session.createSelectionQuery( "from Loan l where l.note = 'first'", Loan.class )
				.getSingleResult().book;
	}

	@Entity(name = "Book")
	@Audited
	public static class Book {
		@Id
		int id = 1;
		String title;

		public String getTitle() {
			return title;
		}
	}

	@Entity(name = "Loan")
	@Audited
	public static class Loan {
		@Id
		int id = 1;
		String note;
		@ManyToOne(fetch = LAZY)
		Book book;
	}

	@Entity(name = "Shelf")
	public static class Shelf {
		@Id
		int id = 1;
		@ManyToOne
		Book book;
	}

	public static class Ids implements ChangesetIdentifierSupplier<Integer> {
		private int next;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++next;
		}
	}
}
