package org.hibernate.temporal;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OrderColumn;

import org.hibernate.Hibernate;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Temporal;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.audit.AuditLog.ALL_CHANGESETS;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parts of the state of an audit revision which are read at the changeset of
 * the revision in an all-changesets session: an {@code @Any} target, a to-one in
 * an embeddable element, an entity map key, a temporal element of a collection
 * fetched through a to-one, the target of a to-one of an unchanged target, the
 * temporal collection of a temporal target, a proxy being refreshed, and the size
 * and contents of a collection, counted without initializing it. A mutation query
 * may not target an entity with history in a historical session.
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		AllChangesetsRevisionStateTest.Event.class,
		AllChangesetsRevisionStateTest.Edition.class,
		AllChangesetsRevisionStateTest.Image.class,
		AllChangesetsRevisionStateTest.Article.class,
		AllChangesetsRevisionStateTest.Doc.class,
		AllChangesetsRevisionStateTest.Author.class,
		AllChangesetsRevisionStateTest.Book.class,
		AllChangesetsRevisionStateTest.Writer.class
})
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER,
				value = "org.hibernate.temporal.AllChangesetsRevisionStateTest$ChangesetIds")
})
class AllChangesetsRevisionStateTest {

	private static Object historyCreatedFor;

	@BeforeEach
	void createHistory(SessionFactoryScope scope) {
		if ( historyCreatedFor == scope.getSessionFactory() ) {
			return;
		}
		historyCreatedFor = scope.getSessionFactory();
		// changeset 1
		scope.inTransaction( session -> {
			final var image = new Image();
			image.id = 1;
			image.title = "I1";
			session.persist( image );
			final var other = new Image();
			other.id = 2;
			other.title = "J1";
			session.persist( other );
			final var doc = new Doc();
			doc.id = 1;
			doc.title = "D1";
			doc.tags.add( "one" );
			doc.lines.addAll( Arrays.asList( "a", null, "c" ) );
			session.persist( doc );
			final var author = new Author();
			author.id = 1;
			author.name = "Original name";
			session.persist( author );
			final var article = new Article();
			article.id = 1;
			article.author = author;
			article.documents.add( doc );
			session.persist( article );
			final var event = new Event();
			event.id = 1;
			event.label = "First";
			event.article = article;
			event.doc = doc;
			event.target = image;
			final var link = new Link();
			link.image = image;
			event.links.add( link );
			event.notes.put( image, "note" );
			event.gallery.put( "cover", image );
			event.pairs.put( image, other );
			event.images.add( image );
			session.persist( event );
			final var edition = new Edition();
			edition.id = 1;
			edition.name = "Edition";
			final var editionLink = new Link();
			editionLink.image = image;
			edition.links.add( editionLink );
			edition.notes.put( image, "note" );
			edition.covers.add( image );
			session.persist( edition );
			final var writer = new Writer();
			writer.id = 1;
			writer.name = "Writer";
			session.persist( writer );
			final var book = new Book();
			book.id = 1;
			book.title = "T1";
			book.writer = writer;
			session.persist( book );
		} );
		// changeset 2
		scope.inTransaction( session -> {
			session.find( Image.class, 1 ).title = "I2";
			final var doc = session.find( Doc.class, 1 );
			doc.title = "D2";
			doc.tags.add( "two" );
			session.find( Author.class, 1 ).name = "Revised name";
			final var event = Hibernate.unproxy( session.find( Event.class, 1 ), Event.class );
			event.label = "Second";
			event.images.add( session.find( Image.class, 2 ) );
			final var book = session.find( Book.class, 1 );
			book.title = "T2";
			session.remove( session.find( Writer.class, 1 ) );
			book.writer = null;
		} );
		// changeset 3
		scope.inTransaction( session -> {
			session.find( Image.class, 1 ).title = "I3";
			final var doc = session.find( Doc.class, 1 );
			doc.title = "D3";
			doc.tags.add( "three" );
		} );
	}

