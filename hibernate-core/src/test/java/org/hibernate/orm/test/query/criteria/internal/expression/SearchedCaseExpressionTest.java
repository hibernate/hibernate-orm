/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria.internal.expression;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Vasyl Danyliuk
 */
@DomainModel(
		annotatedClasses = {
				SearchedCaseExpressionTest.Event.class,
				SearchedCaseExpressionTest.EventType.class
		}
)
@SessionFactory
public class SearchedCaseExpressionTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testCaseClause(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			CriteriaBuilder cb = session.getCriteriaBuilder();

			CriteriaQuery<Event> criteria = cb.createQuery( Event.class );

			Root<Event> event = criteria.from( Event.class );
			Path<EventType> type = event.get( "type" );

			Expression<String> caseWhen = cb.<EventType, String>selectCase( type )
					.when( EventType.TYPE1, "Admin Event" )
					.when( EventType.TYPE2, "User Event" )
					.when( EventType.TYPE3, "Reporter Event" )
					.otherwise( "" );

			criteria.select( event );
			criteria.where( cb.equal( caseWhen, "Admin Event" ) ); // OK when use cb.like() method and others
			List<Event> resultList = session.createQuery( criteria ).getResultList();

			assertThat( resultList ).isNotNull();
		} );
	}

	@Test
	public void testEqualClause(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			CriteriaBuilder cb = session.getCriteriaBuilder();

			CriteriaQuery<Event> criteria = cb.createQuery( Event.class );

			Root<Event> event = criteria.from( Event.class );
			Path<EventType> type = event.get( "type" );

			Expression<String> caseWhen = cb.<String>selectCase()
					.when( cb.equal( type, EventType.TYPE1 ), "Type1" )
					.otherwise( "" );

			criteria.select( event );
			criteria.where( cb.equal( caseWhen, "Admin Event" ) ); // OK when use cb.like() method and others
			List<Event> resultList = session.createQuery( criteria ).getResultList();

			assertThat( resultList ).isNotNull();
		} );
	}

	@Test
	@JiraKey(value = "HHH-13167")
	public void testMissingElseClause(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Event event = new Event();
			event.id = 1L;
			event.type = EventType.TYPE1;

			session.persist( event );
		} );

		scope.inTransaction( session -> {
			CriteriaBuilder cb = session.getCriteriaBuilder();

			CriteriaQuery<Event> criteria = cb.createQuery( Event.class );

			Root<Event> root = criteria.from( Event.class );
			Path<EventType> type = root.get( "type" );

			Expression<String> caseWhen = cb.<String>selectCase()
					.when( cb.equal( type, EventType.TYPE1 ), "Matched" );

			criteria.select( root );
			criteria.where( cb.equal( caseWhen, "Matched" ) );

			Event event = session.createQuery( criteria ).getSingleResult();
			assertThat( event.id ).isEqualTo( 1L );
		} );
	}


	@Test
	@JiraKey( "HHH-19896" )
	public void testCaseWhenWithOtherwiseClauseExecutedAfterOneWithout(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Event event = new Event();
			event.id = 1L;
			event.type = EventType.TYPE1;

			session.persist( event );
		} );

		scope.inTransaction( session -> {
			CriteriaBuilder cb = session.getCriteriaBuilder();

			CriteriaQuery<Event> criteria = cb.createQuery( Event.class );

			Root<Event> root = criteria.from( Event.class );
			Path<EventType> type = root.get( "type" );

			Expression<String> caseWhen = cb.<String>selectCase()
					.when( cb.equal( type, EventType.TYPE1 ), "Matched" );

			criteria.select( root );
			criteria.where( cb.equal( caseWhen, "Matched" ) );

			Event event = session.createQuery( criteria ).getSingleResult();
			assertThat( event.id ).isEqualTo( 1L );
		} );

		scope.inTransaction( session -> {
			CriteriaBuilder cb = session.getCriteriaBuilder();

			CriteriaQuery<Event> criteria = cb.createQuery( Event.class );

			Root<Event> event = criteria.from( Event.class );
			Path<EventType> type = event.get( "type" );

			Expression<String> caseWhen = cb.<String>selectCase()
					.when( cb.equal( type, EventType.TYPE1 ), "Type1" )
					.otherwise( "" );

			criteria.select( event );
			criteria.where( cb.equal( caseWhen, "Admin Event" ) ); // OK when use cb.like() method and others
			List<Event> resultList = session.createQuery( criteria ).getResultList();

			assertThat( resultList ).isNotNull();
			assertThat( resultList ).hasSize( 0 );
		} );
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		private Long id;

		@Column
		@Enumerated(EnumType.STRING)
		private EventType type;

		protected Event() {
		}

		public EventType getType() {
			return type;
		}

		public Event type(EventType type) {
			this.type = type;
			return this;
		}
	}

	public enum EventType {
		TYPE1, TYPE2, TYPE3
	}
}
