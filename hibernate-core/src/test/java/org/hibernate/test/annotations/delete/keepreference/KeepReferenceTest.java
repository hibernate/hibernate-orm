/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.delete.keepreference;

import org.assertj.core.util.Sets;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import java.util.List;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.*;

/**
 * @author Richard Bizik
 */
public class KeepReferenceTest extends BaseCoreFunctionalTestCase {


	@Test
	@TestForIssue( jiraKey = "HHH-13900")
	public void keepReferenceShouldKeepReference(){
		Transaction transaction;
		Session session = openSession();
		transaction = session.beginTransaction();

		Universe universe = new Universe();
		session.save(universe);

		DeathStar deathStar = new DeathStar();
		deathStar.setUniverse(universe);
		universe.setDeathStar(deathStar);

		Vader vader = new Vader();
		vader.setDeathStar(deathStar);

		deathStar.setVader(vader);
		session.save(deathStar);

		Trooper trooper = new Trooper();
		trooper.setCode("TK-421");
		trooper.setDeathStar(deathStar);
		deathStar.setTroppers(Sets.newLinkedHashSet(trooper));

		session.save(trooper);

		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		deathStar = session.get(DeathStar.class, deathStar.getId());
		assertEquals(1, deathStar.getTroppers().size());
		assertNotNull(deathStar.getVader());
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		session.delete(deathStar);
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();

		//universe should not be deleted
		universe = session.get(Universe.class, universe.getId());
		assertNotNull(universe);
		//deathStar should be deleted
		deathStar = session.get(DeathStar.class, deathStar.getId());
		assertNull(deathStar);
		List universes = session.createSQLQuery("SELECT * from universe").list();
		List deathStars = session.createSQLQuery("SELECT * from deathstar").list();
		List troopers = session.createSQLQuery("SELECT * from trooper").list();
		List vaders = session.createSQLQuery("SELECT * from vader").list();

		assertEquals(1, universes.size());
		assertEquals(1, deathStars.size());
		assertEquals(1, troopers.size());
		assertEquals(1, vaders.size());

		//check if deleted is set
		assertTrue((Boolean)((Object[])deathStars.get(0))[1]);
		assertTrue((Boolean)((Object[])troopers.get(0))[1]);
		assertTrue((Boolean)((Object[])vaders.get(0))[1]);
		//universe should not be delted
		assertFalse((Boolean)((Object[])universes.get(0))[1]);

		//check if references are kept
		assertNotNull(((Object[])deathStars.get(0))[2]);
		assertNotNull(((Object[])troopers.get(0))[3]);

		session.close();

		cleanupData();
	}

	private void cleanupData() {
		doInHibernate( this::sessionFactory, s -> {
			s.createSQLQuery("delete from trooper").executeUpdate();
			s.createSQLQuery("delete from deathstar").executeUpdate();
			s.createSQLQuery("delete from vader").executeUpdate();
			s.createSQLQuery("delete from universe").executeUpdate();
		});
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				BaseEntity.class,
				Universe.class,
				DeathStar.class,
				Vader.class,
				Trooper.class
		};
	}

	@Override
	protected String[] getAnnotatedPackages() {
		return new String[] {
				"org.hibernate.test.annotations.query.keepReference"
		};
	}
}
