package org.hibernate.orm.test.jpa.criteria;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Jpa(
		annotatedClasses = {
				InCriteriaTest.Participation.class,
				InCriteriaTest.Submission.class
		}
)
@TestForIssue( jiraKey = "HHH-15802")
public class InCriteriaTest {

	@Test
	public void testIn(EntityManagerFactoryScope scope){
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Participation> criteria = builder.createQuery( Participation.class);
					Root<Participation> root = criteria.from( Participation.class);
					criteria.select(root);

					Subquery<Participation> subQuery = criteria.subquery( Participation.class);
					Root<Submission> rootSubQuery = subQuery.from(Submission.class);
					subQuery.select(rootSubQuery.join( "submitters"));

					criteria.where(root.get("id").in(subQuery));
					entityManager.createQuery(criteria).getResultList();
				}
		);
	}

	@Entity(name = "participations")
	public static class Participation  {

		private int id;

		private String description;

		private Set<Submission> submissions;

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
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
	public class Submission  {

		private int submissionid;

		private String description;

		private Set<Participation> submitters = new HashSet<>();

		@ManyToMany
		@JoinTable(name = "submissions_participations", inverseJoinColumns = @JoinColumn(name = "submitters_id"), joinColumns = @JoinColumn(name = "submissions_submissionid"))
		public Set<Participation> getSubmitters() {
			return submitters;
		}

		public void setSubmitters(Set<Participation> submitters) {
			this.submitters = submitters;
		}

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
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
	}
}
