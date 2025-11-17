/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import org.hibernate.annotations.BatchSize;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.criteria.CriteriaQuery;

@Jpa(
		annotatedClasses = {
				BatchingInheritanceTest.Cheese.class,
				BatchingInheritanceTest.SmellyCheese.class,
				BatchingInheritanceTest.Smell.class,
		}
)
@JiraKey( value = "HHH-15694")
public class BatchingInheritanceTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope){
		scope.inTransaction(
			entityManager -> {
				Smell smell = new Smell(1l, "mold");
				SmellyCheese cheese = new SmellyCheese(2l, "Gorgonzola", smell);

				entityManager.persist( smell );
				entityManager.persist( cheese );
			}
		);
	}

	@Test
	public void testCriteria(EntityManagerFactoryScope scope){
		scope.inTransaction(
			entityManager -> {
				CriteriaQuery<Cheese> criteria = entityManager.getCriteriaBuilder().createQuery( Cheese.class);
				entityManager.createQuery(criteria.select(criteria.from(Cheese.class))).getResultList();
			}
		);
	}

	@MappedSuperclass()
	public static abstract class EntityBase {
		@Id
		private Long id;

		private String name;

		public EntityBase() {
		}

		public EntityBase(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "Cheese")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class Cheese extends EntityBase {

		public Cheese() {
		}

		public Cheese(Long id, String name) {
			super( id, name );
		}
	}

	@Entity(name = "Smell")
	@BatchSize(size = 10)
	public static class Smell extends EntityBase {

		public Smell() {
		}

		public Smell(Long id, String name) {
			super( id, name );
		}
	}

	@Entity(name = "SmellyCheese")
	public static class SmellyCheese extends Cheese {
		@JoinColumn(nullable = false)
		@ManyToOne
		private Smell smell;

		public SmellyCheese() {
		}

		public SmellyCheese(Long id, String name, Smell smell) {
			super(id, name);
			this.smell = smell;
		}


		public Smell getSmell() {
			return smell;
		}

		public void setSmell(Smell smell) {
			this.smell = smell;
		}
	}
}
