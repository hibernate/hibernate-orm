/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.generated;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.GeneratorType;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.tuple.ValueGenerator;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class GeneratorTypeTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class
		};
	}

	@Test
	public void test() {
		//tag::mapping-generated-GeneratorType-persist-example[]
		CurrentUser.INSTANCE.logIn( "Alice" );

		doInJPA( this::entityManagerFactory, entityManager -> {

			Person person = new Person();
			person.setId( 1L );
			person.setFirstName( "John" );
			person.setLastName( "Doe" );

			entityManager.persist( person );
		} );

		CurrentUser.INSTANCE.logOut();
		//end::mapping-generated-GeneratorType-persist-example[]

		//tag::mapping-generated-GeneratorType-update-example[]
		CurrentUser.INSTANCE.logIn( "Bob" );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find( Person.class, 1L );
			person.setFirstName( "Mr. John" );
		} );

		CurrentUser.INSTANCE.logOut();
		//end::mapping-generated-GeneratorType-update-example[]
	}

	//tag::mapping-generated-GeneratorType-example[]
	public static class CurrentUser {

		public static final CurrentUser INSTANCE = new CurrentUser();

		private static final ThreadLocal<String> storage = new ThreadLocal<>();

		public void logIn(String user) {
			storage.set( user );
		}

		public void logOut() {
			storage.remove();
		}

		public String get() {
			return storage.get();
		}
	}

	public static class LoggedUserGenerator implements ValueGenerator<String> {
		
		@Override
		public String generateValue(
				Session session, Object owner) {
			return CurrentUser.INSTANCE.get();
		}
	}
	
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String firstName;

		private String lastName;

		@GeneratorType( type = LoggedUserGenerator.class, when = GenerationTime.INSERT)
		private String createdBy;

		@GeneratorType( type = LoggedUserGenerator.class, when = GenerationTime.ALWAYS)
		private String updatedBy;

	//end::mapping-generated-GeneratorType-example[]
		public Person() {}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public String getCreatedBy() {
			return createdBy;
		}

		public String getUpdatedBy() {
			return updatedBy;
		}

		//tag::mapping-generated-GeneratorType-example[]
	}
	//end::mapping-generated-GeneratorType-example[]
}
