/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.community.dialect.TiDBDialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialects;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Jpa(
		annotatedClasses = {
				CriteriaWithWhereClauseAndColumnDefinitionTest.Submission.class,
				CriteriaWithWhereClauseAndColumnDefinitionTest.Task.class
		}
)
@JiraKey(value = "HHH-15805")
@RequiresDialects({
		@RequiresDialect(MariaDBDialect.class),
		@RequiresDialect(MySQLDialect.class),
		@RequiresDialect(H2Dialect.class),
		@RequiresDialect(HSQLDialect.class),
		@RequiresDialect(TiDBDialect.class),
		@RequiresDialect(SybaseDialect.class),
		@RequiresDialect(SybaseDialect.class),
})
public class CriteriaWithWhereClauseAndColumnDefinitionTest {

	@Test
	public void testCriteriaQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();

					CriteriaQuery<Tuple> criteria = builder.createTupleQuery();
					Root<Task> root = criteria.from( Task.class );

					Subquery<Long> subQuery = criteria.subquery( Long.class );
					Root<Submission> rootSubQuery = subQuery.from( Submission.class );
					subQuery.where( builder.equal( rootSubQuery.get( "peerReviewParticipation" ), 5 ) );
					subQuery.select( builder.count( rootSubQuery ) );

					criteria.select( builder.tuple( root.get( "id" ), subQuery ) );

					TypedQuery<Tuple> query = entityManager.createQuery( criteria );

					query.getResultList();
				}
		);
	}

	@Entity(name = "submission")
	@Table(name = "SUBMISSION_TABLE")
	public static class Submission {

		private int submissionid;
		private Task task;
		private Integer peerReviewParticipation;


		@ManyToOne
		@JoinColumn(name = "taskid", nullable = false)
		public Task getTask() {
			return task;
		}

		public void setTask(Task task) {
			this.task = task;
		}

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public int getSubmissionid() {
			return submissionid;
		}

		public void setSubmissionid(int submissionid) {
			this.submissionid = submissionid;
		}

		@Column(columnDefinition = "TINYINT")
		public Integer getPeerReviewParticipation() {
			return peerReviewParticipation;
		}

		public void setPeerReviewParticipation(Integer peerReviewParticipation) {
			this.peerReviewParticipation = peerReviewParticipation;
		}
	}

	@Entity(name = "task")
	@Table(name = "TASK_TABLE")
	public static class Task {

		private int taskid;

		private String description;

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public int getTaskid() {
			return taskid;
		}

		public void setTaskid(int taskid) {
			this.taskid = taskid;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}
}
