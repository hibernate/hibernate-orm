/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.orphan.one2one.fk.reversed.bidirectional.multilevelcascade;

import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class DeleteMultiLevelOrphansTest extends BaseCoreFunctionalTestCase {

	private void createData() {
		Preisregelung preisregelung = new Preisregelung();
		preisregelung.setId( 17960L );

		Tranchenmodell tranchenmodell = new Tranchenmodell();
		tranchenmodell.setId( 1951L );

		Tranche tranche1 = new Tranche();
		tranche1.setId( 1951L);

		Tranche tranche2 = new Tranche();
		tranche2.setId( 1952L);

		preisregelung.setTranchenmodell( tranchenmodell );
		tranchenmodell.setPreisregelung( preisregelung );

		tranchenmodell.getTranchen().add( tranche1 );
		tranche1.setTranchenmodell( tranchenmodell );
		tranchenmodell.getTranchen().add( tranche2 );
		tranche2.setTranchenmodell( tranchenmodell );

		Session session = openSession();
		session.beginTransaction();
		session.save( preisregelung );
		session.getTransaction().commit();
		session.close();
	}

	private void cleanupData() {
		Session session = openSession();
		session.beginTransaction();
		session.createQuery( "delete Tranche" ).executeUpdate();
		session.createQuery( "delete Preisregelung" ).executeUpdate();
		session.createQuery( "delete Tranchenmodell" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9091")
	public void testOrphanedWhileManaged() {
		createData();

		Session session = openSession();
		session.beginTransaction();
		List results = session.createQuery( "from Tranchenmodell" ).list();
		assertEquals( 1, results.size() );
		results = session.createQuery( "from Preisregelung" ).list();
		assertEquals( 1, results.size() );
		Preisregelung preisregelung = (Preisregelung) results.get( 0 );
		assertNotNull( preisregelung.getTranchenmodell() );
		preisregelung.setTranchenmodell( null );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();

		preisregelung = (Preisregelung) session.get( Preisregelung.class, preisregelung.getId() );
		assertNull( preisregelung.getTranchenmodell() );
		results = session.createQuery( "from Tranchenmodell" ).list();
		assertEquals( 0, results.size() );
		results = session.createQuery( "from Preisregelung" ).list();
		assertEquals( 1, results.size() );

		session.getTransaction().commit();
		session.close();

		cleanupData();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9091")
	public void testReplacedWhileManaged() {
		createData();

		Session session = openSession();
		session.beginTransaction();
		List results = session.createQuery( "from Tranchenmodell" ).list();
		assertEquals( 1, results.size() );
		results = session.createQuery( "from Preisregelung" ).list();
		assertEquals( 1, results.size() );
		Preisregelung preisregelung = (Preisregelung) results.get( 0 );
		assertNotNull( preisregelung.getTranchenmodell() );

		// Replace with a new Tranchenmodell instance
		Tranchenmodell tranchenmodellNew = new Tranchenmodell();
		tranchenmodellNew.setId( 1952L );
		preisregelung.setTranchenmodell( tranchenmodellNew );
		tranchenmodellNew.setPreisregelung( preisregelung );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		results = session.createQuery( "from Tranchenmodell" ).list();
		assertEquals( 1, results.size() );
		Tranchenmodell tranchenmodellQueried = (Tranchenmodell) results.get( 0 );
		assertEquals( tranchenmodellNew.getId(), tranchenmodellQueried.getId() );
		results = session.createQuery( "from Preisregelung" ).list();
		assertEquals( 1, results.size() );
		Preisregelung preisregelung1Queried =  (Preisregelung) results.get( 0 );
		assertEquals( tranchenmodellQueried, preisregelung1Queried.getTranchenmodell() );
		results = session.createQuery( "from Tranche" ).list();
		assertEquals( 0, results.size() );

		session.getTransaction().commit();
		session.close();

		cleanupData();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				Preisregelung.class,
				Tranche.class,
				Tranchenmodell.class
		};
	}

}
