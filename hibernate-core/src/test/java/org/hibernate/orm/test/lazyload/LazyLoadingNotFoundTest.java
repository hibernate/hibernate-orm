/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lazyload;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.LazyInitializationException;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Nikolay Golubev
 */
@DomainModel(
		annotatedClasses = {
				LazyLoadingNotFoundTest.Parent.class,
				LazyLoadingNotFoundTest.Child.class
		}
)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = Environment.ENABLE_LAZY_LOAD_NO_TRANS, value = "true"))
public class LazyLoadingNotFoundTest {

	@Test
	@JiraKey(value = "HHH-11179")
	public void testNonExistentLazyInitOutsideTransaction(SessionFactoryScope scope) {
		Child loadedChild = scope.fromTransaction(
				session -> session.getReference( Child.class, -1L )
		);

		try {
			loadedChild.getParent();
			fail( "lazy init did not fail on non-existent proxy" );
		}
		catch (LazyInitializationException e) {
			assertNotNull( e.getMessage() );
		}
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToMany
		private List<Child> children = new ArrayList<>();

		public Long getId() {
			return id;
		}

		public List<Child> getChildren() {
			return children;
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private Parent parent;

		public Long getId() {
			return id;
		}

		public Parent getParent() {
			return parent;
		}
	}

}
