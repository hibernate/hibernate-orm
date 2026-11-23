package org.hibernate.orm.test.collection;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import static jakarta.persistence.InheritanceType.TABLE_PER_CLASS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rows of an owned one-to-many list whose element is a {@code TABLE_PER_CLASS} hierarchy are
 * written to the table of the concrete subclass of each element, including when the element type
 * of the collection is not the root of its hierarchy.
 */
@JiraKey("HHH-21239")
public abstract class AbstractOwnedListTablePerClassTest {

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testInsert(SessionFactoryScope scope) {
		persistBox( scope );

		scope.inTransaction( session -> {
			assertThat( positions( session, "ThingA" ) ).containsExactly( 0, 2 );
			assertThat( positions( session, "ThingB" ) ).containsExactly( 1 );
		} );

		assertLoadsAs( scope, 1, 2, 3 );
	}

	@Test
	public void testRemoveElement(SessionFactoryScope scope) {
		persistBox( scope );

		scope.inTransaction( session -> session.find( Box.class, 1 ).things.remove( 1 ) );

		scope.inTransaction( session -> {
			assertThat( positions( session, "ThingA" ) ).containsExactly( 0, 1 );
			assertThat( positions( session, "ThingB" ) ).containsExactly( (Integer) null );
		} );

		assertLoadsAs( scope, 1, 3 );
	}

	@Test
	public void testReorder(SessionFactoryScope scope) {
		persistBox( scope );

		scope.inTransaction( session -> {
			final Box box = session.find( Box.class, 1 );
			box.things.add( box.things.remove( 0 ) );
		} );

		scope.inTransaction( session -> {
			assertThat( positions( session, "ThingA" ) ).containsExactly( 2, 1 );
			assertThat( positions( session, "ThingB" ) ).containsExactly( 0 );
		} );

		assertLoadsAs( scope, 2, 3, 1 );
	}

	/**
	 * An element which is an uninitialized proxy has no entity entry from which to resolve its concrete subclass.
	 */
	@Test
	public void testAdditionOfProxy(SessionFactoryScope scope) {
		assertAdditionOfProxy( scope, ThingB.class, false );
	}

	/**
	 * A proxy obtained through a supertype does not know its concrete subclass.
	 */
	@Test
	public void testAdditionOfSupertypeProxy(SessionFactoryScope scope) {
		assertAdditionOfProxy( scope, Thing.class, false );
	}

	/**
	 * A proxy obtained from another session cannot be initialized by the session which flushes the collection.
	 */
	@Test
	public void testAdditionOfDetachedSupertypeProxy(SessionFactoryScope scope) {
		assertAdditionOfProxy( scope, Thing.class, true );
	}

	private static void assertAdditionOfProxy(
			SessionFactoryScope scope,
			Class<? extends Thing> proxyType,
			boolean detached) {
		persistBox( scope );
		scope.inTransaction( session -> session.persist( new ThingB( 5 ) ) );
		final Thing detachedProxy =
				detached ? scope.fromTransaction( session -> session.getReference( proxyType, 5 ) ) : null;

		scope.inTransaction( session -> {
			final Box box = session.find( Box.class, 1 );
			// read the collection first, so the addition goes through a collection update
			assertThat( box.things ).hasSize( 3 );
			final Thing proxy = detached ? detachedProxy : session.getReference( proxyType, 5 );
			assertThat( Hibernate.isInitialized( proxy ) ).isFalse();
			box.things.add( proxy );
		} );

		scope.inTransaction( session -> {
			assertThat( positions( session, "ThingA" ) ).containsExactly( 0, 2 );
			assertThat( positions( session, "ThingB" ) ).containsExactly( 1, 3 );
		} );

		assertLoadsAs( scope, 1, 2, 3, 5 );
	}

	/**
	 * A proxy which exists before the collection is read is the element held by the collection.
	 */
	@Test
	public void testRemoveProxyElement(SessionFactoryScope scope) {
		persistBox( scope );

		scope.inTransaction( session -> {
			final Thing proxy = session.getReference( ThingB.class, 2 );
			final Box box = session.find( Box.class, 1 );
			assertThat( box.things.get( 1 ) ).isSameAs( proxy );
			box.things.remove( 1 );
		} );

		assertLoadsAs( scope, 1, 3 );
	}

	@Test
	public void testReorderWithProxyElement(SessionFactoryScope scope) {
		persistBox( scope );

		scope.inTransaction( session -> {
			final Thing proxy = session.getReference( ThingB.class, 2 );
			final Box box = session.find( Box.class, 1 );
			assertThat( box.things.get( 1 ) ).isSameAs( proxy );
			box.things.add( box.things.remove( 0 ) );
		} );

		assertLoadsAs( scope, 2, 3, 1 );
	}

	/**
	 * Persists a box holding the things 1 (A), 2 (B) and 3 (A), in that order.
	 */
	private static void persistBox(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Box box = new Box();
			final List<Thing> things = List.of( new ThingA( 1 ), new ThingB( 2 ), new ThingA( 3 ) );
			box.things.addAll( things );
			things.forEach( session::persist );
			session.persist( box );
		} );
	}

	private static void assertLoadsAs(SessionFactoryScope scope, Integer... thingIds) {
		scope.inTransaction( session ->
				assertThat( session.find( Box.class, 1 ).things )
						.extracting( thing -> thing.id )
						.containsExactly( thingIds ) );
	}

	/**
	 * The positions held in the table of the given concrete subclass, ordered by thing id.
	 */
	private static List<Integer> positions(SessionImplementor session, String table) {
		return session.createNativeQuery( "select pos from " + table + " order by id", Integer.class )
				.getResultList();
	}

	@Entity(name = "Box")
	public static class Box {
		@Id
		int id = 1;

		@OneToMany
		@JoinColumn(name = "box_id")
		@OrderColumn(name = "pos")
		List<Thing> things = new ArrayList<>();
	}

	/**
	 * The element type of the collection is not the root of its hierarchy.
	 */
	@Entity(name = "Item")
	@Inheritance(strategy = TABLE_PER_CLASS)
	public static abstract class Item {
		@Id
		int id;

		Item() {
		}

		Item(int id) {
			this.id = id;
		}
	}

	@Entity(name = "Thing")
	public static abstract class Thing extends Item {
		Thing() {
		}

		Thing(int id) {
			super( id );
		}
	}

	@Entity(name = "ThingA")
	public static class ThingA extends Thing {
		ThingA() {
		}

		ThingA(int id) {
			super( id );
		}
	}

	@Entity(name = "ThingB")
	public static class ThingB extends Thing {
		ThingB() {
		}

		ThingB(int id) {
			super( id );
		}
	}
}
