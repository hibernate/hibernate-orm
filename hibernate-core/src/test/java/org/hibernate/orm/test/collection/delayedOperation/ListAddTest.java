/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.delayedOperation;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.collection.spi.PersistentCollection;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public class ListAddTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Quizz.class, Question.class };
	}

	@BeforeEach
	public void before() {
		inTransaction(
				session -> {
					Quizz quizz = new Quizz( 1 );
					session.persist( quizz );
					quizz.addQuestion( new Question( 1, "question 1" ) );
					quizz.addQuestion( new Question( 2, "question 2" ) );
					quizz.addQuestion( new Question( 3, "question 3" ) );
				}
		);
	}

	@AfterEach
	public void after() {
		inTransaction(
				session -> {
					session.createQuery( "delete Question" ).executeUpdate();
					session.createQuery( "delete Quizz" ).executeUpdate();
				}
		);
	}

	/**
	 * This test fails, but shouldn't
	 */
	@Test
	public void addQuestionWithIndexShouldAddQuestionAtSpecifiedIndex() {
		inTransaction(
				session -> {
					Quizz quizz = session.get( Quizz.class, 1 );
					quizz.addQuestion( 1, new Question( 4, "question that should be at index 1" ) );
				}
		);

		inTransaction(
				session -> {
					Quizz quizz = session.get( Quizz.class, 1 );
					assertEquals( 4, quizz.getQuestions().size() );
					assertEquals( 4, quizz.getQuestions().get( 1 ).getId().longValue() );
				}
		);
	}

	@Test
	public void addQuestionToDetachedQuizz() {
		Quizz quizz = inTransaction(
				session -> {
					return session.get( Quizz.class, 1 );
				}
		);

		assertFalse( ( (PersistentCollection) quizz.getQuestions() ).wasInitialized() );

		try {
			// this is the crux of the comment on HHH-9195 in regard to uninitialized, detached collections and
			// not allowing additions
			quizz.addQuestion( new Question( 4, "question 4" ) );

			// indexed-addition should fail
			quizz.addQuestion( 1, new Question( 5, "question that should be at index 1" ) );
			fail( "Expecting a failure" );
		}
		catch (LazyInitializationException ignore) {
			// expected
		}

//		session = openSession();
//		session.beginTransaction();
//		session.merge( quizz );
//		session.getTransaction().commit();
//		session.close();
//
//		session = openSession();
//		session.getTransaction().begin();
//		quizz = session.get( Quizz.class,  1);
//		assertEquals( 5, quizz.getQuestions().size() );
//		assertEquals( 5, quizz.getQuestions().get( 1 ).getId().longValue() );
//		session.getTransaction().commit();
//		session.close();
	}

	/**
	 * This test succeeds thanks to a dirty workaround consisting in initializing the ordered question list after the
	 * question has been inserted
	 */
	@Test
	public void addQuestionWithIndexAndInitializeTheListShouldAddQuestionAtSpecifiedIndex() {

		inTransaction(
				session -> {
					Quizz quizz = session.get( Quizz.class, 1 );
					quizz.addQuestionAndInitializeLazyList(
							1,
							new Question( 4, "question that should be at index 1" )
					);
				}
		);

		inTransaction(
				session -> {
					Quizz quizz = session.get( Quizz.class, 1 );

					assertEquals( 4, quizz.getQuestions().size() );
					assertEquals( 4, quizz.getQuestions().get( 1 ).getId().longValue() );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10375")
	public void testAddQuestionAfterSessionIsClosed() {

		Quizz quizz = inTransaction(
				session -> {
					Quizz q = session.get( Quizz.class, 1 );
					assertThat( "expected 3 questions", q.getQuestions().size(), is( 3 ) );
					return q;
				}
		);

		quizz.addQuestion( new Question( 4, "question 4" ) );
		assertThat( "expected 4 questions", quizz.getQuestions().size(), is( 4 ) );

		quizz.addQuestion( 1, new Question( 5, "question 5" ) );
		assertThat( "expected 5 questions", quizz.getQuestions().size(), is( 5 ) );
	}

	@Entity(name = "Question")
	@Table(name = "Question")
	public static class Question {
		@Id
		private Integer id;
		private String text;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		private Quizz quizz;

		public Question() {
		}

		public Question(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public Quizz getQuizz() {
			return quizz;
		}

		public void setQuizz(Quizz quizz) {
			this.quizz = quizz;
		}

		@Override
		public String toString() {
			return "Question{" +
					"id=" + id +
					", text='" + text + '\'' +
					'}';
		}
	}

	@Entity(name = "Quizz")
	@Table(name = "Quiz")
	public static class Quizz {
		@Id
		private Integer id;

		@OneToMany(mappedBy = "quizz", cascade = CascadeType.ALL, orphanRemoval = true)
		@OrderColumn(name = "position")
		private List<Question> questions = new ArrayList<>();

		public Quizz() {
		}

		public Quizz(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<Question> getQuestions() {
			return questions;
		}

		public void addQuestion(Question question) {
			question.setQuizz( this );
			questions.add( question );
		}

		public void addQuestion(int index, Question question) {
			question.setQuizz( this );
			questions.add( index, question );
		}

		public void addQuestionAndInitializeLazyList(int index, Question question) {
			question.setQuizz( this );
			questions.add( index, question );
			Hibernate.initialize( questions );
		}
	}
}
