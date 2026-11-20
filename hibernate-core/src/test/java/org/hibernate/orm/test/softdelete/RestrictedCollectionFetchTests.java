package org.hibernate.orm.test.softdelete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SoftDelete;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A join of a collection which is restricted only by soft delete is the
 * join which a fetch of the collection uses, a left join of a collection
 * with a where restriction is not, and a restriction which hides an
 * element of a map hides its key too.
 */
@DomainModel(annotatedClasses = {
		RestrictedCollectionFetchTests.Owner.class,
		RestrictedCollectionFetchTests.Tag.class,
		RestrictedCollectionFetchTests.Note.class,
		RestrictedCollectionFetchTests.Child.class,
		RestrictedCollectionFetchTests.SoftChild.class
})
@SessionFactory
@JiraKey("HHH-21214")
public class RestrictedCollectionFetchTests {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Tag t11 = new Tag( 11, "tag11" );
			final Tag t12 = new Tag( 12, "tag12" );
			session.persist( t11 );
			session.persist( t12 );
			final Note n11 = new Note( 11 );
			final Note n12 = new Note( 12 );
			final Note n13 = new Note( 13 );
			session.persist( n11 );
			session.persist( n12 );
			session.persist( n13 );
			// two elements in each bag, and a map whose only value is soft-deleted
			final Owner owner = new Owner( 1 );
			owner.tags.add( t11 );
			owner.tags.add( t12 );
			owner.labels.add( t11 );
			owner.labels.add( t12 );
			owner.plain.add( n11 );
			owner.plain.add( n12 );
			owner.keyed.put( t11, n13 );
			owner.names.add( "tag11" );
			owner.names.add( "tag12" );
			session.persist( owner );
			session.persist( new Child( 11, owner ) );
			session.persist( new Child( 12, owner ) );
			session.persist( new SoftChild( 11, owner ) );
			session.persist( new SoftChild( 12, owner ) );
		} );
		scope.inTransaction( session -> session.remove( session.find( Note.class, 13 ) ) );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@CsvSource({
			"tags, left join", "tags, join",
			"plain, left join", "plain, join",
			"labels, join",
			"children, join"
	})
	public void fetchGraphDoesNotDuplicateElements(String collection, String join, SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var graph = session.createEntityGraph( Owner.class );
			graph.addAttributeNodes( collection );
			final List<Owner> owners = session.createSelectionQuery(
							"select o from Owner o " + join + " o." + collection + " e", Owner.class )
					.setHint( "jakarta.persistence.fetchgraph", graph )
					.getResultList();
			assertThat( owners ).hasSize( 1 );
			final Owner owner = owners.get( 0 );
			final List<?> elements = switch ( collection ) {
				case "tags" -> owner.tags;
				case "labels" -> owner.labels;
				case "plain" -> owner.plain;
				default -> owner.children;
			};
			assertThat( Hibernate.isInitialized( elements ) ).isTrue();
			assertThat( elements ).hasSize( 2 );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {"labels", "children", "names", "softChildren"})
	public void laterJoinDoesNotTruncateFetchedCollection(String collection, SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var graph = session.createEntityGraph( Owner.class );
			graph.addAttributeNodes( collection );
			// the join of the collection is restricted by a later join, so it is not the join the fetch uses
			final String element = "names".equals( collection ) ? "t.name = e" : "t.id = e.id";
			final Owner owner = session.createSelectionQuery(
							"select o from Owner o left join o." + collection + " e"
									+ " join Tag t on " + element + " and t.id = 11", Owner.class )
					.setHint( "jakarta.persistence.fetchgraph", graph )
					.getSingleResult();
			final List<?> elements = switch ( collection ) {
				case "labels" -> owner.labels;
				case "children" -> owner.children;
				case "names" -> owner.names;
				default -> owner.softChildren;
			};
			assertThat( Hibernate.isInitialized( elements ) ).isTrue();
			assertThat( elements ).hasSize( 2 );
		} );
	}

	@Test
	public void leftJoinHidesKeyOfHiddenEntry(SessionFactoryScope scope) {
		scope.inSession( session -> {
			// the key is read from the table of the key
			final Object[] named = session.createSelectionQuery(
					"select o.id, key(e).name from Owner o left join o.keyed e", Object[].class ).getSingleResult();
			assertThat( named ).containsExactly( 1, null );
			// the key is read from the join table
			final Object[] identified = session.createSelectionQuery(
					"select o.id, key(e).id from Owner o left join o.keyed e", Object[].class ).getSingleResult();
			assertThat( identified ).containsExactly( 1, null );
		} );
	}

	@Entity(name = "Owner")
	@Table(name = "owners")
	public static class Owner {
		@Id
		Integer id;

		@ManyToMany
		@JoinTable(name = "owner_tags")
		@SoftDelete
		List<Tag> tags = new ArrayList<>();

		@ManyToMany
		@JoinTable(name = "owner_labels", inverseJoinColumns = @JoinColumn(name = "label_id"))
		@SQLJoinTableRestriction("label_id <> 10")
		List<Tag> labels = new ArrayList<>();

		@ManyToMany
		@JoinTable(name = "owner_plain")
		List<Note> plain = new ArrayList<>();

		@OneToMany(mappedBy = "owner")
		@SQLRestriction("id <> 10")
		List<Child> children = new ArrayList<>();

		@OneToMany(mappedBy = "owner")
		List<SoftChild> softChildren = new ArrayList<>();

		@ElementCollection
		@CollectionTable(name = "owner_names")
		@SoftDelete
		List<String> names = new ArrayList<>();

		@ManyToMany
		@JoinTable(name = "owner_keyed")
		@MapKeyJoinColumn(name = "key_id")
		Map<Tag, Note> keyed = new HashMap<>();

		public Owner() {
		}

		public Owner(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Tag")
	@Table(name = "tags")
	public static class Tag {
		@Id
		Integer id;
		String name;

		public Tag() {
		}

		public Tag(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Note")
	@Table(name = "notes")
	@SoftDelete
	public static class Note {
		@Id
		Integer id;

		public Note() {
		}

		public Note(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Child")
	@Table(name = "children")
	public static class Child {
		@Id
		Integer id;
		@ManyToOne
		Owner owner;

		public Child() {
		}

		public Child(Integer id, Owner owner) {
			this.id = id;
			this.owner = owner;
		}
	}

	@Entity(name = "SoftChild")
	@Table(name = "soft_children")
	@SoftDelete
	public static class SoftChild {
		@Id
		Integer id;
		@ManyToOne
		Owner owner;

		public SoftChild() {
		}

		public SoftChild(Integer id, Owner owner) {
			this.id = id;
			this.owner = owner;
		}
	}
}
