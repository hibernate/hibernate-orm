/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.array;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.orm.test.annotations.array.Contest.Month;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(annotatedClasses = { Competitor.class, Contest.class})
@SessionFactory
public class ArrayTest {

	@Test
	public void testOneToMany(SessionFactoryScope scope)  {
		Competitor c1 = new Competitor();
		c1.setName( "Renault" );
		Competitor c2 = new Competitor();
		c2.setName( "Ferrari" );
		Contest c = new Contest();
		c.setResults( new Competitor[]{c1, c2} );
		c.setHeldIn(new Month[]{Month.January, Month.December});
		scope.inTransaction(
				session -> {
					session.persist( c );
				}
		);

		scope.inTransaction(
				session -> {
					Contest contest = session.get( Contest.class, c.getId() );
					assertNotNull( contest );
					assertNotNull( contest.getResults() );
					assertEquals( 2, contest.getResults().length );
					assertEquals( c2.getName(), contest.getResults()[1].getName() );
					assertEquals( 2, contest.getHeldIn().length );
					assertEquals( Month.January, contest.getHeldIn()[0] );
				}
		);
	}
}
