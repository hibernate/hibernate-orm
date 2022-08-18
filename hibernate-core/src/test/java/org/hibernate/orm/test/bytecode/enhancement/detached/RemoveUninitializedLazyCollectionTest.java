/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.detached;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;


/**
 * @author Christian Beikov
 */
@TestForIssue(jiraKey = "HHH-14387")
@RunWith( BytecodeEnhancerRunner.class )
@EnhancementOptions(
		lazyLoading = true,
		inlineDirtyChecking = true,
		biDirectionalAssociationManagement = true
)
public class RemoveUninitializedLazyCollectionTest extends BaseCoreFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{
				Parent.class,
				Child1.class,
				Child2.class
		};
	}

	@After
	public void tearDown() {
		TransactionUtil.doInJPA(
				this::sessionFactory,
				session -> {
					session.createQuery( "delete from Child1" ).executeUpdate();
					session.createQuery( "delete from Child2" ).executeUpdate();
					session.createQuery( "delete from Parent" ).executeUpdate();
				}
		);
	}

	@Before
	public void setup() {
		Parent parent = new Parent( 1L, "test" );
		TransactionUtil.doInJPA(
				this::sessionFactory,
				entityManager -> {
					entityManager.persist( parent );
					entityManager.persist( new Child2( 1L, "child2", parent ) );
				}
		);
	}

	@Test
	public void testDeleteParentWithBidirOrphanDeleteCollectionBasedOnPropertyRef() {
		EntityManager em = sessionFactory().createEntityManager();
		try {
			// Lazily initialize the child1 collection
			List<Child1> child1 = em.find( Parent.class, 1L ).getChild1();
			Hibernate.initialize( child1 );

			org.hibernate.testing.orm.transaction.TransactionUtil.inTransaction(
					em,
					entityManager -> {
						Parent parent = new Parent();
						parent.setId( 1L );
						parent.setName( "new name" );
						entityManager.merge( parent );
					}
			);

		}
		finally {
			em.close();
		}
	}

	@Entity(name = "Parent")
	public static class Parent {

		private Long id;

		private String name;

		private List<Child1> child1 = new ArrayList<>();

		private List<Child2> child2 = new ArrayList<>();

		public Parent() {
		}

		public Parent(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, mappedBy = "parent")
		public List<Child1> getChild1() {
			return child1;
		}

		public void setChild1(List<Child1> child1) {
			this.child1 = child1;
		}

		@OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, mappedBy = "parent")
		public List<Child2> getChild2() {
			return child2;
		}

		public void setChild2(List<Child2> child2) {
			this.child2 = child2;
		}

	}

	@Entity(name = "Child1")
	public static class Child1 {

		@Id
		private Long id;

		private String name;

		@ManyToOne
		@JoinColumn
		private Parent parent;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		@Override
		public String toString() {
			return "Child1 [id=" + id + ", name=" + name + "]";
		}

	}

	@Entity(name = "Child2")
	public static class Child2 {
		@Id
		private Long id;

		private String name;

		@ManyToOne
		@JoinColumn
		private Parent parent;

		public Child2() {
		}

		public Child2(Long id, String name, Parent parent) {
			this.id = id;
			this.name = name;
			this.parent = parent;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

	}

}
