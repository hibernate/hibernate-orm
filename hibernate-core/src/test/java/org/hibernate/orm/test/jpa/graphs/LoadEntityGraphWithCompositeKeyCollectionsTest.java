/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.SessionFactory;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = {LoadEntityGraphWithCompositeKeyCollectionsTest.Exercise.class, LoadEntityGraphWithCompositeKeyCollectionsTest.Activity.class, LoadEntityGraphWithCompositeKeyCollectionsTest.ActivityAnswer.class, LoadEntityGraphWithCompositeKeyCollectionsTest.ActivityDocument.class,})

@JiraKey(value = "HHH-19137")
public class LoadEntityGraphWithCompositeKeyCollectionsTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().unwrap( SessionFactory.class ).getSchemaManager().truncateMappedObjects();
		scope.releaseEntityManagerFactory();
	}

	@Test
	void testLoadFromEntityWithAllCollectionsFilled(EntityManagerFactoryScope scope) {
		Integer exerciseId = scope.fromTransaction( entityManager -> {
			Activity activityWithAnswersAndDocuments = createActivity();

			ActivityAnswer activityAnswer1 = new ActivityAnswer(
					activityWithAnswersAndDocuments,
					"question_01",
					"answer_01" );
			ActivityAnswer activityAnswer2 = new ActivityAnswer(
					activityWithAnswersAndDocuments,
					"question_02",
					"answer_02" );

			Set<ActivityAnswer> answers = new HashSet<>();
			answers.add( activityAnswer1 );
			answers.add( activityAnswer2 );
			activityWithAnswersAndDocuments.setAnswers( answers );

			Set<ActivityDocument> documents = new HashSet<>();
			documents.add( new ActivityDocument( activityWithAnswersAndDocuments, "question_01", "document_01" ) );
			activityWithAnswersAndDocuments.setDocuments( documents );

			entityManager.persist( activityWithAnswersAndDocuments );
			return activityWithAnswersAndDocuments.getExercise().getId();
		} );

		scope.inTransaction( entityManager -> {
			List<Activity> activities = buildQuery( entityManager, exerciseId ).getResultList();

			assertEquals( 1, activities.size() );
			assertEquals( 2, activities.get( 0 ).getAnswers().size() );
			assertEquals( 1, activities.get( 0 ).getDocuments().size() );

		} );
	}

	@Test
	void testLoadFromEntityWithOneEmptyCollection(EntityManagerFactoryScope scope) {
		Integer exerciseId = scope.fromTransaction( entityManager -> {
			Activity activityWithoutDocuments = createActivity();

			ActivityAnswer activityAnswer1 = new ActivityAnswer( activityWithoutDocuments, "question_01", "answer_01" );
			ActivityAnswer activityAnswer2 = new ActivityAnswer( activityWithoutDocuments, "question_02", "answer_02" );

			Set<ActivityAnswer> answers = new HashSet<>();
			answers.add( activityAnswer1 );
			answers.add( activityAnswer2 );
			activityWithoutDocuments.setAnswers( answers );

			entityManager.persist( activityWithoutDocuments );
			return activityWithoutDocuments.getExercise().getId();

		} );

		scope.inTransaction( entityManager -> {
			List<Activity> activities = buildQuery( entityManager, exerciseId ).getResultList();

			assertEquals( 1, activities.size() );
			assertEquals( 2, activities.get( 0 ).getAnswers().size() );
			assertEquals( 0, activities.get( 0 ).getDocuments().size() );
		} );
	}

	private TypedQuery<Activity> buildQuery(EntityManager entityManager, Integer exerciseId) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Activity> query = builder.createQuery( Activity.class );

		Root<Activity> root = query.from( Activity.class );
		query.select( root ).where( builder.equal( root.get( "activityExerciseId" ).get( "exerciseId" ), exerciseId ) );

		TypedQuery<Activity> typedQuery = entityManager.createQuery( query );
		String graphType = GraphSemantic.LOAD.getJakartaHintName();
		String entityGraphName = "with.collections";
		typedQuery.setHint( graphType, entityManager.getEntityGraph( entityGraphName ) );

		return typedQuery;
	}

	private Activity createActivity() {
		return new Activity( new Exercise( 1, "Pull up" ), "general-ref" );
	}

	@Entity(name = "Activity")
	@Table(name = "activities")
	@NamedEntityGraph(name = "with.collections",
			attributeNodes = {@NamedAttributeNode(value = "answers"), @NamedAttributeNode(value = "documents")})
	public static class Activity {

		@EmbeddedId
		private ActivityExerciseId activityExerciseId;

		@MapsId("exerciseId")
		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@JoinColumn(name = "exercise_id")
		private Exercise exercise;

		@OneToMany(mappedBy = "activityAnswerId.activity", cascade = CascadeType.ALL)
		private Set<ActivityAnswer> answers = new HashSet<>();

		@OneToMany(mappedBy = "activityDocumentId.activity", orphanRemoval = true, cascade = CascadeType.ALL)
		private Set<ActivityDocument> documents = new HashSet<>();

		public Activity() {
		}

		public Activity(Exercise exercise, ActivityExerciseId activityExerciseId) {
			this.exercise = exercise;
			this.activityExerciseId = activityExerciseId;
		}

		public Activity(Exercise exercise, String activityId) {
			this( exercise, new ActivityExerciseId( exercise.getId(), activityId ) );
		}

		public Exercise getExercise() {
			return exercise;
		}

		public Set<ActivityAnswer> getAnswers() {
			return answers;
		}

		public Set<ActivityDocument> getDocuments() {
			return documents;
		}

		public void setAnswers(Set<ActivityAnswer> answers) {
			this.answers = answers;
		}

		public void setDocuments(Set<ActivityDocument> documents) {
			this.documents = documents;
		}
	}

	@Embeddable
	public static class ActivityExerciseId {

		private Integer exerciseId;

		@Column(name = "activity_id")
		private String activityId;

		public ActivityExerciseId() {
		}

		public ActivityExerciseId(Integer exerciseId, String activityId) {
			this.exerciseId = exerciseId;
			this.activityId = activityId;
		}

		@Override
		public boolean equals(Object o) {
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			ActivityExerciseId that = (ActivityExerciseId) o;
			return Objects.equals( exerciseId, that.exerciseId ) && Objects.equals( activityId, that.activityId );
		}

		@Override
		public int hashCode() {
			return Objects.hash( exerciseId, activityId );
		}
	}

	@Entity(name = "Exercise")
	@Table(name = "exercises")
	public static class Exercise {

		@Id
		private Integer id;

		private String name;

		public Exercise() {
		}

		public Exercise(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}
	}

	@Entity(name = "ActivityAnswer")
	@Table(name = "activity_answers")
	public static class ActivityAnswer {

		@EmbeddedId
		private ActivityAnswerId activityAnswerId;

		private String answer;

		public ActivityAnswer() {
		}

		public ActivityAnswer(ActivityAnswerId activityAnswerId, String answer) {
			this.activityAnswerId = activityAnswerId;
			this.answer = answer;
		}

		public ActivityAnswer(Activity activity, String questionId, String answer) {
			this( new ActivityAnswerId( activity, questionId ), answer );
		}
	}

	@Embeddable
	public static class ActivityAnswerId {

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumns({@JoinColumn(name = "exercise_id", referencedColumnName = "exercise_id"), @JoinColumn(
				name = "activity_id", referencedColumnName = "activity_id")})
		private Activity activity;

		@Column(name = "question_id")
		private String questionId;

		public ActivityAnswerId() {
		}

		public ActivityAnswerId(Activity activity, String questionId) {
			this.activity = activity;
			this.questionId = questionId;
		}

		@Override
		public boolean equals(Object o) {
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			ActivityAnswerId that = (ActivityAnswerId) o;
			return Objects.equals( activity, that.activity ) && Objects.equals( questionId, that.questionId );
		}

		@Override
		public int hashCode() {
			return Objects.hash( activity, questionId );
		}
	}

	@Entity(name = "ActivityDocument")
	@Table(name = "activity_documents")
	public static class ActivityDocument {

		@EmbeddedId
		private ActivityDocumentId activityDocumentId;

		private String name;

		public ActivityDocument() {
		}

		public ActivityDocument(ActivityDocumentId activityDocumentId, String name) {
			this.activityDocumentId = activityDocumentId;
			this.name = name;
		}

		public ActivityDocument(Activity activity, String questionId, String name) {
			this( new ActivityDocumentId( activity, questionId ), name );
		}
	}

	@Embeddable
	public static class ActivityDocumentId {

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumns({@JoinColumn(name = "exercise_id", referencedColumnName = "exercise_id"), @JoinColumn(
				name = "activity_id", referencedColumnName = "activity_id")})
		private Activity activity;

		@Column(name = "question_id")
		private String questionId;

		public ActivityDocumentId() {
		}

		public ActivityDocumentId(Activity activity, String questionId) {
			this.activity = activity;
			this.questionId = questionId;
		}

		@Override
		public boolean equals(Object o) {
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			ActivityDocumentId that = (ActivityDocumentId) o;
			return Objects.equals( activity, that.activity ) && Objects.equals( questionId, that.questionId );
		}

		@Override
		public int hashCode() {
			return Objects.hash( activity, questionId );
		}
	}
}
