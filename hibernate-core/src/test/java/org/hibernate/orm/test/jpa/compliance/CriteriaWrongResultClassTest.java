/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.orm.test.jpa.compliance.CriteriaWrongResultClassTest_.Department_;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.JpaComplianceSettings.JPA_QUERY_COMPLIANCE;

@Jpa(
		annotatedClasses = {
				CriteriaWrongResultClassTest.Department.class,
				CriteriaWrongResultClassTest.Person.class
		},
		properties = @Setting(name = JPA_QUERY_COMPLIANCE, value = "true")
)
public class CriteriaWrongResultClassTest {
	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final Department department = new Department( 1, "Hibernate" );
					final Person gavin = new Person( 1, "Gavin King", "gavin" );
					final Person steve = new Person( 2, "Steve Ebersole", "steve" );
					department.addPerson( gavin );
					department.addPerson( steve );

					entityManager.persist( gavin );
					entityManager.persist( steve );
					entityManager.persist( department );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey("HHH-20434")
	public void getMapAttributeTest(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final var criteriaBuilder = entityManager.getCriteriaBuilder();
					final var query = criteriaBuilder.createQuery( Person.class );
					final var department = query.from( Department.class );
					query.where( criteriaBuilder.equal( department.get( Department_.id ), 1 ) );
					query.select( department.get( Department_.people ) );
					final List<Person> people = entityManager.createQuery( query ).getResultList();
					assertThat( people )
							.extracting( Person::getName )
							.containsExactlyInAnyOrder( "Gavin King", "Steve Ebersole" );
				}
		);
	}

	@Entity(name = "Department")
	@Table(name = "DEPARTMENT_TABLE")
	public static class Department {
		@Id
		private int id;

		private String name;

		@OneToMany
		@JoinTable(name = "DEPARTMENT_EMPLOYEE_TABLE")
		@MapKey("nickname")
		private Map<String, Person> people = new HashMap<>();

		public Department() {
		}

		public Department(
				int id,
				String name) {
			this.id = id;
			this.name = name;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Map<String, Person> getPeople() {
			return people;
		}

		public void addPerson(Person person){
			people.put( person.getNickname(), person );
		}
	}

	@Entity(name = "Person")
	@Table(name = "EMPLOYEE_TABLE")
	public static class Person {

		@Id
		private int id;

		private String name;

		private String nickname;

		public Person() {
		}

		public Person(int id, String name, String nickname) {
			this.id = id;
			this.name = name;
			this.nickname = nickname;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getNickname() {
			return nickname;
		}
	}

}
