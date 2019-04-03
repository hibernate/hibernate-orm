/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.collectionelement.recreate;

import java.util.Date;

import org.hibernate.BaseSessionEventListener;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sergey Astakhov
 */
public class RecreateCollectionTest extends SessionFactoryBasedFunctionalTest {

	private static class StatementsCounterListener extends BaseSessionEventListener {
		int statements;

		@Override
		public void jdbcExecuteStatementEnd() {
			statements++;
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9474")
	public void testUpdateCollectionOfElements() {
		inTransaction(
				session -> {

					Poi poi1 = new Poi( "Poi 1" );
					Poi poi2 = new Poi( "Poi 2" );

					session.save( poi1 );
					session.save( poi2 );

					RaceExecution race = new RaceExecution();

					session.save( race );

					Date currentTime = new Date();

					race.arriveToPoi( poi1, currentTime );
					race.expectedArrive( poi2, new Date( currentTime.getTime() + 60 * 1000 ) );

					session.flush();

					assertEquals( 2, race.getPoiArrival().size() );

					StatementsCounterListener statementsCounterListener = new StatementsCounterListener();

					session.addEventListeners( statementsCounterListener );

					race.arriveToPoi( poi2, new Date( currentTime.getTime() + 2 * 60 * 1000 ) );

					session.flush();

					assertEquals( 2, race.getPoiArrival().size() );

					// There is should be one UPDATE statement. Without fix there is one DELETE and two INSERT-s.

					assertEquals( 1, statementsCounterListener.statements );

				}
		);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Poi.class,
				RaceExecution.class
		};
	}

}
