/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-12712" )
public class ImplicitJoinIsNullTest extends BaseEntityManagerFunctionalTestCase {

	@Test
	@FailureExpected( jiraKey = "HHH-12712" )
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Parent parent1 = new Parent(1);
			new Child(3, parent1);
			entityManager.persist(parent1);

			Parent parent2 = new Parent(2);
			entityManager.persist(parent2);
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Parent> parents = entityManager.createQuery(
				 "select p " +
				 "from Parent p " +
				 "left join p.child c " +
				 "where c is null", Parent.class)
			.getResultList();
			assertEquals(1, parents.size());
			assertEquals(2, parents.get(0).getId());
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Parent> parents = entityManager.createQuery(
				 "select p " +
				 "from Parent p " +
				 "where p.child is null", Parent.class)
			.getResultList();
			assertEquals(1, parents.size());
			assertEquals(2, parents.get(0).getId());
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Parent.class, Child.class };
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		private long id;

		@OneToOne(mappedBy = "parent", cascade = CascadeType.ALL)
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
	public static class Child {
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
