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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		annotatedClasses = LocateTest.Person.class
)
public class LocateTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					for ( int i = 0; i < 10; i++ ) {
						Person person;
						if ( i == 3 ) {
							person = new Person( i, "Andrea" );
						}
						else if ( i == 4 ) {
							person = new Person( i, "Andrew" );
						}
						else {
							person = new Person( i, "Luigi " + i );
						}
						entityManager.persist( person );
					}
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope){
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testLocate(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					final CriteriaQuery<Integer> query = criteriaBuilder.createQuery( Integer.class );
					final Root<Person> personRoot = query.from( Person.class );

					query.select( personRoot.get( "id" ) );

					final Expression<Integer> locate = criteriaBuilder.locate(
							personRoot.get( "name" ),
							criteriaBuilder.literal( "nd" )
					);

					query.where( criteriaBuilder.equal( locate, 2 ) );

					final List<Integer> results = entityManager.createQuery( query ).getResultList();

					assertEquals( 2, results.size() );
					assertTrue( results.contains( 3 ) );
					assertTrue( results.contains( 4 ) );
				}
		);
	}

	@Test
	public void testLocate2(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					final CriteriaQuery<Integer> query = criteriaBuilder.createQuery( Integer.class );
					final Root<Person> personRoot = query.from( Person.class );

					query.select( personRoot.get( "id" ) );

					final Expression<Integer> locate = criteriaBuilder.locate(
							personRoot.get( "name" ),
							"nd" ,
							1
					);

					query.where( criteriaBuilder.equal( locate, 2 ) );

					final List<Integer> results = entityManager.createQuery( query ).getResultList();

					assertEquals( 2, results.size() );
					assertTrue( results.contains( 3 ) );
					assertTrue( results.contains( 4 ) );
				}
		);
	}

	@Test
	public void locateQueryTest(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					final List<Integer> ids = entityManager.createQuery(
									"select distinct p.id from Person p where locate('nd', p.name) = 2" )
							.getResultList();

					assertEquals( 2, ids.size() );
					assertTrue( ids.contains( 3 ) );
					assertTrue( ids.contains( 4 ) );
				}
		);
	}

	@Entity(name = "Person")
	@Table(name = "PERSON_TABLE")
	public static class Person {

		@Id
		private Integer id;

		private String name;

		Person() {
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