	@Test
	void anyTargetOfRevision(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			final var titles = new ArrayList<String>();
			for ( var event : session.createSelectionQuery(
					"from Event e where e.id = 1 order by changesetId(e)", Event.class ).getResultList() ) {
				titles.add( ( (Image) Hibernate.unproxy( event.target ) ).title );
			}
			assertEquals( List.of( "I1", "I2" ), titles );
		}
	}

	@Test
	void toOneInEmbeddableElementOfRevision(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			assertEquals( List.of( "I1", "I2" ), session.createSelectionQuery(
					"select i.title from Event e join e.links l join l.image i where e.id = 1 order by changesetId(e)",
					String.class ).getResultList() );
		}
	}

	@Test
	void toOneInEmbeddableElementOfCurrentOwner(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			assertEquals( List.of( "I3" ), session.createSelectionQuery(
					"select i.title from Edition ed join ed.links l join l.image i where ed.id = 1",
					String.class ).getResultList() );
		}
	}

	@Test
	void entityMapKeyOfRevision(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			assertEquals( List.of( "I1", "I2" ), session.createSelectionQuery(
					"select key(m).title from Event e join e.notes m where e.id = 1 order by changesetId(e)",
					String.class ).getResultList() );
		}
		try (var session = scope.getSessionFactory().withOptions().atChangeset( 2 ).openSession()) {
			assertEquals( List.of( "I2" ), session.createSelectionQuery(
					"select key(m).title from Event e join e.notes m where e.id = 1",
					String.class ).getResultList() );
		}
	}

	@Test
	void temporalCollectionFetchedThroughToOneOfRevision(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			final var titles = new ArrayList<List<String>>();
			for ( var event : session.createSelectionQuery(
					"from Event e left join fetch e.article a left join fetch a.documents where e.id = 1 order by changesetId(e)",
					Event.class ).getResultList() ) {
				titles.add( event.article.documents.stream().map( doc -> doc.title ).toList() );
			}
			assertEquals( List.of( List.of( "D1" ), List.of( "D2" ) ), titles );
		}
	}

	@Test
	void sizeAndContentsOfCollectionOfRevision(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			final var events = session.createSelectionQuery(
					"from Event e where e.id = 1 order by changesetId(e)", Event.class ).getResultList();
			final var first = events.get( 0 );
			final var second = events.get( 1 );
			assertEquals( 1, Hibernate.size( first.images ) );
			assertEquals( 2, Hibernate.size( second.images ) );
			assertFalse( Hibernate.isInitialized( first.images ) );
			assertFalse( Hibernate.isInitialized( second.images ) );
			final var added = second.images.stream().filter( image -> image.id == 2 ).findFirst().orElseThrow();
			assertFalse( Hibernate.contains( first.images, added ) );
			assertFalse( Hibernate.isInitialized( first.images ) );
			assertTrue( Hibernate.contains( second.images, added ) );
		}
		try (var session = scope.getSessionFactory().withOptions().atChangeset( 1 ).openSession()) {
			final var event = session.find( Event.class, 1 );
			assertEquals( 1, Hibernate.size( event.images ) );
			assertFalse( Hibernate.isInitialized( event.images ) );
			assertFalse( Hibernate.contains( event.images, session.find( Image.class, 2 ) ) );
			assertTrue( Hibernate.contains( event.images, session.find( Image.class, 1 ) ) );
			assertFalse( Hibernate.isInitialized( event.images ) );
			assertEquals( "note", Hibernate.get( event.notes, session.find( Image.class, 1 ) ) );
			assertFalse( Hibernate.isInitialized( event.notes ) );
		}
	}

	@Test
	void sizeOfTemporalCollectionAtChangeset(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( 1 ).openSession()) {
			final var doc = session.find( Doc.class, 1 );
			assertEquals( 1, Hibernate.size( doc.tags ) );
			assertFalse( Hibernate.isInitialized( doc.tags ) );
			assertEquals( Set.of( "one" ), doc.tags );
		}
	}

	@Test
	void entityMapValueAtChangeset(SessionFactoryScope scope) {
		for ( int changeset = 1; changeset <= 2; changeset++ ) {
			try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
				final var event = session.find( Event.class, 1 );
				final var cover = Hibernate.get( event.gallery, "cover" );
				assertEquals( "I" + changeset, Hibernate.unproxy( cover, Image.class ).title );
				assertFalse( Hibernate.isInitialized( event.gallery ) );
			}
		}
	}

	@Test
	void entityMapKeyAndValueOfSameTypeAtChangeset(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( 1 ).openSession()) {
			final var event = session.find( Event.class, 1 );
			final var image = session.find( Image.class, 1 );
			final var other = session.find( Image.class, 2 );
			assertSame( other, Hibernate.get( event.pairs, image ) );
			// the value is not a key
			assertNull( Hibernate.get( event.pairs, other ) );
			assertFalse( Hibernate.isInitialized( event.pairs ) );
		}
	}

	@Test
	void lookupsMatchSnapshots(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			final var current = session.find( Image.class, 1 );
			// the snapshot of the image held by the first revision of the event
			final var snapshot = session.createSelectionQuery(
					"select i from Event e join e.images i where e.id = 1 and i.id = 1 order by changesetId(e)",
					Image.class ).getResultList().get( 0 );
			assertNotSame( current, snapshot );
			final var first = session.createSelectionQuery(
					"from Event e where e.id = 1 order by changesetId(e)", Event.class ).getResultList().get( 0 );
			final var edition = session.find( Edition.class, 1 );

			// a collection of a revision holds the snapshot, and a collection of current state the current image
			assertTrue( Hibernate.contains( first.images, snapshot ) );
			assertFalse( Hibernate.contains( first.images, current ) );
			assertEquals( "note", Hibernate.get( first.notes, snapshot ) );
			assertNull( Hibernate.get( first.notes, current ) );
			assertTrue( Hibernate.contains( edition.covers, current ) );
			assertFalse( Hibernate.contains( edition.covers, snapshot ) );
			assertEquals( "note", Hibernate.get( edition.notes, current ) );
			assertNull( Hibernate.get( edition.notes, snapshot ) );
			assertFalse( Hibernate.isInitialized( first.images ) );
			assertFalse( Hibernate.isInitialized( first.notes ) );
			assertFalse( Hibernate.isInitialized( edition.covers ) );
			assertFalse( Hibernate.isInitialized( edition.notes ) );

			// just as the initialized collections find
			assertTrue( first.images.contains( snapshot ) );
			assertFalse( first.images.contains( current ) );
			assertEquals( "note", first.notes.get( snapshot ) );
			assertNull( first.notes.get( current ) );
			assertTrue( edition.covers.contains( current ) );
			assertFalse( edition.covers.contains( snapshot ) );
			assertEquals( "note", edition.notes.get( current ) );
			assertNull( edition.notes.get( snapshot ) );
		}
	}

	@Test
	void listWithGapAtChangeset(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( 1 ).openSession()) {
			final var doc = session.find( Doc.class, 1 );
			assertEquals( 3, Hibernate.size( doc.lines ) );
			assertEquals( "c", Hibernate.get( doc.lines, 2 ) );
			assertFalse( Hibernate.isInitialized( doc.lines ) );
			assertEquals( Arrays.asList( "a", null, "c" ), doc.lines );
		}
	}

	@Test
	void entityMapKeyOfCurrentOwner(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			assertEquals( List.of( "I3" ), session.createSelectionQuery(
					"select key(n).title from Edition e join e.notes n where e.id = 1",
					String.class ).getResultList() );
		}
	}

	@Test
	void sizeFunctionOfRevision(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			assertEquals( List.of( 1, 2 ), session.createSelectionQuery(
					"select size(e.images) from Event e where e.id = 1 order by changesetId(e)",
					Integer.class ).getResultList() );
		}
	}

	@Test
	void plainJoinThroughUnchangedTargetOfRevision(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			// the article is unchanged, so both revisions of the event join its first revision
			assertEquals( List.of( "Original name", "Revised name" ), session.createSelectionQuery(
					"select au.name from Event e join e.article a join a.author au where e.id = 1 order by changesetId(e)",
					String.class ).getResultList() );
			final var names = new ArrayList<String>();
			for ( var article : session.createSelectionQuery(
					"select a from Event e join e.article a where e.id = 1 order by changesetId(e)",
					Article.class ).getResultList() ) {
				names.add( Hibernate.unproxy( article.author, Author.class ).name );
			}
			assertEquals( List.of( "Original name", "Revised name" ), names );
		}
	}

	@Test
	void temporalCollectionOfTemporalTargetOfRevision(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			final var fetched = new ArrayList<Set<String>>();
			for ( var event : session.createSelectionQuery(
					"from Event e join fetch e.doc d join fetch d.tags where e.id = 1 order by changesetId(e)",
					Event.class ).getResultList() ) {
				fetched.add( Set.copyOf( event.doc.tags ) );
			}
			assertEquals( List.of( Set.of( "one" ), Set.of( "one", "two" ) ), fetched );
		}
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			final var lazy = new ArrayList<Set<String>>();
			for ( var event : session.createSelectionQuery(
					"from Event e where e.id = 1 order by changesetId(e)", Event.class ).getResultList() ) {
				lazy.add( Set.copyOf( Hibernate.unproxy( event.doc, Doc.class ).tags ) );
			}
			assertEquals( List.of( Set.of( "one" ), Set.of( "one", "two" ) ), lazy );
		}
	}

	@Test
	void refreshUninitializedProxyOfRevision(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			final var books = session.createSelectionQuery(
					"from Book b where b.id = 1 order by changesetId(b)", Book.class ).getResultList();
			final var writer = books.get( 0 ).writer;
			assertFalse( Hibernate.isInitialized( writer ) );
			// the writer was deleted, but the first revision of the book refers to it
			session.refresh( writer );
			assertEquals( "Writer", Hibernate.unproxy( writer, Writer.class ).name );
		}
	}

	@Test
	void mutationQueryInHistoricalSession(SessionFactoryScope scope) {
		for ( Object changeset : List.of( ALL_CHANGESETS, 1 ) ) {
			try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
				session.getTransaction().begin();
				try {
					assertThrows( IllegalStateException.class, () -> session.createMutationQuery(
							"update Image set title = 'X' where id = 2" ).executeUpdate() );
					assertThrows( IllegalStateException.class, () -> session.createMutationQuery(
							"delete from Doc where id = 1" ).executeUpdate() );
					assertEquals( 1, session.createMutationQuery(
							"update Edition set name = 'X' where id = 1" ).executeUpdate() );
				}
				finally {
					session.getTransaction().rollback();
				}
			}
		}
		scope.inTransaction( session -> assertTrue(
				session.createSelectionQuery( "from Image where title = 'X'", Image.class ).getResultList().isEmpty() ) );
	}

	@Embeddable
	static class Link {
		@ManyToOne
		Image image;
	}

	@Audited
	@Entity(name = "Event")
	static class Event {
		@Id
		int id;
		String label;
		@ManyToOne(fetch = LAZY)
		Article article;
		@ManyToOne(fetch = LAZY)
		Doc doc;
		@Any(fetch = LAZY)
		@AnyDiscriminatorValue(discriminator = "I", entity = Image.class)
		@AnyKeyJavaClass(Integer.class)
		@Column(name = "target_type")
		@JoinColumn(name = "target_id")
		Object target;
		@ElementCollection
		List<Link> links = new ArrayList<>();
		@ElementCollection
		@MapKeyJoinColumn(name = "image_key")
		Map<Image, String> notes = new HashMap<>();
		@ManyToMany
		Set<Image> images = new HashSet<>();
		@ManyToMany
		@MapKeyColumn(name = "label")
		@JoinTable(name = "Event_gallery")
		Map<String, Image> gallery = new HashMap<>();
		@ManyToMany
		@MapKeyJoinColumn(name = "key_id")
		@JoinTable(name = "Event_pairs", inverseJoinColumns = @JoinColumn(name = "val_id"))
		Map<Image, Image> pairs = new HashMap<>();
	}

	@Entity(name = "Edition")
	static class Edition {
		@Id
		int id;
		String name;
		@ElementCollection
		List<Link> links = new ArrayList<>();
		@ElementCollection
		@CollectionTable(name = "Edition_notes")
		@MapKeyJoinColumn(name = "image_key")
		Map<Image, String> notes = new HashMap<>();
		@ManyToMany
		@JoinTable(name = "Edition_covers")
		Set<Image> covers = new HashSet<>();
	}

	@Audited
	@Entity(name = "Image")
	static class Image {
		@Id
		int id;
		String title;
	}

	@Audited
	@Entity(name = "Article")
	static class Article {
		@Id
		int id;
		@ManyToOne(fetch = LAZY)
		Author author;
		@ManyToMany
		Set<Doc> documents = new HashSet<>();
	}

	@Audited
	@Entity(name = "Author")
	static class Author {
		@Id
		int id;
		String name;
	}

	@Audited
	@Entity(name = "Book")
	static class Book {
		@Id
		int id;
		String title;
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

	@Temporal
	@Entity(name = "Doc")
	static class Doc {
		@Id
		int id;
		String title;
		@ElementCollection
		Set<String> tags = new HashSet<>();
		@ElementCollection
		@OrderColumn
		@Column(name = "line")
		List<String> lines = new ArrayList<>();
	}

	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}
}
