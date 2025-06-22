/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batchfetch;

import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.LockModeType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@DomainModel(
		annotatedClasses = {
				BatchFetchRefreshTest.Parent.class,
				BatchFetchRefreshTest.Child.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "8")
)
public class BatchFetchRefreshTest {

	@Test
	public void testRefreshWithBatch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {

			// Retrieve one of the parents into the session.
			Parent parent = session.find( Parent.class, 1 );
			assertNotNull( parent );

			// Retrieve children but keep their parents lazy!
			// This allows batch fetching to do its thing when we refresh below.
			session.createQuery( "FROM Child" ).getResultList();

			session.refresh( parent, LockModeType.PESSIMISTIC_WRITE );

			// Just something to force delazification of children on parent entity
			// The parent is obviously attached to the session (we just refreshed it!)
			parent.getChildren().size();

			// Another interesting thing to note - em.getLockMode returns an incorrect value after the above refresh
			assertEquals( LockModeType.PESSIMISTIC_WRITE, session.getLockMode( parent ) );
		} );
	}

	@BeforeEach
	public void setupData(SessionFactoryScope scope) {
		final int numParents = 5;
		final int childrenPerParent = 2;

		scope.inTransaction( session -> {
			int k = 1;
			for ( int i = 1; i <= numParents; i++ ) {
				Parent parent = new Parent();
				parent.parentId = i;
				parent.name = "Parent_" + i;

				session.persist( parent );

				// Create some children for each parent...
				for ( int j = 0; j < childrenPerParent; j++, k++ ) {
					Child child = new Child();
					child.childId = k;
					child.name = "Child_" + i + "_" + j;
					child.age = 15;
					child.parent = parent;
					parent.getChildren().add( child );
					session.persist( child );
				}
			}
		} );

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
