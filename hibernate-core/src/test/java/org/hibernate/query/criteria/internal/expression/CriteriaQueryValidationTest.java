/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.expression;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.junit.Test;

/**
 * @author Marko Bekhta
 */
public class CriteriaQueryValidationTest extends BaseCoreFunctionalTestCase {

	@Test
	@RequiresDialect(H2Dialect.class)
	public void testCaseClause() {
		doInHibernate( this::sessionFactory, session -> {
			CriteriaBuilder cb = session.getCriteriaBuilder();

			CriteriaQuery<String> criteriaQuery = cb.createQuery( String.class );

			Root<Event> event = criteriaQuery.from( Event.class );

			ParameterExpression<Long> minAttendeesCount = cb.parameter( Long.class, "min_attendees_count" );
			criteriaQuery.select( event.get( "type" ) )
					.having( cb.ge( cb.sum( event.get( "attendees" ) ), minAttendeesCount ) );

			try {
				session.createQuery( criteriaQuery )
						.setParameter( minAttendeesCount, 2L )
						.getResultList();
				fail( "Shouldn't get to this point as IllegalStateException must be thrown by query validation" );
			} catch (IllegalArgumentException e) {
				assertEquals( "Error occurred validating the Criteria", e.getMessage() );
				assertTrue( e.getCause() instanceof IllegalStateException );
			}

			criteriaQuery.groupBy( event.get( "type" ) );

			List<String> resultList = session.createQuery( criteriaQuery )
					.setParameter( minAttendeesCount, 2L )
					.getResultList();
			assertNotNull( resultList );
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

		private String type;

		private Long attendees;

		protected Event() {
		}

	}

}