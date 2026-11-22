package org.hibernate.temporal.audit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

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

import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The index of an audited unidirectional one-to-many collection, an order column
 * or a map key column in the table of the element, is audited with the collection,
 * and read at a changeset.
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditOneToManyIndexTest.Playlist.class,
		AuditOneToManyIndexTest.Track.class,
		AuditOneToManyIndexTest.Shelf.class,
		AuditOneToManyIndexTest.Book.class,
		AuditOneToManyIndexTest.Label.class
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

	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}
}
