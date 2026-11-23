package org.hibernate.orm.test.collection.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = InitializedCollectionAccessTest.Owner.class)
@SessionFactory
@JiraKey("HHH-21222")
class InitializedCollectionAccessTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owner = new Owner();
			owner.id = 1;
			owner.tags.add( "a" );
			owner.items.add( "a" );
			owner.props.put( "k", "a" );
			session.persist( owner );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void containsInInitializedSet(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owner = session.find( Owner.class, 1 );
			Hibernate.initialize( owner.tags );
			owner.tags.add( "b" );
			owner.tags.remove( "a" );
			assertTrue( Hibernate.contains( owner.tags, "b" ) );
			assertFalse( Hibernate.contains( owner.tags, "a" ) );
		} );
	}

	@Test
	void getFromInitializedMap(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owner = session.find( Owner.class, 1 );
			Hibernate.initialize( owner.props );
			owner.props.put( "k", "b" );
			owner.props.put( "j", "c" );
			assertEquals( "b", Hibernate.get( owner.props, "k" ) );
			assertEquals( "c", Hibernate.get( owner.props, "j" ) );
			owner.props.remove( "k" );
			assertNull( Hibernate.get( owner.props, "k" ) );
		} );
	}

	@Test
	void getFromInitializedList(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owner = session.find( Owner.class, 1 );
			Hibernate.initialize( owner.items );
			owner.items.set( 0, "b" );
			owner.items.add( "c" );
			assertEquals( "b", Hibernate.get( owner.items, 0 ) );
			assertEquals( "c", Hibernate.get( owner.items, 1 ) );
		} );
	}

	@Test
	void getMissingListIndex(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var items = session.find( Owner.class, 1 ).items;
			assertNull( Hibernate.get( items, 1 ) );
			assertNull( Hibernate.get( items, -1 ) );
			assertFalse( Hibernate.isInitialized( items ) );

			Hibernate.initialize( items );
			assertNull( Hibernate.get( items, 1 ) );
			assertNull( Hibernate.get( items, -1 ) );
		} );
	}

	@Test
	void uninitializedCollectionsAreNotInitialized(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owner = session.find( Owner.class, 1 );
			assertTrue( Hibernate.contains( owner.tags, "a" ) );
			assertEquals( "a", Hibernate.get( owner.props, "k" ) );
			assertEquals( "a", Hibernate.get( owner.items, 0 ) );
			assertFalse( Hibernate.isInitialized( owner.tags ) );
			assertFalse( Hibernate.isInitialized( owner.props ) );
			assertFalse( Hibernate.isInitialized( owner.items ) );
		} );
	}

	@Entity(name = "Owner")
	static class Owner {
		@Id
		Integer id;
		@ElementCollection
		Set<String> tags = new HashSet<>();
		@ElementCollection
		@OrderColumn
		List<String> items = new ArrayList<>();
		@ElementCollection
		Map<String, String> props = new HashMap<>();
	}
}
