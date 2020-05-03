/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.callbacks;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@TestForIssue( jiraKey = "HHH-13020" )
public class ProtectedConstructorTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Parent.class,
				Child.class
		};
	}

	@Test
	public void test() {
		Child child = new Child();

		EntityManager entityManager = null;
		EntityTransaction txn = null;
		try {
			entityManager = createEntityManager();
			txn = entityManager.getTransaction();
			txn.begin();
			entityManager.persist( child );
			txn.commit();

			entityManager.clear();

			Integer childId = child.getId();

			Child childReference = entityManager.getReference( Child.class, childId );
			assertEquals( child.getParent().getName(), childReference.getParent().getName() );
		}
		catch (Throwable e) {
			if ( txn != null && txn.isActive() ) {
				txn.rollback();
			}
			throw e;
		}
		finally {
			if ( entityManager != null ) {
				entityManager.close();
			}
		}
	}

	@Entity(name = "Parent")
	public static class Parent {

		private Integer id;
		private String name;

		protected Parent() {
			name = "Empty";
		}

		public Parent(String s) {
			this.name = s;
		}

		@Id
		@Column(name = "id")
		@GeneratedValue(strategy = GenerationType.AUTO)
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
	}


	@Entity(name = "Child")
	public static class Child {

		private Integer id;
		private Parent parent;

		public Child() {
			this.parent = new Parent( "Name" );
		}

		@Id
		@Column(name = "id")
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}
}
