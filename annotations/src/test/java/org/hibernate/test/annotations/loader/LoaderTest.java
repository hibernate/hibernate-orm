//$Id$
package org.hibernate.test.annotations.loader;

import java.util.Set;
import java.util.Iterator;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;


/**
 * @author Emmanuel Bernard
 */
public class LoaderTest extends TestCase {


	protected String[] getXmlFiles() {
		return new String[] {
				"org/hibernate/test/annotations/loader/Loader.hbm.xml"
		};
	}

	public void testBasic() throws Exception {
		Session s = openSession( );
		Transaction tx = s.beginTransaction();
		Team t = new Team();
		Player p = new Player();
		p.setName("me");
		t.getPlayers().add(p);
		p.setTeam(t);
		

		try {
			s.persist(p);
			s.persist(t);
			tx.commit();
			s.close();
			
			s= openSession( );
			tx = s.beginTransaction();
			Team t2 = (Team)s.load(Team.class,new Long(1));
			Set<Player> players = t2.getPlayers();
			Iterator<Player> iterator = players.iterator();
			assertEquals("me", iterator.next().getName());
			tx.commit();
			
		}
		catch (Exception e) {
			e.printStackTrace();
			if ( tx != null ) tx.rollback();
		}
		finally {
			s.close();
		}
	}

	/**
	 * @see org.hibernate.test.annotations.TestCase#getMappings()
	 */
	protected Class[] getMappings() {
		return new Class[]{
				Player.class,
				Team.class
		};
	}

}

