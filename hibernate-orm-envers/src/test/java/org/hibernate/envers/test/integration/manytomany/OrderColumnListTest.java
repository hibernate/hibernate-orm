/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytomany;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OrderColumn;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-7940")
public class OrderColumnListTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Parent.class, Child.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1 - Create indexed entries.
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			Parent p = new Parent( 1 );
			p.addChild( new Child( 1, "child1" ) );
			p.addChild( new Child( 2, "child2" ) );
			entityManager.persist( p );
			p.getChildren().forEach( entityManager::persist );
		} );

		// Revision 2 - remove an indexed entry, resetting positions.
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final Parent p = entityManager.find( Parent.class, 1 );
			// should remove child with id 1
			p.removeChild( p.getChildren().get( 0 ) );
			entityManager.merge( p );
		} );

		// Revision 3 - add new indexed entity to reset positions
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final Parent p = entityManager.find( Parent.class, 1 );
			// add child with id 3
			final Child child = new Child( 3, "child3" );
			p.getChildren().add( 0, child );
			child.getParents().add( p );
			entityManager.persist( child );
			entityManager.merge( p );
		} );

		// Revision 4 - remove all children
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final Parent p = entityManager.find( Parent.class, 1 );
			while ( !p.getChildren().isEmpty() ) {
				Child child = p.getChildren().get( 0 );
				p.removeChild( child );
				entityManager.remove( child );
			}
			entityManager.merge( p );
		} );
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( Arrays.asList( 1, 2, 3, 4 ), getAuditReader().getRevisions( Parent.class, 1 ) );
		assertEquals( Arrays.asList( 1, 2 ), getAuditReader().getRevisions( Child.class, 1 ) );
		assertEquals( Arrays.asList( 1, 2, 3, 4 ), getAuditReader().getRevisions( Child.class, 2 ) );
		assertEquals( Arrays.asList( 3, 4 ), getAuditReader().getRevisions( Child.class, 3 ) );
	}

	@Test
	public void testIndexedCollectionRev1() {
		final Parent p = getAuditReader().find( Parent.class, 1, 1 );
		assertEquals( 2, p.getChildren().size() );
		assertEquals( new Child( 1, "child1", p ), p.getChildren().get( 0 ) );
		assertEquals( new Child( 2, "child2", p ), p.getChildren().get( 1 ) );
	}

	@Test
	public void testIndexedCollectionRev2() {
		final Parent p = getAuditReader().find( Parent.class, 1, 2 );
		assertEquals( 1, p.getChildren().size() );
		assertEquals( new Child( 2, "child2", p ), p.getChildren().get( 0 ) );
	}

	@Test
	public void testIndexedCollectionRev3() {
		final Parent p = getAuditReader().find( Parent.class, 1, 3 );
		assertEquals( 2, p.getChildren().size() );
		assertEquals( new Child( 3, "child3", p ), p.getChildren().get( 0 ) );
		assertEquals( new Child( 2, "child2", p ), p.getChildren().get( 1 ) );
	}

	@Test
	public void testIndexedCollectionRev4() {
		final Parent p = getAuditReader().find( Parent.class, 1, 4 );
		assertEquals( 0, p.getChildren().size() );
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

			return id != null ? id.equals( parent.id ) : parent.id == null;

		}

		@Override
		public int hashCode() {
			return id != null ? id.hashCode() : 0;
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

			if ( id != null ? !id.equals( child.id ) : child.id != null ) {
				return false;
			}
			if ( name != null ? !name.equals( child.name ) : child.name != null ) {
				return false;
			}
			return parents != null ? parents.equals( child.parents ) : child.parents == null;

		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + ( name != null ? name.hashCode() : 0 );
			result = 31 * result + ( parents != null ? parents.hashCode() : 0 );
			return result;
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
