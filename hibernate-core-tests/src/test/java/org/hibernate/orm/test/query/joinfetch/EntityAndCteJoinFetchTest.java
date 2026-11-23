package org.hibernate.orm.test.query.joinfetch;

import org.hibernate.query.SemanticException;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Only a join on an association may be a fetch join: {@code fetch}
 * is meaningless on a join whose target is an entity name or a CTE.
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		EntityAndCteJoinFetchTest.Owner.class,
		EntityAndCteJoinFetchTest.Item.class
} )
public class EntityAndCteJoinFetchTest {
	private static final String CTE = "with owners as (select o.id id from Owner o) ";

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Owner owner = new Owner( 1L );
			session.persist( owner );
			session.persist( new Item( 1L, owner ) );
			// an item with no owner
			session.persist( new Item( 2L, null ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testFetchOnEntityJoin(SessionFactoryScope scope) {
		assertRejected( scope,
				"select i from Item i join fetch Owner o on i.owner = o",
				"Fetch join target 'Owner' is an entity" );
		assertRejected( scope,
				"select i from Item i left join fetch Owner o on i.owner = o",
				"Fetch join target 'Owner' is an entity" );
		assertRejected( scope,
				"select i from Item i join fetch " + Owner.class.getName() + " o on i.owner = o",
				"Fetch join target '" + Owner.class.getName() + "' is an entity" );
	}

	@Test
	public void testFetchOnCteJoin(SessionFactoryScope scope) {
		assertRejected( scope,
				CTE + "select i from Item i join fetch owners o on i.owner.id = o.id",
				"Fetch join target 'owners' is a CTE" );
		assertRejected( scope,
				CTE + "select i from Item i left join fetch owners o on i.owner.id = o.id",
				"Fetch join target 'owners' is a CTE" );
	}

	@Test
	public void testEntityJoinWithoutFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select i from Item i join Owner o on i.owner = o",
					Item.class
			).getResultList() ).hasSize( 1 );
			assertThat( session.createQuery(
					"select i from Item i left join Owner o on i.owner = o",
					Item.class
			).getResultList() ).hasSize( 2 );
		} );
	}

	@Test
	public void testCteJoinWithoutFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					CTE + "select i from Item i join owners o on i.owner.id = o.id",
					Item.class
			).getResultList() ).hasSize( 1 );
			assertThat( session.createQuery(
					CTE + "select i from Item i left join owners o on i.owner.id = o.id",
					Item.class
			).getResultList() ).hasSize( 2 );
		} );
	}

	private static void assertRejected(SessionFactoryScope scope, String hql, String message) {
		scope.inSession( session -> assertThatThrownBy( () -> session.createQuery( hql, Item.class ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasCauseInstanceOf( SemanticException.class )
				.hasMessageContaining( message )
				.hasMessageContaining( hql ) );
	}

	@Entity( name = "Owner" )
	public static class Owner {
		@Id
		Long id;

		Owner() {
		}

		Owner(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "Item" )
	public static class Item {
		@Id
		Long id;
		@ManyToOne( fetch = FetchType.LAZY )
		Owner owner;

		Item() {
		}

		Item(Long id, Owner owner) {
			this.id = id;
			this.owner = owner;
		}
	}
}
