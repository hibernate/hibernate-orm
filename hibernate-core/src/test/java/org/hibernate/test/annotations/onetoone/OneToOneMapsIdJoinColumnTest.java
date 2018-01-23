/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetoone;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;

import org.hibernate.annotations.NaturalId;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Vlad Mihalcea
 */
public class OneToOneMapsIdJoinColumnTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				PersonDetails.class
		};
	}

	@Test
	public void testLifecycle() {
		Person _person = doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person( "ABC-123" );

			PersonDetails details = new PersonDetails();
			details.setNickName( "John Doe" );

			person.setDetails( details );
			entityManager.persist( person );

			return person;
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find( Person.class, _person.getId() );

			PersonDetails details = entityManager.find( PersonDetails.class, _person.getId() );
		} );
	}

	@Entity(name = "Person")
	public static class Person  {

		@Id
		private String id;

		@OneToOne(mappedBy = "person", cascade = CascadeType.PERSIST, optional = false)
		private PersonDetails details;

		public Person() {}

		public Person(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public void setDetails(PersonDetails details) {
			this.details = details;
			details.setPerson( this );
		}
	}

	@Entity(name = "PersonDetails")
	public static class PersonDetails  {

		@Id
		private String id;

		private String nickName;

		@OneToOne
		@MapsId
		@JoinColumn(name = "person_id")
		private Person person;

		public String getNickName() {
			return nickName;
		}

		public void setNickName(String nickName) {
			this.nickName = nickName;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
	}

}
