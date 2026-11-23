package org.hibernate.orm.test.collection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.StaleStateException;
import org.hibernate.WrongClassException;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.OrderColumn;

import static jakarta.persistence.InheritanceType.TABLE_PER_CLASS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The order column or map key column of an inverse one-to-many collection whose element is a
 * {@code TABLE_PER_CLASS} hierarchy is written to the table of the concrete subclass of each element.
 * <p>
 * Both {@code ActionQueue} implementations write the index column from the unowned side (see
 * HHH-5732 and HHH-18830), so both are exercised here.
 */
@JiraKey("HHH-21239")
public abstract class AbstractInverseIndexedTablePerClassTest {

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testInitialWrite(SessionFactoryScope scope) {
		persistCrate( scope );

		scope.inTransaction( session -> {
			assertThat( positions( session, "ThingA" ) ).containsExactly( 0, 2 );
			assertThat( positions( session, "ThingB" ) ).containsExactly( 1 );
		} );

		assertLoadsAs( scope, 1, 2, 3 );
	}

	@Test
	public void testReorder(SessionFactoryScope scope) {
		persistCrate( scope );

		scope.inTransaction( session -> {
			final Crate crate = session.find( Crate.class, 1 );
			crate.things.add( crate.things.remove( 0 ) );
		} );

		scope.inTransaction( session -> {
			assertThat( positions( session, "ThingA" ) ).containsExactly( 2, 1 );
			assertThat( positions( session, "ThingB" ) ).containsExactly( 0 );
		} );

		assertLoadsAs( scope, 2, 3, 1 );
	}

	@Test
	public void testQueuedAddition(SessionFactoryScope scope) {
		persistCrate( scope );

		scope.inTransaction( session -> {
			final Crate crate = session.find( Crate.class, 1 );
			// the collection is uninitialized, so the addition is a queued operation
			final ThingB appended = new ThingB( 4, crate );
			crate.things.add( appended );
			session.persist( appended );
		} );

		scope.inTransaction( session -> {
			assertThat( positions( session, "ThingA" ) ).containsExactly( 0, 2 );
			assertThat( positions( session, "ThingB" ) ).containsExactly( 1, 3 );
		} );

		assertLoadsAs( scope, 1, 2, 3, 4 );
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
		persistCrate( scope );
		scope.inTransaction( session -> session.persist( new ThingB( 4, null ) ) );
		final Thing detachedProxy =
				detached ? scope.fromTransaction( session -> session.getReference( proxyType, 4 ) ) : null;

		scope.inTransaction( session -> {
			final Crate crate = session.find( Crate.class, 1 );
			// read the collection first, so the addition goes through a collection update
			assertThat( crate.things ).hasSize( 3 );
			final Thing proxy = detached ? detachedProxy : session.getReference( proxyType, 4 );
			assertThat( Hibernate.isInitialized( proxy ) ).isFalse();
			crate.things.add( proxy );
		} );

		scope.inTransaction( session -> {
			assertThat( positions( session, "ThingA" ) ).containsExactly( 0, 2 );
			assertThat( positions( session, "ThingB" ) ).containsExactly( 1, 3 );
		} );
	}

	/**
	 * A proxy obtained through a subtype which the identified row does not belong to must not be
	 * resolved to the row of a sibling subtype.
	 */
	@Test
	public void testAdditionOfProxyOfWrongSubtype(SessionFactoryScope scope) {
		persistCrate( scope );
		scope.inTransaction( session -> session.persist( new ThingB( 4, null ) ) );

		assertThatThrownBy( () -> scope.inTransaction( session -> {
			final Crate crate = session.find( Crate.class, 1 );
			assertThat( crate.things ).hasSize( 3 );
			final Thing proxy = session.getReference( Branch.class, 4 );
			assertThat( Hibernate.isInitialized( proxy ) ).isFalse();
			crate.things.add( proxy );
		} ) ).isInstanceOf( WrongClassException.class );

		scope.inTransaction( session -> assertThat( positions( session, "ThingB" ) ).containsExactly( 1, null ) );
	}

