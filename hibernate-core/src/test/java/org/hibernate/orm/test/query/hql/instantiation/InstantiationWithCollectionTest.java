/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.instantiation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.collection.spi.PersistentCollection;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SessionFactory
@DomainModel(annotatedClasses = {InstantiationWithCollectionTest.Entity.class,
		InstantiationWithCollectionTest.ChildEntity.class})
public class InstantiationWithCollectionTest {

	@Test
	void testElements(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();

		Entity entity = new Entity("hello", "world");
		scope.inTransaction(s -> s.persist(entity));

		List<Object[]> tuples =
				scope.fromSession(s -> s.createSelectionQuery("select id, names from Entity", Object[].class).getResultList());
		assertEquals(1, tuples.size());
		assertNotNull(tuples.get(0)[0]);
		assertThat( assertDetachedSet( tuples.get(0)[1] ) ).containsExactlyInAnyOrder("hello", "world");

		List<Object[]> tuples2 =
				scope.fromSession(s -> s.createSelectionQuery("select id, element(names) from Entity", Object[].class).getResultList());
		assertEquals(2, tuples2.size());
		tuples2.forEach(tuple -> assertNotNull(tuple[0]));
		tuples2.forEach(tuple -> assertNotNull(tuple[1]));

		List<EntityNamesCopy> copies =
				scope.fromSession(s -> s.createSelectionQuery("select id, names from Entity", EntityNamesCopy.class).getResultList());
		assertEquals(1, copies.size());
		copies.forEach(Assertions::assertNotNull);
		copies.forEach(copy -> assertNotNull(copy.id));
		copies.forEach(copy -> assertThat( assertDetachedSet( copy.names ) ).containsExactlyInAnyOrder("hello", "world"));

		List<EntityCopy> copies2 =
				scope.fromSession(s -> s.createSelectionQuery("select id, element(names) from Entity", EntityCopy.class).getResultList());
		assertEquals(2, copies2.size());
		copies2.forEach(Assertions::assertNotNull);
		copies2.forEach(copy -> assertNotNull(copy.id));
		copies2.forEach(copy -> assertNotNull(copy.name));

		List<EntityNamesCopy> newCopies =
				scope.fromSession(s -> s.createSelectionQuery("select new EntityNamesCopy(id, names) from Entity", EntityNamesCopy.class).getResultList());
		assertEquals(1, newCopies.size());
		newCopies.forEach(Assertions::assertNotNull);
		newCopies.forEach(copy -> assertNotNull(copy.id));
		newCopies.forEach(copy -> assertThat( assertDetachedSet( copy.names ) ).containsExactlyInAnyOrder("hello", "world"));

		List<EntityCopy> newCopies2 =
				scope.fromSession(s -> s.createSelectionQuery("select new EntityCopy(id, element(names)) from Entity", EntityCopy.class).getResultList());
		assertEquals(2, newCopies2.size());
		newCopies2.forEach(Assertions::assertNotNull);
		newCopies2.forEach(copy -> assertNotNull(copy.id));
		newCopies2.forEach(copy -> assertNotNull(copy.name));
	}

