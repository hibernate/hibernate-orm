/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Jeremy Carnus
 * @author Guillaume Smet
 */
@JiraKey(value = "HHH-12989")
public class InWithHeterogeneousCollectionTest extends BaseCoreFunctionalTestCase {

	@Test
	@RequiresDialect(H2Dialect.class)
	public void testCaseClause() {
		doInHibernate( this::sessionFactory, session -> {
			CriteriaBuilder cb = session.getCriteriaBuilder();

			CriteriaQuery<Event> criteria = cb.createQuery( Event.class );

			Root<Event> eventRoot = criteria.from( Event.class );
			Path<String> namePath = eventRoot.get( "name" );
			Path<String> tagPath = eventRoot.get( "tag" );

			Expression<String> expression = cb.function(
					"lower",
					String.class,
					namePath );

			criteria.select( eventRoot );
			criteria.where( tagPath.in( Arrays.asList( expression, "my-tag" ) ) );
			List<Event> resultList = session.createQuery( criteria ).getResultList();

			Assert.assertEquals( 2, resultList.size() );
		} );
	}

	@Before
	public void setup() {
		doInHibernate( this::sessionFactory, session -> {
			session.persist( new Event( 1L, "EventName1", "EventName1".toLowerCase( Locale.ROOT ) ) );
			session.persist( new Event( 2L, "EventName2", "my-tag" ) );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Event.class };
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		private Long id;

		@Column
		private String name;

		@Column
		private String tag;

		protected Event() {
		}

		public Event(Long id, String name, String tag) {
			this.id = id;
			this.name = name;
			this.tag = tag;
		}

		public String getName() {
			return name;
		}

		public String getTag() {
			return tag;
		}
	}
}
