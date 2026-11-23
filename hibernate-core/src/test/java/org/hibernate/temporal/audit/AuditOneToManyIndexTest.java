package org.hibernate.temporal.audit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import org.hibernate.Hibernate;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.internal.CollectionLookupLoader;
import org.hibernate.loader.ast.internal.CollectionLookupLoader.Operation;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The index of an audited unidirectional one-to-many collection, an order column
 * or a map key column in the table of the element, is audited with the collection,
 * and read at a changeset. A map key which is an attribute of the element, or an
 * association of the element, is read from the audit table of the element, and an
 * association referring to a unique key is looked up by the identifier of the key.
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditOneToManyIndexTest.Playlist.class,
		AuditOneToManyIndexTest.Track.class,
		AuditOneToManyIndexTest.Shelf.class,
		AuditOneToManyIndexTest.Book.class,
		AuditOneToManyIndexTest.Label.class,
		AuditOneToManyIndexTest.Directory.class,
		AuditOneToManyIndexTest.DirectoryEntry.class,
		AuditOneToManyIndexTest.Bookcase.class,
		AuditOneToManyIndexTest.Novel.class,
		AuditOneToManyIndexTest.Genre.class,
		AuditOneToManyIndexTest.Cabinet.class,
		AuditOneToManyIndexTest.Dossier.class,
		AuditOneToManyIndexTest.Tag.class
})
@ServiceRegistry(settings = @Setting(name = CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditOneToManyIndexTest$ChangesetIds"))
class AuditOneToManyIndexTest {

	@Test
	void orderColumnOfJoinColumn(SessionFactoryScope scope) {
		final Object first = scope.fromTransaction( session -> {
			final var playlist = new Playlist();
			playlist.tracks.add( track( session, 1 ) );
			playlist.tracks.add( track( session, 2 ) );
			session.persist( playlist );
			return session.getCurrentChangesetIdentifier();
		} );
		final Object second = scope.fromTransaction( session -> {
			final var tracks = session.find( Playlist.class, 1 ).tracks;
			tracks.add( tracks.remove( 0 ) );
			return session.getCurrentChangesetIdentifier();
		} );

		assertEquals( List.of( 1, 2 ), tracksAt( scope, first ) );
		assertEquals( List.of( 2, 1 ), tracksAt( scope, second ) );
		try (var session = scope.getSessionFactory().withOptions().atChangeset( first ).openSession()) {
			final var tracks = session.find( Playlist.class, 1 ).tracks;
			assertSame( session.find( Track.class, 2 ), Hibernate.get( tracks, 1 ) );
			assertEquals( 2, Hibernate.size( tracks ) );
			assertFalse( Hibernate.isInitialized( tracks ) );
		}
	}

	@Test
	void mapKeyColumnOfJoinColumn(SessionFactoryScope scope) {
		final Object first = scope.fromTransaction( session -> {
			final var label = new Label();
			label.id = 1;
			session.persist( label );
			final var other = new Label();
			other.id = 2;
			session.persist( other );
			final var book = new Book();
			book.id = 1;
			session.persist( book );
			final var shelf = new Shelf();
			shelf.books.put( label, book );
			session.persist( shelf );
			return session.getCurrentChangesetIdentifier();
		} );
		final Object second = scope.fromTransaction( session -> {
			final var books = session.find( Shelf.class, 1 ).books;
			books.put( session.find( Label.class, 2 ), books.remove( session.find( Label.class, 1 ) ) );
			return session.getCurrentChangesetIdentifier();
		} );

		try (var session = scope.getSessionFactory().withOptions().atChangeset( first ).openSession()) {
			final var books = session.find( Shelf.class, 1 ).books;
			assertSame( session.find( Book.class, 1 ), Hibernate.get( books, session.find( Label.class, 1 ) ) );
			assertNull( Hibernate.get( books, session.find( Label.class, 2 ) ) );
			assertFalse( Hibernate.isInitialized( books ) );
			assertEquals( Map.of( 1, 1 ), labels( books ) );
		}
		try (var session = scope.getSessionFactory().withOptions().atChangeset( second ).openSession()) {
			assertEquals( Map.of( 2, 1 ), labels( session.find( Shelf.class, 1 ).books ) );
		}
	}

	@Test
	void mapKeyAttributeOfJoinColumn(SessionFactoryScope scope) {
		final Object first = scope.fromTransaction( session -> {
			final var directory = new Directory();
			directory.entries.put( "a", entry( session, 1, "a" ) );
			session.persist( directory );
			return session.getCurrentChangesetIdentifier();
		} );
		final Object second = scope.fromTransaction( session -> {
			session.find( Directory.class, 1 ).entries.put( "b", entry( session, 2, "b" ) );
			return session.getCurrentChangesetIdentifier();
		} );
		final Object third = scope.fromTransaction( session -> {
			session.find( Directory.class, 1 ).entries.remove( "a" );
			return session.getCurrentChangesetIdentifier();
		} );

		try (var session = scope.getSessionFactory().withOptions().atChangeset( first ).openSession()) {
			final var entries = session.find( Directory.class, 1 ).entries;
			assertSame( session.find( DirectoryEntry.class, 1 ), Hibernate.get( entries, "a" ) );
			assertNull( Hibernate.get( entries, "b" ) );
			assertTrue( containsKey( session, Directory.class, "entries", "a" ) );
			assertFalse( containsKey( session, Directory.class, "entries", "b" ) );
			assertFalse( Hibernate.isInitialized( entries ) );
			assertEquals( Map.of( "a", 1 ), codes( entries ) );
		}
		try (var session = scope.getSessionFactory().withOptions().atChangeset( second ).openSession()) {
			assertEquals( Map.of( "a", 1, "b", 2 ), codes( session.find( Directory.class, 1 ).entries ) );
		}
		try (var session = scope.getSessionFactory().withOptions().atChangeset( third ).openSession()) {
			final var entries = session.find( Directory.class, 1 ).entries;
			assertNull( Hibernate.get( entries, "a" ) );
			assertSame( session.find( DirectoryEntry.class, 2 ), Hibernate.get( entries, "b" ) );
			assertEquals( Map.of( "b", 2 ), codes( entries ) );
		}
	}

	@Test
	void mapKeyAssociationOfJoinColumn(SessionFactoryScope scope) {
		final Object first = scope.fromTransaction( session -> {
			final var bookcase = new Bookcase();
			bookcase.novels.put( genre( session, 1 ), novel( session, 1, 1 ) );
			genre( session, 2 );
			session.persist( bookcase );
			return session.getCurrentChangesetIdentifier();
		} );
		final Object second = scope.fromTransaction( session -> {
			session.find( Bookcase.class, 1 ).novels.put( session.find( Genre.class, 2 ), novel( session, 2, 2 ) );
			return session.getCurrentChangesetIdentifier();
		} );
		final Object third = scope.fromTransaction( session -> {
			session.find( Bookcase.class, 1 ).novels.remove( session.find( Genre.class, 1 ) );
			return session.getCurrentChangesetIdentifier();
		} );

		try (var session = scope.getSessionFactory().withOptions().atChangeset( first ).openSession()) {
			final var novels = session.find( Bookcase.class, 1 ).novels;
			assertSame( session.find( Novel.class, 1 ), Hibernate.get( novels, session.find( Genre.class, 1 ) ) );
			assertNull( Hibernate.get( novels, session.find( Genre.class, 2 ) ) );
			assertFalse( Hibernate.isInitialized( novels ) );
			assertEquals( Map.of( 1, 1 ), genres( novels ) );
		}
		try (var session = scope.getSessionFactory().withOptions().atChangeset( second ).openSession()) {
			assertEquals( Map.of( 1, 1, 2, 2 ), genres( session.find( Bookcase.class, 1 ).novels ) );
		}
		try (var session = scope.getSessionFactory().withOptions().atChangeset( third ).openSession()) {
			final var novels = session.find( Bookcase.class, 1 ).novels;
			assertNull( Hibernate.get( novels, session.find( Genre.class, 1 ) ) );
			assertEquals( Map.of( 2, 2 ), genres( novels ) );
		}
	}

	@Test
	void mapKeyAssociationReferringToUniqueKey(SessionFactoryScope scope) {
		final Object changeset = scope.fromTransaction( session -> {
			// the code of each tag is the identifier of the other
			final var first = new Tag();
			first.id = 1;
			first.code = 2;
			session.persist( first );
			final var second = new Tag();
			second.id = 2;
			second.code = 1;
			session.persist( second );
			final var dossier = new Dossier();
			dossier.id = 1;
			dossier.tag = first;
			session.persist( dossier );
			final var cabinet = new Cabinet();
			cabinet.dossiers.put( first, dossier );
			session.persist( cabinet );
			return session.getCurrentChangesetIdentifier();
		} );

		scope.inSession( session -> assertDossiers( session ) );
		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			assertDossiers( session );
		}
	}

	private static void assertDossiers(org.hibernate.Session session) {
		final var dossiers = session.find( Cabinet.class, 1 ).dossiers;
		assertSame( session.find( Dossier.class, 1 ), Hibernate.get( dossiers, session.find( Tag.class, 1 ) ) );
		assertNull( Hibernate.get( dossiers, session.find( Tag.class, 2 ) ) );
		assertFalse( Hibernate.isInitialized( dossiers ) );
		assertTrue( containsKey( session, Cabinet.class, "dossiers", session.find( Tag.class, 1 ) ) );
		assertFalse( containsKey( session, Cabinet.class, "dossiers", session.find( Tag.class, 2 ) ) );
	}

	/**
	 * The lookup of a key of the map of the owner with identifier 1, which only an
	 * extra-lazy map performs, and so no map mapped by {@code @MapKey}, since only a
	 * collection mapped in XML is extra-lazy.
	 */
	static boolean containsKey(org.hibernate.Session session, Class<?> owner, String role, Object key) {
		final var implementor = (SharedSessionContractImplementor) session;
		final var collection =
				implementor.getFactory().getMappingMetamodel()
						.getCollectionDescriptor( owner.getName() + "." + role )
						.getAttributeMapping();
		return new CollectionLookupLoader( collection, Operation.INDEX_EXISTS, implementor.getLoadQueryInfluencers() )
				.exists( 1, key, implementor );
	}

	private static Genre genre(SharedSessionContract session, int id) {
		final var genre = new Genre();
		genre.id = id;
		( (org.hibernate.Session) session ).persist( genre );
		return genre;
	}

	private static Novel novel(SharedSessionContract session, int id, int genre) {
		final var novel = new Novel();
		novel.id = id;
		novel.genre = ( (org.hibernate.Session) session ).find( Genre.class, genre );
		( (org.hibernate.Session) session ).persist( novel );
		return novel;
	}

	private static Map<Integer, Integer> genres(Map<Genre, Novel> novels) {
		final Map<Integer, Integer> ids = new HashMap<>();
		novels.forEach( (genre, novel) -> ids.put( genre.id, novel.id ) );
		return ids;
	}

	private static DirectoryEntry entry(SharedSessionContract session, int id, String code) {
		final var entry = new DirectoryEntry();
		entry.id = id;
		entry.code = code;
		( (org.hibernate.Session) session ).persist( entry );
		return entry;
	}

	private static Map<String, Integer> codes(Map<String, DirectoryEntry> entries) {
		final Map<String, Integer> ids = new HashMap<>();
		entries.forEach( (code, entry) -> ids.put( code, entry.id ) );
		return ids;
	}

	private static Track track(SharedSessionContract session, int id) {
		final var track = new Track();
		track.id = id;
		( (org.hibernate.Session) session ).persist( track );
		return track;
	}

	private static List<Integer> tracksAt(SessionFactoryScope scope, Object changeset) {
		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			return session.find( Playlist.class, 1 ).tracks.stream().map( track -> track.id ).toList();
		}
	}

	private static Map<Integer, Integer> labels(Map<Label, Book> books) {
		final Map<Integer, Integer> ids = new HashMap<>();
		books.forEach( (label, book) -> ids.put( label.id, book.id ) );
		return ids;
	}

	@Audited
	@Entity(name = "Playlist")
	static class Playlist {
		@Id
		int id = 1;
		@OneToMany
		@JoinColumn(name = "playlist_id")
		@OrderColumn(name = "track_index")
		List<Track> tracks = new ArrayList<>();
	}

	@Audited
	@Entity(name = "Track")
	static class Track {
		@Id
		int id;
	}

	@Audited
	@Entity(name = "Shelf")
	static class Shelf {
		@Id
		int id = 1;
		@OneToMany
		@JoinColumn(name = "shelf_id")
		@MapKeyJoinColumn(name = "label_id", nullable = true)
		Map<Label, Book> books = new HashMap<>();
	}

	@Audited
	@Entity(name = "Book")
	static class Book {
		@Id
		int id;
	}

	@Audited
	@Entity(name = "Label")
	static class Label {
		@Id
		int id;
	}

	@Audited
	@Entity(name = "Directory")
	static class Directory {
		@Id
		int id = 1;
		@OneToMany
		@JoinColumn(name = "directory_id")
		@MapKey(name = "code")
		Map<String, DirectoryEntry> entries = new HashMap<>();
	}

	@Audited
	@Entity(name = "DirectoryEntry")
	static class DirectoryEntry {
		@Id
		int id;
		String code;
	}

	@Audited
	@Entity(name = "Bookcase")
	static class Bookcase {
		@Id
		int id = 1;
		@OneToMany
		@JoinColumn(name = "bookcase_id")
		@MapKey(name = "genre")
		Map<Genre, Novel> novels = new HashMap<>();
	}

	@Audited
	@Entity(name = "Novel")
	static class Novel {
		@Id
		int id;
		@ManyToOne
		Genre genre;
	}

	@Audited
	@Entity(name = "Genre")
	static class Genre {
		@Id
		int id;
	}

	@Audited
	@Entity(name = "Cabinet")
	static class Cabinet {
		@Id
		int id = 1;
		@OneToMany
		@JoinColumn(name = "cabinet_id")
		@MapKey(name = "tag")
		Map<Tag, Dossier> dossiers = new HashMap<>();
	}

	@Audited
	@Entity(name = "Dossier")
	static class Dossier {
		@Id
		int id;
		@ManyToOne
		@JoinColumn(name = "tag_code", referencedColumnName = "code")
		Tag tag;
	}

	@Audited
	@Entity(name = "Tag")
	static class Tag {
		@Id
		int id;
		@Column(unique = true)
		int code;
	}

	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}
}
