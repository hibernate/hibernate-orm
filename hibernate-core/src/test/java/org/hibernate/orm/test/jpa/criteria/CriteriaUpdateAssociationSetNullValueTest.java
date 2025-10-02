/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import org.hibernate.SessionFactory;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@Jpa(
		annotatedClasses = {
				CriteriaUpdateAssociationSetNullValueTest.Parent.class,
				CriteriaUpdateAssociationSetNullValueTest.Child.class}
)
@JiraKey("HHH-19085")
public class CriteriaUpdateAssociationSetNullValueTest {

	private static final Long PARENT_ID = 1L;

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					em.persist( new Parent( PARENT_ID, "Lionello", new Child( 2L, "Andrea" ) ) );
				}

		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().unwrap( SessionFactory.class ).getSchemaManager().truncateMappedObjects();
	}

	@Test
	void testUpdateSetAssociationToNullValue(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					CriteriaBuilder cb = em.getCriteriaBuilder();
					CriteriaUpdate<Parent> update = cb.createCriteriaUpdate( Parent.class );
					Root<Parent> msg = update.from( Parent.class );
					update.set( msg.get( "child" ), (Child) null );
					em.createQuery( update ).executeUpdate();
				}
		);

		scope.inTransaction(
				em -> {
					Parent parent = em.find( Parent.class, PARENT_ID );
					assertThat( parent ).isNotNull();
					assertThat( parent.getName() ).isNotNull();
					assertThat( parent.getChild() ).isNull();
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		private Long id;

		@Basic
		private String name;

		@ManyToOne(cascade = CascadeType.PERSIST)
		private Child child;

		public Parent() {
		}

		public Parent(Long id, String name, Child child) {
			this.id = id;
			this.name = name;
			this.child = child;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Child getChild() {
			return child;
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		private Long id;

		private String name;

		public Child() {
		}

		public Child(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

}
