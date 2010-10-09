//$Id$
package org.hibernate.test.annotations.array;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.annotations.array.Contest.Month;

/**
 * @author Emmanuel Bernard
 */
public class ArrayTest extends TestCase {

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

	public ArrayTest(String x) {
		super( x );
	}

	@SuppressWarnings("unchecked")
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Competitor.class,
				Contest.class
		};
	}
}
