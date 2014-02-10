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
package org.hibernate.test.annotations.loader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.Set;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.Transaction;
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

	@Test
	public void testGetNotExisting() {
		Session s = openSession();

		try {
			long notExistingId = 1l;
			s.load( Team.class, notExistingId );
			s.get( Team.class, notExistingId );
		}
		catch (ObjectNotFoundException e) {
			fail("#get threw an ObjectNotFoundExcepton");
		}
		finally {
			s.close();
		}
	}
}

