/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetoone.optional;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpected;
import org.junit.Test;

import static javax.persistence.CascadeType.ALL;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class BidirectionalOptionalOneToOneTest
		extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Parent.class,
				Child.class
		};
	}

	@Test
	public void test() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			Parent a = new Parent();
			a.id = 1L;
			Child b = new Child();
			b.id = 1L;
			a.setChild(b);
			b.setParent(a);

			entityManager.persist(a);
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Parent a = entityManager.find(Parent.class, 1L);

			entityManager.remove(a);
		} );
	}

	@javax.persistence.Entity( name = "Parent" )
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

	@javax.persistence.Entity( name = "Child" )
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
