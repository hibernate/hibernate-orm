/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.proxy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The bidirectional collection-change revision of a related entity must be mapped from the
 * entity's real state even when the session holds the related entity as an uninitialized
 * proxy. The proxy is seeded here by the child's lazy to-one during {@code em.find}; deleting
 * the child then fires the collection-remove event whose element resolves to that proxy.
 * Without unwrapping, the audit row is mapped off the proxy wrapper and every audited
 * property (including the version, when audited) is written as null.
 *
 * <p>Reported at
 * <a href="https://discourse.hibernate.org/t/12382">discourse.hibernate.org/t/12382</a>.
 *
 * @author Apoorva Manjunath
 */
@JiraKey("HHH-20779")
@Jpa(annotatedClasses = {
		ProxiedRelatedEntityCollectionChangeTest.Parent.class,
		ProxiedRelatedEntityCollectionChangeTest.Child.class
}, integrationSettings = {
		@Setting(name = EnversSettings.DO_NOT_AUDIT_OPTIMISTIC_LOCKING_FIELD, value = "false")
})
@EnversTest
public class ProxiedRelatedEntityCollectionChangeTest {

	private Long parentId;
	private Long childId;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// Revision 1: the parent is referenced by the child twice - via the lazy
		// to-one (proxy seeder) and via the owned many-to-many
		scope.inTransaction( em -> {
			Parent parent = new Parent( "parent" );
			Child child = new Child( "child" );
			child.setMainParent( parent );
			child.getLinkedParents().add( parent );
			em.persist( parent );
			em.persist( child );
			parentId = parent.getId();
			childId = child.getId();
		} );

		// Revision 2: loading the child hydrates mainParent as an uninitialized proxy;
		// the delete's collection-remove event resolves the linkedParents element to it
		scope.inTransaction( em -> em.remove( em.find( Child.class, childId ) ) );
	}

	@Test
	public void testCollectionChangeRevisionKeepsRelatedEntityData(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			AuditReader auditReader = AuditReaderFactory.get( em );
			List<Number> revisions = auditReader.getRevisions( Parent.class, parentId );
			assertEquals( 2, revisions.size() );

			Parent atModRevision = auditReader.find( Parent.class, parentId, revisions.get( revisions.size() - 1 ) );
			assertNotNull( atModRevision );
			assertEquals( "parent", atModRevision.getName() );
			assertNotNull( atModRevision.getVersion() );
		} );
	}

	@Entity(name = "Parent")
	@Audited
	public static class Parent {
		@Id
		@GeneratedValue
		private Long id;

		@Version
		private Integer version;

		private String name;

		@ManyToMany(mappedBy = "linkedParents", fetch = FetchType.LAZY)
		private Set<Child> children = new HashSet<>();

		public Parent() {
		}

		public Parent(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public Integer getVersion() {
			return version;
		}

		public String getName() {
			return name;
		}

		public Set<Child> getChildren() {
			return children;
		}
	}

	@Entity(name = "Child")
	@Audited
	public static class Child {
		@Id
		@GeneratedValue
		private Long id;

		@Version
		private Integer version;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private Parent mainParent;

		@ManyToMany(fetch = FetchType.LAZY)
		private Set<Parent> linkedParents = new HashSet<>();

		public Child() {
		}

		public Child(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Parent getMainParent() {
			return mainParent;
		}

		public void setMainParent(Parent mainParent) {
			this.mainParent = mainParent;
		}

		public Set<Parent> getLinkedParents() {
			return linkedParents;
		}
	}
}
