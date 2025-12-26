/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.RelationTargetNotFoundAction;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests that {@link org.hibernate.envers.RelationTargetNotFoundAction} works correctly when combined with
 * {@link RelationTargetAuditMode#NOT_AUDITED}. When the target entity is deleted, the behavior depends on
 * the configured action: ERROR throws {@link jakarta.persistence.EntityNotFoundException}, while IGNORE
 * returns null.
 *
 * To allow deletion of Child entities without foreign key constraint violations, the relationship uses
 * {@link jakarta.persistence.ConstraintMode#NO_CONSTRAINT} to prevent database foreign key creation.
 *
 * @author Minjae Seon
 */
@JiraKey(value = "HHH-19861")
@Jpa(annotatedClasses = {
		NotAuditedRelationTargetNotFoundActionTest.ParentWithError.class,
		NotAuditedRelationTargetNotFoundActionTest.ParentWithIgnore.class,
		NotAuditedRelationTargetNotFoundActionTest.Child.class
})
@EnversTest
public class NotAuditedRelationTargetNotFoundActionTest {
	private Long childForErrorId;
	private Long childForIgnoreId;
	private Long parentWithErrorId;
	private Long parentWithIgnoreId;

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

	@Entity(name = "ParentWithError")
	@Audited
	public static class ParentWithError {
		@Id
		@GeneratedValue
		private Long id;

		private String content;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "child_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
		@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, targetNotFoundAction = RelationTargetNotFoundAction.ERROR)
		private Child child;

		public ParentWithError() {
		}

		public ParentWithError(String content, Child child) {
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

	@Entity(name = "ParentWithIgnore")
	@Audited
	public static class ParentWithIgnore {
		@Id
		@GeneratedValue
		private Long id;

		private String content;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "child_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
		@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED, targetNotFoundAction = RelationTargetNotFoundAction.IGNORE)
		private Child child;

		public ParentWithIgnore() {
		}

		public ParentWithIgnore(String content, Child child) {
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
		// Revision 1: Create children and parents
		scope.inTransaction( em -> {
			final Child childForError = new Child( "Child for Error" );
			em.persist( childForError );

			final Child childForIgnore = new Child( "Child for Ignore" );
			em.persist( childForIgnore );

			final ParentWithError parentWithError = new ParentWithError( "Initial content with error", childForError );
			em.persist( parentWithError );

			final ParentWithIgnore parentWithIgnore = new ParentWithIgnore( "Initial content with ignore", childForIgnore );
			em.persist( parentWithIgnore );

			this.childForErrorId = childForError.getId();
			this.childForIgnoreId = childForIgnore.getId();
			this.parentWithErrorId = parentWithError.getId();
			this.parentWithIgnoreId = parentWithIgnore.getId();
		});
	}

	@Test
	public void testLoadParentWithErrorAfterChildDeleted(EntityManagerFactoryScope scope) {
		// Delete the child entity that ParentWithError references
		scope.inTransaction( em -> {
			final Child childForError = em.find( Child.class, this.childForErrorId );
			em.remove( childForError );
		});

		// Try to load parent's audit history with ERROR action
		// This should throw EntityNotFoundException
		scope.inTransaction( em -> {
			final ParentWithError parent = AuditReaderFactory.get( em ).find( ParentWithError.class, this.parentWithErrorId, 1 );

			assertNotNull( parent );
			assertEquals( "Initial content with error", parent.getContent() );

			// Try to access the child - should throw EntityNotFoundException
			try {
				Child childRef = parent.getChild();
				// Access a property to trigger lazy loading
				childRef.getName();
				fail( "Should have thrown EntityNotFoundException" );
			}
			catch ( EntityNotFoundException e ) {
				// Expected behavior
			}
		});
	}

	@Test
	public void testLoadParentWithIgnoreAfterChildDeleted(EntityManagerFactoryScope scope) {
		// Delete the child entity that ParentWithIgnore references
		scope.inTransaction( em -> {
			final Child childForIgnore = em.find( Child.class, this.childForIgnoreId );
			em.remove( childForIgnore );
		});

		// Try to load parent's audit history with IGNORE action
		// This should not throw EntityNotFoundException and return null
		scope.inTransaction( em -> {
			final ParentWithIgnore parent = AuditReaderFactory.get( em ).find( ParentWithIgnore.class, this.parentWithIgnoreId, 1 );
			assertNotNull( parent );
			assertEquals( "Initial content with ignore", parent.getContent() );

			// Try to access the child - should return null
			Child childRef = parent.getChild();
			assertNull( childRef );
		});
	}
}
