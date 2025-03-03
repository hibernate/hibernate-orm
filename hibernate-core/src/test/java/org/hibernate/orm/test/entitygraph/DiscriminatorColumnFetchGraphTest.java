/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.jpa.SpecHints;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static jakarta.persistence.GenerationType.AUTO;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				DiscriminatorColumnFetchGraphTest.Dog.class,
				DiscriminatorColumnFetchGraphTest.Animal.class,
				DiscriminatorColumnFetchGraphTest.Behaviour.class
		}
)
@JiraKey(value = "HHH-15622")
public class DiscriminatorColumnFetchGraphTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Behaviour behaviour = new Behaviour();
					entityManager.persist( behaviour );

					Dog dog = new Dog( behaviour, 10 );
					entityManager.persist( dog );
				}
		);
	}

	@Test
	public void fetchGraphTest(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<Behaviour> query = cb.createQuery( Behaviour.class );

					Root<Behaviour> behaviourRoot = query.from( Behaviour.class );

					List<Behaviour> behaviours = entityManager.createQuery( query.select( behaviourRoot ) )
							.setHint(
									SpecHints.HINT_SPEC_FETCH_GRAPH,
									entityManager.getEntityGraph( "Behaviour.animal" )
							)
							.getResultList();
					assertThat( behaviours.size() ).isEqualTo( 1 );
					Behaviour behaviour = behaviours.get( 0 );
					assertThat( Hibernate.isPropertyInitialized( behaviour, "animal" ) );
				}
		);
	}

	@Entity(name = "Animal")
	@DiscriminatorColumn
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static abstract class Animal {

		@Id
		@GeneratedValue(strategy = AUTO)
		private Long id;

		@MapsId
		@OneToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(name = "behaviour_id", nullable = false, updatable = false)
		private Behaviour behaviour;

		protected Animal() {
		}

		public Animal(Behaviour behaviour) {
			this.behaviour = behaviour;
			behaviour.setAnimal( this );
		}
	}

	@Entity(name = "Dog")
	public static class Dog extends Animal {

		protected Dog() {
		}

		public Dog(Behaviour behaviour, Integer age) {
			super( behaviour );
			this.age = age;
		}

		private Integer age;
	}

	@NamedEntityGraph(
			name = "Behaviour.animal",
			attributeNodes = @NamedAttributeNode("animal")
	)
	@Entity(name = "Behaviour")
	public static class Behaviour {

		@Id
		@GeneratedValue(strategy = AUTO)
		private Long id;

		@OneToOne(mappedBy = "behaviour", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
		private Animal animal;

		public Behaviour() {
		}

		public void setAnimal(Animal animal) {
			this.animal = animal;
		}
	}

}
