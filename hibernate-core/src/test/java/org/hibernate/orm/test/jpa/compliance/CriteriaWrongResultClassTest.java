/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.compliance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;

@Jpa(
		annotatedClasses = {
				CriteriaWrongResultClassTest.Department.class,
				CriteriaWrongResultClassTest.Person.class
		},
		properties = @Setting(name = AvailableSettings.JPA_QUERY_COMPLIANCE, value = "true")
)
public class CriteriaWrongResultClassTest {
	@Test
	public void getMapAttributeTest(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaQuery query = criteriaBuilder.createQuery( Expression.class );
					final From<Department, Department> department = query.from( Department.class );
					query.where( criteriaBuilder.equal( department.get( "id" ), 1 ) );
					query.select( department.get( "people" ) );
					List<Person> people = entityManager.createQuery( query ).getResultList();
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
		@MapKey(name = "nickname")
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
