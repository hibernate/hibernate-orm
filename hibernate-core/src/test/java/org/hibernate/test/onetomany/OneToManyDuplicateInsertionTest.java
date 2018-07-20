/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetomany;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

public class OneToManyDuplicateInsertionTest extends BaseEntityManagerFunctionalTestCase {

	private int parentId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Parent.class, Child.class, ParentCascade.class, ChildCascade.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6776")
	public void testDuplicateInsertion() {
		// persist parent entity in a transaction

		doInJPA( this::entityManagerFactory, em -> {
			Parent parent = new Parent();
			em.persist( parent );
			parentId = parent.getId();
		} );

		// relate and persist child entity in another transaction

		doInJPA( this::entityManagerFactory, em -> {
			Parent parent = em.find( Parent.class, parentId );
			Child child = new Child();
			child.setParent( parent );
			parent.getChildren().add( child );
			em.persist( child );

			assertEquals( 1, parent.getChildren().size() );
		} );

		// get the parent again

		doInJPA( this::entityManagerFactory, em -> {
			Parent parent = em.find( Parent.class, parentId );

			assertEquals( 1, parent.getChildren().size() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7404")
	public void testDuplicateInsertionWithCascadeAndMerge() {
		doInJPA( this::entityManagerFactory, em -> {
			ParentCascade p = new ParentCascade();
			// merge with 0 children
			p = em.merge( p );
			parentId = p.getId();
		} );

		doInJPA( this::entityManagerFactory, em -> {
			ParentCascade p = em.find( ParentCascade.class, parentId );
			final ChildCascade child = new ChildCascade();
			child.setParent( p );
			p.getChildren().add( child );
			em.merge( p );
		} );

		doInJPA( this::entityManagerFactory, em -> {
			// again, load the Parent by id
			ParentCascade p = em.find( ParentCascade.class, parentId );

			// check that we have only 1 element in the list
			assertEquals( 1, p.getChildren().size() );
		} );
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		@GeneratedValue
		private int id;

		@OneToMany(mappedBy = "parent")
		private List<Child> children = new LinkedList<Child>();

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void setChildren(List<Child> children) {
			this.children = children;
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue
		private int id;

		@ManyToOne
		private Parent parent;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "ParentCascade")
	public static class ParentCascade {

		@Id
		@GeneratedValue
		private Integer id;

		@OneToMany(mappedBy = "parent", cascade = { CascadeType.ALL })
		private List<ChildCascade> children = new ArrayList<ChildCascade>();

		public Integer getId() {
			return id;
		}

		public List<ChildCascade> getChildren() {
			return children;
		}

		public void setChildren(List<ChildCascade> children) {
			this.children = children;
		}
	}

	@Entity(name = "ChildCascade")
	public static class ChildCascade {

		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		private ParentCascade parent;

		public Integer getId() {
			return id;
		}

		public ParentCascade getParent() {
			return parent;
		}

		public void setParent(ParentCascade parent) {
			this.parent = parent;
		}
	}
}
