/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.selectcase;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.metadata.Person_;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@TestForIssue(jiraKey = "HHH-12230")
@SkipForDialect(value = DB2Dialect.class, comment = "We would need casts in the case clauses. See HHH-12822.")
public class GroupBySelectCaseTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12230")
	public void selectCaseInGroupByAndSelectExpression() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<Person> from = query.from( Person.class );

			Predicate childPredicate = cb.between( from.get( Person_.AGE ), 0, 10 );
			Predicate teenagerPredicate = cb.between( from.get( Person_.AGE ), 11, 20 );
			CriteriaBuilder.Case<String> selectCase = cb.selectCase();
			selectCase.when( childPredicate, "child" )
					.when( teenagerPredicate, "teenager" )
					.otherwise( "adult" );

			query.multiselect( selectCase );
			query.groupBy( selectCase );

			List<Tuple> resultList = entityManager.createQuery( query ).getResultList();
			assertNotNull( resultList );
			assertTrue( resultList.isEmpty() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12230")
	public void selectCaseInOrderByAndSelectExpression() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			Root<Person> from = query.from( Person.class );

			Predicate childPredicate = cb.between( from.get( Person_.AGE ), 0, 10 );
			Predicate teenagerPredicate = cb.between( from.get( Person_.AGE ), 11, 20 );
			CriteriaBuilder.Case<String> selectCase = cb.selectCase();
			selectCase.when( childPredicate, "child" )
					.when( teenagerPredicate, "teenager" )
					.otherwise( "adult" );

			query.multiselect( selectCase );
			query.orderBy( cb.asc( selectCase ) );

			List<Tuple> resultList = entityManager.createQuery( query ).getResultList();
			assertNotNull( resultList );
			assertTrue( resultList.isEmpty() );
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private Integer age;
	}
}
