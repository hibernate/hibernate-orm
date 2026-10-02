package org.hibernate.orm.test.inheritance;

import java.util.List;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests discriminator restrictions in bulk updates of a single-table hierarchy.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		SingleTableBulkUpdateByTypeTest.Pet.class,
		SingleTableBulkUpdateByTypeTest.Dog.class,
		SingleTableBulkUpdateByTypeTest.Cat.class
})
@SessionFactory
@JiraKey("HHH-13301")
public class SingleTableBulkUpdateByTypeTest {
	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Dog( 1L, "first dog" ) );
			session.persist( new Cat( 2L, "cat" ) );
			session.persist( new Dog( 3L, "second dog" ) );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testUpdateMatchingType(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final int updated = session.createMutationQuery(
					"update Pet p set p.name = :name where p.id = :id and type(p) in (:types)"
			)
					.setParameter( "name", "updated dog" )
					.setParameter( "id", 1L )
					.setParameterList( "types", List.of( Dog.class ) )
					.executeUpdate();
			assertEquals( 1, updated );
		} );
		assertNames( scope, "updated dog", "cat", "second dog" );
	}

	@Test
	void testUpdateNonMatchingType(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final int updated = session.createMutationQuery(
					"update Pet p set p.name = :name where p.id = :id and type(p) in (:types)"
			)
					.setParameter( "name", "must not be updated" )
					.setParameter( "id", 2L )
					.setParameterList( "types", List.of( Dog.class ) )
					.executeUpdate();
			assertEquals( 0, updated );
		} );
		assertNames( scope, "first dog", "cat", "second dog" );
	}

	private void assertNames(SessionFactoryScope scope, String... expected) {
		scope.inTransaction( session -> assertEquals(
				List.of( expected ),
				session.createQuery( "select p.name from Pet p order by p.id", String.class ).getResultList()
		) );
	}

	@Entity(name = "Pet")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "PET_TYPE")
	public abstract static class Pet {
		@Id
		private Long id;
		private String name;

		protected Pet() {
		}

		protected Pet(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Dog")
	@DiscriminatorValue("DOG")
	public static class Dog extends Pet {
		public Dog() {
		}

		public Dog(Long id, String name) {
			super( id, name );
		}
	}

	@Entity(name = "Cat")
	@DiscriminatorValue("CAT")
	public static class Cat extends Pet {
		public Cat() {
		}

		public Cat(Long id, String name) {
			super( id, name );
		}
	}
}
