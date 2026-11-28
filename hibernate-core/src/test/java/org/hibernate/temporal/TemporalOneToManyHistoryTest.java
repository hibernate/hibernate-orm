package org.hibernate.temporal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Temporal;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// A one-to-many association writes its join column, and its order column,
/// to the row of the associated entity. A revision of that entity is a copy
/// of its row, so the entity is given a new revision whenever the association
/// writes its row, even though the entity itself is unchanged.
@JiraKey("HHH-21285")
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = TemporalOneToManyHistoryTest.SUPPLIER)
})
@DomainModel(annotatedClasses = {
		TemporalOneToManyHistoryTest.Parent.class,
		TemporalOneToManyHistoryTest.Child.class,
		TemporalOneToManyHistoryTest.Label.class,
		TemporalOneToManyHistoryTest.Team.class,
		TemporalOneToManyHistoryTest.Member.class,
		TemporalOneToManyHistoryTest.Shelf.class,
		TemporalOneToManyHistoryTest.Book.class,
		TemporalOneToManyHistoryTest.Folder.class,
		TemporalOneToManyHistoryTest.Note.class
})
@SessionFactory
class TemporalOneToManyHistoryTest {
	static final String SUPPLIER = "org.hibernate.temporal.TemporalInsertUpdateOrderingTest$Changesets";

	@Entity(name = "OtmParent")
	@Table(name = "otm_parent")
	@Temporal
	@FilterDef(name = "firstChild")
	static class Parent {
		@Id int id;
		String name;
		@OneToMany(cascade = CascadeType.PERSIST)
		@JoinColumn(name = "parent_id")
		@OrderColumn(name = "pos")
		@Filter(name = "firstChild", condition = "pos = 0")
		List<Child> children = new ArrayList<>();
		@OneToMany(cascade = CascadeType.PERSIST)
		@JoinColumn(name = "parent_id")
		Set<Label> labels = new HashSet<>();

		Parent() {
		}

		Parent(int id, int... children) {
			this.id = id;
			for ( int child : children ) {
				this.children.add( new Child( child ) );
			}
		}
	}

	@Entity(name = "OtmChild")
	@Table(name = "otm_child")
	@Temporal
	static class Child {
		@Id int id;
		String name;

		Child() {
		}

		Child(int id) {
			this.id = id;
			name = "c" + id;
		}
	}

	@Entity(name = "OtmLabel")
	@Table(name = "otm_label")
	@Temporal
	static class Label {
		@Id int id;

		Label() {
		}

		Label(int id) {
			this.id = id;
		}
	}

	@Entity(name = "OtmTeam")
	@Table(name = "otm_team")
	@Temporal
	static class Team {
		@Id int id;
		// the order column is not mapped by the owning side, so this side writes it
		@OneToMany(mappedBy = "team", cascade = CascadeType.PERSIST)
		@OrderColumn(name = "pos")
		List<Member> members = new ArrayList<>();
	}

	@Entity(name = "OtmMember")
	@Table(name = "otm_member")
	@Temporal
	static class Member {
		@Id int id;
		@ManyToOne Team team;

		Member() {
		}

		Member(int id, Team team) {
			this.id = id;
			this.team = team;
			team.members.add( this );
		}
	}

	@Entity(name = "OtmShelf")
	@Table(name = "otm_shelf")
	@Temporal
	static class Shelf {
		@Id int id = 1;
		// a version of a book has one revision, so the association cannot give it another
		@OneToMany(cascade = CascadeType.PERSIST)
		@JoinColumn(name = "shelf_id")
		@OrderColumn(name = "pos")
		@Temporal.Excluded
		List<Book> books = new ArrayList<>();
	}

	@Entity(name = "OtmBook")
	@Table(name = "otm_book")
	@Temporal
	static class Book {
		@Id int id;
		@Version int version;

		Book() {
		}

		Book(int id) {
			this.id = id;
		}
	}

	@Entity(name = "OtmFolder")
	@Table(name = "otm_folder")
	@Temporal
	static class Folder {
		@Id int id = 1;
		@OneToMany(cascade = CascadeType.PERSIST)
		@JoinColumn(name = "folder_id")
		@OrderColumn(name = "pos")
		@Temporal.Excluded
		List<Note> notes = new ArrayList<>();
	}

