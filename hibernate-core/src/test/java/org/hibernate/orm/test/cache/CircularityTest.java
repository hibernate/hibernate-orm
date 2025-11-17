/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				CircularityTest.GrandParent.class,
				CircularityTest.Parent.class,
				CircularityTest.Child.class
		},
		integrationSettings = @Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true")
)
@JiraKey(value = "HHH-16197")
public class CircularityTest {

	private static final String CHILD_ID = "c1";

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Child c1 = new Child( CHILD_ID );
					Parent p1 = new Parent( "p1", c1 );
					GrandParent gp1 = new GrandParent("gp1", p1);

					entityManager.persist( c1 );
					entityManager.persist( p1 );
					entityManager.persist( gp1 );
				}
		);
	}

	@Test
	public void testSelection(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Child child = entityManager.createQuery(
							"SELECT child from Child child WHERE child.id = :id",
							Child.class
					).setParameter( "id", CHILD_ID ).getSingleResult();
					assertThat( child ).isNotNull();
					assertThat( child ).isEqualTo( child.getParent().getChild() );
					assertThat( child.getParent().getGrandParent().getChild() ).isEqualTo( child.getParent() );
				}
		);
	}

	@Entity(name = "Child")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "hibernate.test")
	public static class Child {

		@Id
		private String id;

		private String name;

		@ManyToOne
		private Parent parent;

		public Child() {
		}

		public Child(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public Parent getParent() {
			return parent;
		}

	}

	@Entity(name = "Parent")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "hibernate.test")
	public static class Parent {

		@Id
		private String id;

		private String name;

		@OneToOne
		private Child child;

		@OneToOne
		private GrandParent grandParent;

		public Parent(String id, Child child) {
			this.id = id;
			this.child = child;
			this.child.parent = this;
		}

		public Parent() {
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Child getChild() {
			return child;
		}

		public void setChild(Child child) {
			this.child = child;
		}

		public GrandParent getGrandParent() {
			return grandParent;
		}
	}

	@Entity(name = "GrandParent")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "hibernate.test")
	public static class GrandParent {

		@Id
		private String id;

		private String name;

		@OneToOne
		private Parent child;

		public GrandParent(String id, Parent child) {
			this.id = id;
			this.child = child;
			this.child.grandParent = this;
		}

		public GrandParent() {
		}

		public String getId() {
			return id;
		}

		public Parent getChild() {
			return child;
		}
	}
}
