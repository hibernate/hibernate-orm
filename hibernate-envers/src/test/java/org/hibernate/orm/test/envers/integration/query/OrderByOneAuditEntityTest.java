/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the use of the {@link OrderBy} annotation on a one-to-many collection where
 * both sides of the association are audited.
 *
 * This mapping invokes the use of the OneAuditEntityQueryGenerator which we want to
 * verify orders the collection results properly.
 *
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12992")
@Jpa(annotatedClasses = {
		OrderByOneAuditEntityTest.Parent.class,
		OrderByOneAuditEntityTest.Child.class
})
@EnversTest
public class OrderByOneAuditEntityTest {
	@Entity(name = "Parent")
	@Audited
	public static class Parent {
		@Id
		@GeneratedValue
		private Integer id;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
		@OrderBy("index1, index2 desc")
		@AuditMappedBy(mappedBy = "parent", positionMappedBy = "index1")
		@Fetch(FetchMode.SELECT)
		@BatchSize(size = 100)
		private List<Child> children = new ArrayList<>();

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
	}

	@Entity(name = "Child")
	@Audited
	public static class Child {
		@Id
		private Integer id;

		private Integer index1;
		private Integer index2;

		@ManyToOne
		private Parent parent;

		public Child() {

		}

		public Child(Integer id, Integer index1) {
			this.id = id;
			this.index1 = index1;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getIndex1() {
			return index1;
		}

		public void setIndex1(Integer index1) {
			this.index1 = index1;
		}

		public Integer getIndex2() {
			return index2;
		}

		public void setIndex2(Integer index2) {
			this.index2 = index2;
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
			return Objects.equals( id, child.id ) &&
					Objects.equals( index1, child.index1 );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, index1 );
		}

		@Override
		public String toString() {
			return "Child{" +
					"id=" + id +
					", index1=" + index1 +
					'}';
		}
	}

	private Integer parentId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Rev 1
		this.parentId = scope.fromTransaction( em -> {
			final Parent parent = new Parent();

			final Child child1 = new Child();
			child1.setId( 1 );
			child1.setIndex1( 1 );
			child1.setIndex2( 1 );
			child1.setParent( parent );
			parent.getChildren().add( child1 );

			final Child child2 = new Child();
			child2.setId( 2 );
			child2.setIndex1( 2 );
			child2.setIndex2( 2 );
			child2.setParent( parent );
			parent.getChildren().add( child2 );

			em.persist( parent );

			return parent.getId();
		} );

		// Rev 2
		scope.inTransaction( em -> {
			final Parent parent = em.find( Parent.class, parentId );

			final Child child = new Child();
			child.setId( 3 );
			child.setIndex1( 3 );
			child.setIndex2( 3 );
			child.setParent( parent );
			parent.getChildren().add( child );

			em.merge( parent );
		} );

		// Rev 3
		scope.inTransaction( em -> {
			final Parent parent = em.find( Parent.class, parentId );
			parent.getChildren().removeIf( c -> c.getIndex1() == 2 );
			em.merge( parent );
		} );

		// Rev 4
		scope.inTransaction( em -> {
			final Parent parent = em.find( Parent.class, parentId );
			parent.getChildren().clear();
			em.merge( parent );
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 2, 3, 4 ), AuditReaderFactory.get( em ).getRevisions( Parent.class, this.parentId ) );
			assertEquals( Arrays.asList( 1, 4 ), AuditReaderFactory.get( em ).getRevisions( Child.class, 1 ) );
			assertEquals( Arrays.asList( 1, 3 ), AuditReaderFactory.get( em ).getRevisions( Child.class, 2 ) );
			assertEquals( Arrays.asList( 2, 4 ), AuditReaderFactory.get( em ).getRevisions( Child.class, 3 ) );
		} );
	}

	@Test
	public void testRevision1History(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final Parent parent = AuditReaderFactory.get( em ).find( Parent.class, this.parentId, 1 );
			assertNotNull( parent );
			assertTrue( !parent.getChildren().isEmpty() );
			assertEquals( 2, parent.getChildren().size() );
			assertEquals( Arrays.asList( new Child( 1, 1 ), new Child( 2, 2 ) ), parent.getChildren() );
		} );
	}

	@Test
	public void testRevision2History(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final Parent parent = AuditReaderFactory.get( em ).find( Parent.class, this.parentId, 2 );
			assertNotNull( parent );
			assertTrue( !parent.getChildren().isEmpty() );
			assertEquals( 3, parent.getChildren().size() );
			assertEquals( Arrays.asList( new Child( 1, 1 ), new Child( 2, 2 ), new Child( 3, 3 ) ), parent.getChildren() );
		} );
	}

	@Test
	public void testRevision3History(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final Parent parent = AuditReaderFactory.get( em ).find( Parent.class, this.parentId, 3 );
			assertNotNull( parent );
			assertTrue( !parent.getChildren().isEmpty() );
			assertEquals( 2, parent.getChildren().size() );
			assertEquals( Arrays.asList( new Child( 1, 1 ), new Child( 3, 3 ) ), parent.getChildren() );
		} );
	}

	@Test
	public void testRevision4History(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final Parent parent = AuditReaderFactory.get( em ).find( Parent.class, this.parentId, 4 );
			assertNotNull( parent );
			assertTrue( parent.getChildren().isEmpty() );
		} );
	}
}