	@Entity(name = "OtmNote")
	@Table(name = "otm_note")
	@Temporal
	static class Note {
		@Id int id;

		Note() {
		}

		Note(int id) {
			this.id = id;
		}
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void reorder(SessionFactoryScope scope) {
		var created = change( scope, session -> session.persist( new Parent( 1, 10, 11, 12 ) ) );
		var reordered = change( scope, session -> {
			var children = session.find( Parent.class, 1 ).children;
			children.add( children.remove( 0 ) );
		} );
		assertEquals( List.of( 10, 11, 12 ), childrenAt( scope, created, 1 ) );
		assertEquals( List.of( 11, 12, 10 ), childrenAt( scope, reordered, 1 ) );
		assertEquals( List.of( 2L, 2L, 2L ), revisions( scope, 10, 11, 12 ) );
		assertEquals( 3L, openRevisions( scope ) );
	}

	@Test
	void swapLeavesTheOtherChildrenAlone(SessionFactoryScope scope) {
		var created = change( scope, session -> session.persist( new Parent( 1, 10, 11, 12 ) ) );
		var swapped = change( scope, session -> {
			var children = session.find( Parent.class, 1 ).children;
			children.set( 2, children.set( 1, children.get( 2 ) ) );
		} );
		assertEquals( List.of( 10, 11, 12 ), childrenAt( scope, created, 1 ) );
		assertEquals( List.of( 10, 12, 11 ), childrenAt( scope, swapped, 1 ) );
		assertEquals( List.of( 1L, 2L, 2L ), revisions( scope, 10, 11, 12 ) );
	}

	@Test
	void appendNewChild(SessionFactoryScope scope) {
		var created = change( scope, session -> session.persist( new Parent( 1, 10, 11 ) ) );
		var appended = change( scope, session -> session.find( Parent.class, 1 ).children.add( new Child( 12 ) ) );
		assertEquals( List.of( 10, 11 ), childrenAt( scope, created, 1 ) );
		assertEquals( List.of( 10, 11, 12 ), childrenAt( scope, appended, 1 ) );
		assertEquals( List.of( 1L, 1L, 1L ), revisions( scope, 10, 11, 12 ) );
	}

	@Test
	void addExistingChild(SessionFactoryScope scope) {
		var created = change( scope, session -> {
			session.persist( new Parent( 1, 10 ) );
			session.persist( new Child( 11 ) );
		} );
		var added = change( scope, session ->
				session.find( Parent.class, 1 ).children.add( session.getReference( Child.class, 11 ) ) );
		assertEquals( List.of( 10 ), childrenAt( scope, created, 1 ) );
		assertEquals( List.of( 10, 11 ), childrenAt( scope, added, 1 ) );
		assertEquals( List.of( 1L, 2L ), revisions( scope, 10, 11 ) );
	}

	@Test
	void removeChildWithoutDeletingIt(SessionFactoryScope scope) {
		var created = change( scope, session -> session.persist( new Parent( 1, 10, 11, 12 ) ) );
		var removed = change( scope, session -> session.find( Parent.class, 1 ).children.remove( 1 ) );
		assertEquals( List.of( 10, 11, 12 ), childrenAt( scope, created, 1 ) );
		assertEquals( List.of( 10, 12 ), childrenAt( scope, removed, 1 ) );
		assertEquals( List.of( 1L, 2L, 2L ), revisions( scope, 10, 11, 12 ) );
		assertEquals( 3L, openRevisions( scope ) );
	}

	@Test
	void removeAndDeleteChild(SessionFactoryScope scope) {
		var created = change( scope, session -> session.persist( new Parent( 1, 10, 11 ) ) );
		var removed = change( scope, session -> session.remove( session.find( Parent.class, 1 ).children.remove( 1 ) ) );
		assertEquals( List.of( 10, 11 ), childrenAt( scope, created, 1 ) );
		assertEquals( List.of( 10 ), childrenAt( scope, removed, 1 ) );
		assertEquals( List.of( 1L, 1L ), revisions( scope, 10, 11 ) );
		assertEquals( 1L, openRevisions( scope ) );
	}

