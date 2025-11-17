/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = {
				CriteriaFunctionParametersBindingTest.Person.class
		}
)
public class CriteriaFunctionParametersBindingTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Person person1 = new Person( 1, "Luigi" );
					Person person2 = new Person( 2, null );
					Person person3 = new Person( 3, "" );
					Person person4 = new Person( 4, "Andrea" );

					entityManager.persist( person1 );
					entityManager.persist( person2 );
					entityManager.persist( person3 );
					entityManager.persist( person4 );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}


	@Test
	public void testParameterBinding(EntityManagerFactoryScope scope) {

		scope.inEntityManager(

				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaQuery<Person> criteriaQuery = criteriaBuilder.createQuery( Person.class );
					final Root<Person> person = criteriaQuery.from( Person.class );
					criteriaQuery.where( criteriaBuilder.equal(
							person.get( "name" ),
							criteriaBuilder.substring(
									criteriaBuilder.parameter( String.class, "string" ),
									criteriaBuilder.parameter( Integer.class, "start" ),
									criteriaBuilder.parameter( Integer.class, "length" )
							)
					) );
					criteriaQuery.select( person );

					final TypedQuery<Person> query = entityManager.createQuery( criteriaQuery );
					query.setParameter( "string", "aLuigi" );
					query.setParameter( "start", 2 );
					query.setParameter( "length", 6 );

					List<Person> people = query.getResultList();
					assertEquals( 1, people.size() );
					assertEquals( 1, people.get( 0 ).getId() );

				}
		);
	}

	@Test
	public void testPredicateArray(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					final CriteriaQuery<String> query = criteriaBuilder.createQuery( String.class );
					final Root<Person> person = query.from( Person.class );

					final Predicate[] predicates = {
							criteriaBuilder.equal(
									person.get( "name" ),
									criteriaBuilder.substring(
											criteriaBuilder.parameter( String.class, "string" ),
											criteriaBuilder.parameter( Integer.class, "start" ),
											criteriaBuilder.parameter( Integer.class, "length" )
									)
							),
							criteriaBuilder.equal(
									person.get( "name" ),
									criteriaBuilder.substring(
											criteriaBuilder.parameter( String.class, "string" ),
											criteriaBuilder.parameter( Integer.class, "start" ),
											criteriaBuilder.parameter( Integer.class, "length" )
									)
							)
					};

					query.select( person.get( "name" ) ).having( predicates ).groupBy( person.get( "name" ) );

					final TypedQuery<String> typedQuery = entityManager.createQuery( query );
					typedQuery.setParameter( "string", "aLuigi" );
					typedQuery.setParameter( "start", 2 );
					typedQuery.setParameter( "length", 6 );

					final List<String> names = typedQuery.getResultList();

					assertEquals( 1, names.size() );
					assertEquals( "Luigi", names.get( 0 ) );
				}
		);
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

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

	}

}
