/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator;

import java.io.Serializable;
import java.util.List;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * Originally from https://github.com/mkaletka/hibernate-test-case-templates/commit/2b3c075cacd07474d5565fa3bd5a6d0a48683dc0
 *
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				MappedSuperclassExtendsEntityTest.TestEntity.class,
				MappedSuperclassExtendsEntityTest.GrandParent.class,
				MappedSuperclassExtendsEntityTest.Parent.class,
				MappedSuperclassExtendsEntityTest.Child1.class,
				MappedSuperclassExtendsEntityTest.Child2.class
		}
)
@SessionFactory
public class MappedSuperclassExtendsEntityTest {

	@Test
	@JiraKey(value = "HHH-12332")
	public void testQueryingSingle(SessionFactoryScope scope) {
		// Make sure that the produced query for th
		scope.inTransaction(
				s ->
						s.createQuery(
								"FROM TestEntity e JOIN e.parents p1 JOIN p1.entities JOIN p1.entities2 JOIN e.parents2 p2 JOIN p2.entities JOIN p2.entities2", Object[].class )
								.getResultList()
		);
	}

	@Test
	@JiraKey(value = "HHH-12332")
	public void testHql(SessionFactoryScope scope) {
		// Make sure that the produced query for th
		scope.inTransaction(
				s ->
						s.createQuery( "from TestEntity", TestEntity.class ).list()
		);
	}

	@Entity(name = "GrandParent")
	@Inheritance
	@DiscriminatorColumn(name = "discriminator")
	public static abstract class GrandParent implements Serializable {
		private static final long serialVersionUID = 1L;

		@Id
		@GeneratedValue
		private Long id;

		@ManyToMany(mappedBy = "parents2")
		private List<TestEntity> entities2;

		public GrandParent() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<TestEntity> getEntities2() {
			return entities2;
		}

		public void setEntities2(List<TestEntity> entities2) {
			this.entities2 = entities2;
		}
	}

	@MappedSuperclass
	public static abstract class Parent extends GrandParent {

		@ManyToMany(mappedBy = "parents")
		private List<TestEntity> entities;

		public List<TestEntity> getEntities() {
			return entities;
		}

		public void setEntities(List<TestEntity> entities) {
			this.entities = entities;
		}

	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		@GeneratedValue
		private Long id;
		@ManyToMany
		private List<GrandParent> parents;
		@ManyToMany
		private List<GrandParent> parents2;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<GrandParent> getParents() {
			return parents;
		}

		public void setParents(List<GrandParent> parents) {
			this.parents = parents;
		}

		public List<GrandParent> getParents2() {
			return parents2;
		}

		public void setParents2(List<GrandParent> parents2) {
			this.parents2 = parents2;
		}
	}

	@Entity(name = "Child1")
	@DiscriminatorValue("CHILD1")
	public static class Child1 extends Parent {
	}

	@Entity(name = "Child2")
	@DiscriminatorValue("CHILD2")
	public static class Child2 extends Parent {
	}
}