	@Test
	void moveChildToAnotherParent(SessionFactoryScope scope) {
		var created = change( scope, session -> {
			session.persist( new Parent( 1, 10, 11 ) );
			session.persist( new Parent( 2 ) );
		} );
		var moved = change( scope, session -> {
			var child = session.find( Parent.class, 1 ).children.remove( 1 );
			session.find( Parent.class, 2 ).children.add( child );
		} );
		assertEquals( List.of( 10, 11 ), childrenAt( scope, created, 1 ) );
		assertEquals( List.of(), childrenAt( scope, created, 2 ) );
		assertEquals( List.of( 10 ), childrenAt( scope, moved, 1 ) );
		assertEquals( List.of( 11 ), childrenAt( scope, moved, 2 ) );
		assertEquals( List.of( 1L, 2L ), revisions( scope, 10, 11 ) );
	}

	@Test
	void clear(SessionFactoryScope scope) {
		var created = change( scope, session -> session.persist( new Parent( 1, 10, 11 ) ) );
		var cleared = change( scope, session -> session.find( Parent.class, 1 ).children.clear() );
		assertEquals( List.of( 10, 11 ), childrenAt( scope, created, 1 ) );
		assertEquals( List.of(), childrenAt( scope, cleared, 1 ) );
		assertEquals( List.of( 2L, 2L ), revisions( scope, 10, 11 ) );
		assertEquals( 2L, openRevisions( scope ) );
	}

	@Test
	void replaceCollection(SessionFactoryScope scope) {
		var created = change( scope, session -> session.persist( new Parent( 1, 10, 11 ) ) );
		var replaced = change( scope, session -> {
			var parent = session.find( Parent.class, 1 );
			parent.children = new ArrayList<>( List.of( parent.children.get( 1 ), new Child( 12 ) ) );
		} );
		assertEquals( List.of( 10, 11 ), childrenAt( scope, created, 1 ) );
		assertEquals( List.of( 11, 12 ), childrenAt( scope, replaced, 1 ) );
		assertEquals( 3L, openRevisions( scope ) );
	}

	@Test
	void removeParentOfUnloadedChildren(SessionFactoryScope scope) {
		var created = change( scope, session -> session.persist( new Parent( 1, 10, 11 ) ) );
		var removed = change( scope, session -> session.remove( session.find( Parent.class, 1 ) ) );
		assertEquals( List.of( 10, 11 ), childrenAt( scope, created, 1 ) );
		assertNull( childrenAt( scope, removed, 1 ) );
		assertEquals( List.of( 2L, 2L ), revisions( scope, 10, 11 ) );
		assertEquals( 2L, openRevisions( scope ) );
		assertEquals( 2L, count( scope, "select count(*) from otm_child_history"
				+ " where superseded is null and parent_id is null and pos is null" ) );
	}

	@Test
	void removeParentOfLoadedChildren(SessionFactoryScope scope) {
		var created = change( scope, session -> session.persist( new Parent( 1, 10, 11 ) ) );
		var removed = change( scope, session -> {
			var parent = session.find( Parent.class, 1 );
			session.remove( parent.children.get( 0 ) );
			session.remove( parent );
		} );
		assertEquals( List.of( 10, 11 ), childrenAt( scope, created, 1 ) );
		assertNull( childrenAt( scope, removed, 1 ) );
		assertEquals( List.of( 1L, 2L ), revisions( scope, 10, 11 ) );
		assertEquals( 1L, openRevisions( scope ) );
	}

	@Test
	void removeParentOfChildrenHiddenByFilter(SessionFactoryScope scope) {
		var created = change( scope, session -> session.persist( new Parent( 1, 10, 11 ) ) );
		var removed = change( scope, session -> {
			session.enableFilter( "firstChild" );
			var parent = session.find( Parent.class, 1 );
			assertEquals( 1, parent.children.size() );
			// the row of the hidden child is written too
			session.remove( parent );
		} );
		assertEquals( List.of( 10, 11 ), childrenAt( scope, created, 1 ) );
		assertNull( childrenAt( scope, removed, 1 ) );
		assertEquals( List.of( 2L, 2L ), revisions( scope, 10, 11 ) );
		assertEquals( 2L, count( scope, "select count(*) from otm_child_history"
				+ " where superseded is null and parent_id is null" ) );
	}