	@Test
	void testAssociation(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();

		Entity entity = new Entity("hello", "world");
		entity.children.add(new ChildEntity(entity, "goodbye"));
		entity.children.add(new ChildEntity(entity, "gavin"));
		scope.inTransaction(s -> s.persist(entity));

		List<Object[]> tuples =
				scope.fromSession(s -> s.createSelectionQuery("select id, children from Entity", Object[].class).getResultList());
		assertEquals(1, tuples.size());
		assertNotNull(tuples.get(0)[0]);
		assertThat( InstantiationWithCollectionTest.<ChildEntity>assertDetachedSet( tuples.get(0)[1] ) )
				.extracting( ChildEntity::getName )
				.containsExactlyInAnyOrder("goodbye", "gavin");

		List<Object[]> tuples2 =
				scope.fromSession(s -> s.createSelectionQuery("select id, element(children) from Entity", Object[].class).getResultList());
		assertEquals(2, tuples2.size());
		tuples2.forEach(tuple -> assertNotNull(tuple[0]));
		tuples2.forEach(tuple -> assertNotNull(tuple[1]));

		List<Object[]> tuples3 =
				scope.fromSession(s -> s.createSelectionQuery("select id, element(children).name from Entity", Object[].class).getResultList());
		assertEquals(2, tuples3.size());
		tuples3.forEach(tuple -> assertNotNull(tuple[0]));
		tuples3.forEach(tuple -> assertNotNull(tuple[1]));

		List<EntityCopy> copies =
				scope.fromSession(s -> s.createSelectionQuery("select id, element(children).name from Entity", EntityCopy.class).getResultList());
		assertEquals(2, copies.size());
		copies.forEach(Assertions::assertNotNull);
		copies.forEach(copy -> assertNotNull(copy.id));
		copies.forEach(copy -> assertNotNull(copy.name));

		List<EntityChildrenCopy> collectionCopies =
				scope.fromSession(s -> s.createSelectionQuery("select id, children from Entity", EntityChildrenCopy.class).getResultList());
		assertEquals(1, collectionCopies.size());
		collectionCopies.forEach(Assertions::assertNotNull);
		collectionCopies.forEach(copy -> assertNotNull(copy.id));
		collectionCopies.forEach(copy -> assertThat( InstantiationWithCollectionTest.<ChildEntity>assertDetachedSet( copy.children ) )
				.extracting( ChildEntity::getName )
				.containsExactlyInAnyOrder("goodbye", "gavin"));

		List<EntityCopy> newCopies =
				scope.fromSession(s -> s.createSelectionQuery("select new EntityCopy(id, element(children).name) from Entity", EntityCopy.class).getResultList());
		assertEquals(2, newCopies.size());
		newCopies.forEach(Assertions::assertNotNull);
		newCopies.forEach(copy -> assertNotNull(copy.id));
		newCopies.forEach(copy -> assertNotNull(copy.name));

		List<EntityChildrenCopy> newCollectionCopies =
				scope.fromSession(s -> s.createSelectionQuery("select new EntityChildrenCopy(id, children) from Entity", EntityChildrenCopy.class).getResultList());
		assertEquals(1, newCollectionCopies.size());
		newCollectionCopies.forEach(Assertions::assertNotNull);
		newCollectionCopies.forEach(copy -> assertNotNull(copy.id));
		newCollectionCopies.forEach(copy -> assertThat( InstantiationWithCollectionTest.<ChildEntity>assertDetachedSet( copy.children ) )
				.extracting( ChildEntity::getName )
				.containsExactlyInAnyOrder("goodbye", "gavin"));
	}

	@SuppressWarnings("unchecked")
	private static <T> Set<T> assertDetachedSet(Object collection) {
		assertThat( collection ).isInstanceOf( Set.class );
		assertThat( collection ).isNotInstanceOf( PersistentCollection.class );
		return (Set<T>) collection;
	}

	static class EntityCopy {
		Long id;
		String name;

		public EntityCopy(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	static class EntityNamesCopy {
		Long id;
		Set<String> names;

		public EntityNamesCopy(Long id, Set<String> names) {
			this.id = id;
			this.names = names;
		}
	}

	static class EntityChildrenCopy {
		Long id;
		Set<ChildEntity> children;

		public EntityChildrenCopy(Long id, Set<ChildEntity> children) {
			this.id = id;
			this.children = children;
		}
	}

	@jakarta.persistence.Entity(name="Entity")
	static class Entity {
		@GeneratedValue @Id
		Long id;
		@ElementCollection
		Set<String> names;
		@OneToMany(mappedBy = "parent", cascade = CascadeType.PERSIST)
		Set<ChildEntity> children;

		Entity(String... names) {
			this.names = Set.of(names);
			this.children = new HashSet<>();
		}

		Entity() {
			names = new HashSet<>();
			children = new HashSet<>();
		}
	}

	@jakarta.persistence.Entity(name="ChildEntity")
	static class ChildEntity {
		@GeneratedValue @Id Long id;
		String name;
		@ManyToOne Entity parent;

		ChildEntity(Entity parent, String name) {
			this.parent = parent;
			this.name = name;
		}

		ChildEntity() {
		}

		public String getName() {
			return name;
		}
	}
}
