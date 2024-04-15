/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.booleans;

import java.util.List;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Useful to verify Boolean mappings; in particular we had a new
 * error on recent versions of H2, so let's ensure this works
 * on whatever version the testsuite is being run with.
 */
public class BooleanMappingTest extends BaseCoreFunctionalTestCase {

	@Test
	@TestForIssue(jiraKey = "HHH-15002")
	public void testFetchEager() {
		doInHibernate( this::sessionFactory, s -> {
				Event activeEvent = new Event();
				activeEvent.setActive( Boolean.TRUE );
				s.persist( activeEvent );

				Event inactiveEvent = new Event();
				inactiveEvent.setActive( Boolean.FALSE );
				s.persist( inactiveEvent );
		} );

		final List<Event> activeEvents = doInHibernate( this::sessionFactory, s -> {
			return s.createQuery( "SELECT e FROM Event e WHERE e.active = true", Event.class ).getResultList();
		} );

		assertNotNull( activeEvents );
		assertEquals( 1, activeEvents.size() );

		final List<Event> allEvents = doInHibernate( this::sessionFactory, s -> {
			return s.createQuery( "FROM Event", Event.class ).getResultList();
		} );

		assertNotNull( allEvents );
		assertEquals( 2, allEvents.size() );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Event.class,
		};
	}

}
