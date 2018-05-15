/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.loader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.Set;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class LoaderTest extends BaseCoreFunctionalTestCase {
	@Override
	protected String[] getXmlFiles() {
		return new String[] {
				"org/hibernate/test/annotations/loader/Loader.hbm.xml"
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
		t.getPlayers().add( p );
		p.setTeam( t );
		s.persist(p);
		s.persist( t );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Team t2 = s.load( Team.class, t.getId() );
		Set<Player> players = t2.getPlayers();
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

	@Test
	public void testGetNotExisting() {
		Session s = openSession();
		s.beginTransaction();

		try {
			long notExistingId = 1l;
			s.load( Team.class, notExistingId );
			s.get( Team.class, notExistingId );
			s.getTransaction().commit();
		}
		catch (ObjectNotFoundException e) {
			if ( s.getTransaction().getStatus() == TransactionStatus.ACTIVE ) {
				s.getTransaction().rollback();
			}
			fail("#get threw an ObjectNotFoundExcepton");
		}
		finally {
			s.close();
		}
	}
}

