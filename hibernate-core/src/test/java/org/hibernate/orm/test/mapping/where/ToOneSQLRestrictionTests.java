/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.where;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.annotations.SQLRestriction;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = { ToOneSQLRestrictionTests.Parent.class, ToOneSQLRestrictionTests.Child.class })
@SessionFactory
public class ToOneSQLRestrictionTests {

	@Test
	public void testAssociationIdQueryAppliesRestriction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent deletedParent = new Parent( 1L, true );
			final Parent activeParent = new Parent( 2L, false );
			session.persist( deletedParent );
			session.persist( activeParent );
			session.persist( new Child( 1L, deletedParent ) );
			session.persist( new Child( 2L, activeParent ) );
		} );

		scope.inTransaction( session -> {
			// This query dereferences the to-one association id without an explicit join.
			// Hibernate may optimize that path to the child table foreign key column, but
			// the target entity still has an @SQLRestriction that must be applied.
			final List<Child> deletedParentChildren = findChildrenByParentId( session, 1L );
			final List<Child> activeParentChildren = findChildrenByParentId( session, 2L );

			assertThat( deletedParentChildren ).isEmpty();
			assertThat( activeParentChildren ).extracting( Child::getId ).containsExactly( 2L );
		} );
	}

	private List<Child> findChildrenByParentId(Session session, Long parentId) {
		return session.createQuery(
						"select child " +
								"from Child child " +
								"where child.parent.id = :parentId",
						Child.class
				)
				.setParameter( "parentId", parentId )
				.getResultList();
	}

	@Entity(name = "Parent")
	@SQLRestriction("deleted = false")
	public static class Parent {
		@Id
		private Long id;

		@Column(nullable = false)
		private boolean deleted;

		protected Parent() {
		}

		private Parent(Long id, boolean deleted) {
			this.id = id;
			this.deleted = deleted;
		}
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Parent parent;

		protected Child() {
		}

		private Child(Long id, Parent parent) {
			this.id = id;
			this.parent = parent;
		}

		public Long getId() {
			return id;
		}
	}
}
