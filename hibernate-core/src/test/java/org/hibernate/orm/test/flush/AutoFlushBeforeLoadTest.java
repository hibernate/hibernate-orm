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

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(biDirectionalAssociationManagement = true)
@JiraKey(value = "HHH-17947")
public class AutoFlushBeforeLoadTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				Phone.class,
				DriversLicense.class
		};
	}

	@Before
	public void setupData() {
		inTransaction(
				session -> {
					Person p = new Person( 1L, "John Doe" );
					session.persist( p );
				}
		);
	}

	@After
	public void tearDown() {
		inTransaction(
				session -> {
					session.createMutationQuery( "delete from DriversLicense" ).executeUpdate();
					session.createMutationQuery( "delete from Phone" ).executeUpdate();
					session.createMutationQuery( "delete from Person" ).executeUpdate();
				}
		);
	}

	@Test
	public void testCollectionInitialization() {
		inTransaction(
				session -> {
					Phone phone = new Phone( session.getReference( Person.class, 1L ), "1234567890" );
					session.persist( phone );
					Person person = session.createQuery( "select p from Person p", Person.class ).getSingleResult();
					assertEquals( 1, person.getPhones().size() );
				}
		);
	}

	@Test
	public void testOneToOneInitialization() {
		inTransaction(
				session -> {
					DriversLicense driversLicense = new DriversLicense( 1L, session.getReference( Person.class, 1L ), "999" );
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

		@OneToOne(mappedBy = "owner", fetch = FetchType.LAZY)
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

		private String phoneNumber;

		public Phone() {
		}

		public Phone(Person person, String phoneNumber) {
			this.person = person;
			this.phoneNumber = phoneNumber;
		}

		public Long getId() {
			return id;
		}

		public String getPhoneNumber() {
			return phoneNumber;
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
