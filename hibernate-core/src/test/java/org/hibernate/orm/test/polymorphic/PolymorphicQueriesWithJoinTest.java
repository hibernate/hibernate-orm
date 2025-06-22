/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.polymorphic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.TypedQuery;

@DomainModel(
		annotatedClasses = {
				PolymorphicQueriesWithJoinTest.Person.class,
				PolymorphicQueriesWithJoinTest.Dog.class,
				PolymorphicQueriesWithJoinTest.Cat.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-15944")
public class PolymorphicQueriesWithJoinTest {

	public static final Long PERSON_ID = 1l;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person owner = new Person( PERSON_ID );

					Cat cat = new Cat();
					Dog dog = new Dog();

					cat.addToOwners( owner );
					dog.addToOwners( owner );

					session.persist( owner );
					session.persist( cat );
					session.persist( dog );
				}
		);
	}

	@Test
	public void testSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Animal> animals = session
							.createQuery(
									"SELECT an FROM org.hibernate.orm.test.polymorphic.PolymorphicQueriesWithJoinTest$Animal an",
									Animal.class
							)
							.getResultList();

				}
		);
	}

	@Test
	public void testSelectWithJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Person> owners = session
							.createQuery(
									"SELECT p FROM org.hibernate.orm.test.polymorphic.PolymorphicQueriesWithJoinTest$Animal an JOIN an.owners p",
									Person.class
							)
							.getResultList();
				}

		);
	}

	@Test
	public void testSelectWithFetchJoin(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Animal> animals = session
							.createQuery(
									"SELECT an FROM org.hibernate.orm.test.polymorphic.PolymorphicQueriesWithJoinTest$Animal an JOIN FETCH an.owners p",
									Animal.class
							)
							.getResultList();
				}

		);
	}

	@Test
	public void testSelectWithWhereClause(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person owner = session.find( Person.class, PERSON_ID );
					TypedQuery<Animal> query = session.createQuery(
							"SELECT an FROM org.hibernate.orm.test.polymorphic.PolymorphicQueriesWithJoinTest$Animal an WHERE :owner = some elements(an.owners) ",
							Animal.class
					);
					query.setParameter( "owner", owner );
					List<Animal> animals = query.getResultList();
				}

		);
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private long id;

		private String name;

		public Person() {
		}

		public Person(long id) {
			this.id = id;
		}

		public long getId() {
			return this.id;
		}
	}


	public interface Animal {
		Set<Person> getOwners();

		void addToOwners(Person person);
	}

	@Entity(name = "Cat")
	public static class Cat implements Animal {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private long id;

		private String name;

		@ManyToMany
		private Set<Person> owners = new HashSet<>();

		@Override
		public Set<Person> getOwners() {
			return this.owners;
		}

		@Override
		public void addToOwners(Person person) {
			owners.add( person );
		}

	}


	@Entity(name = "Dog")
	public static class Dog implements Animal {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private long id;

		private String name;

		@ManyToMany
		private Set<Person> owners = new HashSet<>();

		@Override
		public Set<Person> getOwners() {
			return this.owners;
		}

		@Override
		public void addToOwners(Person person) {
			owners.add( person );
		}
	}
}
