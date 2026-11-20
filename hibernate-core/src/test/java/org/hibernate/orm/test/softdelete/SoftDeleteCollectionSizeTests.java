package org.hibernate.orm.test.softdelete;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.annotations.SoftDelete;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The subquery of a function or predicate of a collection does not see
 * the soft-deleted elements of the collection.
 */
@DomainModel(annotatedClasses = {
		SoftDeleteCollectionSizeTests.Owner.class,
		SoftDeleteCollectionSizeTests.Note.class,
		SoftDeleteCollectionSizeTests.Tag.class,
		SoftDeleteCollectionSizeTests.Label.class
})
@SessionFactory
@JiraKey("HHH-21215")
public class SoftDeleteCollectionSizeTests {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// every element of the first owner is soft-deleted, one of the two
			// elements of the second owner is, and the third owner has no elements
			for ( int id = 1; id <= 3; id++ ) {
				session.persist( new Owner( id ) );
			}
			for ( int id = 1; id <= 3; id++ ) {
				final Owner owner = session.find( Owner.class, id == 1 ? 1 : 2 );
				final Note note = new Note( id, owner );
				final Tag tag = new Tag( id );
				final Label label = new Label( id );
				session.persist( note );
				session.persist( tag );
				session.persist( label );
				owner.notes.add( note );
				owner.tags.add( tag );
				owner.labels.add( label );
				owner.names.add( "name" + id );
			}
		} );
		scope.inTransaction( session -> {
			for ( int id = 1; id <= 2; id++ ) {
				final Owner owner = session.find( Owner.class, id );
				// soft-deletes a row of the join table and a row of the collection table
				owner.tags.remove( session.find( Tag.class, id ) );
				owner.names.remove( "name" + id );
				// soft-deletes the entities, leaving the row of the join table to the label
				session.remove( session.find( Note.class, id ) );
				session.remove( session.find( Label.class, id ) );
			}
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@ValueSource(strings = {"notes", "tags", "labels", "names"})
	public void sizeInWhereClause(String collection, SessionFactoryScope scope) {
		assertEquals( List.of( 2 ),
				ids( scope, "select o.id from Owner o where size(o." + collection + ") > 0 order by o.id" ) );
		assertEquals( List.of( 1, 3 ),
				ids( scope, "select o.id from Owner o where size(o." + collection + ") = 0 order by o.id" ) );
	}

	@ParameterizedTest
	@ValueSource(strings = {"notes", "tags", "labels", "names"})
	public void sizeInSelectClause(String collection, SessionFactoryScope scope) {
		assertEquals( List.of( 0, 1, 0 ),
				ids( scope, "select size(o." + collection + ") from Owner o order by o.id" ) );
	}

	@ParameterizedTest
	@ValueSource(strings = {"notes", "tags", "labels", "names"})
	public void maxElement(String collection, SessionFactoryScope scope) {
		assertEquals( List.of( 1, 3 ),
				ids( scope, "select o.id from Owner o where max(elements(o." + collection + ")) is null order by o.id" ) );
	}

	@ParameterizedTest
	@ValueSource(strings = {"notes", "tags", "labels", "names"})
	public void memberOf(String collection, SessionFactoryScope scope) {
		scope.inSession( session -> {
			// the second element of each collection is soft-deleted, the third is not
			assertEquals( List.of(), owners( session, collection, element( session, collection, 2 ) ) );
			assertEquals( List.of( 2 ), owners( session, collection, element( session, collection, 3 ) ) );
		} );
	}

	private static Object element(Session session, String collection, int id) {
		return switch ( collection ) {
			case "notes" -> session.getReference( Note.class, id );
			case "tags" -> session.getReference( Tag.class, id );
			case "labels" -> session.getReference( Label.class, id );
			default -> "name" + id;
		};
	}

	private static List<Integer> owners(Session session, String collection, Object element) {
		return session.createSelectionQuery(
						"select o.id from Owner o where :element member of o." + collection, Integer.class )
				.setParameter( "element", element )
				.getResultList();
	}

	private static List<Integer> ids(SessionFactoryScope scope, String hql) {
		return scope.fromSession( session -> session.createSelectionQuery( hql, Integer.class ).getResultList() );
	}

	@Entity(name = "Owner")
	@Table(name = "sd_size_owner")
	public static class Owner {
		@Id
		Integer id;
		// a collection of soft-deleted entities
		@OneToMany(mappedBy = "owner")
		List<Note> notes = new ArrayList<>();
		// a join table with soft-deleted rows
		@ManyToMany
		@JoinTable(name = "sd_size_owner_tags")
		@SoftDelete
		Set<Tag> tags = new HashSet<>();
		// a join table to soft-deleted entities
		@ManyToMany
		@JoinTable(name = "sd_size_owner_labels")
		Set<Label> labels = new HashSet<>();
		// a collection table with soft-deleted rows
		@ElementCollection
		@SoftDelete
		Set<String> names = new HashSet<>();

		public Owner() {
		}

		public Owner(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Note")
	@Table(name = "sd_size_note")
	@SoftDelete
	public static class Note {
		@Id
		Integer id;
		@ManyToOne
		Owner owner;

		public Note() {
		}

		public Note(Integer id, Owner owner) {
			this.id = id;
			this.owner = owner;
		}
	}

	@Entity(name = "Tag")
	@Table(name = "sd_size_tag")
	public static class Tag {
		@Id
		Integer id;

		public Tag() {
		}

		public Tag(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Label")
	@Table(name = "sd_size_label")
	@SoftDelete
	public static class Label {
		@Id
		Integer id;

		public Label() {
		}

		public Label(Integer id) {
			this.id = id;
		}
	}
}
