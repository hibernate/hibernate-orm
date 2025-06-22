/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;


import java.util.List;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = NamedQueryTest.Person.class,
		properties = @Setting(name = AvailableSettings.CRITERIA_COPY_TREE, value = "true")
)
public class NamedQueryTest {

	@BeforeEach
	public void setup(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.persist( new Person( 1, "Andrea" ) );
					entityManager.persist( new Person( 2, "Alberto" ) );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testNameQueryCreationFromCriteria(EntityManagerFactoryScope scope) {

		final EntityManagerFactory entityManagerFactory = scope.getEntityManagerFactory();

		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManagerFactory.getCriteriaBuilder();
					final CriteriaQuery<Integer> query = criteriaBuilder.createQuery( Integer.class );
					final Root<Person> person = query.from( Person.class );

					query.select( person.get( "id" ) );
					query.where( criteriaBuilder.equal( person.get( "name" ), "Alberto" ) );

					entityManagerFactory.addNamedQuery( "criteria_query", entityManager.createQuery( query ) );

					List<Integer> ids = entityManager.createNamedQuery( "criteria_query", Integer.class )
							.getResultList();
					assertEquals( 1, ids.size() );
					assertEquals( 2, ids.get( 0 ) );
				}
		);

	}

	@Test
	public void testNativeWithMaxResults(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final Query nativeQuery = entityManager.createNativeQuery(
							"Select p.id from PERSON_TABLE p" );
					nativeQuery.setMaxResults( 1 );
					scope.getEntityManagerFactory().addNamedQuery( "native", nativeQuery );

					final Query namedQuery = entityManager.createNamedQuery( "native" );
					assertEquals( 1, namedQuery.getMaxResults() );

					namedQuery.setMaxResults( 2 );
					assertEquals( 2, namedQuery.getMaxResults() );

					final List<Integer> ids = namedQuery.getResultList();
					assertEquals( 2, ids.size() );
					assertThat( ids, hasItems( 1, 2 ) );
				} );
	}

	@Test
	public void testCriteriaWithMaxResults(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaQuery<Integer> criteriaQuery = criteriaBuilder.createQuery( Integer.class );
					final Root<Person> person = criteriaQuery.from( Person.class );
					criteriaQuery.select( person.get( "id" ) );
					criteriaQuery.orderBy( criteriaBuilder.asc( person.get( "id" ) ) );

					final TypedQuery<Integer> typedQuery = entityManager.createQuery( criteriaQuery );
					typedQuery.setMaxResults( 1 );

					scope.getEntityManagerFactory().addNamedQuery( "criteria", typedQuery );

					final Query namedQuery = entityManager.createNamedQuery( "criteria" );
					assertEquals( 1, namedQuery.getMaxResults() );
					namedQuery.setMaxResults( 2 );
					assertEquals( 2, namedQuery.getMaxResults() );

					final List<Integer> ids = namedQuery.getResultList();
					assertEquals( 2, ids.size() );
					assertThat( ids, hasItems( 1, 2 ) );
				} );
	}

	@Test
	public void testHqlWithMaxResults(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final Query query = entityManager.createQuery( "Select p.id from Person p" );
					query.setMaxResults( 1 );
					scope.getEntityManagerFactory().addNamedQuery( "query", query );

					final Query namedQuery = entityManager.createNamedQuery( "query" );
					assertEquals( 1, namedQuery.getMaxResults() );

					namedQuery.setMaxResults( 2 );
					assertEquals( 2, namedQuery.getMaxResults() );

					final List<Integer> ids = namedQuery.getResultList();
					assertEquals( 2, ids.size() );
					assertThat( ids, hasItems( 1, 2 ) );
				} );
	}

	@Entity(name = "Person")
	@Table(name = "PERSON_TABLE")
	public static class Person {
		@Id
		private Integer id;

		private String name;

		public Person() {
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

}