	@Test
	void excludedAssociationUpdatesTheOpenRevision(SessionFactoryScope scope) {
		var created = change( scope, session -> {
			var folder = new Folder();
			folder.notes.add( new Note( 50 ) );
			folder.notes.add( new Note( 51 ) );
			session.persist( folder );
		} );
		var reordered = change( scope, session -> {
			var notes = session.find( Folder.class, 1 ).notes;
			notes.add( notes.remove( 0 ) );
		} );
		// a note keeps the revision it had, which now holds its new position
		assertEquals( List.of( 51, 50 ), notesAt( scope, reordered ) );
		assertEquals( List.of( 51, 50 ), notesAt( scope, created ) );
		assertEquals( 2L, count( scope, "select count(*) from otm_note_history" ) );
		assertEquals( 2L, count( scope, "select count(*) from otm_note_history where superseded is null" ) );
	}

	@Test
	void excludedAssociationOfVersionedChildren(SessionFactoryScope scope) {
		var created = change( scope, session -> {
			var shelf = new Shelf();
			shelf.books.add( new Book( 40 ) );
			shelf.books.add( new Book( 41 ) );
			shelf.books.add( new Book( 42 ) );
			session.persist( shelf );
		} );
		var reordered = change( scope, session -> {
			var books = session.find( Shelf.class, 1 ).books;
			books.add( books.remove( 0 ) );
		} );
		assertEquals( List.of( 41, 42, 40 ), booksAt( scope, reordered ) );
		// a version of a book has one revision, which now holds its new position
		assertEquals( List.of( 41, 42, 40 ), booksAt( scope, created ) );
		assertEquals( 3L, count( scope, "select count(*) from otm_book_history" ) );
		assertEquals( 3L, count( scope, "select count(*) from otm_book_history"
				+ " where superseded is null and version = 0" ) );
	}

	@Test
	void repeatedFlushInOneChangeset(SessionFactoryScope scope) {
		var created = change( scope, session -> session.persist( new Parent( 1, 10, 11, 12 ) ) );
		var reordered = change( scope, session -> {
			var children = session.find( Parent.class, 1 ).children;
			children.add( children.remove( 0 ) );
			session.flush();
			children.add( children.remove( 0 ) );
		} );
		assertEquals( List.of( 10, 11, 12 ), childrenAt( scope, created, 1 ) );
		assertEquals( List.of( 12, 10, 11 ), childrenAt( scope, reordered, 1 ) );
		assertEquals( List.of( 2L, 2L, 2L ), revisions( scope, 10, 11, 12 ) );
	}

	@Test
	void changeChildAndPosition(SessionFactoryScope scope) {
		var created = change( scope, session -> session.persist( new Parent( 1, 10, 11 ) ) );
		var changed = change( scope, session -> {
			var children = session.find( Parent.class, 1 ).children;
			children.add( children.remove( 0 ) );
			children.get( 0 ).name = "changed";
		} );
		assertEquals( List.of( 10, 11 ), childrenAt( scope, created, 1 ) );
		assertEquals( List.of( 11, 10 ), childrenAt( scope, changed, 1 ) );
		assertEquals( List.of( 2L, 2L ), revisions( scope, 10, 11 ) );
		assertEquals( 1L, count( scope, "select count(*) from otm_child_history"
				+ " where superseded is null and name = 'changed' and pos = 0" ) );
	}

	@Test
	void unorderedSet(SessionFactoryScope scope) {
		var created = change( scope, session -> {
			var parent = new Parent( 1 );
			parent.labels.add( new Label( 20 ) );
			parent.labels.add( new Label( 21 ) );
			session.persist( parent );
		} );
		var changed = change( scope, session -> {
			var labels = session.find( Parent.class, 1 ).labels;
			labels.removeIf( label -> label.id == 20 );
			labels.add( new Label( 22 ) );
		} );
		assertEquals( Set.of( 20, 21 ), labelsAt( scope, created ) );
		assertEquals( Set.of( 21, 22 ), labelsAt( scope, changed ) );
		assertEquals( 2L, count( scope, "select count(*) from otm_label_history where id = 20" ) );
		assertEquals( 1L, count( scope, "select count(*) from otm_label_history where id = 21" ) );
		assertEquals( 1L, count( scope, "select count(*) from otm_label_history where id = 22" ) );
	}

