/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.query.SemanticException;
import org.hibernate.type.descriptor.java.CoercionException;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Jpa(
		annotatedClasses = {
				QueryPredicateAndParameterComparableTest.Participation.class,
				QueryPredicateAndParameterComparableTest.Submission.class
		}
)
@JiraKey(value = "HHH-15802")
public class QueryPredicateAndParameterComparableTest {

	@Test
	public void testWrongInPredicateType(EntityManagerFactoryScope scope) {
		Assertions.assertThrows(
				SemanticException.class, () -> scope.inTransaction(
						entityManager -> {
							CriteriaBuilder builder = entityManager.getCriteriaBuilder();
							CriteriaQuery<Participation> criteria = builder.createQuery( Participation.class );
							Root<Participation> root = criteria.from( Participation.class );
							criteria.select( root );

							Subquery<Participation> subQuery = criteria.subquery( Participation.class );
							Root<Submission> rootSubQuery = subQuery.from( Submission.class );
							subQuery.select( rootSubQuery.join( "submitters" ) );

							criteria.where( root.get( "id" ).in( subQuery ) );
							entityManager.createQuery( criteria ).getResultList();
						}
				)
		);
	}

	@Test
	public void testCorrectInPredicateType(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Participation> criteria = builder.createQuery( Participation.class );
					Root<Participation> root = criteria.from( Participation.class );
					criteria.select( root );

					Subquery<Participation> subQuery = criteria.subquery( Participation.class );
					Root<Participation> rootSubQuery = subQuery.from( Participation.class );
					subQuery.select( rootSubQuery );

					criteria.where( root.in( subQuery ) );
					entityManager.createQuery( criteria ).getResultList();
				}
		);
	}

	@Test
	public void testWrongInPredicateType2(EntityManagerFactoryScope scope) {
		Assertions.assertThrows(
				SemanticException.class, () -> scope.inTransaction(
						entityManager -> {
							CriteriaBuilder builder = entityManager.getCriteriaBuilder();
							CriteriaQuery<Participation> criteria = builder.createQuery( Participation.class );
							Root<Participation> root = criteria.from( Participation.class );
							criteria.select( root );

							Subquery<Participation> subQuery = criteria.subquery( Participation.class );
							Root<Submission> rootSubQuery = subQuery.from( Submission.class );
							subQuery.select( rootSubQuery.get( "participation" ) );

							criteria.where( root.get( "id" ).in( subQuery ) );
							entityManager.createQuery( criteria ).getResultList();
						}
				)
		);
	}

	@Test
	public void testWrongTypeEqualPredicate(EntityManagerFactoryScope scope) {
		Assertions.assertThrows(
				CoercionException.class, () -> scope.inTransaction(
						entityManager -> {
							CriteriaBuilder builder = entityManager.getCriteriaBuilder();
							CriteriaQuery<Participation> criteria = builder.createQuery( Participation.class );
							Root<Participation> root = criteria.from( Participation.class );
							criteria.select( root );

							Subquery<Participation> subQuery = criteria.subquery( Participation.class );
							Root<Submission> rootSubQuery = subQuery.from( Submission.class );
							subQuery.select( rootSubQuery.get( "participation" ) );

							criteria.where( builder.equal( root.get( "id" ), new Participation() ) );
							entityManager.createQuery( criteria ).getResultList();
						}
				)
		);
	}

	@Test
	public void testWrongTypeEqualPredicate2(EntityManagerFactoryScope scope) {
		Assertions.assertThrows(
				SemanticException.class, () -> scope.inTransaction(
						entityManager -> {
							CriteriaBuilder builder = entityManager.getCriteriaBuilder();
							CriteriaQuery<Participation> criteria = builder.createQuery( Participation.class );
							Root<Participation> root = criteria.from( Participation.class );
							criteria.select( root );

							Subquery<Participation> subQuery = criteria.subquery( Participation.class );
							Root<Submission> rootSubQuery = subQuery.from( Submission.class );
							subQuery.select( rootSubQuery.get( "participation" ) );

							criteria.where( builder.equal( root, 1 ) );
							entityManager.createQuery( criteria ).getResultList();
						}
				)
		);
	}

	@Test
	public void test3(EntityManagerFactoryScope scope) {
		Assertions.assertThrows(
				IllegalArgumentException.class, () -> scope.inTransaction(
						entityManager -> {
							entityManager.createQuery( "select p from participations p where p = 1" ).getResultList();
						}
				)
		);
	}

	@Test
	public void testWrongTypeParameter(EntityManagerFactoryScope scope) {
		Assertions.assertThrows(
				IllegalArgumentException.class, () -> scope.inTransaction(
						entityManager -> {
							entityManager.createQuery( "select p from participations p where p = :id" ).setParameter( "id", 1 ).getResultList();
						}
				)
		);
	}

	@Entity(name = "participations")
	public static class Participation {

		private int id;

		private String description;

		private Set<Submission> submissions;

		@Id
		@GeneratedValue
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		@ManyToMany(fetch = FetchType.LAZY)
		@JoinTable(name = "submissions_participations", joinColumns = @JoinColumn(name = "submitters_id"), inverseJoinColumns = @JoinColumn(name = "submissions_submissionid"))
		public Set<Submission> getSubmissions() {
			return submissions;
		}

		public void setSubmissions(Set<Submission> submissions) {
			this.submissions = submissions;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

	}

	@Entity(name = "submissions")
	public static class Submission {

		private int submissionid;

		private String description;

		private Set<Participation> submitters = new HashSet<>();

		private Participation participation;

		@ManyToMany
		@JoinTable(name = "submissions_participations", inverseJoinColumns = @JoinColumn(name = "submitters_id"), joinColumns = @JoinColumn(name = "submissions_submissionid"))
		public Set<Participation> getSubmitters() {
			return submitters;
		}

		public void setSubmitters(Set<Participation> submitters) {
			this.submitters = submitters;
		}

		@Id
		@GeneratedValue
		public int getSubmissionid() {
			return submissionid;
		}

		public void setSubmissionid(int submissionid) {
			this.submissionid = submissionid;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		@ManyToOne
		public Participation getParticipation() {
			return participation;
		}

		public void setParticipation(Participation participation) {
			this.participation = participation;
		}
	}
}
