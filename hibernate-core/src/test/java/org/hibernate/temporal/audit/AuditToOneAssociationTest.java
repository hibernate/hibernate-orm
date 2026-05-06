/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.temporal.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLog;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.audit.ModificationType;
import org.hibernate.cfg.StateManagementSettings;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.AuditedTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests @Audited with @ManyToOne and @OneToOne associations.
 */
@AuditedTest
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditToOneAssociationTest.Publisher.class,
		AuditToOneAssociationTest.Author.class,
		AuditToOneAssociationTest.Book.class,
		AuditToOneAssociationTest.LazyBook.class,
		AuditToOneAssociationTest.Person.class,
		AuditToOneAssociationTest.Passport.class
})
@ServiceRegistry(settings = @Setting(name = StateManagementSettings.CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditToOneAssociationTest$TxIdSupplier"))
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuditToOneAssociationTest {
	private static int currentTxId;

	public static class TxIdSupplier implements ChangesetIdentifierSupplier<Integer> {
		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++currentTxId;
		}
	}

	// @ManyToOne revision markers
	private int revCreate;      // Book(1) + Author(1) + Publisher(1) + LazyBook(2) created
	private int revUpdate;      // all updated to V2
	private int revReassign;    // Book(1).author reassigned to Author(2)
	private int revNull;        // Book(1).author set to null

	// Null association lifecycle markers
	private int revNullCreate;  // Book(10) created with null author
	private int revNullSet;     // Book(10).author set to Author(10)
	private int revNullClear;   // Book(10).author cleared

	// @OneToOne markers
	private int revO2oCreate;   // Person(1) + Passport(1)
	private int revO2oReassign; // Person(1).passport reassigned to Passport(2)
	private int revO2oNull;     // Person(1).passport set to null

	@BeforeClassTemplate
	void initData(SessionFactoryScope scope) {
		currentTxId = 0;
		final var sf = scope.getSessionFactory();

		// Rev 1: create Publisher + Author + Book + LazyBook
		sf.inTransaction( session -> {
			var pub = new Publisher( 1L, "Pub V1" );
			session.persist( pub );
			var author = new Author( 1L, "Author V1", pub );
			session.persist( author );
			session.persist( new Book( 1L, "Book V1", author ) );
			session.persist( new LazyBook( 2L, "Lazy Book V1", author ) );
		} );
		revCreate = currentTxId;

		// Rev 2: update all names
		sf.inTransaction( session -> {
			session.find( Publisher.class, 1L ).setName( "Pub V2" );
			session.find( Author.class, 1L ).setName( "Author V2" );
			session.find( Book.class, 1L ).setTitle( "Book V2" );
		} );
		revUpdate = currentTxId;

		// Rev 3: reassign Book.author to new Author
		sf.inTransaction( session -> {
			var author2 = new Author( 2L, "Author B" );
			session.persist( author2 );
			session.find( Book.class, 1L ).setAuthor( author2 );
		} );
		revReassign = currentTxId;

		// Rev 4: null out Book.author
		sf.inTransaction( session ->
				session.find( Book.class, 1L ).setAuthor( null )
		);
		revNull = currentTxId;

		// --- Null association lifecycle ---

		// Rev 5: create Book with null author
		sf.inTransaction( session ->
				session.persist( new Book( 10L, "Orphan Book", null ) )
		);
		revNullCreate = currentTxId;

		// Rev 6: set author
		sf.inTransaction( session -> {
			var author = new Author( 10L, "Late Author" );
			session.persist( author );
			session.find( Book.class, 10L ).setAuthor( author );
		} );
		revNullSet = currentTxId;

		// Rev 7: clear author
		sf.inTransaction( session ->
				session.find( Book.class, 10L ).setAuthor( null )
		);
		revNullClear = currentTxId;

		// --- @OneToOne ---

		// Rev 8: create Person + Passport
		sf.inTransaction( session -> {
			var passport = new Passport( 1L, "AB123" );
			session.persist( passport );
			var person = new Person( 1L, "Alice" );
			person.passport = passport;
			session.persist( person );
		} );
		revO2oCreate = currentTxId;

		// Rev 9: reassign passport
		sf.inTransaction( session -> {
			var passport2 = new Passport( 2L, "CD456" );
			session.persist( passport2 );
			session.find( Person.class, 1L ).passport = passport2;
		} );
		revO2oReassign = currentTxId;

		// Rev 10: null passport
		sf.inTransaction( session ->
				session.find( Person.class, 1L ).passport = null
		);
		revO2oNull = currentTxId;
	}

	// --- Write side ---

	@Test
	@Order(1)
	void testWriteSideRevisionCounts(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			// Book(1): create, update, reassign, null = 4 revisions
			assertEquals( 4, auditLog.getRevisions( Book.class, 1L ).size() );
			// Person(1): create, reassign, null = 3 revisions
			assertEquals( 3, auditLog.getRevisions( Person.class, 1L ).size() );
		}
	}

	// --- @ManyToOne point-in-time reads ---

	@Test
	@Order(2)
	void testPointInTimeRead(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		// Rev 1: Book->Author->Publisher at creation
		try (var s = sf.withStatelessOptions().atChangeset( revCreate ).openStatelessSession()) {
			var book = s.get( Book.class, 1L );
			assertEquals( "Book V1", book.getTitle() );
			assertEquals( "Author V1", book.getAuthor().getName() );
			assertEquals( "Pub V1", book.getAuthor().getPublisher().getName() );
		}

		// Rev 2: all updated
		try (var s = sf.withStatelessOptions().atChangeset( revUpdate ).openStatelessSession()) {
			var book = s.get( Book.class, 1L );
			assertEquals( "Book V2", book.getTitle() );
			assertEquals( "Author V2", book.getAuthor().getName() );
			assertEquals( "Pub V2", book.getAuthor().getPublisher().getName() );
		}

		// Rev 3: FK reassigned
		try (var s = sf.withStatelessOptions().atChangeset( revReassign ).openStatelessSession()) {
			var book = s.get( Book.class, 1L );
			assertEquals( 2L, book.getAuthor().id );
			assertEquals( "Author B", book.getAuthor().getName() );
		}

		// Rev 4: FK null
		try (var s = sf.withStatelessOptions().atChangeset( revNull ).openStatelessSession()) {
			assertNull( s.get( Book.class, 1L ).getAuthor() );
		}
	}

	@Test
	@Order(3)
	void testLazyPointInTimeRead(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		try (var s = sf.withOptions().atChangeset( revCreate ).openSession()) {
			var author = s.find( LazyBook.class, 2L ).getAuthor();
			assertEquals( "Author V1", author.getName() );
			assertEquals( "Pub V1", author.getPublisher().getName() );
		}

		try (var s = sf.withOptions().atChangeset( revUpdate ).openSession()) {
			var author = s.find( LazyBook.class, 2L ).getAuthor();
			assertEquals( "Author V2", author.getName() );
			assertEquals( "Pub V2", author.getPublisher().getName() );
		}
	}

	// --- getHistory ---

	@Test
	@Order(4)
	void testGetHistory(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			var history = auditLog.getHistory( Book.class, 1L );
			assertEquals( 4, history.size() );

			// Rev 1: created with Author V1 -> Publisher V1
			assertEquals( ModificationType.ADD, history.get( 0 ).modificationType() );
			assertEquals( "Book V1", history.get( 0 ).entity().getTitle() );
			assertEquals( "Author V1", history.get( 0 ).entity().getAuthor().getName() );
			assertEquals( "Pub V1", history.get( 0 ).entity().getAuthor().getPublisher().getName() );

			// Rev 2: updated
			assertEquals( ModificationType.MOD, history.get( 1 ).modificationType() );
			assertEquals( "Book V2", history.get( 1 ).entity().getTitle() );
			assertEquals( "Author V2", history.get( 1 ).entity().getAuthor().getName() );

			// Rev 3: FK reassigned
			assertEquals( ModificationType.MOD, history.get( 2 ).modificationType() );
			assertEquals( "Author B", history.get( 2 ).entity().getAuthor().getName() );

			// Rev 4: FK null
			assertEquals( ModificationType.MOD, history.get( 3 ).modificationType() );
			assertNull( history.get( 3 ).entity().getAuthor() );
		}
	}

	// --- HQL join fetch ---

	@Test
	@Order(5)
	void testJoinFetchAllRevisions(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions()
				.atChangeset( AuditLog.ALL_CHANGESETS ).openSession()) {
			// Single-level join fetch: inner join filters out revNull (null author)
			final var rows = session.createSelectionQuery(
					"select e, changesetId(e), modificationType(e)"
					+ " from Book e join fetch e.author"
					+ " where e.id = :id"
					+ " order by changesetId(e)",
					Object[].class
			).setParameter( "id", 1L ).getResultList();

			assertEquals( 3, rows.size() );
			assertEquals( "Author V1", ((Book) rows.get( 0 )[0]).getAuthor().getName() );
			assertEquals( "Author V2", ((Book) rows.get( 1 )[0]).getAuthor().getName() );
			assertEquals( "Author B", ((Book) rows.get( 2 )[0]).getAuthor().getName() );

			// Nested join fetch: only revisions where full chain exists (create + update)
			final var nestedRows = session.createSelectionQuery(
					"select e, changesetId(e)"
					+ " from Book e"
					+ " join fetch e.author a"
					+ " join fetch a.publisher"
					+ " where e.id = :id"
					+ " order by changesetId(e)",
					Object[].class
			).setParameter( "id", 1L ).getResultList();

			assertEquals( 2, nestedRows.size() );
			assertEquals( "Pub V1", ((Book) nestedRows.get( 0 )[0]).getAuthor().getPublisher().getName() );
			assertEquals( "Pub V2", ((Book) nestedRows.get( 1 )[0]).getAuthor().getPublisher().getName() );
		}
	}

	@Test
	@Order(6)
	void testJoinFetchPointInTime(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		try (var session = sf.withOptions().atChangeset( revCreate ).openSession()) {
			final var book = session.createSelectionQuery(
					"from Book e join fetch e.author where e.id = :id",
					Book.class
			).setParameter( "id", 1L ).getSingleResult();

			assertEquals( "Book V1", book.getTitle() );
			assertEquals( "Author V1", book.getAuthor().getName() );
		}

		try (var session = sf.withOptions().atChangeset( revUpdate ).openSession()) {
			final var book = session.createSelectionQuery(
					"from Book e join fetch e.author where e.id = :id",
					Book.class
			).setParameter( "id", 1L ).getSingleResult();

			assertEquals( "Book V2", book.getTitle() );
			assertEquals( "Author V2", book.getAuthor().getName() );
		}
	}

	@Test
	@Order(7)
	void testExplicitEntityJoinPointInTime(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		// Explicit entity join (not fetch join), hits consumeEntityJoin path
		try (var session = sf.withOptions().atChangeset( revCreate ).openSession()) {
			final var rows = session.createSelectionQuery(
					"select b, a from Book b join Author a on a.id = b.author.id"
					+ " where b.id = :id",
					Object[].class
			).setParameter( "id", 1L ).getResultList();

			assertEquals( 1, rows.size() );
			assertEquals( "Book V1", ((Book) rows.get( 0 )[0]).getTitle() );
			assertEquals( "Author V1", ((Author) rows.get( 0 )[1]).getName() );
		}

		try (var session = sf.withOptions().atChangeset( revUpdate ).openSession()) {
			final var rows = session.createSelectionQuery(
					"select b, a from Book b join Author a on a.id = b.author.id"
					+ " where b.id = :id",
					Object[].class
			).setParameter( "id", 1L ).getResultList();

			assertEquals( 1, rows.size() );
			assertEquals( "Book V2", ((Book) rows.get( 0 )[0]).getTitle() );
			assertEquals( "Author V2", ((Author) rows.get( 0 )[1]).getName() );
		}
	}

	// --- Null association lifecycle ---

	@Test
	@Order(8)
	void testNullAssociationPointInTimeRead(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		try (var s = sf.withStatelessOptions().atChangeset( revNullCreate ).openStatelessSession()) {
			assertNull( s.get( Book.class, 10L ).getAuthor() );
		}

		try (var s = sf.withStatelessOptions().atChangeset( revNullSet ).openStatelessSession()) {
			assertEquals( "Late Author", s.get( Book.class, 10L ).getAuthor().getName() );
		}

		try (var s = sf.withStatelessOptions().atChangeset( revNullClear ).openStatelessSession()) {
			assertNull( s.get( Book.class, 10L ).getAuthor() );
		}
	}

	@Test
	@Order(9)
	void testNullAssociationGetHistory(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			var history = auditLog.getHistory( Book.class, 10L );
			assertEquals( 3, history.size() );

			assertEquals( ModificationType.ADD, history.get( 0 ).modificationType() );
			assertNull( history.get( 0 ).entity().getAuthor() );

			assertEquals( ModificationType.MOD, history.get( 1 ).modificationType() );
			assertEquals( "Late Author", history.get( 1 ).entity().getAuthor().getName() );

			assertEquals( ModificationType.MOD, history.get( 2 ).modificationType() );
			assertNull( history.get( 2 ).entity().getAuthor() );
		}
	}

	@Test
	@Order(10)
	void testLeftJoinFetchNullAssociationAllRevisions(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions()
				.atChangeset( AuditLog.ALL_CHANGESETS ).openSession()) {
			final var rows = session.createSelectionQuery(
					"select e, changesetId(e), modificationType(e)"
					+ " from Book e left join fetch e.author"
					+ " where e.id = :id"
					+ " order by changesetId(e)",
					Object[].class
			).setParameter( "id", 10L ).getResultList();

			assertEquals( 3, rows.size() );
			assertNull( ((Book) rows.get( 0 )[0]).getAuthor() );
			assertEquals( "Late Author", ((Book) rows.get( 1 )[0]).getAuthor().getName() );
			assertNull( ((Book) rows.get( 2 )[0]).getAuthor() );
		}
	}

	// --- @OneToOne ---

	@Test
	@Order(11)
	void testOneToOnePointInTimeRead(SessionFactoryScope scope) {
		final var sf = scope.getSessionFactory();

		try (var s = sf.withStatelessOptions().atChangeset( revO2oCreate ).openStatelessSession()) {
			assertEquals( "AB123", s.get( Person.class, 1L ).passport.number );
		}

		try (var s = sf.withStatelessOptions().atChangeset( revO2oReassign ).openStatelessSession()) {
			assertEquals( "CD456", s.get( Person.class, 1L ).passport.number );
		}

		try (var s = sf.withStatelessOptions().atChangeset( revO2oNull ).openStatelessSession()) {
			assertNull( s.get( Person.class, 1L ).passport );
		}
	}

	@Test
	@Order(12)
	void testOneToOneGetHistory(SessionFactoryScope scope) {
		try (var auditLog = AuditLogFactory.create( scope.getSessionFactory() )) {
			var history = auditLog.getHistory( Person.class, 1L );
			assertEquals( 3, history.size() );
			assertEquals( "AB123", history.get( 0 ).entity().passport.number );
			assertEquals( "CD456", history.get( 1 ).entity().passport.number );
			assertNull( history.get( 2 ).entity().passport );
		}
	}

	// ---- Entity classes ----

	@Audited
	@Entity(name = "Publisher")
	static class Publisher {
		@Id
		long id;
		@Column(name = "name_col")
		String name;

		Publisher() {
		}

		Publisher(long id, String name) {
			this.id = id;
			this.name = name;
		}

		String getName() {
			return name;
		}

		void setName(String name) {
			this.name = name;
		}
	}

	@Audited
	@Entity(name = "Author")
	static class Author {
		@Id
		long id;
		@Column(name = "name_col")
		String name;
		@ManyToOne
		Publisher publisher;

		Author() {
		}

		Author(long id, String name) {
			this.id = id;
			this.name = name;
		}

		Author(long id, String name, Publisher publisher) {
			this.id = id;
			this.name = name;
			this.publisher = publisher;
		}

		String getName() {
			return name;
		}

		void setName(String name) {
			this.name = name;
		}

		Publisher getPublisher() {
			return publisher;
		}
	}

	@Audited
	@Entity(name = "Book")
	static class Book {
		@Id
		long id;
		String title;
		@ManyToOne
		Author author;

		Book() {
		}

		Book(long id, String title, Author author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}

		String getTitle() {
			return title;
		}

		void setTitle(String title) {
			this.title = title;
		}

		Author getAuthor() {
			return author;
		}

		void setAuthor(Author author) {
			this.author = author;
		}
	}

	@Audited
	@Entity(name = "LazyBook")
	static class LazyBook {
		@Id
		long id;
		String title;
		@ManyToOne(fetch = FetchType.LAZY)
		Author author;

		LazyBook() {
		}

		LazyBook(long id, String title, Author author) {
			this.id = id;
			this.title = title;
			this.author = author;
		}

		String getTitle() {
			return title;
		}

		void setTitle(String title) {
			this.title = title;
		}

		Author getAuthor() {
			return author;
		}
	}

	@Audited
	@Entity(name = "Person")
	static class Person {
		@Id
		long id;
		@Column(name = "name_col")
		String name;
		@OneToOne
		Passport passport;

		Person() {
		}

		Person(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Audited
	@Entity(name = "Passport")
	static class Passport {
		@Id
		long id;
		@Column(name = "number_col")
		String number;

		Passport() {
		}

		Passport(long id, String number) {
			this.id = id;
			this.number = number;
		}
	}
}
