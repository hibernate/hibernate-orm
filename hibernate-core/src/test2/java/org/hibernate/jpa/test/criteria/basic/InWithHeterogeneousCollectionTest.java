/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.basic;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Jeremy Carnus
 * @author Guillaume Smet
 */
@TestForIssue(jiraKey = "HHH-12989")
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
			session.save( new Event( 1L, "EventName1", "EventName1".toLowerCase( Locale.ROOT ) ) );
			session.save( new Event( 2L, "EventName2", "my-tag" ) );
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
