/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal;

import java.util.Arrays;
import java.util.List;
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
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Jeremy Carnus
 */
@TestForIssue(jiraKey = "HHH-12989")
public class InWithHeterogeneousCollectionTest extends BaseCoreFunctionalTestCase {

	@Test
	@RequiresDialect(H2Dialect.class)
	public void testCaseClause() {
		doInHibernate( this::sessionFactory, session -> {
			CriteriaBuilder cb = session.getCriteriaBuilder();

			CriteriaQuery<Event> criteria = cb.createQuery( Event.class );

			Root<Event> event = criteria.from( Event.class );
			Path<String> name = event.get( "name" );

			Expression<String> replaceName = cb.function(
					"replace",
					String.class,
					name,
					cb.literal( "a" ),
					cb.literal( "a" )
			);

			criteria.select( event );
			criteria.where( name.in( Arrays.asList( replaceName, "1" ) ) );
			List<Event> resultList = session.createQuery( criteria ).getResultList();

			Assert.assertNotNull( resultList );
		} );
	}


	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Event.class };
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		private Long id;

		@Column
		private String name;

		protected Event() {
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}