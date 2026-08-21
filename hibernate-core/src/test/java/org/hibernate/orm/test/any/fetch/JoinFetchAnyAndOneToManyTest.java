/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.fetch;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hibernate.Hibernate.isInitialized;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that {@code resolveFromPreviousRow} works correctly for join-fetched
 * {@link Any @Any} associations. When an owner entity has both a join-fetched
 * {@code @Any} and a join-fetched collection, the result set contains multiple
 * rows for the same owner. On the second and subsequent rows, the owner
 * initializer reuses the previous row and calls {@code resolveFromPreviousRow}
 * on the {@code @Any} initializer rather than re-resolving it from scratch.
 */
@SessionFactory(useCollectingStatementInspector = true)
@DomainModel(annotatedClasses = {
		JoinFetchAnyAndOneToManyTest.Owner.class,
		JoinFetchAnyAndOneToManyTest.Item.class,
		JoinFetchAnyAndOneToManyTest.SomeThing.class,
		JoinFetchAnyAndOneToManyTest.SomeOtherThing.class})
class JoinFetchAnyAndOneToManyTest {

	@BeforeEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testJoinFetchAnyWithCollection(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			var owner = new Owner();
			var child = new SomeThing();
			child.description = "Some thing";
			owner.child = child;
			s.persist( child );
			for ( int i = 1; i <= 3; i++ ) {
				var item = new Item();
				item.name = "item-" + i;
				item.owner = owner;
				owner.items.add( item );
			}
			s.persist( owner );
		} );
		final var statementInspector = scope.getCollectingStatementInspector();

		statementInspector.clear();
		scope.inTransaction( s -> {
			var owners = s.createQuery(
					"from Owner o join fetch o.child join fetch o.items",
					Owner.class
			).getResultList();
			assertEquals( 1, owners.size() );
			var owner = owners.get( 0 );
			assertTrue( isInitialized( owner.child ) );
			assertTrue( isInitialized( owner.items ) );
			assertInstanceOf( SomeThing.class, owner.child );
			assertEquals( "Some thing", ((SomeThing) owner.child).description );
			assertEquals( 3, owner.items.size() );
			assertEquals( 1, statementInspector.getSqlQueries().size() );
		} );
	}

	@Test
	void testJoinFetchPolymorphicAnyWithCollection(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			var owner1 = new Owner();
			var child1 = new SomeThing();
			child1.description = "Some thing";
			owner1.child = child1;
			s.persist( child1 );
			for ( int i = 1; i <= 2; i++ ) {
				var item = new Item();
				item.name = "owner1-item-" + i;
				item.owner = owner1;
				owner1.items.add( item );
			}
			s.persist( owner1 );

			var owner2 = new Owner();
			var child2 = new SomeOtherThing();
			owner2.child = child2;
			s.persist( child2 );
			for ( int i = 1; i <= 2; i++ ) {
				var item = new Item();
				item.name = "owner2-item-" + i;
				item.owner = owner2;
				owner2.items.add( item );
			}
			s.persist( owner2 );
		} );
		final var statementInspector = scope.getCollectingStatementInspector();

		statementInspector.clear();
		scope.inTransaction( s -> {
			var owners = s.createQuery(
					"from Owner o join fetch o.child join fetch o.items order by o.id",
					Owner.class
			).getResultList();
			assertEquals( 2, owners.size() );
			var owner1 = owners.get( 0 );
			var owner2 = owners.get( 1 );
			assertTrue( isInitialized( owner1.child ) );
			assertTrue( isInitialized( owner1.items ) );
			assertInstanceOf( SomeThing.class, owner1.child );
			assertEquals( "Some thing", ((SomeThing) owner1.child).description );
			assertEquals( 2, owner1.items.size() );
			assertTrue( isInitialized( owner2.child ) );
			assertTrue( isInitialized( owner2.items ) );
			assertInstanceOf( SomeOtherThing.class, owner2.child );
			assertEquals( 2, owner2.items.size() );
			assertEquals( 1, statementInspector.getSqlQueries().size() );
		} );
	}

	@Entity(name = "Owner")
	static class Owner {
		@Id
		@GeneratedValue
		Long id;

		@Any(fetch = FetchType.LAZY)
		@AnyKeyJavaClass(Long.class)
		@JoinColumn(name = "CHILD_ID")
		@Column(name = "CHILD_TYPE")
		@AnyDiscriminatorValue(
				discriminator = "SomeThing",
				entity = SomeThing.class)
		@AnyDiscriminatorValue(
				discriminator = "SomeOtherThing",
				entity = SomeOtherThing.class)
		Thing child;

		@OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
		List<Item> items = new ArrayList<>();
	}

	@Entity(name = "Item")
	static class Item {
		@Id
		@GeneratedValue
		Long id;
		String name;
		@ManyToOne
		Owner owner;
	}

	static class Thing {
	}

	@Entity(name = "SomeThing")
	static class SomeThing extends Thing {
		@Id
		@GeneratedValue
		Long id;
		String description;
	}

	@Entity(name = "SomeOtherThing")
	static class SomeOtherThing extends Thing {
		@Id
		@GeneratedValue
		Long id;
	}
}
