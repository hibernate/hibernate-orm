/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.HANADialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.Test;

import jakarta.persistence.*;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

@SkipForDialect( dialectClass = HANADialect.class,
		reason = "The INSERT statement for table [Child] contains no column, and this is not supported")
public class ReSaveReferencedDeletedEntity extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Child.class, Parent.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.USE_IDENTIFIER_ROLLBACK, "true" );
	}

	@Test
	@JiraKey("HHH-14416")
	public void testReSaveDeletedEntity() {
		doInHibernate( this::sessionFactory, session -> {
			Parent parent = new Parent();

			Child child = new Child();
			parent.setChild( child );

			session.persist( parent );

			parent.setChild( null );
			session.remove(child);

			session.flush();

			parent.setChild( child );
			session.persist(child);
		} );
	}

	@Test
	@JiraKey("HHH-14416")
	public void testReSaveDeletedEntityWithDetach() {
		doInHibernate( this::sessionFactory, session -> {
			Parent parent = new Parent();

			Child child = new Child();
			parent.setChild( child );

			session.persist( parent );

			parent.setChild( null );
			session.remove(child);

			session.flush();
			session.detach(child);

			parent.setChild( child );
			session.persist(child);
		} );
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		@GeneratedValue
		private Integer id;
		private String name;

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
		@GeneratedValue
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
