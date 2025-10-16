/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import org.hibernate.envers.Audited;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-7940")
@EnversTest
@Jpa(annotatedClasses = {OrderColumnListTest.Parent.class, OrderColumnListTest.Child.class})
public class OrderColumnListTest {
	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1 - Create indexed entries.
		scope.inTransaction( em -> {
			Parent p = new Parent( 1 );
			p.addChild( new Child( 1, "child1" ) );
			p.addChild( new Child( 2, "child2" ) );
			em.persist( p );
			p.getChildren().forEach( em::persist );
		} );

		// Revision 2 - remove an indexed entry, resetting positions.
		scope.inTransaction( em -> {
			final Parent p = em.find( Parent.class, 1 );
			// should remove child with id 1
			p.removeChild( p.getChildren().get( 0 ) );
			em.merge( p );
		} );

		// Revision 3 - add new indexed entity to reset positions
		scope.inTransaction( em -> {
			final Parent p = em.find( Parent.class, 1 );
			// add child with id 3
			final Child child = new Child( 3, "child3" );
			p.getChildren().add( 0, child );
			child.setParent( p );
			em.persist( child );
			em.merge( p );
		} );

		// Revision 4 - remove all children
		scope.inTransaction( em -> {
			final Parent p = em.find( Parent.class, 1 );
			while ( !p.getChildren().isEmpty() ) {
				Child child = p.getChildren().get( 0 );
				p.removeChild( child );
				em.remove( child );
			}
			em.merge( p );
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			assertEquals( Arrays.asList( 1, 2, 3, 4 ), auditReader.getRevisions( Parent.class, 1 ) );
			assertEquals( Arrays.asList( 1, 2 ), auditReader.getRevisions( Child.class, 1 ) );
			assertEquals( Arrays.asList( 1, 2, 3, 4 ), auditReader.getRevisions( Child.class, 2 ) );
			assertEquals( Arrays.asList( 3, 4 ), auditReader.getRevisions( Child.class, 3 ) );
		} );
	}

	@Test
	public void testIndexedCollectionRev1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			final Parent p = auditReader.find( Parent.class, 1, 1 );
			assertEquals( 2, p.getChildren().size() );
			assertEquals( new Child( 1, "child1", p ), p.getChildren().get( 0 ) );
			assertEquals( new Child( 2, "child2", p ), p.getChildren().get( 1 ) );
		} );
	}

	@Test
	public void testIndexedCollectionRev2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			final Parent p = auditReader.find( Parent.class, 1, 2 );
			assertEquals( 1, p.getChildren().size() );
			assertEquals( new Child( 2, "child2", p ), p.getChildren().get( 0 ) );
		} );
	}

	@Test
	public void testIndexedCollectionRev3(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			final Parent p = auditReader.find( Parent.class, 1, 3 );
			assertEquals( 2, p.getChildren().size() );
			assertEquals( new Child( 3, "child3", p ), p.getChildren().get( 0 ) );
			assertEquals( new Child( 2, "child2", p ), p.getChildren().get( 1 ) );
		} );
	}

	@Test
	public void testIndexedCollectionRev4(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			var auditReader = AuditReaderFactory.get( em );
			final Parent p = auditReader.find( Parent.class, 1, 4 );
			assertEquals( 0, p.getChildren().size() );
		} );
	}

	@Audited
	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Integer id;

		@OneToMany(mappedBy = "parent")
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
			if ( child.getParent() != null ) {
				removeChild( child );
			}
			child.setParent( this );
			getChildren().add( child );
		}

		public void removeChild(Child child) {
			if ( child != null ) {
				final Parent p = child.getParent();
				if ( p != null ) {
					p.getChildren().remove( child );
					child.setParent( null );
				}
			}
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
	}

	@Audited
	@Entity(name = "Child")
	public static class Child {
		@Id
		private Integer id;
		private String name;

		@ManyToOne
		private Parent parent;

		Child() {

		}

		Child(Integer id, String name) {
			this( id, name, null );
		}

		Child(Integer id, String name, Parent parent) {
			this.id = id;
			this.name = name;
			this.parent = parent;
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

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
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
			return parent != null ? parent.equals( child.parent ) : child.parent == null;

		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + ( name != null ? name.hashCode() : 0 );
			result = 31 * result + ( parent != null ? parent.hashCode() : 0 );
			return result;
		}
	}
}
