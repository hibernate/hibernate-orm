/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria.internal.expression;

import java.util.List;
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


import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Vasyl Danyliuk
 */
public class SearchedCaseExpressionTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testCaseClause() {
		doInHibernate( this::sessionFactory, session -> {
			CriteriaBuilder cb = session.getCriteriaBuilder();

			CriteriaQuery<Event> criteria = cb.createQuery(Event.class);

			Root<Event> event = criteria.from(Event.class);
			Path<EventType> type = event.get("type");

			Expression<String> caseWhen = cb.<EventType, String>selectCase(type)
					.when(EventType.TYPE1, "Admin Event")
					.when(EventType.TYPE2, "User Event")
					.when(EventType.TYPE3, "Reporter Event")
					.otherwise("");

			criteria.select(event);
			criteria.where(cb.equal(caseWhen, "Admin Event")); // OK when use cb.like() method and others
			List<Event> resultList = session.createQuery(criteria).getResultList();

			Assert.assertNotNull(resultList);
		} );
	}

	@Test
	public void testEqualClause() {
		doInHibernate( this::sessionFactory, session -> {
			CriteriaBuilder cb = session.getCriteriaBuilder();

			CriteriaQuery<Event> criteria = cb.createQuery(Event.class);

			Root<Event> event = criteria.from(Event.class);
			Path<EventType> type = event.get("type");

			Expression<String> caseWhen = cb.<String>selectCase()
					.when(cb.equal(type, EventType.TYPE1), "Type1")
					.otherwise("");

			criteria.select(event);
			criteria.where(cb.equal(caseWhen, "Admin Event")); // OK when use cb.like() method and others
			List<Event> resultList = session.createQuery(criteria).getResultList();


			Assert.assertNotNull(resultList);
		} );
	}

	@Test
	@JiraKey(value = "HHH-13167")
	public void testMissingElseClause() {
		doInHibernate( this::sessionFactory, session -> {
			Event event = new Event();
			event.id = 1L;
			event.type = EventType.TYPE1;

			session.persist( event );
		} );

		doInHibernate( this::sessionFactory, session -> {
			CriteriaBuilder cb = session.getCriteriaBuilder();

			CriteriaQuery<Event> criteria = cb.createQuery( Event.class );

			Root<Event> root = criteria.from( Event.class );
			Path<EventType> type = root.get( "type" );

			Expression<String> caseWhen = cb.<String> selectCase()
					.when( cb.equal( type, EventType.TYPE1 ), "Matched" );

			criteria.select( root );
			criteria.where( cb.equal( caseWhen, "Matched" ) );

			Event event = session.createQuery( criteria ).getSingleResult();
			assertEquals( 1L, (long) event.id );
		} );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ Event.class, EventType.class };
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
