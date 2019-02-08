/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.array;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.annotations.array.Contest.Month.December;
import static org.hibernate.orm.test.annotations.array.Contest.Month.January;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
public class ArrayTest extends SessionFactoryBasedFunctionalTest {

	@Test
	public void testOneToMany() {
		Competitor c1 = new Competitor();
		c1.setName( "Renault" );
		Competitor c2 = new Competitor();
		c2.setName( "Ferrari" );
		Contest contest = new Contest();
		contest.setResults( new Competitor[] { c1, c2 } );
		contest.setHeldIn( new Contest.Month[] { January, December } );

		inTransaction(
				session -> {
					session.persist( contest );
				}
		);

		inTransaction(
				session -> {
					Contest persistedContest = session.get( Contest.class, contest.getId() );
					assertNotNull( persistedContest );
					assertNotNull( persistedContest.getResults() );
					assertEquals( 2, persistedContest.getResults().length );
					assertEquals( c2.getName(), persistedContest.getResults()[1].getName() );
					assertEquals( 2, persistedContest.getHeldIn().length );
					assertEquals( January, persistedContest.getHeldIn()[0] );
				}
		);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Competitor.class, Contest.class };
	}
}
