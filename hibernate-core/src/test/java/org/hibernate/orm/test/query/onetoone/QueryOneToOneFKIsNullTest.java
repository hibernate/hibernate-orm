/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.onetoone;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = {QueryOneToOneFKIsNullTest.Parent.class,
						QueryOneToOneFKIsNullTest.Child.class})
@JiraKey("HHH-12712")
public class QueryOneToOneFKIsNullTest {
	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			var parent1 = new Parent(1);
			new Child(3, parent1);
			entityManager.persist(parent1);
			var parent2 = new Parent(2);
			entityManager.persist(parent2);
		});

		scope.inTransaction(entityManager -> {
			var query = entityManager.createQuery("from Parent p where p.child is null", Parent.class);
			var parents = query.getResultList();
			assertEquals(1, parents.size());
			assertEquals(2L, parents.get(0).getId());
		});
	}

	@Entity(name = "Parent")
	static class Parent {
		@Id
		private long id;

		@OneToOne(mappedBy = QueryOneToOneFKIsNullTest_.Child_.PARENT,
				cascade = CascadeType.ALL)
		private Child child;

		Parent() {}

		public Parent(long id) {
			this.id = id;
		}

		public long getId() {
			return id;
		}

		public Child getChild() {
			return child;
		}

		void setChild(Child child) {
			this.child = child;
		}

		@Override
		public String toString() {
			return "Parent [id=" + id + ", child=" + child + "]";
		}
	}


	@Entity(name = "Child")
	static class Child {
		@Id
		private long id;

		@OneToOne
		private Parent parent;

		Child() {}

		public Child(long id, Parent parent) {
			this.id = id;
			setParent(parent);
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
			parent.setChild(this);
		}

		@Override
		public String toString() {
			return "Child [id=" + id + "]";
		}
	}
}
