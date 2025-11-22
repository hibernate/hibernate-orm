/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.query;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests that {@link RelationTargetAuditMode#NOT_AUDITED} works correctly when loading audit history.
 * When a relation is marked with NOT_AUDITED mode, the target entity is loaded from the current
 * table rather than from audit tables, so changes to the target entity are visible when querying
 * historical revisions.
 *
 * @author Minjae Seon
 */
@JiraKey(value = "HHH-19861")
public class RelationTargetNotAuditedTest extends BaseEnversJPAFunctionalTestCase {

	private Long childId;
	private Long parentId;

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

	@Entity(name = "Parent")
	@Table(name = "Parent")
	@Audited
	public static class Parent {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		private String content;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "child_id", nullable = false)
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

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Parent.class, Child.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		// Revision 1: Create child and parent
		em.getTransaction().begin();
		Child child = new Child("Child 1");
		em.persist(child);
		Parent parent = new Parent("Initial content", child);
		em.persist(parent);
		em.getTransaction().commit();

		childId = child.getId();
		parentId = parent.getId();

		// Revision 2: Update parent content
		em.getTransaction().begin();
		parent = em.find(Parent.class, parentId);
		parent.setContent("Updated content");
		em.getTransaction().commit();

		// Revision 3: Update child name (should not create audit record for parent)
		em.getTransaction().begin();
		child = em.find(Child.class, childId);
		child.setName("Child 1 Updated");
		em.getTransaction().commit();
	}

	@Test
	public void testLoadParentAtRevision1() {
		AuditReader auditReader = getAuditReader();
		Parent parentRev1 = auditReader.find(Parent.class, parentId, 1);

		assertNotNull("Parent at revision 1 should not be null", parentRev1);
		assertEquals("Initial content", parentRev1.getContent());
		assertNotNull("Child reference should not be null", parentRev1.getChild());
		assertEquals(childId, parentRev1.getChild().getId());
		// Child should be loaded from current table, so it should have the updated name
		assertEquals("Child 1 Updated", parentRev1.getChild().getName());
	}

	@Test
	public void testLoadParentAtRevision2() {
		AuditReader auditReader = getAuditReader();
		Parent parentRev2 = auditReader.find(Parent.class, parentId, 2);

		assertNotNull("Parent at revision 2 should not be null", parentRev2);
		assertEquals("Updated content", parentRev2.getContent());
		assertNotNull("Child reference should not be null", parentRev2.getChild());
		assertEquals(childId, parentRev2.getChild().getId());
		// Child should be loaded from current table
		assertEquals("Child 1 Updated", parentRev2.getChild().getName());
	}

	@Test
	public void testQueryParentRevisions() {
		AuditReader auditReader = getAuditReader();
		List<Number> revisions = auditReader.getRevisions(Parent.class, parentId);

		// Parent should have 2 revisions (creation and update)
		assertEquals("Parent should have 2 revisions", 2, revisions.size());
	}

	@Test
	public void testQueryChildRevisions() {
		AuditReader auditReader = getAuditReader();
		List<Number> revisions = auditReader.getRevisions(Child.class, childId);

		// Child should have 2 revisions (creation and update)
		assertEquals("Child should have 2 revisions", 2, revisions.size());
	}
}
