package org.hibernate.temporal.audit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.audit.AuditLog.ALL_CHANGESETS;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * In an all-changesets session, the associations of an audit revision resolve at
 * that revision, through temporal snapshots, unique keys, collections without
 * history of their own, and stateless fetches.
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditRevisionAssociationTest.Event.class,
		AuditRevisionAssociationTest.Doc.class,
		AuditRevisionAssociationTest.Author.class,
		AuditRevisionAssociationTest.Detail.class,
		AuditRevisionAssociationTest.Comment.class,
		AuditRevisionAssociationTest.Tag.class
})
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER,
				value = "org.hibernate.temporal.audit.AuditRevisionAssociationTest$ChangesetIds")
})
class AuditRevisionAssociationTest {

	private static Object historyCreatedFor;

	@BeforeEach
	void createHistory(SessionFactoryScope scope) {
		if ( historyCreatedFor == scope.getSessionFactory() ) {
			return;
		}
		historyCreatedFor = scope.getSessionFactory();
		// changeset 1
		scope.inTransaction( session -> {
			final var author = new Author();
			author.id = 1;
			author.name = "A1";
			session.persist( author );
			final var doc = new Doc();
			doc.id = 1;
			doc.title = "D1";
			doc.author = author;
			session.persist( doc );
			final var tag1 = new Tag();
			tag1.id = 1;
			session.persist( tag1 );
			final var tag2 = new Tag();
			tag2.id = 2;
			session.persist( tag2 );
			final var event = new Event();
			event.id = 1;
			event.label = "First";
			event.doc = doc;
			event.tags.add( tag1 );
			session.persist( event );
			final var detail = new Detail();
			detail.id = 1;
			detail.text = "T1";
			detail.event = event;
			session.persist( detail );
			for ( int id = 1; id <= 2; id++ ) {
				final var comment = new Comment();
				comment.id = id;
				comment.event = event;
				session.persist( comment );
			}
		} );
		// changeset 2
		scope.inTransaction( session -> {
			session.find( Author.class, 1 ).name = "A2";
			session.find( Doc.class, 1 ).title = "D2";
			session.find( Detail.class, 1 ).text = "T2";
			final var event = Hibernate.unproxy( session.find( Event.class, 1 ), Event.class );
			event.label = "Second";
			event.tags.add( session.find( Tag.class, 2 ) );
		} );
		// changeset 3
		scope.inTransaction( session -> {
			session.find( Author.class, 1 ).name = "A3";
			session.find( Doc.class, 1 ).title = "D3";
			session.find( Detail.class, 1 ).text = "T3";
		} );
	}

	@Test
	void associationOfTemporalSnapshot(SessionFactoryScope scope) {
		final List<String> expected = List.of( "First D1/A1", "Second D2/A2" );
		try (var session = allChangesets( scope )) {
			assertEquals( expected, session.createSelectionQuery(
					"from Event e join fetch e.doc d join fetch d.author where e.id = 1 order by changesetId(e)",
					Event.class ).getResultList().stream()
					.map( event -> event.label + " " + event.doc.title + "/" + event.doc.author.name )
					.toList() );
		}
		try (var session = allChangesets( scope )) {
			assertEquals( expected, events( session ).stream().map( event -> {
				final var doc = Hibernate.unproxy( event.doc, Doc.class );
				return event.label + " " + doc.title + "/" + Hibernate.unproxy( doc.author, Author.class ).name;
			} ).toList() );
		}
	}

	@Test
	void uniqueKeyOfRevisionsAndCurrentState(SessionFactoryScope scope) {
		try (var session = allChangesets( scope )) {
			assertEquals( List.of( "T1", "T2" ), events( session ).stream()
					.map( event -> Hibernate.unproxy( event.detail, Detail.class ).text ).toList() );
			// the current event is current state, and so is its detail
			final var detail = Hibernate.unproxy( session.find( Event.class, 1 ).detail, Detail.class );
			assertEquals( "T3", detail.text );
			assertFalse( session.isReadOnly( detail ) );
		}
	}

	@Test
	void collectionWithoutHistoryOfRevisions(SessionFactoryScope scope) {
		try (var session = allChangesets( scope )) {
			final var events = session.createSelectionQuery(
					"from Event e join fetch e.comments where e.id = 1 order by changesetId(e)", Event.class )
					.getResultList();
			assertEquals( List.of( 2, 2 ), events.stream().map( event -> event.comments.size() ).toList() );
			assertNotSame( events.get( 0 ).comments, events.get( 1 ).comments );
		}
		try (var session = allChangesets( scope )) {
			final var events = events( session );
			assertEquals( List.of( 2, 2 ), events.stream().map( event -> event.comments.size() ).toList() );
			assertNotSame( events.get( 0 ).comments, events.get( 1 ).comments );
		}
	}

	@Test
	void statelessFetchOfRevisionCollections(SessionFactoryScope scope) {
		try (var session = scope.getSessionFactory().withStatelessOptions().atChangeset( ALL_CHANGESETS )
				.openStatelessSession()) {
			final var sizes = new ArrayList<Integer>();
			for ( var event : session.createSelectionQuery(
					"from Event e where e.id = 1 order by changesetId(e)", Event.class ).getResultList() ) {
				session.fetch( event.tags );
				sizes.add( event.tags.size() );
			}
			assertEquals( List.of( 1, 2 ), sizes );
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
		@ManyToOne(fetch = LAZY) Doc doc;
		@OneToOne(mappedBy = "event", fetch = LAZY) Detail detail;
		@OneToMany(mappedBy = "event") List<Comment> comments = new ArrayList<>();
		@ManyToMany Set<Tag> tags = new HashSet<>();
	}

	@Entity(name = "Doc")
	@Temporal
	static class Doc {
		@Id int id;
		String title;
		@ManyToOne(fetch = LAZY) Author author;
	}

	@Entity(name = "Author")
	@Audited
	static class Author {
		@Id int id;
		String name;
	}

	@Entity(name = "Detail")
	@Audited
	static class Detail {
		@Id int id;
		String text;
		@OneToOne(fetch = LAZY) Event event;
	}

	@Entity(name = "EventComment")
	static class Comment {
		@Id int id;
		@ManyToOne(fetch = LAZY) Event event;
	}

	@Entity(name = "Tag")
	@Audited
	static class Tag {
		@Id int id;
	}

	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}
}
