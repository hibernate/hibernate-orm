/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs.embeddedid;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.orm.test.jpa.graphs.embeddedid.entities.Activity;
import org.hibernate.orm.test.jpa.graphs.embeddedid.entities.ActivityAnswer;
import org.hibernate.orm.test.jpa.graphs.embeddedid.entities.ActivityAnswerId;
import org.hibernate.orm.test.jpa.graphs.embeddedid.entities.ActivityDocument;
import org.hibernate.orm.test.jpa.graphs.embeddedid.entities.ActivityDocumentId;
import org.hibernate.orm.test.jpa.graphs.embeddedid.entities.ActivityExerciseId;
import org.hibernate.orm.test.jpa.graphs.embeddedid.entities.Exercise;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = {
				Exercise.class,
				Activity.class,
				ActivityExerciseId.class,
				ActivityAnswer.class,
				ActivityAnswerId.class,
				ActivityDocument.class,
				ActivityDocumentId.class
		}
)

@JiraKey(value = "HHH-19137")
public class LoadEntityGraphWithCompositeKeyCollectionsTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.releaseEntityManagerFactory();
	}

	@Test
	void testLoadFromEntityWithAllCollectionsFilled(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
					Activity activityWithAnswersAndDocuments = createActivity();

					ActivityAnswer activityAnswer1 = createActivityAnswer(
							activityWithAnswersAndDocuments, "question_01",
							"answer_01" );
					ActivityAnswer activityAnswer2 = createActivityAnswer(
							activityWithAnswersAndDocuments, "question_02",
							"answer_02" );

					Set<ActivityAnswer> answers = new HashSet<>();
					answers.add( activityAnswer1 );
					answers.add( activityAnswer2 );
					activityWithAnswersAndDocuments.setAnswers( answers );

					Set<ActivityDocument> documents = new HashSet<>();
					documents.add( createActivityDocument( activityWithAnswersAndDocuments, "question_01", "document_01" ) );
					activityWithAnswersAndDocuments.setDocuments( documents );

					entityManager.persist( activityWithAnswersAndDocuments );
				}
		);

		scope.inTransaction( entityManager -> {

			List<Activity> activities = buildQuery( entityManager ).getResultList();

			assertEquals( 1, activities.size() );
			assertEquals( 2, activities.get( 0 ).getAnswers().size() );
			assertEquals( 1, activities.get( 0 ).getDocuments().size() );

		} );
	}

	@Test
	void testLoadFromEntityWithOneEmptyCollection(EntityManagerFactoryScope scope) {
		System.out.println( "!! one empty collection" );
		scope.inTransaction( entityManager -> {
					Activity activityWithoutDocuments = createActivity();

					ActivityAnswer activityAnswer1 = createActivityAnswer( activityWithoutDocuments, "question_01",
							"answer_01" );
					ActivityAnswer activityAnswer2 = createActivityAnswer( activityWithoutDocuments, "question_02",
							"answer_02" );

					Set<ActivityAnswer> answers = new HashSet<>();
					answers.add( activityAnswer1 );
					answers.add( activityAnswer2 );
					activityWithoutDocuments.setAnswers( answers );

					entityManager.persist( activityWithoutDocuments );
				}
		);

		scope.inTransaction( entityManager -> {
			List<Activity> activities = buildQuery( entityManager ).getResultList();

			assertEquals( 1, activities.size() );
			assertEquals( 2, activities.get( 0 ).getAnswers().size() );
			assertEquals( 0, activities.get( 0 ).getDocuments().size() );
		} );
	}

	private TypedQuery<Activity> buildQuery(EntityManager entityManager) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Activity> query = builder.createQuery( Activity.class );

		Root<Activity> root = query.from( Activity.class );
		query.select( root )
				.where( builder.equal( root.get( "activityExerciseId" ).get( "exerciseId" ), 1 ) );

		TypedQuery<Activity> typedQuery = entityManager.createQuery( query );
		String graphType = GraphSemantic.LOAD.getJakartaHintName();
		String entityGraphName = "with.collections";
		typedQuery.setHint( graphType, entityManager.getEntityGraph( entityGraphName ) );

		return typedQuery;
	}

	private Activity createActivity() {
		Exercise exercise = new Exercise();
		Activity activity = new Activity();
		ActivityExerciseId activityExerciseId = new ActivityExerciseId();
		activityExerciseId.setExerciseId( exercise.getId() );
		activityExerciseId.setActivityId( "general-ref" );
		activity.setExercise( exercise ).setActivityExerciseId( activityExerciseId );
		return activity;
	}

	private ActivityAnswer createActivityAnswer(Activity activity, String questionId, String answer) {
		ActivityAnswer newAnswer = new ActivityAnswer();
		ActivityAnswerId answerId = new ActivityAnswerId();
		answerId.setActivity( activity ).setQuestionId( questionId );
		newAnswer.setActivityAnswerId( answerId );
		newAnswer.setAnswer( answer );
		return newAnswer;
	}

	private ActivityDocument createActivityDocument(Activity activity, String questionId, String name) {
		ActivityDocument newDocument = new ActivityDocument();
		ActivityDocumentId documentId = new ActivityDocumentId();
		documentId.setActivity( activity ).setQuestionId( questionId );
		newDocument.setActivityDocumentId( documentId );
		newDocument.setName( name );
		return newDocument;
	}
}
