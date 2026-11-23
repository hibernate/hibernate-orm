package org.hibernate.orm.test.query.sqm;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.JoinType;

import org.hibernate.Session;
import org.hibernate.query.sqm.spi.SqmStatementAccess;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * In the from clause of a subquery, a join may follow a path from the outer
 * query, and a join of such a path may follow an entity. Each query must
 * return the right rows, and so must the HQL rendered for it.
 * <p>
 * The queries count the rows of the subquery, so that a table which is
 * joined twice, or a join which is lost, changes the result.
 */
@DomainModel(annotatedClasses = {
		CorrelatedPathJoinTest.Parent.class,
		CorrelatedPathJoinTest.Item.class,
		CorrelatedPathJoinTest.Tag.class,
		CorrelatedPathJoinTest.Animal.class,
		CorrelatedPathJoinTest.Dog.class
})
@SessionFactory
@JiraKey("HHH-21128")
public class CorrelatedPathJoinTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Tag red = new Tag( 1, "red" );
			final Tag blue = new Tag( 2, "blue" );
			session.persist( red );
			session.persist( blue );
			final Parent first = new Parent( 1, "b" );
			first.tags.add( red );
			first.tags.add( blue );
			final Parent second = new Parent( 2, "A" );
			second.tags.add( blue );
			session.persist( first );
			session.persist( second );
			// the third parent has no items, tags or animals
			session.persist( new Parent( 3, "c" ) );
			session.persist( new Item( 1, "x", first ) );
			session.persist( new Item( 2, "y", first ) );
			session.persist( new Item( 3, "y", second ) );
			session.persist( new Dog( 1, first, "x" ) );
			session.persist( new Animal( 2, first ) );
			session.persist( new Dog( 3, second, "zzz" ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	static Stream<Arguments> fromClausesPerParent() {
		return Stream.of(
				// joins which follow a path from the outer query
				arguments( "p.items i join Item x on x.id = i.id", List.of( 2, 1, 0 ) ),
				arguments( "p.items i left join Item x on x.id = i.id", List.of( 2, 1, 0 ) ),
				arguments( "p.items i left join Item x on x.id = i.id and x.name = 'x' where x.id is null", List.of( 1, 1, 0 ) ),
				arguments( "p.items i cross join Item x", List.of( 6, 3, 0 ) ),
				arguments( "p.items i join (select y.id as id from Item y) d on d.id = i.id", List.of( 2, 1, 0 ) ),
				arguments( "p.items i join i.parent q join Item x on x.parent = q", List.of( 4, 1, 0 ) ),
				arguments( "p.items i left join i.parent q join Item x on x.id = i.id", List.of( 2, 1, 0 ) ),
				// two paths from the same outer alias
				arguments( "p.items i join p.tags t", List.of( 4, 1, 0 ) ),
				arguments( "p.items i join p.tags t join Tag u on u.name = 'red'", List.of( 4, 1, 0 ) ),
				arguments( "p.items i join p.tags t join Item y on y.id = i.id", List.of( 4, 1, 0 ) ),
				arguments( "p.items i join p.tags t join Parent z on z.id = i.parent.id and z.name <> t.name", List.of( 4, 1, 0 ) ),
				arguments( "p.items i join p.tags t cross join Tag u", List.of( 8, 2, 0 ) ),
				arguments( "p.items i join p.tags t join (select y.id as id from Tag y) d on d.id = t.id", List.of( 4, 1, 0 ) ),
				arguments( "p.items i join p.tags t join i.parent q on q.id = p.id join Tag u on u.id = t.id", List.of( 4, 1, 0 ) ),
				// an entity join between two paths, and three paths
				arguments( "p.items i join Tag u on u.name = 'red' join p.tags t", List.of( 4, 1, 0 ) ),
				arguments( "p.items i left join Item x on x.id = i.id join p.tags t", List.of( 4, 1, 0 ) ),
				arguments( "p.items i join p.tags t join Tag u on u.id = t.id join p.animals a join Item y on y.id = i.id", List.of( 8, 1, 0 ) ),
				// a path from the outer query which follows an entity
				arguments( "Item x join p.items i where x.id = i.id", List.of( 2, 1, 0 ) ),
				arguments( "Item x, p.items i where x.id = i.id", List.of( 2, 1, 0 ) ),
				arguments( "p.items i, Item x join Item y on y.id = x.id where x.id = i.id", List.of( 2, 1, 0 ) )
		);
	}

	@ParameterizedTest
	@MethodSource("fromClausesPerParent")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInSelect.class)
	public void countPerParent(String fromClause, List<Integer> expected, SessionFactoryScope scope) {
		scope.inSession( session -> assertRendered( session, expected,
				"select (select cast(count(*) as Integer) from " + fromClause + ") from Parent p order by p.id" ) );
	}

	static Stream<Arguments> fromClausesWithLeftJoinOfSecondPath() {
		return Stream.of(
				arguments( "p.items i left join p.tags t on t.name = 'red'", List.of( 2, 1, 0 ) ),
				arguments( "p.items i left join p.tags t on t.name = 'red' join Tag u on u.id = t.id", List.of( 2, 0, 0 ) )
		);
	}

	@ParameterizedTest
	@MethodSource("fromClausesWithLeftJoinOfSecondPath")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInSelect.class)
	public void countPerParentWithLeftJoinOfSecondPath(String fromClause, List<Integer> expected, SessionFactoryScope scope) {
		scope.inSession( session -> {
			final String hql =
					"select (select cast(count(*) as Integer) from " + fromClause + ") from Parent p order by p.id";
			assertEquals( expected, session.createSelectionQuery( hql, Integer.class ).getResultList() );
			// An outer join of a path from the outer query is rendered on a single-row derived root,
			// which is given an alias when it is rendered again, so only the results are compared.
			final String rendered = render( session, hql );
			assertEquals( expected, session.createSelectionQuery( rendered, Integer.class ).getResultList(), rendered );
		} );
	}

	static Stream<Arguments> fromClausesPerItem() {
		return Stream.of(
				// paths from an alias of a join of the outer query
				arguments( "oi.parent q join Item x on x.parent = q", List.of( 2, 2, 1 ) ),
				arguments( "oi.parent q left join Item x on x.parent = q and x.name = 'x'", List.of( 1, 1, 1 ) ),
				// paths from two different outer aliases
				arguments( "p.items i join oi.parent q join Item y on y.parent = q", List.of( 4, 4, 1 ) ),
				arguments( "p.items i join oi.parent q left join Item y on y.parent = q and y.name = 'x'", List.of( 2, 2, 1 ) )
		);
	}

	@ParameterizedTest
	@MethodSource("fromClausesPerItem")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInSelect.class)
	public void countPerItem(String fromClause, List<Integer> expected, SessionFactoryScope scope) {
		scope.inSession( session -> assertRendered( session, expected,
				"select (select cast(count(*) as Integer) from " + fromClause + ")"
						+ " from Parent p join p.items oi order by oi.id" ) );
	}

	@Test
	public void collectionsOfDifferentOuterRoots(SessionFactoryScope scope) {
		scope.inSession( session -> assertRendered( session, List.of( 2 ), """
				select p.id from Parent p, Parent q where p.id <> q.id and exists (
					select 1 from p.items i join q.items j join Item y on y.id = j.id
					where i.name = 'y' and y.name = 'x'
				)
				""" ) );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInSelect.class)
	public void pathFromOuterJoinAliasThenAttributeJoin(SessionFactoryScope scope) {
		scope.inSession( session -> assertRendered( session, List.of( 2, 2, 1 ),
				"select (select cast(count(*) as Integer) from a.owner o join o.items i)"
						+ " from Parent p join p.animals a order by a.id" ) );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInSelect.class)
	public void treatOfOuterJoinAlias(SessionFactoryScope scope) {
		scope.inSession( session -> assertRendered( session, List.of( 1, 0, 0 ),
				"select (select cast(count(*) as Integer) from Item x where x.name = treat(a as Dog).tricks)"
						+ " from Parent p join p.animals a order by a.id" ) );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInSelect.class)
	public void treatOfOuterRoot(SessionFactoryScope scope) {
		scope.inSession( session -> assertRendered( session, List.of( 1, 0, 0 ),
				"select (select cast(count(*) as Integer) from Item x where x.name = treat(a as Dog).tricks)"
						+ " from Animal a order by a.id" ) );
	}

	@Test
	public void leftJoinOfOuterToOnePath(SessionFactoryScope scope) {
		scope.inSession( session -> assertRendered( session, List.of( 1, 2, 3 ), """
				select i.id from Item i where exists (
					select 1 from Item x left join i.parent q on q.name = 'zzz'
					where x.id = i.id and q.id is null
				) order by i.id
				""" ) );
	}

	@Test
	public void manyToManyPathAfterEntity(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertRendered( session, List.of( 1 ), """
					select p.id from Parent p where exists (
						select 1 from Item x join p.tags t join Tag u on u.id = t.id
						where x.parent = p and u.name = 'red'
					) order by p.id
					""" );
			assertRendered( session, List.of( 2 ), """
					select p.id from Parent p where exists (
						select 1 from Item x left join p.tags t on t.name = 'red'
						where x.parent = p and t.id is null
					) order by p.id
					""" );
		} );
	}

	@Test
	public void pathsFromDifferentOuterAliasesAfterEntity(SessionFactoryScope scope) {
		scope.inSession( session -> assertRendered( session, List.of( 1 ), """
				select distinct p.id from Parent p join p.items oi where exists (
					select 1 from Item x join p.tags t join oi.parent q join Tag u on u.id = t.id and q.id = p.id
					where x.id = oi.id and u.name = 'red'
				) order by p.id
				""" ) );
	}

	@Test
	public void pathFromGrandparentQuery(SessionFactoryScope scope) {
		scope.inSession( session -> assertRendered( session, List.of( 1 ), """
				select p.id from Parent p where exists (
					select 1 from Item a where a.parent = p and exists (
						select 1 from Item x join p.items i join Item y on y.id = i.id
						where x.id = a.id and y.name = 'x'
					)
				) order by p.id
				""" ) );
	}

	@Test
	public void sameOuterPathTwiceAfterEntity(SessionFactoryScope scope) {
		scope.inSession( session -> assertRendered( session, List.of( 1 ), """
				select p.id from Parent p where exists (
					select 1 from Item x join p.items i join p.items j join Item y on y.id = i.id and y.id <> j.id
					where x.id = i.id
				) order by p.id
				""" ) );
	}

	@Test
	public void outerPathThenLeftJoinThenRightJoin(SessionFactoryScope scope) {
		scope.inSession( session -> assertRendered( session, List.of( 1, 2 ), """
				select p.id from Parent p where exists (
					select 1 from Item x join p.items i left join Item z on z.id = i.id
					right join Item y on y.id = i.id
					where y.name = 'y' and x.id is not null
				) order by p.id
				""" ) );
	}

	@Test
	public void resultCount(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertEquals( 2L, session.createSelectionQuery( """
					select p.id from Parent p where exists (
						select 1 from p.items i join p.tags t join Tag u on u.id = t.id
					)
					""", Integer.class ).getResultCount() );
			assertEquals( 1L, session.createSelectionQuery( """
					select p.id from Parent p where exists (
						select 1 from Item x join p.items i join Item y on y.id = i.id
						where x.id = i.id and y.name = 'x'
					)
					""", Integer.class ).getResultCount() );
		} );
	}

	@Test
	public void criteriaLeftJoinOfCorrelatedRoot(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var cb = session.getCriteriaBuilder();
			final var query = cb.createQuery( Integer.class );
			final var parent = query.from( Parent.class );
			final var subquery = query.subquery( Integer.class );
			final var item = subquery.correlate( parent ).join( "items", JoinType.LEFT );
			item.on( cb.equal( item.get( "name" ), "x" ) );
			subquery.select( cb.literal( 1 ) ).where( item.get( "id" ).isNull() );
			query.select( parent.get( "id" ) ).where( cb.exists( subquery ) ).orderBy( cb.asc( parent.get( "id" ) ) );
			// the second and third parents have no item named x
			assertEquals( List.of( 2, 3 ), session.createQuery( query ).getResultList() );
		} );
	}

	/**
	 * The query must return the expected results, and so must the HQL rendered
	 * for it, which must itself be rendered the same way.
	 */
	private static void assertRendered(Session session, List<Integer> expected, String hql) {
		assertEquals( expected, session.createSelectionQuery( hql, Integer.class ).getResultList() );
		final String rendered = render( session, hql );
		assertEquals( expected, session.createSelectionQuery( rendered, Integer.class ).getResultList(), rendered );
		assertEquals( rendered, render( session, rendered ) );
	}

	private static String render(Session session, String hql) {
		return session.createSelectionQuery( hql, Integer.class )
				.unwrap( SqmStatementAccess.class ).getSqmStatement().toHqlString();
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		Integer id;
		String name;
		@OneToMany(mappedBy = "parent")
		List<Item> items = new ArrayList<>();
		@OneToMany(mappedBy = "owner")
		List<Animal> animals = new ArrayList<>();
		@ManyToMany
		List<Tag> tags = new ArrayList<>();

		public Parent() {
		}

		public Parent(Integer id, String name) {
			this.id = id;
			this.name = name;
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

	@Entity(name = "Animal")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Animal {
		@Id
		Integer id;
		@ManyToOne
		Parent owner;

		public Animal() {
		}

		public Animal(Integer id, Parent owner) {
			this.id = id;
			this.owner = owner;
		}
	}

	@Entity(name = "Dog")
	public static class Dog extends Animal {
		String tricks;

		public Dog() {
		}

		public Dog(Integer id, Parent owner, String tricks) {
			super( id, owner );
			this.tricks = tricks;
		}
	}
}
