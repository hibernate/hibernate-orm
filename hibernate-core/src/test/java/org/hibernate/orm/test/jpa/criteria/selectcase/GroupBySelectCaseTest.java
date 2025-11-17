/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.selectcase;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.orm.test.jpa.metadata.Person_;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-12230")
@DomainModel( annotatedClasses = GroupBySelectCaseTest.Person.class )
@SessionFactory
public class GroupBySelectCaseTest {

	@Test
	public void selectCaseInGroupByAndSelectExpression(SessionFactoryScope sessions) {
		sessions.inTransaction( (entityManager) -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Tuple> query = cb.createTupleQuery();
			final Root<Person> from = query.from( Person.class );

			final Predicate childPredicate = cb.between( from.get( Person_.AGE ), 0, 10 );
			final Predicate teenagerPredicate = cb.between( from.get( Person_.AGE ), 11, 20 );
			final CriteriaBuilder.Case<String> selectCase = cb.selectCase();
			selectCase.when( childPredicate, "child" )
					.when( teenagerPredicate, "teenager" )
					.otherwise( "adult" );

			query.multiselect( selectCase );
			query.groupBy( selectCase );

			final List<Tuple> resultList = entityManager.createQuery( query ).getResultList();
			assertThat( resultList ).isNotNull();
			assertThat( resultList ).isEmpty();
		} );
	}

	@Test
	public void selectCaseInOrderByAndSelectExpression(SessionFactoryScope sessions) {
		sessions.inTransaction( (entityManager) -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Tuple> query = cb.createTupleQuery();
			final Root<Person> from = query.from( Person.class );

			final Predicate childPredicate = cb.between( from.get( Person_.AGE ), 0, 10 );
			final Predicate teenagerPredicate = cb.between( from.get( Person_.AGE ), 11, 20 );
			final CriteriaBuilder.Case<String> selectCase = cb.selectCase();
			selectCase.when( childPredicate, "child" )
					.when( teenagerPredicate, "teenager" )
					.otherwise( "adult" );

			query.multiselect( selectCase );
			query.orderBy( cb.asc( selectCase ) );

			final List<Tuple> resultList = entityManager.createQuery( query ).getResultList();
			assertThat( resultList ).isNotNull();
			assertThat( resultList ).isEmpty();
		} );
	}

	@Entity(name = "Person")
	@Table(name="persons")
	public static class Person {
		@Id
		private Long id;
		private Integer age;
	}
}
