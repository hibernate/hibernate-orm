package org.hibernate.orm.test.collection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;

import static jakarta.persistence.InheritanceType.TABLE_PER_CLASS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rows of an owned one-to-many map whose element is a {@code TABLE_PER_CLASS} hierarchy are
 * written to the table of the concrete subclass of each element.
 */
@JiraKey("HHH-21239")
public abstract class AbstractOwnedMapTablePerClassTest {

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testInsert(SessionFactoryScope scope) {
		persistBox( scope );

		scope.inTransaction( session -> {
			assertThat( slots( session, "ThingA" ) ).containsExactly( "left", null );
			assertThat( slots( session, "ThingB" ) ).containsExactly( "right" );
		} );

		assertLoadsAs( scope, Map.of( "left", 1, "right", 2 ) );
	}

	@Test
	public void testRemoveEntry(SessionFactoryScope scope) {
		persistBox( scope );

		scope.inTransaction( session -> session.find( Box.class, 1 ).things.remove( "right" ) );

		scope.inTransaction( session -> {
			assertThat( slots( session, "ThingA" ) ).containsExactly( "left", null );
			assertThat( slots( session, "ThingB" ) ).containsExactly( (String) null );
		} );

		assertLoadsAs( scope, Map.of( "left", 1 ) );
	}

	@Test
	public void testReplaceValue(SessionFactoryScope scope) {
		persistBox( scope );

		scope.inTransaction( session -> {
			final Box box = session.find( Box.class, 1 );
			box.things.put( "right", session.find( ThingA.class, 3 ) );
		} );

		scope.inTransaction( session -> {
			assertThat( slots( session, "ThingA" ) ).containsExactly( "left", "right" );
			assertThat( slots( session, "ThingB" ) ).containsExactly( (String) null );
		} );

		assertLoadsAs( scope, Map.of( "left", 1, "right", 3 ) );
	}

	@Test
	public void testMoveValueToAnotherKey(SessionFactoryScope scope) {
		persistBox( scope );

		scope.inTransaction( session -> {
			final Box box = session.find( Box.class, 1 );
			box.things.put( "middle", box.things.remove( "right" ) );
		} );

		scope.inTransaction( session -> {
			assertThat( slots( session, "ThingA" ) ).containsExactly( "left", null );
			assertThat( slots( session, "ThingB" ) ).containsExactly( "middle" );
		} );

		assertLoadsAs( scope, Map.of( "left", 1, "middle", 2 ) );
	}

	@Test
	public void testAdditionOfProxy(SessionFactoryScope scope) {
		persistBox( scope );

		scope.inTransaction( session -> {
			final Box box = session.find( Box.class, 1 );
			// read the collection first, so the addition goes through a collection update
			assertThat( box.things ).hasSize( 2 );
			final Thing proxy = session.getReference( Thing.class, 3 );
			assertThat( Hibernate.isInitialized( proxy ) ).isFalse();
			box.things.put( "middle", proxy );
		} );

		scope.inTransaction( session -> {
			assertThat( slots( session, "ThingA" ) ).containsExactly( "left", "middle" );
			assertThat( slots( session, "ThingB" ) ).containsExactly( "right" );
		} );

		assertLoadsAs( scope, Map.of( "left", 1, "middle", 3, "right", 2 ) );
	}

	@Test
	public void testClear(SessionFactoryScope scope) {
		persistBox( scope );

		scope.inTransaction( session -> session.find( Box.class, 1 ).things.clear() );

		scope.inTransaction( session -> {
			assertThat( slots( session, "ThingA" ) ).containsExactly( null, null );
			assertThat( slots( session, "ThingB" ) ).containsExactly( (String) null );
		} );

		assertLoadsAs( scope, Map.of() );
	}

	/**
	 * Persists a box holding thing 1 (A) on the left and thing 2 (B) on the right, and thing 3 (A) which
	 * is not in the box.
	 */
	private static void persistBox(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Box box = new Box();
			final List<Thing> things = List.of( new ThingA( 1 ), new ThingB( 2 ), new ThingA( 3 ) );
			box.things.put( "left", things.get( 0 ) );
			box.things.put( "right", things.get( 1 ) );
			things.forEach( session::persist );
			session.persist( box );
		} );
	}

	private static void assertLoadsAs(SessionFactoryScope scope, Map<String, Integer> thingIdsBySlot) {
		scope.inTransaction( session -> {
			final Map<String, Integer> loaded = new HashMap<>();
			session.find( Box.class, 1 ).things.forEach( (slot, thing) -> loaded.put( slot, thing.id ) );
			assertThat( loaded ).isEqualTo( thingIdsBySlot );
		} );
	}

	/**
	 * The slots held in the table of the given concrete subclass, ordered by thing id.
	 */
	private static List<String> slots(SessionImplementor session, String table) {
		return session.createNativeQuery( "select slot from " + table + " order by id", String.class )
				.getResultList();
	}

	@Entity(name = "Box")
	public static class Box {
		@Id
		int id = 1;

		@OneToMany
		@JoinColumn(name = "box_id")
		@MapKeyColumn(name = "slot", nullable = true)
		Map<String, Thing> things = new HashMap<>();
	}

	/**
	 * The element type of the collection is not the root of its hierarchy, and its superclass has
	 * a table of its own, with no foreign key to the box.
	 */
	@Entity(name = "Item")
	@Inheritance(strategy = TABLE_PER_CLASS)
	public static class Item {
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
