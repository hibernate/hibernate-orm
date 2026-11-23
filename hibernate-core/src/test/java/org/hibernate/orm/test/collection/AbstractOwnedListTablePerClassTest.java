package org.hibernate.orm.test.collection;

import java.util.ArrayList;
import java.util.List;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.OrderColumn;

import static jakarta.persistence.InheritanceType.TABLE_PER_CLASS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The rows of an owned one-to-many list whose element is a {@code TABLE_PER_CLASS} hierarchy are
 * written to the table of the concrete subclass of each element, including when the element type
 * of the collection is not the root of its hierarchy. Removing every row of the collection touches
 * each table which holds a row of the element type, and no other.
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

	@Test
	public void testClear(SessionFactoryScope scope) {
		persistBox( scope );

		scope.inTransaction( session -> session.find( Box.class, 1 ).things.clear() );

		scope.inTransaction( session -> {
			assertThat( positions( session, "ThingA" ) ).containsExactly( null, null );
			assertThat( positions( session, "ThingB" ) ).containsExactly( (Integer) null );
		} );

		assertLoadsAs( scope );
	}

	@Test
	public void testRemoveOwner(SessionFactoryScope scope) {
		persistBox( scope );

		scope.inTransaction( session -> session.remove( session.find( Box.class, 1 ) ) );

		scope.inTransaction( session -> {
			assertThat( session.find( Box.class, 1 ) ).isNull();
			assertThat( positions( session, "ThingA" ) ).containsExactly( null, null );
			assertThat( positions( session, "ThingB" ) ).containsExactly( (Integer) null );
		} );
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
	 * A proxy obtained through a subtype which the identified row does not belong to must not be
	 * resolved to the row of a sibling subtype.
	 */
	@Test
	public void testAdditionOfProxyOfWrongSubtype(SessionFactoryScope scope) {
		persistBox( scope );
		scope.inTransaction( session -> session.persist( new ThingB( 5 ) ) );

		assertThatThrownBy( () -> scope.inTransaction( session -> {
			final Box box = session.find( Box.class, 1 );
			assertThat( box.things ).hasSize( 3 );
			final Thing proxy = session.getReference( Branch.class, 5 );
			assertThat( Hibernate.isInitialized( proxy ) ).isFalse();
			box.things.add( proxy );
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
		persistBox( scope );
		scope.inTransaction( session -> session.persist( new ThingB( 5 ) ) );

		try {
			scope.inTransaction( session -> {
				final Box box = session.find( Box.class, 1 );
				assertThat( box.things ).hasSize( 3 );
				final Thing proxy = session.getReference( ThingA.class, 5 );
				assertThat( Hibernate.isInitialized( proxy ) ).isFalse();
				box.things.add( proxy );
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
	 * The element type of the collection is a concrete class with a table of its own.
	 */
	@Test
	public void testClearConcreteElementType(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Shelf shelf = new Shelf();
			final List<Tool> tools = List.of( new Tool( 1 ), new PowerTool( 2 ) );
			shelf.tools.addAll( tools );
			tools.forEach( session::persist );
			session.persist( shelf );
		} );

		scope.inTransaction( session -> session.find( Shelf.class, 1 ).tools.clear() );

		scope.inTransaction( session -> {
			assertThat( positions( session, "Tool" ) ).containsExactly( (Integer) null );
			assertThat( positions( session, "PowerTool" ) ).containsExactly( (Integer) null );
			assertThat( session.find( Shelf.class, 1 ).tools ).isEmpty();
		} );
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
			session.persist( new Label( 4 ) );
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

	@Entity(name = "Branch")
	public static abstract class Branch extends Thing {
		Branch() {
		}

		Branch(int id) {
			super( id );
		}
	}

	@Entity(name = "Twig")
	public static class Twig extends Branch {
		Twig() {
		}

		Twig(int id) {
			super( id );
		}
	}

	/**
	 * A subclass which is not an element of the collection, and whose table has no foreign key to the box.
	 */
	@Entity(name = "Label")
	public static class Label extends Item {
		Label() {
		}

		Label(int id) {
			super( id );
		}
	}

	@Entity(name = "Shelf")
	public static class Shelf {
		@Id
		int id = 1;

		@OneToMany
		@JoinColumn(name = "shelf_id")
		@OrderColumn(name = "pos")
		List<Tool> tools = new ArrayList<>();
	}

	@Entity(name = "Tool")
	@Inheritance(strategy = TABLE_PER_CLASS)
	public static class Tool {
		@Id
		int id;

		Tool() {
		}

		Tool(int id) {
			this.id = id;
		}
	}

	@Entity(name = "PowerTool")
	public static class PowerTool extends Tool {
		PowerTool() {
		}

		PowerTool(int id) {
			super( id );
		}
	}
}
