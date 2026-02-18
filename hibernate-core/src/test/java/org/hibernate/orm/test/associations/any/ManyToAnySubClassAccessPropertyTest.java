/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations.any;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ManyToAny;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import static jakarta.persistence.InheritanceType.SINGLE_TABLE;
import static org.hibernate.annotations.CascadeType.ALL;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		annotatedClasses = {
				ManyToAnySubClassAccessPropertyTest.Person.class,
				ManyToAnySubClassAccessPropertyTest.Animal.class,
				ManyToAnySubClassAccessPropertyTest.Cat.class,
				ManyToAnySubClassAccessPropertyTest.Dog.class,
				ManyToAnySubClassAccessPropertyTest.DogHandler.class,
				ManyToAnySubClassAccessPropertyTest.Military.class,
		}
)
@JiraKey("HHH-17871")
class ManyToAnySubClassAccessPropertyTest {

	@Test
	void testManyToAnyThatReferencedAMappedSuperclassDefinedInMultipleSubClass(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Dog dog = new Dog();
					dog.getOwners().add( new DogHandler() );
					entityManager.persist( dog );
					assertTrue( Hibernate.isInitialized( dog ) );
				}
		);
	}

	@Entity
	@Inheritance(strategy = SINGLE_TABLE)
	@DiscriminatorColumn(name = "ROLE_COL", length = 100)
	@Table(name = "TANIMAL")
	public static class Animal {

		@Id
		@GeneratedValue
		public Integer id;

		@Transient
		private Set<Person> owners = new HashSet<>();

		public Set<Person> getOwners() {
			return owners;
		}

		public void setOwners(Set<Person> owners) {
			this.owners = owners;
		}
	}

	@Entity
	@DiscriminatorValue("DOG")
	public static class Dog extends Animal {

		@Access(AccessType.PROPERTY)
		@ManyToAny
		@AnyKeyJavaClass(Integer.class)
		@Cascade(ALL)
		@Column(name = "ROLE_COL")
		@JoinTable(name = "DOG_OWNER", joinColumns = @JoinColumn(name = "SOURCE"), inverseJoinColumns = @JoinColumn(name = "DEST"))
		@Override
		public Set<Person> getOwners() {
			return super.getOwners();
		}
	}

	@Entity
	@DiscriminatorValue("CAT")
	public static class Cat extends Animal {

		@Access(AccessType.PROPERTY)
		@ManyToAny
		@AnyKeyJavaClass(Integer.class)
		@Cascade(ALL)
		@Column(name = "ROLE_COL")
		@JoinTable(name = "DOG_OWNER", joinColumns = @JoinColumn(name = "SOURCE"), inverseJoinColumns = @JoinColumn(name = "DEST"))
		@Override
		public Set<Person> getOwners() {
			return super.getOwners();
		}
	}

	@MappedSuperclass
	public static class Person {

		@Id
		@GeneratedValue
		private Integer id;
	}

	@Entity(name = "DogHandler")
	public static class DogHandler extends Person {
	}

	@Entity(name = "Military")
	public static class Military extends Person {
	}
}
