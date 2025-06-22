/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@Jpa( annotatedClasses = { EagerManyToOneTest.Child.class, EagerManyToOneTest.Parent.class } )
@JiraKey("HHH-15902")
public class EagerManyToOneTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Parent parent = new Parent( 1l, "Fab" );
					Child child = new Child( 2l, new Date() );
					parent.addChild( child );
					entityManager.persist( parent );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testFindParent(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Parent parent = entityManager.find( Parent.class, 1 );
					assertThat( parent ).isNotNull();
					// the Child filter should apply
					assertThat( parent.children ).isEmpty();
				}
		);
		scope.inTransaction(
				entityManager -> {
					Child child = entityManager.find( Child.class, 2 );
					assertThat( child ).isNull();
				}
		);
		scope.inTransaction(
				entityManager -> {
					List<Child> children = entityManager.createQuery( "select c from Child c", Child.class )
							.getResultList();
					assertThat( children.size() ).isEqualTo( 0 );
				}
		);
	}

	@Entity(name = "Child")
	@Table(name = "children")
	@SQLRestriction("deleted_at IS NULL")
	public static class Child {
		@Id
		private Long id;

		@JoinColumn(name = "parent_id", nullable = false, updatable = false)
		@ManyToOne(fetch = FetchType.EAGER)
		private Parent parent;

		@Column(name = "deleted_at")
		private Date deletedAt;

		public Child() {
		}

		public Child(Long id, Date deletedAt) {
			this.id = id;
			this.deletedAt = deletedAt;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "Parent")
	@Table(name = "parents")
	public static class Parent {
		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
		private List<Child> children = new ArrayList<>();

		public Parent() {
		}

		public Parent(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public void addChild(Child child) {
			children.add( child );
			child.setParent( this );
		}

	}
}
