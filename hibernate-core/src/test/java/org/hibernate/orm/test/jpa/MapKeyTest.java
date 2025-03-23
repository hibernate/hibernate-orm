/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		annotatedClasses = {
				MapKeyTest.School.class,
				MapKeyTest.Person.class
		}
)
public class MapKeyTest {


	@Test
	public void testMapKeyTemporal(EntityManagerFactoryScope scope) throws Exception {

		SimpleDateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd" );
		final Date date1 = formatter.parse( "2022-02-02" );
		final Date date2 = java.sql.Date.valueOf( formatter.format( Calendar.getInstance().getTime() ) );

		Set<Date> expectedDates = new HashSet<>();
		expectedDates.add( date1 );
		expectedDates.add( date2 );

		School school1 = new School( 1, "High School" );
		School school2 = new School( 2, "Primary School" );

		Person person1 = new Person( 1, "Andrea", school2 );
		Person person2 = new Person( 2, "Luigi", school2 );

		Set<Person> expectedPeople = new HashSet<>();
		expectedPeople.add( person1 );
		expectedPeople.add( person2 );

		scope.inTransaction(
				entityManager -> {
					Map<Date, Person> lastNames = new HashMap<>();
					lastNames.put( date1, person1 );
					lastNames.put( date2, person2 );
					school2.setStudentsByDate( lastNames );

					entityManager.persist( school1 );
					entityManager.persist( school2 );

					entityManager.persist( person1 );
					entityManager.persist( person2 );
				}
		);

		scope.inTransaction(
				entityManager -> {
					Person person = entityManager.find( Person.class, 2 );
					School school = person.getSchool();

					Map<Date, Person> studentsByDate = school.getStudentsByDate();

					Set<Date> dates = studentsByDate.keySet();
					assertEquals( expectedDates.size(), dates.size() );
					assertTrue( expectedDates.containsAll( dates ) );

					Collection<Person> people = studentsByDate.values();
					assertEquals( expectedPeople.size(), people.size() );
					assertTrue( expectedPeople.containsAll( people ) );
				}
		);
	}

	@Entity
	@Table(name = "PERSON_TABLE")
	public static class Person {

		@Id
		private int id;

		private String name;

		@ManyToOne
		private School school;

		public Person() {
		}

		public Person(int id, String name, School school) {
			this.id = id;
			this.name = name;
			this.school = school;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public School getSchool() {
			return school;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Person person = (Person) o;
			return name.equals( person.name );
		}

		@Override
		public int hashCode() {
			return Objects.hash( name );
		}
	}

	@Entity
	@Table(name = "SCHOOL_TABLE")
	public static class School {

		@Id
		private int id;

		private String name;

		@OneToMany(mappedBy = "school")
		@MapKeyClass(Date.class)
		@MapKeyColumn(name = "THE_DATE")
		@MapKeyTemporal(TemporalType.DATE)
		private Map<Date, Person> studentsByDate;

		@Temporal( TemporalType.DATE )
		private Date aDate;

		public School() {
		}

		public School(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Map getStudentsByDate() {
			return studentsByDate;
		}

		public void setStudentsByDate(Map studentsByDate) {
			this.studentsByDate = studentsByDate;
		}

	}

}
