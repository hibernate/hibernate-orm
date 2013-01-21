/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.array;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.array.Contest.Month;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
public class ArrayTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testOneToMany() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Competitor c1 = new Competitor();
		c1.setName( "Renault" );
		Competitor c2 = new Competitor();
		c2.setName( "Ferrari" );
		Contest contest = new Contest();
		contest.setResults( new Competitor[]{c1, c2} );
		contest.setHeldIn(new Month[]{Month.January, Month.December});
		s.persist( contest );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		contest = (Contest) s.get( Contest.class, contest.getId() );
		assertNotNull( contest );
		assertNotNull( contest.getResults() );
		assertEquals( 2, contest.getResults().length );
		assertEquals( c2.getName(), contest.getResults()[1].getName() );
		assertEquals( 2, contest.getHeldIn().length );
		assertEquals( Month.January, contest.getHeldIn()[0] );
		tx.commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Competitor.class, Contest.class };
	}
}
