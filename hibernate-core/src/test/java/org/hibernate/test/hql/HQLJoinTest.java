package org.hibernate.test.hql;

import org.hibernate.Query;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Oleksander Dukhno
 */
public class HQLJoinTest extends BaseCoreFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Question.class,
				Student.class,
				StudentAnswer.class
		};
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7321")
	public void testCrossJoin() {
		Session s = openSession();
		s.beginTransaction();
		Student student = new Student();
		student.setId( 1L );
		Collection<Question> questions = new ArrayList<Question>();
		for ( int i = 1; i < 4; i++ ) {
			Question question = new Question();
			question.setId( i );
			questions.add( question );
			s.persist( question );
		}
		Iterator<Question> listIterator = questions.iterator();
		List<StudentAnswer> studentAnswers = new ArrayList<StudentAnswer>();
		for ( int i = 1; i < 3; i++ ) {
			Question question = listIterator.next();
			StudentAnswer sa = new StudentAnswer();
			sa.setId( i );
			sa.setAnswer( "Answer on the question N " + question.getId() );
			sa.setStudent( student );
			sa.setQuestion( question );
			question.setAnswers( Collections.singletonList( sa ) );
			s.persist( sa );
			studentAnswers.add( sa );
		}
		student.setAnswers( studentAnswers );
		s.persist( student );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();

		Query q = s.createQuery(
				"SELECT s.id, q.id, sa.answer\n" +
						"FROM Student s, Question q\n" +
						"LEFT JOIN s.answers AS sa WITH sa.question.id = q.id"
		);
		q.list();

		s.getTransaction().commit();
		s.close();
	}

}
