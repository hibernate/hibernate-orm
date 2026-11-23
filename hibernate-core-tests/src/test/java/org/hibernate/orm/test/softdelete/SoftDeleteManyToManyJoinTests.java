package org.hibernate.orm.test.softdelete;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.SoftDelete;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A left join or an implicit join of a collection, where either the element
 * entity or the row of the join table is soft-deleted, never exposes a hidden
 * element.
 */
@DomainModel(annotatedClasses = {
		SoftDeleteManyToManyJoinTests.Owner.class,
		SoftDeleteManyToManyJoinTests.Note.class,
		SoftDeleteManyToManyJoinTests.Item.class,
		SoftDeleteManyToManyJoinTests.Tag.class
})
@SessionFactory
@JiraKey("HHH-21214")
public class SoftDeleteManyToManyJoinTests {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// the ids of the tags are those of the notes
			for ( int id : new int[] { 10, 20, 21 } ) {
				session.persist( new Note( id, "note" + id ) );
				session.persist( new Tag( id, "note" + id ) );
				session.persist( new Item( id, "note" + id ) );
			}
			final Owner first = new Owner( 1 );
			first.plain.add( session.find( Note.class, 10 ) );
			first.byTag.put( session.find( Tag.class, 10 ), session.find( Note.class, 10 ) );
			first.tags.add( session.find( Tag.class, 10 ) );
			first.owned.put( session.find( Tag.class, 10 ), session.find( Item.class, 10 ) );
			session.persist( first );
			final Owner second = new Owner( 2 );
			second.plain.add( session.find( Note.class, 20 ) );
			second.plain.add( session.find( Note.class, 21 ) );
			second.byTag.put( session.find( Tag.class, 20 ), session.find( Note.class, 20 ) );
			second.byTag.put( session.find( Tag.class, 21 ), session.find( Note.class, 21 ) );
			second.tags.add( session.find( Tag.class, 20 ) );
			second.tags.add( session.find( Tag.class, 21 ) );
			second.owned.put( session.find( Tag.class, 20 ), session.find( Item.class, 20 ) );
			second.owned.put( session.find( Tag.class, 21 ), session.find( Item.class, 21 ) );
			session.persist( second );
			// the third owner has no elements at all
			session.persist( new Owner( 3 ) );
		} );
		// the only element of the first owner is soft-deleted, and so is one of the two elements of the second
		scope.inTransaction( session -> {
			session.remove( session.find( Note.class, 10 ) );
			session.remove( session.find( Note.class, 20 ) );
			session.remove( session.find( Item.class, 10 ) );
			session.remove( session.find( Item.class, 20 ) );
			session.find( Owner.class, 1 ).tags.clear();
			session.find( Owner.class, 2 ).tags.remove( session.find( Tag.class, 20 ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	// left join of a collection of soft-deleted entities

	@Test
	public void leftJoinHidesDeletedElement(SessionFactoryScope scope) {
		assertEquals( Set.of( "1:null", "2:21", "3:null" ),
				rows( scope, "select o.id, e.id from Owner o left join o.plain e" ) );
		assertEquals( Set.of( "1:null", "2:note21", "3:null" ),
				rows( scope, "select o.id, e.name from Owner o left join o.plain e" ) );
		assertEquals( Set.of( "1:null:null", "2:21:note21", "3:null:null" ),
				rows( scope, "select o.id, e.id, e.name from Owner o left join o.plain e" ) );
	}

	@Test
	public void leftJoinWithRestriction(SessionFactoryScope scope) {
		assertEquals( List.of( 1, 3 ),
				ids( scope, "select o.id from Owner o left join o.plain e where e.id is null order by o.id" ) );
		assertEquals( List.of( 2 ),
				ids( scope, "select o.id from Owner o left join o.plain e where e.id is not null order by o.id" ) );
		assertEquals( Set.of( "1:null", "2:21", "3:null" ),
				rows( scope, "select o.id, e.id from Owner o left join o.plain e on e.name like 'note%'" ) );
	}

	@Test
	public void leftJoinOfMapHidesDeletedElement(SessionFactoryScope scope) {
		assertEquals( Set.of( "1:null", "2:21", "3:null" ),
				rows( scope, "select o.id, e.id from Owner o left join o.byTag e" ) );
		assertEquals( Set.of( "1:null:null", "2:21:21", "3:null:null" ),
				rows( scope, "select o.id, key(e).id, value(e).id from Owner o left join o.byTag e" ) );
		assertEquals( Set.of( "1:null:null", "2:note21:note21", "3:null:null" ),
				rows( scope, "select o.id, key(e).name, value(e).name from Owner o left join o.byTag e" ) );
	}

	@Test
	public void leftJoinFetchHidesDeletedElement(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final List<Owner> owners = session
					.createSelectionQuery( "from Owner o left join fetch o.plain order by o.id", Owner.class )
					.getResultList();
			assertEquals( 3, owners.size() );
			assertThat( owners.get( 0 ).plain ).isEmpty();
			assertThat( owners.get( 1 ).plain ).extracting( note -> note.id ).containsExactly( 21 );
			assertThat( owners.get( 2 ).plain ).isEmpty();
		} );
		scope.inSession( session -> {
			final List<Owner> owners = session
					.createSelectionQuery( "from Owner o left join fetch o.byTag order by o.id", Owner.class )
					.getResultList();
			assertEquals( 3, owners.size() );
			assertThat( owners.get( 0 ).byTag ).isEmpty();
			assertThat( owners.get( 1 ).byTag.values() ).extracting( note -> note.id ).containsExactly( 21 );
			assertThat( owners.get( 2 ).byTag ).isEmpty();
		} );
	}

	// one-to-many map of soft-deleted entities, keyed by an entity

	@Test
	public void leftJoinOfOneToManyMapHidesDeletedElement(SessionFactoryScope scope) {
		assertEquals( Set.of( "1:null", "2:21", "3:null" ),
				rows( scope, "select o.id, e.id from Owner o left join o.owned e" ) );
		assertEquals( Set.of( "1:null:null", "2:21:21", "3:null:null" ),
				rows( scope, "select o.id, key(e).id, value(e).id from Owner o left join o.owned e" ) );
		assertEquals( Set.of( "1:null:null", "2:note21:note21", "3:null:null" ),
				rows( scope, "select o.id, key(e).name, value(e).name from Owner o left join o.owned e" ) );
		scope.inSession( session -> {
			final List<Owner> owners = session
					.createSelectionQuery( "from Owner o left join fetch o.owned order by o.id", Owner.class )
					.getResultList();
			assertEquals( 3, owners.size() );
			assertThat( owners.get( 0 ).owned ).isEmpty();
			assertThat( owners.get( 1 ).owned.values() ).extracting( item -> item.id ).containsExactly( 21 );
			assertThat( owners.get( 2 ).owned ).isEmpty();
		} );
	}

	@Test
	public void implicitJoinOfOneToManyMapHidesDeletedElement(SessionFactoryScope scope) {
		assertEquals( List.of( 2 ),
				ids( scope, "select o.id from Owner o where value(o.owned) is not null" ) );
		assertEquals( List.of( 2 ),
				ids( scope, "select o.id from Owner o where key(o.owned).name like 'note%'" ) );
	}

	// left join of a collection with a soft-deleted join table

	@Test
	public void leftJoinHidesDeletedJoinTableRow(SessionFactoryScope scope) {
		assertEquals( Set.of( "1:null", "2:21", "3:null" ),
				rows( scope, "select o.id, e.id from Owner o left join o.tags e" ) );
		assertEquals( Set.of( "1:null:null", "2:21:note21", "3:null:null" ),
				rows( scope, "select o.id, e.id, e.name from Owner o left join o.tags e" ) );
		scope.inSession( session -> {
			final List<Owner> owners = session
					.createSelectionQuery( "from Owner o left join fetch o.tags order by o.id", Owner.class )
					.getResultList();
			assertEquals( 3, owners.size() );
			assertThat( owners.get( 0 ).tags ).isEmpty();
			assertThat( owners.get( 1 ).tags ).extracting( tag -> tag.id ).containsExactly( 21 );
			assertThat( owners.get( 2 ).tags ).isEmpty();
		} );
	}

	// implicit join

	@Test
	public void implicitJoinHidesDeletedElement(SessionFactoryScope scope) {
		assertEquals( List.of( 2 ),
				ids( scope, "select o.id from Owner o where element(o.plain) is not null" ) );
		assertEquals( List.of( 2 ),
				ids( scope, "select o.id from Owner o where element(o.plain).id is not null" ) );
		assertEquals( List.of(),
				ids( scope, "select o.id from Owner o where element(o.plain).id = 10" ) );
		assertEquals( List.of( 2 ),
				ids( scope, "select o.id from Owner o where element(o.plain).id = 21" ) );
		assertEquals( List.of( 2 ),
				ids( scope, "select o.id from Owner o where element(o.plain).name like 'note%'" ) );
		assertEquals( List.of( 2 ),
				ids( scope, "select o.id from Owner o where value(o.byTag) is not null" ) );
		assertEquals( List.of( 2 ),
				ids( scope, "select o.id from Owner o where key(o.byTag) is not null" ) );
	}

	@Test
	public void implicitJoinHidesDeletedJoinTableRow(SessionFactoryScope scope) {
		assertEquals( List.of( 2 ),
				ids( scope, "select o.id from Owner o where element(o.tags) is not null" ) );
		assertEquals( List.of(),
				ids( scope, "select o.id from Owner o where element(o.tags).id = 10" ) );
		assertEquals( List.of( 2 ),
				ids( scope, "select o.id from Owner o where element(o.tags).name like 'note%'" ) );
		assertEquals( List.of(),
				ids( scope, "select o.id from Owner o where element(o.tags).name = 'note10'" ) );
	}

	private static List<Integer> ids(SessionFactoryScope scope, String hql) {
		return scope.fromSession( session -> session.createSelectionQuery( hql, Integer.class ).getResultList() );
	}

	private static Set<String> rows(SessionFactoryScope scope, String hql) {
		return scope.fromSession( session -> {
			final Set<String> rows = new HashSet<>();
			final List<Object[]> results = session.createSelectionQuery( hql, Object[].class ).getResultList();
			for ( Object[] row : results ) {
				final StringBuilder text = new StringBuilder();
				for ( Object value : row ) {
					if ( !text.isEmpty() ) {
						text.append( ':' );
					}
					text.append( value );
				}
				rows.add( text.toString() );
			}
			// no row twice
			assertEquals( results.size(), rows.size() );
			return rows;
		} );
	}

	@Entity(name = "Owner")
	public static class Owner {
		@Id
		Integer id;
		@ManyToMany
		@JoinTable(name = "owner_plain")
		Set<Note> plain = new HashSet<>();
		@ManyToMany
		@JoinTable(name = "owner_by_tag")
		@MapKeyJoinColumn(name = "tag_id")
		Map<Tag, Note> byTag = new HashMap<>();
		@ManyToMany
		@JoinTable(name = "owner_tags")
		@SoftDelete
		Set<Tag> tags = new HashSet<>();
		@OneToMany
		@JoinColumn(name = "owner_id")
		@MapKeyJoinColumn(name = "key_id", nullable = true)
		Map<Tag, Item> owned = new HashMap<>();

		public Owner() {
		}

		public Owner(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Note")
	@SoftDelete
	public static class Note {
		@Id
		Integer id;
		String name;

		public Note() {
		}

		public Note(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Item")
	@SoftDelete
	public static class Item {
		@Id
		Integer id;
		String name;

		public Item() {
		}

		public Item(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Tag")
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
}
