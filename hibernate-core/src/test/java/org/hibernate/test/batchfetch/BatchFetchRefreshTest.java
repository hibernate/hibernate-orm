/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.batchfetch;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.LockModeType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.*;

public class BatchFetchRefreshTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test

	public void testRefreshWithBatch() {

		doInHibernate( this::sessionFactory, session -> {

			// Retrieve one of the parents into the session.
			Parent parent = session.find(Parent.class, 1);
			Assert.assertNotNull(parent);

			// Retrieve children but keep their parents lazy!
			// This allows batch fetching to do its thing when we refresh below.
			session.createQuery( "FROM Child" ).getResultList();

			session.refresh( parent, LockModeType.PESSIMISTIC_WRITE );

			// Just something to force delazification of children on parent entity
			// The parent is obviously attached to the session (we just refreshed it!)
			parent.getChildren().size();

			// Another interesting thing to note - em.getLockMode returns an incorrect value after the above refresh
			Assert.assertEquals( LockModeType.PESSIMISTIC_WRITE, session.getLockMode( parent ) );
		});
	}

	@Before
	public void setupData() {
		final int numParents = 5;
		final int childrenPerParent = 2;

		doInHibernate( this::sessionFactory, session -> {
			int k = 1;
			for ( int i = 1; i <= numParents; i++ ) {
				Parent parent = new Parent();
				parent.parentId = i;
				parent.name = "Parent_" + i;

				session.persist( parent );

				// Create some children for each parent...
				for ( int j = 0; j < childrenPerParent; j++,k++ ) {
					Child child = new Child();
					child.childId = k;
					child.name = "Child_" + i + "_" + j;
					child.age = 15;
					child.parent = parent;
					parent.getChildren().add( child );
					session.persist( child );
				}
			}
		});

	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Parent.class,
				Child.class
		};
	}

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, "8" );
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		@Column(name = "parent_id")
		private int parentId;

		@Column(name = "name")
		private String name;

		@OneToMany(mappedBy = "parent")
		private Set<Child> children = new LinkedHashSet<>();

		public int getParentId() {
			return parentId;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<Child> getChildren() {
			return children;
		}

		public void setChildren(Set<Child> children) {
			this.children = children;
		}

	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@Column(name = "child_id")
		private int childId;

		@Column(name = "name")
		private String name;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "parent_id")
		private Parent parent;

		@Column(name = "age")
		private int age;

		public int getChildId() {
			return childId;
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

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

	}
}
