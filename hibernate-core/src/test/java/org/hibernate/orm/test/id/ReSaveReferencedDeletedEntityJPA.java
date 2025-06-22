/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.*;

import java.util.Map;

public class ReSaveReferencedDeletedEntityJPA extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Child.class, Parent.class };
	}

	@Override
	protected Map<Object, Object> buildSettings() {
		Map<Object, Object> settings = super.buildSettings();
		settings.put( AvailableSettings.USE_IDENTIFIER_ROLLBACK, "true"  );
		return settings;
	}

	@Test
	@ JiraKey("HHH-14416")
	public void testRefreshUnDeletedEntityWithReferencesJPA() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Parent parent = new Parent();
		parent.setId(1);

		Child child = new Child();
		child.setId(2);
		parent.setChild( child );

		em.persist( parent );

		em.flush();

		em.remove( parent );

		em.flush();

		em.detach( parent );

		em.persist( parent );

		em.flush();

		em.refresh( child );

		em.getTransaction().commit();
	}

	@Test
	@JiraKey("HHH-14416")
	public void testReSaveDeletedEntityWithReferencesJPA() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Parent parent = new Parent();
		parent.setId(1);

		Child child = new Child();
		child.setId(2);
		parent.setChild( child );

		em.persist( parent );

		parent.setChild( null );
		em.remove( child );

		em.persist( child );

		em.getTransaction().commit();
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Integer id;

		@OneToOne(cascade = CascadeType.ALL)
		private Child child;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Child getChild() {
			return child;
		}

		public void setChild(Child child) {
			this.child = child;
		}
	}
}
