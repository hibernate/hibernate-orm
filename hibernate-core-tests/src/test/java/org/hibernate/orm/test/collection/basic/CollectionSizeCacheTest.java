package org.hibernate.orm.test.collection.basic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = { CollectionSizeCacheTest.Owner.class, CollectionSizeCacheTest.Child.class })
@SessionFactory
@JiraKey("HHH-21222")
class CollectionSizeCacheTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owner = new Owner();
			owner.id = 1;
			owner.tags.add( "a" );
			session.persist( owner );
			final var child = new Child();
			child.id = 1;
			child.owner = owner;
			session.persist( child );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void initializedCollectionWithUnflushedChanges(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owner = session.find( Owner.class, 1 );
			Hibernate.initialize( owner.tags );
			owner.tags.add( "b" );
			assertEquals( 2, Hibernate.size( owner.tags ) );
			owner.tags.clear();
			assertTrue( Hibernate.isEmpty( owner.tags ) );
		} );
	}

	@Test
	void cachedSizeAfterInitialization(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owner = session.find( Owner.class, 1 );
			assertEquals( 1, Hibernate.size( owner.tags ) );
			owner.tags.add( "b" );
			assertTrue( Hibernate.isInitialized( owner.tags ) );
			assertEquals( 2, Hibernate.size( owner.tags ) );
		} );
	}

	@Test
	void cachedSizeWithQueuedOperation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owner = session.find( Owner.class, 1 );
			assertEquals( 1, Hibernate.size( owner.children ) );
			final var child = new Child();
			child.id = 2;
			child.owner = owner;
			session.persist( child );
			// queued, since the collection is inverse
			owner.children.add( child );
			assertFalse( Hibernate.isInitialized( owner.children ) );
			assertEquals( 2, Hibernate.size( owner.children ) );
			assertFalse( Hibernate.isInitialized( owner.children ) );
		} );
	}

	@Test
	void cachedSizeOfDetachedCollection(SessionFactoryScope scope) {
		final var owner = scope.fromSession( session -> {
			final var found = session.find( Owner.class, 1 );
			assertEquals( 1, Hibernate.size( found.tags ) );
			return found;
		} );
		// the size counted in the session is still available after it closes
		assertEquals( 1, Hibernate.size( owner.tags ) );
		assertEquals( 1, owner.tags.size() );
		assertFalse( Hibernate.isInitialized( owner.tags ) );
	}

	@Entity(name = "Owner")
	static class Owner {
		@Id
		Integer id;
		@ElementCollection
		Set<String> tags = new HashSet<>();
		@OneToMany(mappedBy = "owner")
		List<Child> children = new ArrayList<>();
	}

	@Entity(name = "Child")
	static class Child {
		@Id
		Integer id;
		@ManyToOne
		Owner owner;
	}
}
