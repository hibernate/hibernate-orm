/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytomany;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OrderColumn;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-7940")
public class OrderColumnListTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Parent.class, Child.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1 - Create indexed entries.
				entityManager -> {
					Parent p = new Parent( 1 );
					p.addChild( new Child( 1, "child1" ) );
					p.addChild( new Child( 2, "child2" ) );
					entityManager.persist( p );
					p.getChildren().forEach( entityManager::persist );
				},

				// Revision 2 - remove an indexed entry, resetting positions.
				entityManager -> {
					final Parent p = entityManager.find( Parent.class, 1 );
					// should remove child with id 1
					p.removeChild( p.getChildren().get( 0 ) );
					entityManager.merge( p );
				},

				// Revision 3 - add new indexed entity to reset positions
				entityManager -> {
					final Parent p = entityManager.find( Parent.class, 1 );
					// add child with id 3
					final Child child = new Child( 3, "child3" );
					p.getChildren().add( 0, child );
					child.getParents().add( p );
					entityManager.persist( child );
					entityManager.merge( p );
				},

				// Revision 4 - remove all children
				entityManager -> {
					final Parent p = entityManager.find( Parent.class, 1 );
					while ( !p.getChildren().isEmpty() ) {
						Child child = p.getChildren().get( 0 );
						p.removeChild( child );
						entityManager.remove( child );
					}
					entityManager.merge( p );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( Parent.class, 1 ), contains( 1, 2, 3, 4 ) );
		assertThat( getAuditReader().getRevisions( Child.class, 1 ), contains( 1, 2 ) );
		assertThat( getAuditReader().getRevisions( Child.class, 2 ), contains( 1, 2, 3, 4 ) );
		assertThat( getAuditReader().getRevisions( Child.class, 3 ), contains( 3, 4 ) );
	}

	@DynamicTest
	public void testIndexedCollectionRev1() {
		final Parent p = getAuditReader().find( Parent.class, 1, 1 );
		assertThat( p.getChildren(), contains( new Child( 1, "child1", p ), new Child( 2, "child2", p ) ) );
	}

	@DynamicTest
	public void testIndexedCollectionRev2() {
		final Parent p = getAuditReader().find( Parent.class, 1, 2 );
		assertThat( p.getChildren(), contains( new Child( 2, "child2", p ) ) );
	}

	@DynamicTest
	public void testIndexedCollectionRev3() {
		final Parent p = getAuditReader().find( Parent.class, 1, 3 );
		assertThat( p.getChildren(), contains( new Child( 3, "child3", p ), new Child( 2, "child2", p ) ) );
	}

	@DynamicTest
	public void testIndexedCollectionRev4() {
		final Parent p = getAuditReader().find( Parent.class, 1, 4 );
		assertThat( p.getChildren(), CollectionMatchers.isEmpty() );
	}

	@Audited
	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Integer id;

		@ManyToMany
		@OrderColumn
		private List<Child> children = new ArrayList<Child>();

		Parent() {

		}

		Parent(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void setChildren(List<Child> children) {
			this.children = children;
		}

		public void addChild(Child child) {
			child.getParents().add( this );
			getChildren().add( child );
		}

		public void removeChild(Child child) {
			child.getParents().remove( this );
			getChildren().remove( child );
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Parent parent = (Parent) o;
			return Objects.equals( id, parent.id );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id );
		}

		@Override
		public String toString() {
			return "Parent{" +
					"id=" + id +
					'}';
		}
	}

	@Audited
	@Entity(name = "Child")
	public static class Child {
		@Id
		private Integer id;
		private String name;

		@ManyToMany(mappedBy = "children")
		private List<Parent> parents;

		Child() {

		}

		Child(Integer id, String name) {
			this( id, name, null );
		}

		Child(Integer id, String name, Parent... parents) {
			this.id = id;
			this.name = name;
			if ( parents != null && parents.length > 0 ) {
				this.parents = new ArrayList<>( Arrays.asList( parents ) );
			}
			else {
				this.parents = new ArrayList<>();
			}
		}

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

		public List<Parent> getParents() {
			return parents;
		}

		public void setParent(List<Parent> parent) {
			this.parents = parents;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Child child = (Child) o;
			return Objects.equals( id, child.id ) &&
					Objects.equals( name, child.name ) &&
					Objects.equals( parents, child.parents );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, name, parents );
		}

		@Override
		public String toString() {
			return "Child{" +
					"id=" + id +
					", name='" + name + '\'' +
					", parents=" + parents +
					'}';
		}
	}
}
