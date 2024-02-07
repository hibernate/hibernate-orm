package org.hibernate.orm.test.merge;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(annotatedClasses = {
		JsonMergeTest.TestEntity.class
})
@SessionFactory
@JiraKey( "HHH-16338" )
public class JsonMergeTest {

	@Test
	public void testMerge(SessionFactoryScope scope) {
		final TestEntity testEntity = new TestEntity();
		scope.inTransaction(
				session -> {
					Person fistPerson = new Person( "a", "b" );
					Person secondPerson = new Person( "c", "d" );
					testEntity.addPerson( fistPerson );
					testEntity.addPerson( secondPerson );
					session.persist( testEntity );
				}
		);

		scope.inTransaction(
				session -> {
					TestEntity entity = session.get( TestEntity.class, testEntity.getId() );
					Person person = entity.getPeople().get( 0 );
					assertThat( person.getFirstName() ).isEqualTo( "a" );
					person.setFirstName( "new name" );
					session.merge( entity );
				}
		);

		scope.inTransaction(
				session -> {
					TestEntity entity = session.get( TestEntity.class, testEntity.getId() );
					Person person = entity.getPeople().get( 0 );
					assertThat( person.getFirstName() ).isEqualTo( "new name" );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		@GeneratedValue
		public long id;

		@JdbcTypeCode(SqlTypes.JSON)
		public List<Person> people;

		public void addPerson(Person person) {
			if ( people == null ) {
				people = new ArrayList<>();
			}
			people.add( person );
		}

		public long getId() {
			return id;
		}

		public List<Person> getPeople() {
			return people;
		}
	}

	public static class Person {
		public String firstName;
		public String lastName;

		public Person() {
		}

		public Person(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
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
	}
}
