/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.CascadeType.ALL;

/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				BidirectionalOptionalOneToOneTest.Parent.class,
				BidirectionalOptionalOneToOneTest.Child.class
		}
)
public class BidirectionalOptionalOneToOneTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {

		scope.inTransaction( entityManager -> {
			Parent a = new Parent();
			a.id = 1L;
			Child b = new Child();
			b.id = 1L;
			a.setChild( b );
			b.setParent( a );

			entityManager.persist( a );
		} );

		scope.inTransaction( entityManager -> {
			Parent a = entityManager.find( Parent.class, 1L );

			entityManager.remove( a );
		} );
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		@Column(unique = true, nullable = false)
		private Long id;

		@OneToOne(optional = false, mappedBy = "parent", cascade = ALL)
		private Child child;

		public Long getId() {
			return id;
		}

		public Child getChild() {
			return child;
		}

		public void setChild(Child child) {
			this.child = child;
		}

	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@Column(unique = true, nullable = false)
		private Long id;

		@OneToOne(optional = false)
		@JoinColumn(nullable = false)
		private Parent parent;

		public Long getId() {
			return id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

}
