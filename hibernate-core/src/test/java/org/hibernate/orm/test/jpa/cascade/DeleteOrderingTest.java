/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;


@Jpa(annotatedClasses = {
		DeleteOrderingTest.Blacklisted.class,
		DeleteOrderingTest.Directory.class,
		DeleteOrderingTest.Person.class
})
@Jira("https://hibernate.atlassian.net/browse/HHH-4134")
public class DeleteOrderingTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "delete from Blacklisted" ).executeUpdate();
					entityManager.createQuery( "delete from Person" ).executeUpdate();
					entityManager.createQuery( "delete from Directory" ).executeUpdate();
				}
		);
	}

	@Test
	public void testDeleteCascade(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Directory directory = new Directory( 1L );
					Person p1 = new Person( 1L, "P1" );
					directory.addPerson( p1 );
					directory.addPerson( new Person( 2L, "P2" ) );
					directory.blockPerson( p1 );
					entityManager.persist( directory );
				}
		);
		scope.inTransaction(
				entityManager -> {
					entityManager.remove( entityManager.find( Directory.class, 1L ) );
				}
		);
	}

	@Entity(name = "Directory")
	public static class Directory {
		@Id
		public Long id;
		@OneToMany(mappedBy = "directory", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		public Set<Person> people = new HashSet<>();
		@OneToMany(mappedBy = "directory", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		public Set<Blacklisted> suspended = new HashSet<>();

		public Directory() {
		}

		public Directory(Long id) {
			this.id = id;
		}

		public void addPerson(Person person) {
			people.add( person );
			person.directory = this;
		}

		public void blockPerson(Person person) {
			suspended.add( new Blacklisted( 1L, person.directory, person ) );
		}
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		public Long id;
		public String name;
		@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
		@JoinColumn(nullable = false, updatable = false)
		public Directory directory;

		public Person() {
		}

		public Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Blacklisted")
	public static class Blacklisted {
		@Id
		public Long id;
		@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
		@JoinColumn(nullable = false, updatable = false)
		public Directory directory;
		@OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
		@JoinColumn(nullable = false)
		public Person person;

		public Blacklisted() {
		}

		public Blacklisted(Long id, Directory directory, Person person) {
			this.id = id;
			this.directory = directory;
			this.person = person;
		}
	}

}