	/**
	 * A proxy obtained through a subtype with no subclasses is written to the table of that subtype.
	 * If its row belongs to a sibling subtype, no row is written, and the sibling row is left alone.
	 * Hibernate may report the update which matched no row, or ignore it.
	 */
	@Test
	public void testAdditionOfProxyOfWrongLeafType(SessionFactoryScope scope) {
		persistCrate( scope );
		scope.inTransaction( session -> session.persist( new ThingB( 4, null ) ) );

		try {
			scope.inTransaction( session -> {
				final Crate crate = session.find( Crate.class, 1 );
				assertThat( crate.things ).hasSize( 3 );
				final Thing proxy = session.getReference( ThingA.class, 4 );
				assertThat( Hibernate.isInitialized( proxy ) ).isFalse();
				crate.things.add( proxy );
			} );
		}
		catch (OptimisticLockException e) {
			// the update matched no row
			assertThat( e ).hasCauseInstanceOf( StaleStateException.class );
		}

		scope.inTransaction( session -> {
			assertThat( positions( session, "ThingA" ) ).containsExactly( 0, 2 );
			assertThat( positions( session, "ThingB" ) ).containsExactly( 1, null );
		} );

		assertLoadsAs( scope, 1, 2, 3 );
	}

	@Test
	public void testMapKeyWrite(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Crate crate = new Crate();
			final ThingA left = new ThingA( 1, crate );
			final ThingB right = new ThingB( 2, crate );
			crate.thingsBySlot.put( "left", left );
			crate.thingsBySlot.put( "right", right );
			session.persist( crate );
			session.persist( left );
			session.persist( right );
		} );

		scope.inTransaction( session -> {
			assertThat( slots( session, "ThingA" ) ).containsExactly( "left" );
			assertThat( slots( session, "ThingB" ) ).containsExactly( "right" );
		} );

		scope.inTransaction( session -> {
			final Map<String, Thing> thingsBySlot = session.find( Crate.class, 1 ).thingsBySlot;
			assertThat( thingsBySlot ).hasSize( 2 );
			assertThat( thingsBySlot.get( "left" ).id ).isEqualTo( 1 );
			assertThat( thingsBySlot.get( "right" ).id ).isEqualTo( 2 );
		} );
	}

	/**
	 * Persists a crate whose list holds the things 1 (A), 2 (B) and 3 (A), in that order.
	 */
	private static void persistCrate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Crate crate = new Crate();
			final List<Thing> things =
					List.of( new ThingA( 1, crate ), new ThingB( 2, crate ), new ThingA( 3, crate ) );
			crate.things.addAll( things );
			session.persist( crate );
			things.forEach( session::persist );
		} );
	}

	private static void assertLoadsAs(SessionFactoryScope scope, Integer... thingIds) {
		scope.inTransaction( session ->
				assertThat( session.find( Crate.class, 1 ).things )
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

	private static List<String> slots(SessionImplementor session, String table) {
		return session.createNativeQuery( "select slot from " + table + " order by id", String.class )
				.getResultList();
	}

	@Entity(name = "Crate")
	public static class Crate {
		@Id
		int id = 1;

		@OneToMany(mappedBy = "crate")
		@OrderColumn(name = "pos")
		List<Thing> things = new ArrayList<>();

		@OneToMany(mappedBy = "crate")
		@MapKeyColumn(name = "slot", nullable = true)
		Map<String, Thing> thingsBySlot = new HashMap<>();
	}

	/**
	 * The element type of the collections is not the root of its hierarchy.
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
		@ManyToOne
		Crate crate;

		Thing() {
		}

		Thing(int id, Crate crate) {
			super( id );
			this.crate = crate;
		}
	}

	@Entity(name = "ThingA")
	public static class ThingA extends Thing {
		ThingA() {
		}

		ThingA(int id, Crate crate) {
			super( id, crate );
		}
	}

	@Entity(name = "ThingB")
	public static class ThingB extends Thing {
		ThingB() {
		}

		ThingB(int id, Crate crate) {
			super( id, crate );
		}
	}

	@Entity(name = "Branch")
	public static abstract class Branch extends Thing {
		Branch() {
		}

		Branch(int id, Crate crate) {
			super( id, crate );
		}
	}

	@Entity(name = "Twig")
	public static class Twig extends Branch {
		Twig() {
		}

		Twig(int id, Crate crate) {
			super( id, crate );
		}
	}
}
