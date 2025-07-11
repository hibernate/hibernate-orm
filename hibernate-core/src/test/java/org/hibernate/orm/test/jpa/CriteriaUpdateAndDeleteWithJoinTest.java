/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				CriteriaUpdateAndDeleteWithJoinTest.Parent.class,
				CriteriaUpdateAndDeleteWithJoinTest.Child.class
		}
)
@JiraKey( "HHH-19579" )
public class CriteriaUpdateAndDeleteWithJoinTest {
	private static final String CHILD_CODE = "123";

	@BeforeEach
	public void setup(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Child child = new Child( 1L, CHILD_CODE );
					Parent parent = new Parent( 2L, "456", child );
					entityManager.persist( parent );
				}
		);
	}

	@AfterEach
	public void teardown(EntityManagerFactoryScope scope) {
		scope.releaseEntityManagerFactory();
	}

	@Test
	public void testUpdate(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaUpdate<Parent> update = cb.createCriteriaUpdate(Parent.class);

					Root<Parent> root = update.from(Parent.class);
					Join<Parent,Child> joinColor = root.join("child", JoinType.INNER);

					update.set(root.get("code"), "l1s2");
					update.where(cb.equal(joinColor.get("code"), cb.parameter(String.class, "code")));

					int count = entityManager.createQuery(update).setParameter("code", CHILD_CODE).executeUpdate();
					assertThat(count).isEqualTo(1);
				}
		);
	}

	@Test
	public void testDelete(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaDelete<Parent> delete = cb.createCriteriaDelete(Parent.class);

					Root<Parent> root = delete.from(Parent.class);
					Join<Parent,Child> joinColor = root.join("child", JoinType.INNER);

					delete.where(cb.equal(joinColor.get("code"), cb.parameter(String.class, "code")));

					int count = entityManager.createQuery(delete).setParameter("code", CHILD_CODE).executeUpdate();
					assertThat(count).isEqualTo(1);
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		private Long id;

		@Column(name = "code")
		private String code;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@JoinColumn(name = "color_id")
		private Child child;

		public Parent() {
		}

		public Parent(Long id, String code, Child child) {
			this.id = id;
			this.code = code;
			this.child = child;
		}

		public Long getId() {
			return id;
		}

		public String getCode() {
			return code;
		}

		public Child getChild() {
			return child;
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		private Long id;

		@Column(name = "code")
		private String code;

		public Child() {
		}

		public Child(Long id, String code) {
			this.id = id;
			this.code = code;
		}

		public Long getId() {
			return id;
		}

		public String getCode() {
			return code;
		}
	}
}
