/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.generated;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.dialect.SQLServer2005Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(SQLServer2005Dialect.class)
public class GeneratedTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::mapping-generated-Generated-persist-example[]
			Person person = new Person();
			person.setId( 1L );
			person.setFirstName( "John" );
			person.setMiddleName1( "Flávio" );
			person.setMiddleName2( "André" );
			person.setMiddleName3( "Frederico" );
			person.setMiddleName4( "Rúben" );
			person.setMiddleName5( "Artur" );
			person.setLastName( "Doe" );

			entityManager.persist( person );
			entityManager.flush();

			assertEquals("John Flávio André Frederico Rúben Artur Doe", person.getFullName());
			//end::mapping-generated-Generated-persist-example[]
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			//tag::mapping-generated-Generated-update-example[]
			Person person = entityManager.find( Person.class, 1L );
			person.setLastName( "Doe Jr" );

			entityManager.flush();
			assertEquals("John Flávio André Frederico Rúben Artur Doe Jr", person.getFullName());
			//end::mapping-generated-Generated-update-example[]
		} );
	}

	//tag::mapping-generated-Generated-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String firstName;

		private String lastName;

		private String middleName1;

		private String middleName2;

		private String middleName3;

		private String middleName4;

		private String middleName5;

		@Generated( value = GenerationTime.ALWAYS )
		@Column(columnDefinition =
			"AS CONCAT(" +
			"	COALESCE(firstName, ''), " +
			"	COALESCE(' ' + middleName1, ''), " +
			"	COALESCE(' ' + middleName2, ''), " +
			"	COALESCE(' ' + middleName3, ''), " +
			"	COALESCE(' ' + middleName4, ''), " +
			"	COALESCE(' ' + middleName5, ''), " +
			"	COALESCE(' ' + lastName, '') " +
			")")
		private String fullName;

	//end::mapping-generated-Generated-example[]
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

		public String getMiddleName1() {
			return middleName1;
		}

		public void setMiddleName1(String middleName1) {
			this.middleName1 = middleName1;
		}

		public String getMiddleName2() {
			return middleName2;
		}

		public void setMiddleName2(String middleName2) {
			this.middleName2 = middleName2;
		}

		public String getMiddleName3() {
			return middleName3;
		}

		public void setMiddleName3(String middleName3) {
			this.middleName3 = middleName3;
		}

		public String getMiddleName4() {
			return middleName4;
		}

		public void setMiddleName4(String middleName4) {
			this.middleName4 = middleName4;
		}

		public String getMiddleName5() {
			return middleName5;
		}

		public void setMiddleName5(String middleName5) {
			this.middleName5 = middleName5;
		}

		public String getFullName() {
			return fullName;
		}
	//tag::mapping-generated-Generated-example[]
	}
	//end::mapping-generated-Generated-example[]
}
