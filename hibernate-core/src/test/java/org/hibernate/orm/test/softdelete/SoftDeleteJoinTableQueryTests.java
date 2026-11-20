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
import jakarta.persistence.Table;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSubQuery;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A restriction on the join table of a many-to-many association applies to
 * a query which has no need of the table of the associated entity, and the
 * restrictions of a map apply to a query which has no need of the table of
 * its key.
 */
@DomainModel(annotatedClasses = {
		SoftDeleteJoinTableQueryTests.Owner.class,
		SoftDeleteJoinTableQueryTests.Tag.class,
		SoftDeleteJoinTableQueryTests.Note.class
})
@SessionFactory
@JiraKey("HHH-21214")
public class SoftDeleteJoinTableQueryTests {

	// The collections of the owner: with a soft-delete column, an SQL restriction,
	// and a filter on the join table, and then two maps keyed by an entity, one
	// with a soft-delete column on the join table, the other of soft-deleted entities
	private static final String TAGS = "tags";
	private static final String LABELS = "labels";
	private static final String MARKS = "marks";
	private static final String KEYED = "keyed";
	private static final String NOTES = "notes";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Tag hidden = new Tag( 10 );
			final Tag live = new Tag( 20 );
			session.persist( hidden );
			session.persist( live );
			final Note hiddenNote = new Note( 10 );
			final Note liveNote = new Note( 20 );
			session.persist( hiddenNote );
			session.persist( liveNote );
			// every element of the first owner is hidden
			final Owner first = new Owner( 1 );
			first.tags.add( hidden );
			first.labels.add( hidden );
			first.marks.add( hidden );
			first.keyed.put( hidden, hidden );
			first.notes.put( hidden, hiddenNote );
			session.persist( first );
			final Owner second = new Owner( 2 );
			second.tags.add( live );
			second.labels.add( live );
			second.marks.add( live );
			second.keyed.put( live, live );
			second.notes.put( live, liveNote );
			session.persist( second );
			// the third owner has no elements at all
			session.persist( new Owner( 3 ) );
		} );
		// soft-delete the rows of the join tables, and the note of the first owner
		scope.inTransaction( session -> {
			final Owner first = session.find( Owner.class, 1 );
			first.tags.clear();
			first.keyed.clear();
			session.remove( session.find( Note.class, 10 ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@ValueSource(strings = {TAGS, LABELS, MARKS, KEYED, NOTES})
	public void innerJoin(String collection, SessionFactoryScope scope) {
		assertEquals( List.of( 2 ),
				ids( scope, "select o.id from Owner o join o." + collection + " e order by o.id" ) );
		assertEquals( List.of( 20 ),
				ids( scope, "select e.id from Owner o join o." + collection + " e" ) );
		scope.inSession( session -> {
			session.enableFilter( "liveMarks" );
			assertEquals( 1L,
					session.createSelectionQuery( "select count(e) from Owner o join o." + collection + " e",
							Long.class ).getSingleResult() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {TAGS, LABELS, MARKS, KEYED, NOTES})
	public void innerJoinOfElementTable(String collection, SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.enableFilter( "liveMarks" );
			final List<Object[]> rows =
					session.createSelectionQuery( "select o.id, e from Owner o join o." + collection + " e",
							Object[].class ).getResultList();
			assertEquals( 1, rows.size() );
			assertEquals( 2, rows.get( 0 )[0] );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {TAGS, LABELS, MARKS, KEYED, NOTES})
	public void leftJoin(String collection, SessionFactoryScope scope) {
		assertEquals( List.of( 1, 2, 3 ),
				ids( scope, "select o.id from Owner o left join o." + collection + " e order by o.id" ) );
	}

	@ParameterizedTest
	@ValueSource(strings = {TAGS, LABELS, MARKS, KEYED, NOTES})
	public void leftJoinElement(String collection, SessionFactoryScope scope) {
		assertEquals( singletonList( null ),
				ids( scope, "select e.id from Owner o left join o." + collection + " e where o.id = 1" ) );
	}

	@ParameterizedTest
	@ValueSource(strings = {TAGS, LABELS, MARKS, KEYED, NOTES})
	public void leftJoinFetch(String collection, SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.enableFilter( "liveMarks" );
			final Owner owner = session.createSelectionQuery(
					"select o from Owner o left join fetch o." + collection + " where o.id = 1",
					Owner.class ).getSingleResult();
			assertThat( owner.tags ).isEmpty();
			assertThat( owner.labels ).isEmpty();
			assertThat( owner.marks ).isEmpty();
			assertThat( owner.keyed ).isEmpty();
			assertThat( owner.notes ).isEmpty();
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {TAGS, LABELS, MARKS, KEYED, NOTES})
	public void subquery(String collection, SessionFactoryScope scope) {
		assertEquals( List.of( 2 ), ids( scope,
				"select o.id from Owner o where exists (select 1 from o." + collection + " e) order by o.id" ) );
		assertEquals( List.of( 2 ), ids( scope,
				"select o.id from Owner o where exists (select 1 from Tag t join o." + collection
						+ " e on e.id = t.id) order by o.id" ) );
	}

	@ParameterizedTest
	@ValueSource(strings = {TAGS, LABELS, MARKS, KEYED, NOTES})
	public void correlatedJoin(String collection, SessionFactoryScope scope) {
		scope.inSession( session -> {
			session.enableFilter( "liveMarks" );
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Integer> query = cb.createQuery( Integer.class );
			final JpaRoot<Owner> owner = query.from( Owner.class );
			final JpaSubQuery<Integer> subquery = query.subquery( Integer.class );
			subquery.correlate( owner ).join( collection );
			subquery.select( cb.literal( 1 ) );
			query.select( owner.get( "id" ) ).where( cb.exists( subquery ) );
			assertEquals( List.of( 2 ), session.createQuery( query ).getResultList() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {LABELS, MARKS})
	public void elementInSubquery(String collection, SessionFactoryScope scope) {
		assertEquals( List.of( 2 ), ids( scope,
				"select o.id from Owner o where exists (select 1 from Tag t where t.id = element(o." + collection
						+ ").id) order by o.id" ) );
	}

	@ParameterizedTest
	@ValueSource(strings = {TAGS, LABELS, MARKS, KEYED, NOTES})
	public void emptiness(String collection, SessionFactoryScope scope) {
		assertEquals( List.of( 2 ),
				ids( scope, "select o.id from Owner o where o." + collection + " is not empty order by o.id" ) );
		assertEquals( List.of( 1, 3 ),
				ids( scope, "select o.id from Owner o where o." + collection + " is empty order by o.id" ) );
	}

	@ParameterizedTest
	@ValueSource(strings = {TAGS, LABELS, MARKS, KEYED, NOTES})
	public void elementFunction(String collection, SessionFactoryScope scope) {
		assertEquals( List.of( 2 ),
				ids( scope, "select o.id from Owner o where element(o." + collection + ") is not null order by o.id" ) );
	}

	private static List<Integer> ids(SessionFactoryScope scope, String hql) {
		return scope.fromSession( session -> {
			session.enableFilter( "liveMarks" );
			return session.createSelectionQuery( hql, Integer.class ).getResultList();
		} );
	}

	@Entity(name = "Owner")
	@Table(name = "owners")
	@FilterDef(name = "liveMarks")
	public static class Owner {
		@Id
		Integer id;

		@ManyToMany
		@JoinTable(name = "owner_tags")
		@SoftDelete
		Set<Tag> tags = new HashSet<>();

		@ManyToMany
		@JoinTable(name = "owner_labels", inverseJoinColumns = @JoinColumn(name = "label_id"))
		@SQLJoinTableRestriction("label_id <> 10")
		Set<Tag> labels = new HashSet<>();

		@ManyToMany
		@JoinTable(name = "owner_marks", inverseJoinColumns = @JoinColumn(name = "mark_id"))
		@FilterJoinTable(name = "liveMarks", condition = "mark_id <> 10")
		Set<Tag> marks = new HashSet<>();

		@ManyToMany
		@JoinTable(name = "owner_keyed")
		@MapKeyJoinColumn(name = "key_id")
		@SoftDelete
		Map<Tag, Tag> keyed = new HashMap<>();

		@ManyToMany
		@JoinTable(name = "owner_notes")
		@MapKeyJoinColumn(name = "key_id")
		Map<Tag, Note> notes = new HashMap<>();

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

		public Tag() {
		}

		public Tag(Integer id) {
			this.id = id;
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
}
