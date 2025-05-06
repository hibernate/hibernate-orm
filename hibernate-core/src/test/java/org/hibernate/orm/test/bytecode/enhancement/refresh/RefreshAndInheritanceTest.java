/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.refresh;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				RefreshAndInheritanceTest.Company.class,
				RefreshAndInheritanceTest.ManufacturerCompany.class,
				RefreshAndInheritanceTest.ManufacturerComputerSystem.class,
				RefreshAndInheritanceTest.ComputerSystem.class,
				RefreshAndInheritanceTest.Person.class,
		}
)
@JiraKey( value = "HHH-16447")
public class RefreshAndInheritanceTest {

	@BeforeAll
	public void init(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					ManufacturerCompany manufacturerCompany = new ManufacturerCompany();
					manufacturerCompany.setId( 1L );
					manufacturerCompany.setComputerSystem( new ManufacturerComputerSystem() );

					Person person = new Person();
					person.setFirstName( "Henry" );
					manufacturerCompany.addPerson( person );

					entityManager.persist( manufacturerCompany );
				}
		);
	}

	@Test
	public void refreshTest(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					ManufacturerCompany manufacturerCompany = entityManager.find( ManufacturerCompany.class, 1L );
					entityManager.refresh( manufacturerCompany );

					List<Person> people = manufacturerCompany.getPeople();
					Person person1 = people.get( 0 );

					String newFirstName = "name1".equals( person1.getFirstName() ) ? "name2" : "name1";
					entityManager.getTransaction().begin();
					person1.setFirstName( newFirstName );
					entityManager.getTransaction().commit();

					assertThat( person1.getFirstName() ).isEqualTo( newFirstName );

				}
		);
	}


	@Entity(name = "Company")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn(name = "CompanyType", discriminatorType = DiscriminatorType.INTEGER)
	public static abstract class Company {
		@Id
		protected long id;

		@OneToMany(mappedBy = "company", orphanRemoval = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		private List<Person> people = new ArrayList<>();

		public List<Person> getPeople() {
			return people;
		}

		public void addPerson(Person person) {
			people.add( person );
			person.setCompany( this );
		}

		public void setId(long id) {
			this.id = id;
		}
	}

	@Entity(name = "ComputerSystem")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn(name = "CompanyType", discriminatorType = DiscriminatorType.INTEGER)
	public static abstract class ComputerSystem {
		@Id
		@GeneratedValue
		private long id;

		@ManyToOne
		@JoinColumn(name = "OWNER_ID", foreignKey = @ForeignKey())
		protected Company owner = null;

		public void setOwner(Company owner) {
			this.owner = owner;
		}
	}

	@Entity(name = "ManufacturerComputerSystem")
	@DiscriminatorValue("2")
	public static class ManufacturerComputerSystem extends ComputerSystem {
	}

	@Entity(name = "ManufacturerCompany")
	@DiscriminatorValue("1")
	public static class ManufacturerCompany extends Company {
		@OneToOne(orphanRemoval = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@JoinColumn(name = "COMPUTERSYSTEM_ID", foreignKey = @ForeignKey())
		private ManufacturerComputerSystem computerSystem;

		public void setComputerSystem(ManufacturerComputerSystem computerSystem) {
			this.computerSystem = computerSystem;
			computerSystem.setOwner( this );
		}
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		@GeneratedValue
		private long id;

		private String firstName;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "COMPANY_ID", foreignKey = @ForeignKey())
		private Company company;

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setCompany(Company company) {
			this.company = company;
		}
	}

}
