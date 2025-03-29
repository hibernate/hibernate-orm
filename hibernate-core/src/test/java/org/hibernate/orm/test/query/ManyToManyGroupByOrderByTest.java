/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Tuple;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ManyToManyGroupByOrderByTest.Person.class,
		ManyToManyGroupByOrderByTest.Cat.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17837" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-18202" )
public class ManyToManyGroupByOrderByTest {
	@Test
	public void testSelectEntity(SessionFactoryScope scope) {
		// explicit join group by
		scope.inTransaction( session -> {
			final Person result = session.createQuery(
					"select owner from Cat cat join cat.owners owner group by owner",
					Person.class
			).getSingleResult();
			assertThat( result.getId() ).isEqualTo( 1L );
		} );
		// explicit join group by + order by
		scope.inTransaction( session -> {
			final Person result = session.createQuery(
					"select owner from Cat cat join cat.owners owner group by owner order by owner",
					Person.class
			).getSingleResult();
			assertThat( result.getId() ).isEqualTo( 1L );
		} );
		// implicit join group by
		scope.inTransaction( session -> {
			final Person result = session.createQuery(
					"select element(cat.owners) from Cat cat group by element(cat.owners)",
					Person.class
			).getSingleResult();
			assertThat( result.getId() ).isEqualTo( 1L );
		} );
		// implicit join group by + order by
		scope.inTransaction( session -> {
			final Person result = session.createQuery(
					"select element(cat.owners) from Cat cat group by element(cat.owners) order by element(cat.owners)",
					Person.class
			).getSingleResult();
			assertThat( result.getId() ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testSelectAssociationId(SessionFactoryScope scope) {
		// explicit join group by
		scope.inTransaction( session -> {
			final Tuple result = session.createQuery(
					"select owner.id, owner.name from Cat cat join cat.owners owner group by owner",
					Tuple.class
			).getSingleResult();
			assertThat( result.get( 0, Long.class ) ).isEqualTo( 1L );
			assertThat( result.get( 1, String.class ) ).isEqualTo( "Marco" );
		} );
		// explicit join group by + order by
		scope.inTransaction( session -> {
			final Tuple result = session.createQuery(
					"select owner.id, owner.name from Cat cat join cat.owners owner group by owner order by owner",
					Tuple.class
			).getSingleResult();
			assertThat( result.get( 0, Long.class ) ).isEqualTo( 1L );
			assertThat( result.get( 1, String.class ) ).isEqualTo( "Marco" );
		} );
		// implicit join group by
		scope.inTransaction( session -> {
			final Tuple result = session.createQuery(
					"select element(cat.owners).id from Cat cat group by element(cat.owners)",
					Tuple.class
			).getSingleResult();
			assertThat( result.get( 0, Long.class ) ).isEqualTo( 1L );
		} );
		// implicit join group by + order by
		scope.inTransaction( session -> {
			final Tuple result = session.createQuery(
					"select element(cat.owners).id from Cat cat group by element(cat.owners) order by element(cat.owners)",
					Tuple.class
			).getSingleResult();
			assertThat( result.get( 0, Long.class ) ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testDistinctAndAggregates(SessionFactoryScope scope) {
		// explicit join distinct
		scope.inTransaction( session -> {
			final Tuple result = session.createQuery(
					"select distinct owner.id from Cat cat join cat.owners owner group by owner.id order by owner.id",
					Tuple.class
			).getSingleResult();
			assertThat( result.get( 0, Long.class ) ).isEqualTo( 1L );
		} );
		// explicit join distinct + aggregate
		scope.inTransaction( session -> {
			final Tuple result = session.createQuery(
					"select distinct min(owner.id), cat.id from Cat cat join cat.owners owner group by cat.id order by min(owner.id), cat.id",
					Tuple.class
			).getSingleResult();
			assertThat( result.get( 0, Long.class ) ).isEqualTo( 1L );
		} );
		// implicit join distinct
		scope.inTransaction( session -> {
			final Tuple result = session.createQuery(
					"select distinct element(cat.owners).id from Cat cat group by element(cat.owners).id order by element(cat.owners).id",
					Tuple.class
			).getSingleResult();
			assertThat( result.get( 0, Long.class ) ).isEqualTo( 1L );
		} );
		// implicit join distinct + aggregate
		scope.inTransaction( session -> {
			final Tuple result = session.createQuery(
					"select distinct min(element(cat.owners).id), cat.id from Cat cat group by cat.id order by min(element(cat.owners).id), cat.id",
					Tuple.class
			).getSingleResult();
			assertThat( result.get( 0, Long.class ) ).isEqualTo( 1L );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Cat cat = new Cat();
			final Person owner = new Person( 1L, "Marco" );
			cat.addToOwners( owner );
			session.persist( owner );
			session.persist( cat );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Cat" ).executeUpdate();
			session.createMutationQuery( "delete from Person" ).executeUpdate();
		} );
	}

	@Entity( name = "Person" )
	static class Person {
		@Id
		private Long id;

		private String name;

		public Person() {
		}

		public Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		@ManyToMany( mappedBy = "owners" )
		protected Set<Cat> ownedCats = new HashSet<>();

		public Long getId() {
			return id;
		}
	}

	@Entity( name = "Cat" )
	static class Cat {
		@Id
		@GeneratedValue
		protected Long id;

		@JoinTable
		@ManyToMany
		private Set<Person> owners = new HashSet<Person>();

		public Set<Person> getOwners() {
			return this.owners;
		}

		public void addToOwners(Person person) {
			owners.add( person );
		}
	}
}
