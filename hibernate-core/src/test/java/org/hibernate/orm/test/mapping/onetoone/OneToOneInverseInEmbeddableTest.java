/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetoone;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * @author Andrea Boriero
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				OneToOneInverseInEmbeddableTest.Parent.class,
				OneToOneInverseInEmbeddableTest.Child.class,
		}
)
@ServiceRegistry
@SessionFactory
public class OneToOneInverseInEmbeddableTest {
	@Test
	public void basicTest(SessionFactoryScope scope) {
		scope.inSession(
				s -> {
					s.createQuery( "from Child c join fetch c.parent p" ).getResultList();
				}
		);
	}

	@Entity(name = "Parent")
	@Table(name = "PARENT")
	public static class Parent {
		private Integer id;

		private String description;
		private ParentEmbeddable embeddable = new ParentEmbeddable();

		Parent() {
		}

		public Parent(Integer id, String description) {
			this.id = id;
			this.description = description;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		@Embedded
		public ParentEmbeddable getEmbeddable() {
			return embeddable;
		}

		public void setEmbeddable(ParentEmbeddable embeddable) {
			this.embeddable = embeddable;
		}
	}

	@Entity(name = "Child")
	@Table(name = "CHILD")
	public static class ParentEmbeddable {
		private Child child;

		@OneToOne(mappedBy = "parent")
		public Child getChild() {
			return child;
		}

		public void setChild(Child other) {
			this.child = other;
		}
	}

	@Entity(name = "Child")
	@Table(name = "CHILD")
	public static class Child {
		private Integer id;

		private String name;
		private Parent parent;

		Child() {
		}

		Child(Integer id, Parent parent) {
			this.id = id;
			this.parent = parent;
			this.parent.getEmbeddable().setChild( this );
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@OneToOne
		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

}
