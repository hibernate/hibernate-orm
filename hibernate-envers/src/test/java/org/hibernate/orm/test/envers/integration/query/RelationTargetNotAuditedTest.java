/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests that {@link RelationTargetAuditMode#NOT_AUDITED} works correctly when loading audit history.
 * When a relation is marked with NOT_AUDITED mode, the target entity is loaded from the current
 * table rather than from audit tables, so changes to the target entity are visible when querying
 * historical revisions.
 *
 * @author Minjae Seon
 */
@JiraKey(value = "HHH-19861")
@Jpa(annotatedClasses = {
		RelationTargetNotAuditedTest.Parent.class,
		RelationTargetNotAuditedTest.Child.class
})
@EnversTest
public class RelationTargetNotAuditedTest {
	private Long childId;
	private Long parentId;

	@Entity(name = "Child")
	@Audited
	public static class Child {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Child() {
		}

		public Child(String name) {
			this.name = name;
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
	}

	@Entity(name = "Parent")
	@Audited
	public static class Parent {
		@Id
		@GeneratedValue
		private Long id;

		private String content;

		@ManyToOne(fetch = FetchType.LAZY)
		@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
		private Child child;

		public Parent() {
		}

		public Parent(String content, Child child) {
			this.content = content;
			this.child = child;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public Child getChild() {
			return child;
		}

		public void setChild(Child child) {
			this.child = child;
		}
	}

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1: Create child and parent
		scope.inTransaction( em -> {
			final Child child = new Child( "Child 1" );
			em.persist( child );

			final Parent parent = new Parent( "Initial content", child );
			em.persist( parent );


			this.childId = child.getId();
			this.parentId = parent.getId();
		});

		// Revision 2: Update parent content
		scope.inTransaction( em -> {
			final Parent parent = em.find( Parent.class, this.parentId );
			parent.setContent( "Updated content" );
		});

		// Revision 3: Update child name (should not create audit record for parent)
		scope.inTransaction( em -> {
			final Child child = em.find( Child.class, this.childId );
			child.setName( "Child 1 Updated" );
		});
	}

	@Test
	public void testLoadParentAtRevision1(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final Parent parent = AuditReaderFactory.get( em ).find( Parent.class, this.parentId, 1 );

			assertNotNull( parent );
			assertEquals( "Initial content", parent.getContent() );
			assertNotNull( parent.getChild() );
			assertEquals( this.childId, parent.getChild().getId() );
			// Child should be loaded from current table, so it should have the updated name
			assertEquals( "Child 1 Updated", parent.getChild().getName() );
		});
	}

	@Test
	public void testLoadParentAtRevision2(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final Parent parent = AuditReaderFactory.get( em ).find( Parent.class, this.parentId, 2 );

			assertNotNull( parent );
			assertEquals( "Updated content", parent.getContent() );
			assertNotNull( parent.getChild() );
			assertEquals( childId, parent.getChild().getId() );
			// Child should be loaded from current table
			assertEquals( "Child 1 Updated", parent.getChild().getName() );
		});
	}

	@Test
	public void testQueryParentRevisions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final List<Number> revisions = AuditReaderFactory.get( em ).getRevisions( Parent.class, this.parentId );

			// Parent should have 2 revisions (creation and update)
			assertEquals( 2, revisions.size() );
		});
	}

	@Test
	public void testQueryChildRevisions(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			final List<Number> revisions = AuditReaderFactory.get( em ).getRevisions( Child.class, this.childId );

			// Child should have 2 revisions (creation and update)
			assertEquals( 2, revisions.size() );
		});
	}
}
