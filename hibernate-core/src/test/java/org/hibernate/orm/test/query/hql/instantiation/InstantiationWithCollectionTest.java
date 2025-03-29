/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.instantiation;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
				// this kind of query is currently not documented to work, but it does, and I guess I don't see why it's terrible
				scope.fromSession(s -> s.createSelectionQuery("select id, names from Entity", Object[].class).getResultList());
		assertEquals(2, tuples.size());
		tuples.forEach(tuple -> assertNotNull(tuple[0]));
		tuples.forEach(tuple -> assertNotNull(tuple[1]));

		List<Object[]> tuples2 =
				scope.fromSession(s -> s.createSelectionQuery("select id, element(names) from Entity", Object[].class).getResultList());
		assertEquals(2, tuples2.size());
		tuples2.forEach(tuple -> assertNotNull(tuple[0]));
		tuples2.forEach(tuple -> assertNotNull(tuple[1]));

		List<EntityCopy> copies =
				// this kind of query is currently not documented to work, but it does, and I guess I don't see why it's terrible
				scope.fromSession(s -> s.createSelectionQuery("select id, names from Entity", EntityCopy.class).getResultList());
		assertEquals(2, copies.size());
		copies.forEach(Assertions::assertNotNull);
		copies.forEach(copy -> assertNotNull(copy.id));
		copies.forEach(copy -> assertNotNull(copy.name));

		List<EntityCopy> copies2 =
				scope.fromSession(s -> s.createSelectionQuery("select id, element(names) from Entity", EntityCopy.class).getResultList());
		assertEquals(2, copies2.size());
		copies2.forEach(Assertions::assertNotNull);
		copies2.forEach(copy -> assertNotNull(copy.id));
		copies2.forEach(copy -> assertNotNull(copy.name));

		List<EntityCopy> newCopies =
				// this kind of query is currently not documented to work, but it does, and I guess I don't see why it's terrible
				scope.fromSession(s -> s.createSelectionQuery("select new EntityCopy(id, names) from Entity", EntityCopy.class).getResultList());
		assertEquals(2, newCopies.size());
		newCopies.forEach(Assertions::assertNotNull);
		newCopies.forEach(copy -> assertNotNull(copy.id));
		newCopies.forEach(copy -> assertNotNull(copy.name));

		List<EntityCopy> newCopies2 =
				scope.fromSession(s -> s.createSelectionQuery("select new EntityCopy(id, element(names)) from Entity", EntityCopy.class).getResultList());
		assertEquals(2, newCopies.size());
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
				// this kind of query is currently not documented to work, but it does, and I guess I don't see why it's terrible
				scope.fromSession(s -> s.createSelectionQuery("select id, children from Entity", Object[].class).getResultList());
		assertEquals(2, tuples.size());
		tuples.forEach(tuple -> assertNotNull(tuple[0]));
		tuples.forEach(tuple -> assertNotNull(tuple[1]));

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

		List<EntityCopy> newCopies =
				scope.fromSession(s -> s.createSelectionQuery("select new EntityCopy(id, element(children).name) from Entity", EntityCopy.class).getResultList());
		assertEquals(2, newCopies.size());
		newCopies.forEach(Assertions::assertNotNull);
		newCopies.forEach(copy -> assertNotNull(copy.id));
		newCopies.forEach(copy -> assertNotNull(copy.name));
	}

	static class EntityCopy {
		Long id;
		String name;

		public EntityCopy(Long id, String name) {
			this.id = id;
			this.name = name;
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
	}
}
