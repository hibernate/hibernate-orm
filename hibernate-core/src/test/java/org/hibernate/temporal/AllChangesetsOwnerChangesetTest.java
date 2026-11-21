package org.hibernate.temporal;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.Hibernate.unproxy;
import static org.hibernate.audit.AuditLog.ALL_CHANGESETS;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * In an all-changesets session, an association resolves at the changeset of the
 * snapshot that owns it, and an association owned by current state, such as a
 * non-audited entity, refers to current state, however it is fetched.
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		AllChangesetsOwnerChangesetTest.Event.class,
		AllChangesetsOwnerChangesetTest.Article.class,
		AllChangesetsOwnerChangesetTest.Edition.class,
		AllChangesetsOwnerChangesetTest.Archive.class,
		AllChangesetsOwnerChangesetTest.Image.class,
		AllChangesetsOwnerChangesetTest.BatchedImage.class
})
@ServiceRegistry(settings = @Setting(name = CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.AllChangesetsOwnerChangesetTest$ChangesetIds"))
class AllChangesetsOwnerChangesetTest {

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
			image.title = "Original";
			session.persist( image );
			final var batched = new BatchedImage();
			batched.id = 1;
			batched.title = "Original";
			session.persist( batched );
			final var edition = new Edition();
			edition.id = 1;
			edition.cover = image;
			edition.eagerCover = image;
			edition.batchedCover = batched;
			edition.gallery.add( image );
			session.persist( edition );
			final var article = new Article();
			article.id = 1;
			article.edition = edition;
			session.persist( article );
			final var archive = new Archive();
			archive.id = 1;
			archive.cover = image;
			session.persist( archive );
			final var event = new Event();
			event.id = 1;
			event.label = "First";
			event.article = article;
			event.edition = edition;
			event.archive = archive;
			event.batchedImage = batched;
			event.link = new Link();
			event.link.image = image;
			session.persist( event );
		} );
		// changeset 2
		scope.inTransaction( session -> {
			session.find( Image.class, 1 ).title = "Revised";
			session.find( BatchedImage.class, 1 ).title = "Revised";
			session.find( Event.class, 1 ).label = "Second";
		} );
		// changeset 3
		scope.inTransaction( session -> {
			session.find( Image.class, 1 ).title = "Current";
			session.find( BatchedImage.class, 1 ).title = "Current";
		} );
	}

	@Test
	void nestedLazyLoadOfCurrentOwner(SessionFactoryScope scope) {
		try (var session = allChangesets( scope )) {
			// the article loads at the event's revision, but its edition is current, and so is the cover
			for ( var event : events( session ) ) {
				final var article = unproxy( event.article, Article.class );
				final var edition = unproxy( article.edition, Edition.class );
				assertEquals( "Current", unproxy( edition.cover, Image.class ).title );
			}
		}
	}

	@Test
	void nestedJoinFetchOfCurrentOwner(SessionFactoryScope scope) {
		try (var session = allChangesets( scope )) {
			final var events = session.createSelectionQuery( """
					from Event e join fetch e.article a join fetch a.edition ed join fetch ed.cover
					where e.id = 1 order by changesetId(e)
					""", Event.class ).getResultList();
			assertEquals( 2, events.size() );
			for ( var event : events ) {
				assertEquals( "Current", event.article.edition.cover.title );
			}
		}
	}

	@Test
	void streamedRevisions(SessionFactoryScope scope) {
		try (var session = allChangesets( scope );
				var stream = session.createSelectionQuery(
						"from Event e where e.id = 1 order by changesetId(e)", Event.class ).stream()) {
			// navigate while the stream is open, between rows of an all-changesets query
			final var events = stream.toList();
			assertEquals( List.of( "First", "Second" ), events.stream().map( event -> event.label ).toList() );
			for ( var event : events ) {
				final var edition = unproxy( event.edition, Edition.class );
				assertEquals( "Current", unproxy( edition.cover, Image.class ).title );
			}
			assertEquals( "Current", unproxy( session.find( Image.class, 1 ), Image.class ).title );
		}
	}

	@Test
	void batchFetchFromCurrentOwner(SessionFactoryScope scope) {
		try (var session = allChangesets( scope )) {
			final var edition = session.createSelectionQuery( "from Edition where id = 1", Edition.class )
					.getSingleResult();
			assertEquals( "Current", unproxy( edition.batchedCover, BatchedImage.class ).title );
		}
	}

	@Test
	void batchFetchOfSnapshotsAtTheirRevisions(SessionFactoryScope scope) {
		try (var session = allChangesets( scope )) {
			final var events = events( session );
			assertEquals( "Original", unproxy( events.get( 0 ).batchedImage, BatchedImage.class ).title );
			assertEquals( "Revised", unproxy( events.get( 1 ).batchedImage, BatchedImage.class ).title );
		}
	}

	@Test
	void softDeletableCurrentOwner(SessionFactoryScope scope) {
		try (var session = allChangesets( scope )) {
			for ( var event : events( session ) ) {
				final var archive = unproxy( event.archive, Archive.class );
				assertEquals( "Current", unproxy( archive.cover, Image.class ).title );
			}
		}
	}

	@Test
	void collectionOfCurrentOwner(SessionFactoryScope scope) {
		try (var session = allChangesets( scope )) {
			final var edition = session.createSelectionQuery(
					"from Edition ed join fetch ed.gallery where ed.id = 1", Edition.class ).getSingleResult();
			assertEquals( List.of( "Current" ), edition.gallery.stream().map( image -> image.title ).toList() );
		}
		try (var session = allChangesets( scope )) {
			final var edition = session.find( Edition.class, 1 );
			assertEquals( List.of( "Current" ), edition.gallery.stream().map( image -> image.title ).toList() );
		}
	}

	@Test
	void findAndReferenceReadCurrentState(SessionFactoryScope scope) {
		try (var session = allChangesets( scope )) {
			assertEquals( "Current", session.find( Image.class, 1 ).title );
		}
		try (var session = allChangesets( scope )) {
			final var reference = session.getReference( Image.class, 1 );
			assertEquals( "Current", unproxy( reference, Image.class ).title );
			assertSame( unproxy( reference ), unproxy( session.find( Image.class, 1 ) ) );
		}
	}

	@Test
	void embeddedToOneBoundedByOwnerRevision(SessionFactoryScope scope) {
		try (var session = allChangesets( scope )) {
			final var rows = session.createSelectionQuery(
					"select changesetId(e), changesetId(e.link.image) from Event e where e.id = 1 order by changesetId(e)",
					Object[].class ).getResultList();
			assertEquals( 2, rows.size() );
			assertEquals( List.of( 1, 1 ), List.of( rows.get( 0 ) ) );
			assertEquals( List.of( 2, 2 ), List.of( rows.get( 1 ) ) );
		}
	}

	@Test
	void pointInTimeUnchanged(SessionFactoryScope scope) {
		// a plan built in an ordinary session is not reused at a changeset
		scope.inSession( session -> assertEquals( "Current", session.find( Edition.class, 1 ).eagerCover.title ) );
		try (var session = scope.getSessionFactory().withOptions().atChangeset( 1 ).openSession()) {
			final var edition = session.find( Edition.class, 1 );
			assertEquals( "Original", edition.eagerCover.title );
			assertEquals( "Original", unproxy( edition.cover, Image.class ).title );
		}
		try (var session = allChangesets( scope )) {
			assertEquals( "Current", session.find( Edition.class, 1 ).eagerCover.title );
		}
	}

	private static Session allChangesets(SessionFactoryScope scope) {
		return scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession();
	}

	private static List<Event> events(Session session) {
		final var events = session.createSelectionQuery(
				"from Event e where e.id = 1 order by changesetId(e)", Event.class ).getResultList();
		assertEquals( 2, events.size() );
		return events;
	}

	@Entity(name = "Event")
	@Audited
	static class Event {
		@Id int id;
		String label;
		@ManyToOne(fetch = LAZY) Article article;
		@ManyToOne(fetch = LAZY) Edition edition;
		@ManyToOne(fetch = EAGER) Archive archive;
		@ManyToOne(fetch = EAGER) BatchedImage batchedImage;
		@Embedded Link link;
	}

	@Embeddable
	static class Link {
		@ManyToOne(fetch = LAZY) Image image;
	}

	@Entity(name = "Article")
	@Audited
	static class Article {
		@Id int id;
		@ManyToOne(fetch = LAZY) Edition edition;
	}

	@Entity(name = "Edition")
	static class Edition {
		@Id int id;
		@ManyToOne(fetch = LAZY) Image cover;
		@ManyToOne(fetch = EAGER) Image eagerCover;
		@ManyToOne(fetch = EAGER) BatchedImage batchedCover;
		@ManyToMany Set<Image> gallery = new HashSet<>();
	}

	@Entity(name = "Archive")
	@SoftDelete
	static class Archive {
		@Id int id;
		@ManyToOne(fetch = LAZY) Image cover;
	}

	@Entity(name = "Image")
	@Audited
	static class Image {
		@Id int id;
		String title;
	}

	@Entity(name = "BatchedImage")
	@Audited
	@BatchSize(size = 10)
	static class BatchedImage {
		@Id int id;
		String title;
	}

	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}
}
