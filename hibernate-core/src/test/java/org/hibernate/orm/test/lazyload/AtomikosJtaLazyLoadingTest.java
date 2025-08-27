/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lazyload;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		annotatedClasses = {
				AtomikosJtaLazyLoadingTest.Parent.class,
				AtomikosJtaLazyLoadingTest.Child.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(
						name = Environment.ENABLE_LAZY_LOAD_NO_TRANS, value = "true"
				),
				@Setting(
						name = AvailableSettings.JTA_PLATFORM, value = "org.hibernate.testing.jta.TestingJtaPlatformImpl"
				),
				@Setting(
						name = AvailableSettings.CONNECTION_PROVIDER, value = "org.hibernate.testing.jta.JtaAwareConnectionProviderImpl"
				),
				@Setting(
						name = "javax.persistence.transactionType", value = "JTA"
				),
				@Setting(
						name = AvailableSettings.JTA_PLATFORM, value = "Atomikos"
				)
		}
)
public class AtomikosJtaLazyLoadingTest {

	private static final int CHILDREN_SIZE = 3;
	private Long parentID;
	private Long lastChildID;

	@BeforeEach
	public void prepareTest(SessionFactoryScope scope)
			throws Exception {
		scope.inTransaction( session -> {
			Parent p = new Parent();
			for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
				final Child child = new Child(p);
				session.persist( child );
				lastChildID = child.getId();
			}
			session.persist( p );
			parentID = p.getId();
		} );
	}

	@Test
	@JiraKey(value = "HHH-7971")
	public void testLazyCollectionLoadingAfterEndTransaction(SessionFactoryScope scope) {
		Parent loadedParent = scope.fromTransaction(
				session ->
						session.getReference( Parent.class, parentID )
		);

		assertFalse( Hibernate.isInitialized( loadedParent.getChildren() ) );

		int i = 0;
		for ( Child child : loadedParent.getChildren() ) {
			i++;
			assertNotNull( child );
		}

		assertEquals( CHILDREN_SIZE, i );

		Child loadedChild = scope.fromTransaction(
				session ->
						session.getReference( Child.class, lastChildID )
		);

		Parent p = loadedChild.getParent();
		int j = 0;
		for ( Child child : p.getChildren() ) {
			j++;
			assertNotNull( child );
		}

		assertEquals( CHILDREN_SIZE, j );
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		@GeneratedValue
		private Long id;

		private String name;



		@OneToMany(cascade = CascadeType.PERSIST)
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

		public Child() {
		}

		public Child(Parent parent) {
			this.parent = parent;
			parent.getChildren().add( this );
		}

		public Long getId() {
			return id;
		}

		public Parent getParent() {
			return parent;
		}
	}

}
