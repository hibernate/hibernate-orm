/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.embeddable;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@DomainModel(
		annotatedClasses = {
				InheritedPropertyTest.Animal.class,
				InheritedPropertyTest.Cat.class,
				InheritedPropertyTest.Dog.class,
				InheritedPropertyTest.Fish.class,
				InheritedPropertyTest.Mammal.class,
				InheritedPropertyTest.Owner.class
		}
)
@SessionFactory
public class InheritedPropertyTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var cat = new Cat();
			cat.age = 7;
			cat.name = "Jones";
			cat.mother = "Kitty";
			final var owner = new Owner();
			owner.id = 1L;
			owner.pet = cat;
			session.persist( owner );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Owner" ).executeUpdate() );
	}

	@Test
	void testInheritedProperty(SessionFactoryScope scope) {
		assertDoesNotThrow( () -> scope.inSession(
				session -> session.createQuery(
						"select o from Owner o where treat(o.pet as InheritedPropertyTest$Cat).mother = :mother",
						Owner.class ) ) );
		scope.inSession(
				session -> {
					final var cats = session.createQuery(
									"select o from Owner o where treat(o.pet as InheritedPropertyTest$Cat).mother = :mother",
									Owner.class )
							.setParameter( "mother", "Kitty" )
							.getResultList();
					assertEquals( 1, cats.size() );
					final var owner = cats.get( 0 );
					assertInstanceOf( Cat.class, owner.pet );
					assertEquals( "Jones", owner.pet.name );
					assertEquals( "Kitty", ((Cat) owner.pet).mother );
				} );
	}

	@Test
	void testDeclaredPropertyCreateQuery(SessionFactoryScope scope) {
		assertDoesNotThrow( () -> scope.inSession(
				session -> session.createQuery(
						"select o from Owner o where treat(o.pet as InheritedPropertyTest$Mammal).mother = :mother",
						Owner.class ) ) );
	}

	@Entity(name = "Owner")
	public static class Owner {
		@Id
		Long id;

		@Embedded
		Animal pet;
	}

	@Embeddable
	@DiscriminatorColumn(name = "animal_type")
	public static class Animal {
		int age;

		String name;
	}

	@Embeddable
	public static class Fish extends Animal {
		int fins;
	}

	@Embeddable
	public static class Mammal extends Animal {
		String mother;
	}

	@Embeddable
	public static class Cat extends Mammal {
		// [...]
	}

	@Embeddable
	public static class Dog extends Mammal {
		// [...]
	}
}
