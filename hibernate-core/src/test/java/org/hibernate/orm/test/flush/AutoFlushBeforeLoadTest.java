/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.flush;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.NaturalId;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jan Schatteman
 */
@DomainModel(
		annotatedClasses = { AutoFlushBeforeLoadTest.Person.class, AutoFlushBeforeLoadTest.Phone.class, AutoFlushBeforeLoadTest.DriversLicense.class }
)
@SessionFactory
@JiraKey(value = "HHH-10445")
public class AutoFlushBeforeLoadTest {

	@BeforeAll
	public void setupData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person p = new Person( 1L, "John Doe" );
					session.persist( p );
				}
		);
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from DriversLicense" ).executeUpdate();
					session.createMutationQuery( "delete from Phone" ).executeUpdate();
					session.createMutationQuery( "delete from Person" ).executeUpdate();
				}
		);
	}

	@Test
	public void testCollectionInitialization(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Phone phone = new Phone( session.getReference( Person.class, 1L ), "1234567890" );
					session.persist( phone );
					Person person = session.createQuery( "select p from Person p", Person.class ).getSingleResult();
					assertEquals( 1, person.getPhones().size() );
				}
		);
	}

	@Test
	public void testCollectionInitialization1(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DriversLicense driversLicense = new DriversLicense( 1L, new Person( 1L, "John Doe" ), "999" );
					session.persist( driversLicense );
					Person person = session.createQuery( "select p from Person p", Person.class ).getSingleResult();
					assertEquals( driversLicense, person.getDriversLicense() );
					session.clear();
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		@NaturalId
		private String name;

		@OneToOne(mappedBy = "owner")
		private DriversLicense driversLicense;

		@OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
		private List<Phone> phones = new ArrayList<>();

		public Person() {
		}

		public Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<Phone> getPhones() {
			return phones;
		}

		public DriversLicense getDriversLicense() {
			return driversLicense;
		}

		public void setDriversLicense(DriversLicense driversLicense) {
			this.driversLicense = driversLicense;
		}
	}

	@Entity(name = "Phone")
	public static class Phone {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private Person person;

		private String number;

		public Phone() {
		}

		public Phone(Person person, String number) {
			this.person = person;
			this.number = number;
		}

		public Long getId() {
			return id;
		}

		public String getNumber() {
			return number;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
	}


	@Entity(name = "DriversLicense")
	public static class DriversLicense {

		@Id
		private Long id;
		@OneToOne
		@JoinColumn(name = "owner_name", referencedColumnName = "name")
		private Person owner;
		@NaturalId
		@Column(name = "license_number")
		private String number;

		public DriversLicense() {
		}

		public DriversLicense(Long id, Person owner, String number) {
			this.id = id;
			this.owner = owner;
			this.number = number;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Person getOwner() {
			return owner;
		}

		public void setOwner(Person owner) {
			this.owner = owner;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}
	}

}
