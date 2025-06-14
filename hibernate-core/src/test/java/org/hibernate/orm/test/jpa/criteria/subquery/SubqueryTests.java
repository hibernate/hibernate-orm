/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.subquery;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaDerivedRoot;
import org.hibernate.query.criteria.JpaSubQuery;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


@Jpa(
		annotatedClasses = {
				SubqueryTests.Person.class,
				SubqueryTests.Address.class
		}
)
public class SubqueryTests {

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope){
		scope.inTransaction(
				entityManager -> {
					Person person = new Person(1, "Andrea");
					Address address = new Address(2, "Gradoli", "Via Roma");
					person.addAddress( address );
					entityManager.persist( address );
					entityManager.persist( person );

					Person anotherPerson = new Person(2, "Luigi");
					entityManager.persist( anotherPerson );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-15477")
	public void testRootInSubqueryWhereClause(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();

					CriteriaQuery<Person> query = builder.createQuery( Person.class );
					Root<Person> person = query.from( Person.class );

					Subquery<String> subQuery = query.subquery( String.class );

					Root<Address> address = subQuery.from( Address.class );
					subQuery.select( address.get( "city" ) ).where( builder.equal( address.get( "person" ), person ) );

					query.where( builder.exists( subQuery ) );

					List<Person> people = entityManager.createQuery( query ).getResultList();
					assertThat(people.size()).isEqualTo( 1 );
					assertThat( people.get( 0 ).getName() ).isEqualTo( "Andrea" );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-17776")
	public void testNoFromClauseInSubquery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					List<Person> entities = getEntities(entityManager);
					assertEquals( 2, entities.size());
					assertEquals("Jack", entities.get(0).getName());
					assertEquals("Black", entities.get(0).getSurName());
					assertEquals("John", entities.get(1).getName());
					// Trim because in some dialects, the type is CHAR(5), leading to trailing spaces
					assertEquals("Doe", entities.get(1).getSurName().trim());
				}
		);
	}

	private List<Person> getEntities(EntityManager entityManager) {

		HibernateCriteriaBuilder builder = entityManager.unwrap( Session.class ).getCriteriaBuilder();
		JpaCriteriaQuery<Person> mainQuery = builder.createQuery( Person.class );

		JpaSubQuery<Tuple> q1 = mainQuery.subquery(Tuple.class);
		q1.multiselect(
				builder.literal("John").alias("name"),
				builder.literal("Doe").alias("surName")
		);

		JpaSubQuery<Tuple> q2 = mainQuery.subquery(Tuple.class);
		q2.multiselect(
				builder.literal("Jack").alias("name"),
				builder.literal("Black").alias("surName")
		);

		JpaSubQuery<Tuple> unionAllSubQuery = builder.unionAll(q1, q2);
		JpaDerivedRoot<Tuple> mainQueryRoot = mainQuery.from( unionAllSubQuery );

		mainQuery.multiselect(
				mainQueryRoot.get("name").alias("name"),
				mainQueryRoot.get("surName").alias("surName")
		);
		mainQuery.orderBy( builder.asc(mainQueryRoot.get("name")) );

		return entityManager.createQuery(mainQuery).getResultList();
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		Integer id;

		String name;

		String surName;

		@OneToMany
		List<Address> addresses =new ArrayList<>();

		public Person() {
		}

		public Person(Integer id, String name) {
			this(id,name,null);
		}

		public Person(Integer id, String name, String surName) {
			this.id = id;
			this.name = name;
			this.surName = surName;
		}

		public void addAddress(Address address){
			addresses.add( address );
			address.person = this;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getSurName() {
			return surName;
		}

		public List<Address> getAddresses() {
			return addresses;
		}
	}

	@Entity(name = "Address")
	@Table(name = "ADDRESS_TABLE")
	public static class Address {
		@Id
		Integer id;

		private String city;

		private String street;

		@ManyToOne
		private Person person;

		public Address() {
		}

		public Address(Integer id, String city, String street) {
			this.id = id;
			this.city = city;
			this.street = street;
		}
	}
}
