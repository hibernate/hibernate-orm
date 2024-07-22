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
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Sergey Astakhov
 */
@DomainModel(
		annotatedClasses = {
				Poi.class,
				RaceExecution.class
		}
)
@SessionFactory
public class RecreateCollectionTest {

	private static class StatementsCounterListener extends BaseSessionEventListener {
		int statements;

		@Override
		public void jdbcExecuteStatementEnd() {
			statements++;
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9474")
	public void testUpdateCollectionOfElements(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Poi poi1 = new Poi( "Poi 1" );
					Poi poi2 = new Poi( "Poi 2" );

					session.persist( poi1 );
					session.persist( poi2 );

					RaceExecution race = new RaceExecution();

					session.persist( race );

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

}
