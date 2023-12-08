/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.loader.collectioninembedded;

import java.util.Iterator;
import java.util.Set;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@TestForIssue(jiraKey = "")
public class LoaderCollectionInEmbeddedTest extends BaseCoreFunctionalTestCase {
	@Override
	protected String[] getXmlFiles() {
		return new String[] {
				"org/hibernate/test/annotations/loader/collectioninembedded/Loader.hbm.xml"
		};
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Player.class,
				Team.class
		};
	}

	@Test
	public void testBasic() throws Exception {
		// set up data...
		Session s = openSession( );
		Transaction tx = s.beginTransaction();
		Team t = new Team();
		Player p = new Player();
		p.setName( "me" );
		t.getDetails().getPlayers().add( p );
		p.setTeam( t );
		s.persist(p);
		s.persist( t );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Team t2 = s.load( Team.class, t.getId() );
		Set<Player> players = t2.getDetails().getPlayers();
		Iterator<Player> iterator = players.iterator();
		assertEquals( "me", iterator.next().getName() );
		tx.commit();
		s.close();

		// clean up data
		s = openSession();
		tx = s.beginTransaction();
		t = s.get( Team.class, t2.getId() );
		p = s.get( Player.class, p.getId() );
		s.delete( p );
		s.delete( t );
		tx.commit();
		s.close();
	}
}

