/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.orphan.onetomany;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				EmbeddablePersistAndQueryInSameTransactionTest.Child.class,
				EmbeddablePersistAndQueryInSameTransactionTest.Parent.class,
				EmbeddablePersistAndQueryInSameTransactionTest.Dog.class
		}
)
@JiraKey(value = "HHH-16117")
class EmbeddablePersistAndQueryInSameTransactionTest {

	@Test
	public void testIt(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					Child child = new Child( "Fab" );

					Parent parent = new Parent( "1", "Paul", 40 );
					parent.addChild( child );

					Dog dog = new Dog( "Pluto" );
					parent.addDog( dog );

					entityManager.persist( parent );

					Parent found = entityManager.createQuery(
									"select e from Parent e where e.id = :id", Parent.class )
							.setParameter( "id", parent.getId() )
							.getSingleResult();

					assertThat( found.getId() ).isEqualTo( parent.getId() );
				} );
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@Basic
		private String name;

		public Child() {
		}

		public Child(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

	}

	@Entity(name = "Dog")
	public static class Dog {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@Basic
		private String name;

		public Dog() {
		}

		public Dog(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Embeddable
	public static class NestedEmbeddable {

		@OneToMany(orphanRemoval = true, cascade = CascadeType.ALL)
		private List<Dog> dogs = new ArrayList<>();

		public List<Dog> getDogs() {
			return dogs;
		}

		public void addDog(Dog dog) {
			dogs.add( dog );
		}
	}

	@Embeddable
	public static class EmbeddedData {

		private Integer age;

		@OneToMany(orphanRemoval = true, cascade = CascadeType.ALL)
		private List<Child> children = new ArrayList<>();

		@Embedded
		private NestedEmbeddable nestedEmbeddable = new NestedEmbeddable();

		public List<Child> getChildren() {
			return Collections.unmodifiableList( children );
		}

		public void addChild(Child child) {
			this.children.add( child );
		}

		public Integer getAge() {
			return age;
		}

		public void setAge(Integer age) {
			this.age = age;
		}

		public void addDog(Dog dog) {
			nestedEmbeddable.addDog( dog );
		}
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private String id;

		private String name;

		@Embedded
		private final EmbeddedData data = new EmbeddedData();

		public Parent() {
		}

		public Parent(String id, String name, Integer age) {
			this.id = id;
			this.name = name;
			data.setAge( age );
		}

		public String getId() {
			return id;
		}

		public void addChild(Child child) {
			data.addChild( child );
		}

		public void addDog(Dog dog) {
			data.addDog( dog );
		}
	}

}
