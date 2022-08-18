/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.various;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.OptimisticLock;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil2;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@TestForIssue(jiraKey = "HHH-15440")
public class OneToOneOptimisticLockTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Parent.class, Child.class };
	}

	public final static Integer PARENT_ID = 1;

	@Before
	public void setUp() {
		inTransaction(
				session -> {
					Parent parent = new Parent( PARENT_ID );
					session.persist( parent );
				}
		);
	}

	@Test
	public void testUpdateChildDoesNotIncrementParentVersion() {
		Integer version = TransactionUtil2.fromTransaction(
				sessionFactory(),
				session -> {
					Parent parent = session.get( Parent.class, PARENT_ID );
					Integer vers = parent.getVersion();

					Child child = new Child( 2 );
					parent.addChild( child );

					session.persist( child );
					return vers;
				}
		);

		inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, PARENT_ID );
					assertThat( parent.getVersion() ).isEqualTo( version );
				}
		);
	}

	@Entity(name = "Parent")
	@Table(name = "PARENT_TABLE")
	public static class Parent {

		@Id
		Integer id;

		public Parent(Integer id) {
			this.id = id;
		}

		public Parent() {
		}

		@OptimisticLock(excluded = true)
		@OneToOne(mappedBy = "parent")
		Child child;

		@Version
		@Column(name = "VERSION_COLUMN")
		Integer version;

		public void addChild(Child child) {
			this.child = child;
			child.parent = this;
		}

		public Integer getVersion() {
			return version;
		}
	}

	@Entity(name = "Child")
	@Table(name = "CHILD_TABLE")
	public static class Child {

		@Id
		Integer id;

		@OneToOne
		Parent parent;

		public Child() {
		}

		public Child(Integer id) {
			this.id = id;
		}
	}
}
