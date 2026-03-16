/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vincent Bouthinon
 */
@Jpa(annotatedClasses = {OneToManyWithAnyAndSubClassTest.Animal.class,
		OneToManyWithAnyAndSubClassTest.Dog.class,
		OneToManyWithAnyAndSubClassTest.Cat.class,
		OneToManyWithAnyAndSubClassTest.Characteristic.class,
})
@JiraKey("HHH-20257")
class OneToManyWithAnyAndSubClassTest {

	@Test
	void testLogEntityWithAnyKeyJavaClassAsString(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {

					Dog dog = new Dog();
					dog.setName("Rex");
					entityManager.persist(dog);

					Characteristic c = new Characteristic();
					c.setDescription("friendly");
					c.setTarget(dog);
					entityManager.persist(c);

					entityManager.flush();
					entityManager.clear();

					Dog newDog = entityManager.find( Dog.class, dog.getId() );

					assertThat(newDog.getCharacteristics()).hasSize(1);
				}
		);
	}

	@Entity(name = "animal")
	@DiscriminatorValue("Animal")
	@Table(name = "T_ANIMAL")
	public static class Animal {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToMany(fetch = FetchType.EAGER, mappedBy = "target")
		private Set<Characteristic> characteristics = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<Characteristic> getCharacteristics() {
			return characteristics;
		}

		public void setCharacteristics(Set<Characteristic> characteristics) {
			this.characteristics = characteristics;
		}
	}

	@Entity(name = "dog")
	@DiscriminatorValue("Dog")
	public static class Dog extends Animal {
	}

	@Entity(name = "cat")
	@DiscriminatorValue("Cat")
	public class Cat extends Animal {
	}

	@Entity
	@Table(name = "T_CHARACTERISTIC")
	public class Characteristic {

		@Id
		@GeneratedValue
		private Long id;

		public String description;

		@Any(fetch = FetchType.LAZY)
		@AnyKeyJavaClass(Long.class)
		@JoinColumn(name = "TARGET_ID")
		@Column(name = "TARGET_ROLE", length = 80)
		private Animal target;

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public Animal getTarget() {
			return target;
		}

		public void setTarget(Animal target) {
			this.target = target;
		}
	}
}
