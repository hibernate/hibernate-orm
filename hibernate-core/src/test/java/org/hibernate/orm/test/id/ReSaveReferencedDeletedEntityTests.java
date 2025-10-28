/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import org.hibernate.dialect.HANADialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.AvailableSettings.USE_IDENTIFIER_ROLLBACK;

@SuppressWarnings("JUnitMalformedDeclaration")
@SkipForDialect( dialectClass = HANADialect.class,
		reason = "The INSERT statement for table [Child] contains no column, and this is not supported")
@ServiceRegistry(settings = @Setting(name=USE_IDENTIFIER_ROLLBACK, value = "true"))
@DomainModel(annotatedClasses = {
		ReSaveReferencedDeletedEntityTests.Child.class,
		ReSaveReferencedDeletedEntityTests.Parent.class
})
@SessionFactory
public class ReSaveReferencedDeletedEntityTests {
	@AfterEach
	public void dropTestData(SessionFactoryScope factoryScope) throws Exception {
		factoryScope.dropData();
	}

	@Test
	@JiraKey("HHH-14416")
	public void testReSaveDeletedEntity(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
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
	public void testReSaveDeletedEntityWithDetach(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
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
