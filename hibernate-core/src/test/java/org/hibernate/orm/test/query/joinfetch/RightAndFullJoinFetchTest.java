package org.hibernate.orm.test.query.joinfetch;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.tree.spi.from.SqmAttributeJoin;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.JoinType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A fetch join must be an inner or left outer join, since a right or full
 * join produces rows with no owner to attach the fetched association to.
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		RightAndFullJoinFetchTest.Parent.class,
		RightAndFullJoinFetchTest.SpecialParent.class,
		RightAndFullJoinFetchTest.Note.class
} )
public class RightAndFullJoinFetchTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent parent = new Parent( 1L );
			session.persist( parent );
			// a parent with no notes
			session.persist( new SpecialParent( 2L ) );
			session.persist( new Note( 1L, parent ) );
			// a note with no parent
			session.persist( new Note( 2L, null ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testHqlRightJoinFetchCollection(SessionFactoryScope scope) {
		assertHqlRejected( scope, "select p from Parent p right join fetch p.notes", "right outer" );
		assertHqlRejected( scope, "select p from Parent p right outer join fetch p.notes n", "right outer" );
	}

	@Test
	public void testHqlFullJoinFetchCollection(SessionFactoryScope scope) {
		assertHqlRejected( scope, "select p from Parent p full join fetch p.notes", "full" );
		assertHqlRejected( scope, "select p from Parent p full outer join fetch p.notes n", "full" );
	}

	@Test
	public void testHqlRightJoinFetchToOne(SessionFactoryScope scope) {
		assertHqlRejected( scope, "select n from Note n right join fetch n.parent", "right outer" );
	}

	@Test
	public void testHqlFullJoinFetchToOne(SessionFactoryScope scope) {
		assertHqlRejected( scope, "select n from Note n full join fetch n.parent", "full" );
	}

	@Test
	public void testHqlRightAndFullJoinFetchTreat(SessionFactoryScope scope) {
		assertHqlRejected( scope,
				"select n from Note n right join fetch treat(n.parent as SpecialParent)", "right outer" );
		assertHqlRejected( scope,
				"select n from Note n full join fetch treat(n.parent as SpecialParent)", "full" );
	}

	@Test
	public void testCriteriaRightAndFullFetchByName(SessionFactoryScope scope) {
		final var builder = scope.getSessionFactory().getCriteriaBuilder();
		assertCriteriaRejected( () -> builder.createQuery( Parent.class ).from( Parent.class )
				.fetch( "notes", JoinType.RIGHT ) );
		assertCriteriaRejected( () -> builder.createQuery( Parent.class ).from( Parent.class )
				.fetch( "notes", JoinType.FULL ) );
		assertCriteriaRejected( () -> builder.createQuery( Note.class ).from( Note.class )
				.fetch( "parent", JoinType.RIGHT ) );
		assertCriteriaRejected( () -> builder.createQuery( Note.class ).from( Note.class )
				.fetch( "parent", JoinType.FULL ) );
	}

	@Test
	public void testCriteriaRightAndFullFetchByAttribute(SessionFactoryScope scope) {
		final var builder = scope.getSessionFactory().getCriteriaBuilder();
		final var metamodel = scope.getSessionFactory().getJpaMetamodel();
		final var notes = metamodel.entity( Parent.class ).getList( "notes", Note.class );
		final var parent = metamodel.entity( Note.class ).getSingularAttribute( "parent", Parent.class );
		assertCriteriaRejected( () -> builder.createQuery( Parent.class ).from( Parent.class )
				.fetch( notes, JoinType.RIGHT ) );
		assertCriteriaRejected( () -> builder.createQuery( Parent.class ).from( Parent.class )
				.fetch( notes, JoinType.FULL ) );
		assertCriteriaRejected( () -> builder.createQuery( Note.class ).from( Note.class )
				.fetch( parent, JoinType.RIGHT ) );
		assertCriteriaRejected( () -> builder.createQuery( Note.class ).from( Note.class )
				.fetch( parent, JoinType.FULL ) );
	}

	@Test
	public void testCriteriaRightJoinTreatedAsFetch(SessionFactoryScope scope) {
		final var builder = scope.getSessionFactory().getCriteriaBuilder();
		final var join = (SqmAttributeJoin<Note, Parent>)
				builder.createQuery( Note.class ).from( Note.class )
						.<Parent>join( "parent", JoinType.RIGHT );
		assertCriteriaRejected( () -> join.treatAs( SpecialParent.class, null, true ) );
		// the treated join is fine as long as it is not fetched
		assertThat( join.treatAs( SpecialParent.class, null, false ).isFetched() ).isFalse();
	}

	@Test
	public void testHqlInnerAndLeftJoinFetchCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Parent> inner =
					session.createQuery( "select p from Parent p join fetch p.notes", Parent.class )
							.getResultList();
			assertThat( inner ).hasSize( 1 );
			assertThat( Hibernate.isInitialized( inner.get( 0 ).notes ) ).isTrue();
			assertThat( inner.get( 0 ).notes ).hasSize( 1 );
		} );
		scope.inTransaction( session -> {
			final List<Parent> left =
					session.createQuery( "select p from Parent p left join fetch p.notes order by p.id", Parent.class )
							.getResultList();
			assertThat( left ).hasSize( 2 );
			assertThat( left ).allMatch( p -> Hibernate.isInitialized( p.notes ) );
			assertThat( left.get( 0 ).notes ).hasSize( 1 );
			assertThat( left.get( 1 ).notes ).isEmpty();
		} );
	}

	@Test
	public void testHqlInnerAndLeftJoinFetchToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Note> inner =
					session.createQuery( "select n from Note n join fetch n.parent", Note.class )
							.getResultList();
			assertThat( inner ).hasSize( 1 );
			assertThat( Hibernate.isInitialized( inner.get( 0 ).parent ) ).isTrue();
		} );
		scope.inTransaction( session -> {
			final List<Note> left =
					session.createQuery( "select n from Note n left join fetch n.parent order by n.id", Note.class )
							.getResultList();
			assertThat( left ).hasSize( 2 );
			assertThat( Hibernate.isInitialized( left.get( 0 ).parent ) ).isTrue();
			assertThat( left.get( 1 ).parent ).isNull();
		} );
	}

	@Test
	public void testCriteriaInnerAndLeftFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Parent.class );
			query.from( Parent.class ).fetch( "notes", JoinType.INNER );
			assertThat( session.createQuery( query ).getResultList() ).hasSize( 1 );
		} );
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Parent.class );
			query.from( Parent.class ).fetch( "notes", JoinType.LEFT );
			final List<Parent> left = session.createQuery( query ).getResultList();
			assertThat( left ).hasSize( 2 );
			assertThat( left ).allMatch( p -> Hibernate.isInitialized( p.notes ) );
		} );
	}

	@Test
	public void testHqlRightJoinWithoutFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Object[]> rows =
					session.createQuery( "select p, n from Parent p right join p.notes n order by n.id", Object[].class )
							.getResultList();
			assertThat( rows ).hasSize( 2 );
			assertThat( rows.get( 0 )[0] ).isNotNull();
			assertThat( rows.get( 1 )[0] ).isNull();
			assertThat( rows.get( 1 )[1] ).isNotNull();
		} );
	}

	@Test
	public void testCriteriaRightJoinWithoutFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Note.class );
			final var root = query.from( Parent.class );
			final var notes = root.<Note>join( "notes", JoinType.RIGHT );
			query.select( notes ).orderBy( builder.asc( notes.get( "id" ) ) );
			assertThat( session.createQuery( query ).getResultList() ).hasSize( 2 );
		} );
	}

	private static void assertHqlRejected(SessionFactoryScope scope, String hql, String joinType) {
		scope.inSession( session -> assertThatThrownBy( () -> session.createQuery( hql, Object.class ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasCauseInstanceOf( SemanticException.class )
				.hasMessageContaining( "Fetch join is a " + joinType + " join" )
				.hasMessageContaining( hql ) );
	}

	private static void assertCriteriaRejected(Runnable fetch) {
		assertThatThrownBy( fetch::run )
				.isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "Fetch join is a " );
	}

	@Entity( name = "Parent" )
	public static class Parent {
		@Id
		Long id;
		@OneToMany( mappedBy = "parent" )
		List<Note> notes = new ArrayList<>();

		Parent() {
		}

		Parent(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "SpecialParent" )
	public static class SpecialParent extends Parent {
		SpecialParent() {
		}

		SpecialParent(Long id) {
			super( id );
		}
	}

	@Entity( name = "Note" )
	public static class Note {
		@Id
		Long id;
		@ManyToOne( fetch = FetchType.LAZY )
		Parent parent;

		Note() {
		}

		Note(Long id, Parent parent) {
			this.id = id;
			this.parent = parent;
		}
	}
}
