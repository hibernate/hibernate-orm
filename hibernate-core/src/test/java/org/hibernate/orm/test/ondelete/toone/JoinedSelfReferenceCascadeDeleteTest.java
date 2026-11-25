package org.hibernate.orm.test.ondelete.toone;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.event.spi.EventSource;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.OneToOneType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(annotatedClasses = {
		JoinedSelfReferenceCascadeDeleteTest.Base.class,
		JoinedSelfReferenceCascadeDeleteTest.Child.class,
		JoinedSelfReferenceCascadeDeleteTest.Owner.class
})
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = "hibernate.flush.queue.type", value = "graph"),
		@Setting(name = "hibernate.jdbc.batch_size", value = "5")
})
@RequiresDialect(H2Dialect.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCircularCascadeDeleteCheck.class)
public class JoinedSelfReferenceCascadeDeleteTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void removeEntityReferencingItself(SessionFactoryScope scope) {
		persistSelfReference( scope, false );
		scope.inTransaction( s -> s.remove( s.find( Child.class, 1 ) ) );
		assertBothRowsDeleted( scope );
	}

	@Test
	void removeSelfReferencingOrphanBeforeUpdates(SessionFactoryScope scope) {
		persistSelfReference( scope, true );
		scope.inTransaction( s -> {
			final var owner = s.find( Owner.class, 1 );
			assertNotNull( owner.child );
			final var persister = ( (EventSource) s ).getEntityPersister( null, owner );
			final var associationType = assertInstanceOf( OneToOneType.class, persister.getPropertyTypes()[0] );
			assertEquals( ForeignKeyDirection.TO_PARENT, associationType.getForeignKeyDirection() );
			// Clear the inverse side so the managed owner orphans the previously loaded child on flush.
			owner.child = null;
		} );
		assertBothRowsDeleted( scope );
		scope.inTransaction( s -> assertEquals(
				1L,
				s.createNativeQuery( "select count(*) from JoinedSelfOwner", Long.class ).getSingleResult()
		) );
	}

	@Test
	void removeReferrerWithoutChangingItsDistinctTarget(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final var referrer = new Child();
			referrer.id = 1;
			final var target = new Child();
			target.id = 2;
			s.persist( referrer );
			s.persist( target );
			s.flush();
			referrer.self = target;
			target.self = target;
		} );
		scope.inTransaction( s -> s.remove( s.find( Child.class, 1 ) ) );
		scope.inTransaction( s -> {
			assertEquals( 1L, s.createNativeQuery( "select count(*) from JoinedSelfBase", Long.class ).getSingleResult() );
			assertEquals( 1L, s.createNativeQuery( "select count(*) from JoinedSelfChild", Long.class ).getSingleResult() );
			assertEquals( 2, s.createNativeQuery(
					"select self_id from JoinedSelfBase where id = 2", Integer.class ).getSingleResult() );
		} );
	}

	@Test
	void removeMultipleSelfReferencesWithBatching(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			final var first = new Child();
			first.id = 1;
			final var second = new Child();
			second.id = 2;
			s.persist( first );
			s.persist( second );
			s.flush();
			first.self = first;
			second.self = second;
		} );
		scope.inTransaction( s -> {
			final var first = s.find( Child.class, 1 );
			final var second = s.find( Child.class, 2 );
			s.remove( first );
			s.remove( second );
		} );
		assertBothRowsDeleted( scope );
	}

	private static void persistSelfReference(SessionFactoryScope scope, boolean withOwner) {
		scope.inTransaction( s -> {
			var node = new Child();
			node.id = 1;
			if ( withOwner ) {
				final var owner = new Owner();
				owner.id = 1;
				owner.child = node;
				node.owner = owner;
				s.persist( owner );
			}
			s.persist( node );
			s.flush();
			node.self = node;
		} );
	}

	private static void assertBothRowsDeleted(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			assertEquals( 0L, s.createNativeQuery( "select count(*) from JoinedSelfBase", Long.class ).getSingleResult() );
			assertEquals( 0L, s.createNativeQuery( "select count(*) from JoinedSelfChild", Long.class ).getSingleResult() );
		} );
	}

	@Entity(name = "JoinedSelfBase")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Base {
		@Id
		Integer id;
		@ManyToOne
		@OnDelete(action = OnDeleteAction.CASCADE)
		Child self;
	}

	@Entity(name = "JoinedSelfChild")
	public static class Child extends Base {
		String description;
		@OneToOne
		Owner owner;
	}

	@Entity(name = "JoinedSelfOwner")
	public static class Owner {
		@Id
		Integer id;
		@OneToOne(mappedBy = "owner", orphanRemoval = true)
		Child child;
	}
}
