/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.inheritance;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@DomainModel(
		annotatedClasses = {
				EmbeddableInheritanceHierarchyOrderTest.Animal.class,
				EmbeddableInheritanceHierarchyOrderTest.Cat.class,
				EmbeddableInheritanceHierarchyOrderTest.Dog.class,
				EmbeddableInheritanceHierarchyOrderTest.Fish.class,
				EmbeddableInheritanceHierarchyOrderTest.Mammal.class,
				// If Mammal is moved right under Animal (before Dog and Cat), test will pass
				EmbeddableInheritanceHierarchyOrderTest.Owner.class
		}
)
@SessionFactory
public class EmbeddableInheritanceHierarchyOrderTest {

	@AfterAll
	static void clean(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Owner" ).executeUpdate() );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Owner( 1L, new Animal( 2, "Agapius" ) ) );
			session.persist( new Owner( 2L, new Cat( 3, "Bercharius", "Blaesilla" ) ) );
			session.persist( new Owner( 3L, new Dog( 4, "Censurius", "Caesarea" ) ) );
			session.persist( new Owner( 4L, new Fish( 5, "Dionysius", 3 ) ) );
			session.persist( new Owner( 5L, new Mammal( 6, "Epagraphas", "Eanswida" ) ) );
		} );
		scope.inSession( session -> {
			final Owner animalOwner = session.find( Owner.class, 1L );
			assertEquals( 2, animalOwner.getPet().getAge() );
			assertEquals( "Agapius", animalOwner.getPet().getName() );

			final Owner fishOwner = session.find( Owner.class, 4L );
			if ( fishOwner.getPet() instanceof Fish ) {
				final Fish fish = (Fish) fishOwner.getPet();
				assertEquals( 5, fish.getAge() );
				assertEquals( "Dionysius", fish.getName() );
				assertEquals( 3, fish.getFins() );
			}
			else {
				fail( "Not fish owner" );
			}

			final Owner mammalOwner = session.find( Owner.class, 5L );
			if ( mammalOwner.getPet() instanceof Mammal ) {
				final Mammal mammal = (Mammal) mammalOwner.getPet();
				assertEquals( 6, mammal.getAge() );
				assertEquals( "Epagraphas", mammal.getName() );
				assertEquals( "Eanswida", mammal.getMother() );
			}
			else {
				fail( "Not mammal owner" );
			}

			final Owner catOwner = session.find( Owner.class, 2L );
			if ( catOwner.getPet() instanceof Cat ) {
				final Cat cat = (Cat) catOwner.getPet();
				assertEquals( 3, cat.getAge() );
				assertEquals( "Bercharius", cat.getName() );
				assertEquals( "Blaesilla", cat.getMother() );
			}
			else {
				fail( "Not cat owner" );
			}

			final Owner dogOwner = session.find( Owner.class, 3L );
			if ( dogOwner.getPet() instanceof Dog ) {
				final Dog dog = (Dog) dogOwner.getPet();
				assertEquals( 4, dog.getAge() );
				assertEquals( "Censurius", dog.getName() );
				assertEquals( "Caesarea", dog.getMother() );
			}
			else {
				fail( "Not dog owner" );
			}
		} );
	}

	@Embeddable
	@DiscriminatorColumn(name = "animal_type", length = 64)
	static
	class Animal {
		private int age;

		private String name;

		public Animal() {
		}

		public Animal(int age, String name) {
			this.age = age;
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	static
	class Cat extends Mammal {
		//private int mouse;
		// [...]


		public Cat() {
			super();
		}

		public Cat(int age, String name, String mother) {
			super( age, name, mother );
		}
	}

	@Embeddable
	static
	class Dog extends Mammal {
		//private int bone;
		// [...]

		public Dog() {
		}

		public Dog(int age, String name, String mother) {
			super( age, name, mother );
		}
	}

	@Embeddable
	static
	class Fish extends Animal {
		private int fins;

		public Fish() {
		}

		public Fish(int age, String name, int fins) {
			super( age, name );
			this.fins = fins;
		}

		public int getFins() {
			return fins;
		}

		public void setFins(int fins) {
			this.fins = fins;
		}
	}

	@Embeddable
	static
	class Mammal extends Animal {
		private String mother;

		public Mammal() {
		}

		public Mammal(int age, String name, String mother) {
			super( age, name );
			this.mother = mother;
		}

		public String getMother() {
			return mother;
		}

		public void setMother(String mother) {
			this.mother = mother;
		}
	}

	@Entity(name = "Owner")
	static
	class Owner {
		@Id
		private Long id;

		@Embedded
		private Animal pet;

		public Owner() {
		}

		public Owner(Long id, Animal pet) {
			this.id = id;
			this.pet = pet;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Animal getPet() {
			return pet;
		}

		public void setPet(Animal pet) {
			this.pet = pet;
		}
	}
}
