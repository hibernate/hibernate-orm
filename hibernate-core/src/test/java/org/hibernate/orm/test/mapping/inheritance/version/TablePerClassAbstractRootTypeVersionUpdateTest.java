/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.inheritance.version;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		TablePerClassAbstractRootTypeVersionUpdateTest.Animal.class,
		TablePerClassAbstractRootTypeVersionUpdateTest.Dog.class,
		TablePerClassAbstractRootTypeVersionUpdateTest.Shepherd.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17834" )
public class TablePerClassAbstractRootTypeVersionUpdateTest {
	@Test
	public void testOptimisticIncrement(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Dog dog = session.find( Dog.class, 1L, LockModeType.OPTIMISTIC_FORCE_INCREMENT );
			assertThat( dog.getVersion() ).isEqualTo( 0L );

			dog.setName( "updated_1" );
			session.flush();
			assertThat( dog.getVersion() ).isEqualTo( 1L );
		} );
		scope.inSession( session -> assertThat( session.find( Dog.class, 1L ).getVersion() ).isEqualTo( 2L ) );
	}

	@Test
	public void testPessimisticIncrement(SessionFactoryScope scope) {
		scope.inSession( session -> assertThat( session.find( Dog.class, 2L ).getVersion() ).isEqualTo( 0L ) );
		scope.inTransaction( session -> {
			final Shepherd dog = session.find( Shepherd.class, 2L, LockModeType.PESSIMISTIC_FORCE_INCREMENT );
			assertThat( dog.getVersion() ).isEqualTo( 1L );

			dog.setName( "updated_2" );
			session.flush();
			assertThat( dog.getVersion() ).isEqualTo( 2L );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Dog( 1L, "dog_1" ) );
			session.persist( new Shepherd( 2L, "dog_2" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from Animal" ).executeUpdate() );
	}

	@Entity( name = "Animal" )
	@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
	abstract static class Animal {
		@Id
		private Long id;

		private String name;

		@Version
		private Long version;

		public Animal() {
		}

		public Animal(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Long getVersion() {
			return version;
		}
	}

	@Entity( name = "Dog" )
	static class Dog extends Animal {
		public Dog() {
		}

		public Dog(Long id, String name) {
			super( id, name );
		}
	}

	@Entity( name = "Shepherd" )
	static class Shepherd extends Dog {
		public Shepherd() {
		}

		public Shepherd(Long id, String name) {
			super( id, name );
		}
	}
}