	@Test
	void orderColumnWrittenByUnownedSide(SessionFactoryScope scope) {
		var created = change( scope, session -> {
			var team = new Team();
			team.id = 1;
			new Member( 30, team );
			new Member( 31, team );
			new Member( 32, team );
			session.persist( team );
		} );
		var reordered = change( scope, session -> {
			var members = session.find( Team.class, 1 ).members;
			members.add( members.remove( 1 ) );
		} );
		assertEquals( List.of( 30, 31, 32 ), membersAt( scope, created ) );
		assertEquals( List.of( 30, 32, 31 ), membersAt( scope, reordered ) );
		assertEquals( 1L, count( scope, "select count(*) from otm_member_history where id = 30" ) );
		assertEquals( 2L, count( scope, "select count(*) from otm_member_history where id = 31" ) );
		assertEquals( 2L, count( scope, "select count(*) from otm_member_history where id = 32" ) );
	}

	@Test
	void statelessSession(SessionFactoryScope scope) {
		final Object[] changesets = new Object[2];
		scope.inStatelessTransaction( session -> {
			var parent = new Parent( 1, 10, 11 );
			parent.children.forEach( session::insert );
			session.insert( parent );
			changesets[0] = session.getCurrentChangesetIdentifier();
		} );
		scope.inStatelessTransaction( session -> {
			var parent = session.get( Parent.class, 1 );
			session.fetch( parent.children );
			parent.children.add( parent.children.remove( 0 ) );
			session.update( parent );
			changesets[1] = session.getCurrentChangesetIdentifier();
		} );
		assertEquals( List.of( 10, 11 ), childrenAt( scope, changesets[0], 1 ) );
		assertEquals( List.of( 11, 10 ), childrenAt( scope, changesets[1], 1 ) );
		assertEquals( 2L, openRevisions( scope ) );
	}

	private static Object change(SessionFactoryScope scope, Consumer<SessionImplementor> work) {
		return scope.fromTransaction( session -> {
			work.accept( session );
			return session.getCurrentChangesetIdentifier();
		} );
	}

	private static List<Integer> childrenAt(SessionFactoryScope scope, Object changeset, int parentId) {
		try ( var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession() ) {
			var parent = session.find( Parent.class, parentId );
			return parent == null ? null : parent.children.stream().map( child -> child.id ).toList();
		}
	}

	private static Set<Integer> labelsAt(SessionFactoryScope scope, Object changeset) {
		try ( var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession() ) {
			final Set<Integer> labels = new HashSet<>();
			session.find( Parent.class, 1 ).labels.forEach( label -> labels.add( label.id ) );
			return labels;
		}
	}

	private static List<Integer> notesAt(SessionFactoryScope scope, Object changeset) {
		try ( var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession() ) {
			return session.find( Folder.class, 1 ).notes.stream().map( note -> note.id ).toList();
		}
	}

	private static List<Integer> booksAt(SessionFactoryScope scope, Object changeset) {
		try ( var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession() ) {
			return session.find( Shelf.class, 1 ).books.stream().map( book -> book.id ).toList();
		}
	}

	private static List<Integer> membersAt(SessionFactoryScope scope, Object changeset) {
		try ( var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession() ) {
			return session.find( Team.class, 1 ).members.stream().map( member -> member.id ).toList();
		}
	}

	/// The number of revisions of each of the given children.
	private static List<Long> revisions(SessionFactoryScope scope, int... children) {
		final List<Long> revisions = new ArrayList<>();
		for ( int child : children ) {
			revisions.add( count( scope, "select count(*) from otm_child_history where id = " + child ) );
		}
		return revisions;
	}

	private static long openRevisions(SessionFactoryScope scope) {
		return count( scope, "select count(*) from otm_child_history where superseded is null" );
	}

	private static long count(SessionFactoryScope scope, String sql) {
		return scope.fromSession( session -> session.createNativeQuery( sql, Long.class ).getSingleResult() );
	}
}
