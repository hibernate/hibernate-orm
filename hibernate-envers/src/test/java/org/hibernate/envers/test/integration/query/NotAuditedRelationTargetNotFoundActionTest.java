/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.query;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.RelationTargetNotFoundAction;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

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
public class NotAuditedRelationTargetNotFoundActionTest extends BaseEnversJPAFunctionalTestCase {

	private Long childForErrorId;
	private Long childForIgnoreId;
	private Long parentWithErrorId;
	private Long parentWithIgnoreId;

	@Entity(name = "Child")
	@Table(name = "Child")
	@Audited
	public static class Child {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
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
	@Table(name = "ParentWithError")
	@Audited
	public static class ParentWithError {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
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
	@Table(name = "ParentWithIgnore")
	@Audited
	public static class ParentWithIgnore {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
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

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ ParentWithError.class, ParentWithIgnore.class, Child.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1: Create children and parents
		em.getTransaction().begin();
		Child childForError = new Child("Child for Error");
		em.persist(childForError);
		Child childForIgnore = new Child("Child for Ignore");
		em.persist(childForIgnore);

		ParentWithError parentWithError = new ParentWithError("Initial content with error", childForError);
		em.persist(parentWithError);
		ParentWithIgnore parentWithIgnore = new ParentWithIgnore("Initial content with ignore", childForIgnore);
		em.persist(parentWithIgnore);
		em.getTransaction().commit();

		childForErrorId = childForError.getId();
		childForIgnoreId = childForIgnore.getId();
		parentWithErrorId = parentWithError.getId();
		parentWithIgnoreId = parentWithIgnore.getId();
	}

	@Test
	public void testLoadParentWithErrorAfterChildDeleted() {
		EntityManager em = getEntityManager();

		// Delete the child entity that ParentWithError references
		em.getTransaction().begin();
		Child childForError = em.find(Child.class, childForErrorId);
		em.remove(childForError);
		em.getTransaction().commit();

		// Now try to load parent's audit history with ERROR action
		// This should throw EntityNotFoundException
		AuditReader auditReader = getAuditReader();
		ParentWithError parentRev1 = auditReader.find(ParentWithError.class, parentWithErrorId, 1);

		assertNotNull("Parent at revision 1 should not be null", parentRev1);
		assertEquals("Initial content with error", parentRev1.getContent());

		// Try to access the child - should throw EntityNotFoundException
		try {
			Child childRef = parentRev1.getChild();
			// Access a property to trigger lazy loading
			childRef.getName();
			fail("Should have thrown EntityNotFoundException");
		}
		catch (EntityNotFoundException e) {
			// Expected behavior
		}
	}

	@Test
	public void testLoadParentWithIgnoreAfterChildDeleted() {
		EntityManager em = getEntityManager();

		// Delete the child entity that ParentWithIgnore references
		em.getTransaction().begin();
		Child childForIgnore = em.find(Child.class, childForIgnoreId);
		em.remove(childForIgnore);
		em.getTransaction().commit();

		// Now try to load parent's audit history with IGNORE action
		// This should not throw EntityNotFoundException and return null
		AuditReader auditReader = getAuditReader();
		ParentWithIgnore parentRev1 = auditReader.find(ParentWithIgnore.class, parentWithIgnoreId, 1);

		assertNotNull("Parent at revision 1 should not be null", parentRev1);
		assertEquals("Initial content with ignore", parentRev1.getContent());

		// Try to access the child - should return null
		Child childRef = parentRev1.getChild();
		assertNull("Child reference should be null when child is deleted and IGNORE action is set", childRef);
	}
}
