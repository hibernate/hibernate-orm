package org.hibernate.orm.test.softdelete;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSubQuery;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An inner join of a collection of soft-deleted entities is an inner join,
 * and no join of such a collection brings back an entity which is hidden.
 */
@DomainModel(annotatedClasses = {
		SoftDeleteCollectionJoinTests.Parent.class,
		SoftDeleteCollectionJoinTests.Note.class,
		SoftDeleteCollectionJoinTests.Marker.class
})
@SessionFactory
@JiraKey("HHH-21165")
public class SoftDeleteCollectionJoinTests {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent first = new Parent( 1 );
			final Parent second = new Parent( 2 );
			session.persist( first );
			session.persist( second );
			// the third parent has no notes at all
			session.persist( new Parent( 3 ) );
			session.persist( new Note( 1, "x", first ) );
			session.persist( new Note( 2, "y", second ) );
			// two notes with no parent
			session.persist( new Note( 3, "z", null ) );
			session.persist( new Note( 4, "z", null ) );
			// a note of the second parent which the restriction on the entity hides
			session.persist( new Note( 5, "hidden", second ) );
			// a marker for a deleted note, for a live note, and for no note at all
			session.persist( new Marker( 1 ) );
			session.persist( new Marker( 2 ) );
			session.persist( new Marker( 9 ) );
		} );
		// the only note of the first parent is soft-deleted, and so is one of the notes with no parent
		scope.inTransaction( session -> {
			session.remove( session.find( Note.class, 1 ) );
			session.remove( session.find( Note.class, 4 ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void innerJoin(SessionFactoryScope scope) {
		assertEquals( List.of( 2 ), ids( scope, "select p.id from Parent p join p.notes n order by p.id" ) );
	}

	@Test
	public void innerJoinWithRestriction(SessionFactoryScope scope) {
		assertEquals( List.of( 2 ), ids( scope, "select p.id from Parent p join p.notes n on n.name = 'y' order by p.id" ) );
		assertEquals( List.of(), ids( scope, "select p.id from Parent p join p.notes n on n.name = 'x' order by p.id" ) );
	}

	@Test
	public void leftJoin(SessionFactoryScope scope) {
		assertEquals( List.of( 1, 2, 3 ), ids( scope, "select p.id from Parent p left join p.notes n order by p.id" ) );
	}

	@Test
	public void rightJoinDoesNotReturnHiddenNotes(SessionFactoryScope scope) {
		assertThat( ids( scope, "select n.id from Parent p right join p.notes n" ) )
				.contains( 2 )
				.doesNotContain( 1, 4, 5 );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsFullJoin.class)
	public void fullJoinDoesNotReturnHiddenNotes(SessionFactoryScope scope) {
		assertThat( ids( scope, "select n.id from Parent p full join p.notes n" ) )
				.contains( 2 )
				.doesNotContain( 1, 4, 5 );
	}

	@Test
	public void laterRightJoinKeepsItsRows(SessionFactoryScope scope) {
		// every marker survives the last join, with no note for the deleted note and for the missing note
		final Set<String> rows = new HashSet<>();
		scope.inSession( session -> {
			for ( Object[] row : session.createSelectionQuery(
					"select z.id, n.id from Parent p right join p.notes n right join Marker z on z.id = n.id",
					Object[].class ).getResultList() ) {
				rows.add( row[0] + ":" + row[1] );
			}
		} );
		assertEquals( Set.of( "1:null", "2:2", "9:null" ), rows );
	}

	@Test
	public void leftJoinFetchKeepsOwner(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final Parent parent = session
					.createSelectionQuery( "select p from Parent p left join fetch p.notes where p.id = 1", Parent.class )
					.uniqueResult();
			assertThat( parent ).isNotNull();
			assertThat( parent.notes ).isEmpty();
		} );
	}

	@Test
	public void correlatedCollectionInSubquery(SessionFactoryScope scope) {
		assertEquals( List.of( 2 ),
				ids( scope, "select p.id from Parent p where exists (select 1 from p.notes n) order by p.id" ) );
	}

	@Test
	public void correlatedJoin(SessionFactoryScope scope) {
		assertEquals( List.of( 2 ), parentsWithNote( scope, null ) );
		assertEquals( List.of( 2 ), parentsWithNote( scope, "y" ) );
		assertEquals( List.of(), parentsWithNote( scope, "x" ) );
	}

	@Test
	public void correlatedCollectionAfterEntityInSubquery(SessionFactoryScope scope) {
		// the join of the collection follows an entity in the from clause of the subquery
		assertEquals( List.of( 2 ), ids( scope,
				"select p.id from Parent p where exists (select 1 from Marker z join p.notes n on n.id = z.id)"
						+ " order by p.id" ) );
		// the second marker has no note among those of the first and third parents
		assertEquals( List.of( 1, 3 ), ids( scope,
				"select p.id from Parent p where exists (select 1 from Marker z left join p.notes n on n.id = z.id"
						+ " where z.id = 2 and n.id is null) order by p.id" ) );
	}

	@Test
	@JiraKey("HHH-21213")
	public void implicitJoin(SessionFactoryScope scope) {
		// a path through the collection is an inner join, just like an explicit join
		assertEquals( List.of( 2 ), ids( scope, "select element(p.notes).id from Parent p" ) );
		assertEquals( List.of(), ids( scope, "select p.id from Parent p where element(p.notes) is null order by p.id" ) );
		assertEquals( List.of( 2 ),
				ids( scope, "select p.id from Parent p where element(p.notes).name = 'y' order by p.id" ) );
	}

	@Test
	@JiraKey("HHH-21213")
	public void implicitJoinInSubquery(SessionFactoryScope scope) {
		// the path through the collection starts from the root of the outer query
		assertEquals( List.of( 2 ), ids( scope,
				"select p.id from Parent p where exists (select 1 from Marker z where z.id = element(p.notes).id)"
						+ " order by p.id" ) );
	}

	private static List<Integer> ids(SessionFactoryScope scope, String hql) {
		return scope.fromSession( session -> session.createSelectionQuery( hql, Integer.class ).getResultList() );
	}

	private static List<Integer> parentsWithNote(SessionFactoryScope scope, String name) {
		return scope.fromSession( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Integer> query = cb.createQuery( Integer.class );
			final JpaRoot<Parent> parent = query.from( Parent.class );
			final JpaSubQuery<Integer> subquery = query.subquery( Integer.class );
			final var note = subquery.correlate( parent ).join( "notes" );
			if ( name != null ) {
				note.on( cb.equal( note.get( "name" ), name ) );
			}
			subquery.select( cb.literal( 1 ) );
			query.select( parent.get( "id" ) ).where( cb.exists( subquery ) ).orderBy( cb.asc( parent.get( "id" ) ) );
			return session.createQuery( query ).getResultList();
		} );
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		Integer id;
		@OneToMany(mappedBy = "parent")
		List<Note> notes = new ArrayList<>();

		public Parent() {
		}

		public Parent(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Note")
	@SoftDelete
	@SQLRestriction("name <> 'hidden'")
	public static class Note {
		@Id
		Integer id;
		String name;
		@ManyToOne
		Parent parent;

		public Note() {
		}

		public Note(Integer id, String name, Parent parent) {
			this.id = id;
			this.name = name;
			this.parent = parent;
		}
	}

	@Entity(name = "Marker")
	public static class Marker {
		@Id
		Integer id;

		public Marker() {
		}

		public Marker(Integer id) {
			this.id = id;
		}
	}
}
