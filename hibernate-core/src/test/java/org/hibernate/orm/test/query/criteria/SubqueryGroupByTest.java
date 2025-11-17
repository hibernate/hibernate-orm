/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Jpa(
		annotatedClasses = {
				SubqueryGroupByTest.JobTile.class,
				SubqueryGroupByTest.Person.class,
		}
)
public class SubqueryGroupByTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					JobTile worker = new JobTile( 1, "Worker" );
					JobTile plumber = new JobTile( 2, "Plumber" );
					Person p1 = new Person( 1, "Luigi", worker );
					Person p2 = new Person( 2, "Andrea", plumber );

					entityManager.persist( worker );
					entityManager.persist( plumber );
					entityManager.persist( p1 );
					entityManager.persist( p2 );
				}
		);
	}

	@Test
	public void subqueryGroupTest(EntityManagerFactoryScope scope) {

		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaQuery<Person> criteriaQuery = criteriaBuilder.createQuery( Person.class );
					final Root<Person> person = criteriaQuery.from( Person.class );

					criteriaQuery.select( person );

					final Subquery<String> subquery = criteriaQuery.subquery( String.class );
					assertEquals( 0, subquery.getGroupList().size() );

					assertNull( subquery.getSelection() );

					final Root<JobTile> jobTitle = subquery.from( JobTile.class );
					subquery.select( jobTitle.get( "name" ) );
					assertNotNull( subquery.getSelection() );

					subquery.where( criteriaBuilder.equal( jobTitle.get( "name" ), "Worker" ) );

					subquery.groupBy( jobTitle.get( "name" ) );
					assertEquals( 1, subquery.getGroupList().size() );

					criteriaQuery.where( person.get( "jobTile" ).get( "name" ).in( subquery ) );

					List<Person> result = entityManager.createQuery( criteriaQuery ).getResultList();

					assertEquals( 1, result.size() );
					assertEquals( "Luigi", result.get( 0 ).getName() );

				}
		);
	}

	@Entity(name = "Person")
	@Table(name = "PERSON_TABLE")
	public static class Person {

		@Id
		private Integer id;

		private String name;

		@ManyToOne
		private JobTile jobTile;

		public Person() {
		}

		public Person(Integer id, String name, JobTile jobTile) {
			this.id = id;
			this.name = name;
			this.jobTile = jobTile;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public JobTile getJobTile() {
			return jobTile;
		}
	}

	@Entity(name = "JobTile")
	@Table(name = "JOBTITLE_TABLE")
	public static class JobTile {

		@Id
		private Integer id;

		private String name;

		public JobTile() {
		}

		public JobTile(Integer id, String name) {
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
