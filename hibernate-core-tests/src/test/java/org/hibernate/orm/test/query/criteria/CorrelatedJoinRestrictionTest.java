package org.hibernate.orm.test.query.criteria;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A restriction given with {@code on()} to a join of a correlated root
 * must restrict the subquery.
 */
@DomainModel(annotatedClasses = {
		CorrelatedJoinRestrictionTest.Parent.class,
		CorrelatedJoinRestrictionTest.Item.class,
		CorrelatedJoinRestrictionTest.Tag.class,
		CorrelatedJoinRestrictionTest.Note.class
})
@SessionFactory
@JiraKey("HHH-21163")
public class CorrelatedJoinRestrictionTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Tag red = new Tag( 1, "red" );
			final Tag blue = new Tag( 2, "blue" );
			session.persist( red );
			session.persist( blue );
			final Parent first = new Parent( 1 );
			first.tags.add( red );
			final Parent second = new Parent( 2 );
			second.tags.add( blue );
			session.persist( first );
			session.persist( second );
			session.persist( new Item( 1, "x", first ) );
			session.persist( new Item( 2, "y", first ) );
			session.persist( new Item( 3, "y", second ) );
			session.persist( new Note( 1, "x", first ) );
			session.persist( new Note( 2, "y", second ) );
		} );
		// the only note of the first parent is soft-deleted
		scope.inTransaction( session -> session.remove( session.find( Note.class, 1 ) ) );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void oneToManyJoin(SessionFactoryScope scope) {
		assertEquals( List.of( 1 ), parentsWith( scope, "items", "x" ) );
		assertEquals( List.of( 1, 2 ), parentsWith( scope, "items", "y" ) );
		assertEquals( List.of(), parentsWith( scope, "items", "z" ) );
	}

	@Test
	public void manyToManyJoin(SessionFactoryScope scope) {
		assertEquals( List.of( 1 ), parentsWith( scope, "tags", "red" ) );
		assertEquals( List.of( 2 ), parentsWith( scope, "tags", "blue" ) );
		assertEquals( List.of(), parentsWith( scope, "tags", "green" ) );
	}

	@Test
	public void entityJoin(SessionFactoryScope scope) {
		assertEquals( List.of( 1 ), parentsWithEntityJoin( scope, Item.class, "x" ) );
		assertEquals( List.of( 1, 2 ), parentsWithEntityJoin( scope, Item.class, "y" ) );
		assertEquals( List.of(), parentsWithEntityJoin( scope, Item.class, "z" ) );
	}

	@Test
	public void entityJoinOfSoftDeletedEntity(SessionFactoryScope scope) {
		assertEquals( List.of(), parentsWithEntityJoin( scope, Note.class, "x" ) );
		assertEquals( List.of( 2 ), parentsWithEntityJoin( scope, Note.class, "y" ) );
	}

	private static List<Integer> parentsWithEntityJoin(SessionFactoryScope scope, Class<?> entityClass, String name) {
		return scope.fromSession( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Integer> query = cb.createQuery( Integer.class );
			final JpaRoot<Parent> parent = query.from( Parent.class );
			final JpaSubQuery<Integer> subquery = query.subquery( Integer.class );
			final JpaRoot<Parent> correlated = subquery.correlate( parent );
			final var item = correlated.join( entityClass );
			item.on( cb.equal( item.get( "parent" ), correlated ), cb.equal( item.get( "name" ), name ) );
			subquery.select( cb.literal( 1 ) );
			query.select( parent.get( "id" ) ).where( cb.exists( subquery ) ).orderBy( cb.asc( parent.get( "id" ) ) );
			return session.createQuery( query ).getResultList();
		} );
	}

	private static List<Integer> parentsWith(SessionFactoryScope scope, String collection, String name) {
		return scope.fromSession( session -> {
			final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			final JpaCriteriaQuery<Integer> query = cb.createQuery( Integer.class );
			final JpaRoot<Parent> parent = query.from( Parent.class );
			final JpaSubQuery<Integer> subquery = query.subquery( Integer.class );
			final var element = subquery.correlate( parent ).join( collection );
			element.on( cb.equal( element.get( "name" ), name ) );
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
		List<Item> items = new ArrayList<>();
		@ManyToMany
		List<Tag> tags = new ArrayList<>();

		public Parent() {
		}

		public Parent(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Item")
	public static class Item {
		@Id
		Integer id;
		String name;
		@ManyToOne
		Parent parent;

		public Item() {
		}

		public Item(Integer id, String name, Parent parent) {
			this.id = id;
			this.name = name;
			this.parent = parent;
		}
	}

	@Entity(name = "Note")
	@SoftDelete
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
