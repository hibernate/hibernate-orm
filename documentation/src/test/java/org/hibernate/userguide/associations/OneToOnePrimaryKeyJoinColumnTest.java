/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.associations;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;

import org.hibernate.annotations.NaturalId;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class OneToOnePrimaryKeyJoinColumnTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				PersonDetails.class
		};
	}

	@Test
	public void testLifecycle() {
		//tag::identifiers-derived-primarykeyjoincolumn-persist-example[]
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person( "ABC-123" );
			person.setId( 1L );
			entityManager.persist( person );

			PersonDetails personDetails = new PersonDetails();
			personDetails.setNickName( "John Doe" );
			personDetails.setPerson( person );

			entityManager.persist( personDetails );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			PersonDetails personDetails = entityManager.find( PersonDetails.class, 1L );

			assertEquals("John Doe", personDetails.getNickName());
		} );
		//end::identifiers-derived-primarykeyjoincolumn-persist-example[]
	}

	//tag::identifiers-derived-primarykeyjoincolumn[]
	@Entity(name = "Person")
	public static class Person  {

		@Id
		private Long id;

		@NaturalId
		private String registrationNumber;

		public Person() {}

		public Person(String registrationNumber) {
			this.registrationNumber = registrationNumber;
		}

		//Getters and setters are omitted for brevity
		//end::identifiers-derived-primarykeyjoincolumn[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getRegistrationNumber() {
			return registrationNumber;
		}
	//tag::identifiers-derived-primarykeyjoincolumn[]
	}

	@Entity(name = "PersonDetails")
	public static class PersonDetails  {

		@Id
		private Long id;

		private String nickName;

		@OneToOne
		@PrimaryKeyJoinColumn
		private Person person;

		public void setPerson(Person person) {
			this.person = person;
			this.id = person.getId();
		}

		//Other getters and setters are omitted for brevity
		//end::identifiers-derived-primarykeyjoincolumn[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getNickName() {
			return nickName;
		}

		public void setNickName(String nickName) {
			this.nickName = nickName;
		}

		public Person getPerson() {
			return person;
		}

	//tag::identifiers-derived-primarykeyjoincolumn[]
	}
	//end::identifiers-derived-primarykeyjoincolumn[]

}
