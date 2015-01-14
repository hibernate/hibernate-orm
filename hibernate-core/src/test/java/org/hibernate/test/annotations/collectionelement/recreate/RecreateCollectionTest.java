/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.collectionelement.recreate;

import org.hibernate.BaseSessionEventListener;
import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * @author Sergey Astakhov
 */
public class RecreateCollectionTest extends BaseCoreFunctionalTestCase {

	private static class StatementsCounterListener extends BaseSessionEventListener {
		int statements;

		@Override
		public void jdbcExecuteStatementEnd() {
			statements++;
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9474")
	public void testUpdateCollectionOfElements() throws Exception {
		Session s = openSession();

		s.getTransaction().begin();

		Poi poi1 = new Poi( "Poi 1" );
		Poi poi2 = new Poi( "Poi 2" );

		s.save( poi1 );
		s.save( poi2 );

		RaceExecution race = new RaceExecution();

		s.save( race );

		Date currentTime = new Date();

		race.arriveToPoi( poi1, currentTime );
		race.expectedArrive( poi2, new Date( currentTime.getTime() + 60 * 1000 ) );

		s.flush();

		assertEquals( 2, race.getPoiArrival().size() );

		StatementsCounterListener statementsCounterListener = new StatementsCounterListener();

		s.addEventListeners( statementsCounterListener );

		race.arriveToPoi( poi2, new Date( currentTime.getTime() + 2 * 60 * 1000 ) );

		s.flush();

		assertEquals( 2, race.getPoiArrival().size() );

		// There is should be one UPDATE statement. Without fix there is one DELETE and two INSERT-s.

		assertEquals( 1, statementsCounterListener.statements );

		s.getTransaction().rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Poi.class,
				RaceExecution.class
		};
	}

}
