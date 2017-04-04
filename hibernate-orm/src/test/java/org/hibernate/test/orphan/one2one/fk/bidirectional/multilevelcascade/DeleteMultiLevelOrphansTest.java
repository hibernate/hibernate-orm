/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.orphan.one2one.fk.bidirectional.multilevelcascade;

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

		Tranchenmodell tranchenmodell = new Tranchenmodell();

		X x = new X();

		Tranche tranche1 = new Tranche();

		Y y = new Y();

		Tranche tranche2 = new Tranche();

		preisregelung.setTranchenmodell( tranchenmodell );
		tranchenmodell.setPreisregelung( preisregelung );

		tranchenmodell.setX( x );
		x.setTranchenmodell( tranchenmodell );

		tranchenmodell.getTranchen().add( tranche1 );
		tranche1.setTranchenmodell( tranchenmodell );
		tranchenmodell.getTranchen().add( tranche2 );
		tranche2.setTranchenmodell( tranchenmodell );

		tranche1.setY( y );
		y.setTranche( tranche1 );

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
		session.createQuery( "delete Tranchenmodell" ).executeUpdate();
		session.createQuery( "delete Preisregelung" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9091")
	public void testDirectAssociationOrphanedWhileManaged() {
		createData();

		Session session = openSession();
		session.beginTransaction();
		List results = session.createQuery( "from Tranchenmodell" ).list();
		assertEquals( 1, results.size() );
		results = session.createQuery( "from Preisregelung" ).list();
		assertEquals( 1, results.size() );
		Preisregelung preisregelung = ( Preisregelung ) results.get( 0 );
		Tranchenmodell tranchenmodell = preisregelung.getTranchenmodell();
		assertNotNull( tranchenmodell );
		assertNotNull( tranchenmodell.getX() );
		assertEquals( 2, tranchenmodell.getTranchen().size() );
		assertNotNull( tranchenmodell.getTranchen().get( 0 ).getY() );
		preisregelung.setTranchenmodell( null );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();

		preisregelung = ( Preisregelung ) session.get( Preisregelung.class, preisregelung.getId() );
		assertNull( preisregelung.getTranchenmodell() );
		results = session.createQuery( "from Tranchenmodell" ).list();
		assertEquals( 0, results.size() );
		results = session.createQuery( "from Tranche" ).list();
		assertEquals( 0, results.size() );
		results = session.createQuery( "from X" ).list();
		assertEquals( 0, results.size() );
		results = session.createQuery( "from Y" ).list();
		assertEquals( 0, results.size() );

		results = session.createQuery( "from Preisregelung" ).list();
		assertEquals( 1, results.size() );

		session.getTransaction().commit();
		session.close();

		cleanupData();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9091")
	public void testReplacedDirectAssociationWhileManaged() {
		createData();

		Session session = openSession();
		session.beginTransaction();
		List results = session.createQuery( "from Tranchenmodell" ).list();
		assertEquals( 1, results.size() );
		results = session.createQuery( "from Preisregelung" ).list();
		assertEquals( 1, results.size() );
		Preisregelung preisregelung = ( Preisregelung ) results.get( 0 );
		Tranchenmodell tranchenmodell = preisregelung.getTranchenmodell();
		assertNotNull( tranchenmodell );
		assertNotNull( tranchenmodell.getX() );
		assertEquals( 2, tranchenmodell.getTranchen().size() );
		assertNotNull( tranchenmodell.getTranchen().get( 0 ).getY() );

		// Create a new Tranchenmodell with new direct and nested associations
		Tranchenmodell tranchenmodellNew = new Tranchenmodell();
		X xNew = new X();
		tranchenmodellNew.setX( xNew );
		xNew.setTranchenmodell( tranchenmodellNew );
		Tranche trancheNew = new Tranche();
		tranchenmodellNew.getTranchen().add( trancheNew );
		trancheNew.setTranchenmodell( tranchenmodellNew );
		Y yNew = new Y();
		trancheNew.setY( yNew );
		yNew.setTranche( trancheNew );

		// Replace with a new Tranchenmodell instance containing new direct and nested associations
		preisregelung.setTranchenmodell(tranchenmodellNew );
		tranchenmodellNew.setPreisregelung( preisregelung );

		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.getTransaction().begin();

		results = session.createQuery( "from Tranche" ).list();
		assertEquals( 1, results.size() );
		results = session.createQuery( "from Tranchenmodell" ).list();
		assertEquals( 1, results.size() );
		results = session.createQuery( "from X" ).list();
		assertEquals( 1, results.size() );
		results = session.createQuery( "from Y" ).list();
		assertEquals( 1, results.size() );
		results = session.createQuery( "from Preisregelung" ).list();
		assertEquals( 1, results.size() );
		preisregelung = ( Preisregelung ) results.get( 0 );
		tranchenmodell = preisregelung.getTranchenmodell();
		assertNotNull( tranchenmodell );
		assertEquals( tranchenmodellNew.getId(), tranchenmodell.getId() );
		assertNotNull( tranchenmodell.getX() );
		assertEquals( xNew.getId(), tranchenmodell.getX().getId() );
		assertEquals( 1, tranchenmodell.getTranchen().size() );
		assertEquals( trancheNew.getId(), tranchenmodell.getTranchen().get( 0 ).getId() );
		assertEquals( yNew.getId(), tranchenmodell.getTranchen().get( 0 ).getY().getId() );

		// Replace with a new Tranchenmodell instance with no associations
		tranchenmodellNew = new Tranchenmodell();
		preisregelung.setTranchenmodell(tranchenmodellNew );
		tranchenmodellNew.setPreisregelung( preisregelung );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		results = session.createQuery( "from Tranchenmodell" ).list();
		assertEquals( 1, results.size() );
		tranchenmodell = (Tranchenmodell) results.get( 0 );
		assertEquals( tranchenmodellNew.getId(), tranchenmodell.getId() );
		results = session.createQuery( "from Preisregelung" ).list();
		assertEquals( 1, results.size() );
		preisregelung =  (Preisregelung) results.get( 0 );
		assertEquals( tranchenmodell, preisregelung.getTranchenmodell() );
		results = session.createQuery( "from Tranche" ).list();
		assertEquals( 0, results.size() );
		results = session.createQuery( "from X" ).list();
		assertEquals( 0, results.size() );
		results = session.createQuery( "from Y" ).list();
		assertEquals( 0, results.size() );
		session.getTransaction().commit();
		session.close();

		cleanupData();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9091")
	public void testDirectAndNestedAssociationsOrphanedWhileManaged() {
		createData();

		Session session = openSession();
		session.beginTransaction();
		List results = session.createQuery( "from Tranchenmodell" ).list();
		assertEquals( 1, results.size() );
		results = session.createQuery( "from Preisregelung" ).list();
		assertEquals( 1, results.size() );
		Preisregelung preisregelung = ( Preisregelung ) results.get( 0 );
		Tranchenmodell tranchenmodell = preisregelung.getTranchenmodell();
		assertNotNull( tranchenmodell );
		assertNotNull( tranchenmodell.getX() );
		assertEquals( 2, tranchenmodell.getTranchen().size() );
		assertNotNull( tranchenmodell.getTranchen().get( 0 ).getY() );
		preisregelung.setTranchenmodell( null );
		tranchenmodell.setX( null );
		tranchenmodell.getTranchen().get( 0 ).setY( null );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();

		preisregelung = ( Preisregelung ) session.get( Preisregelung.class, preisregelung.getId() );
		assertNull( preisregelung.getTranchenmodell() );
		results = session.createQuery( "from Tranchenmodell" ).list();
		assertEquals( 0, results.size() );
		results = session.createQuery( "from Tranche" ).list();
		assertEquals( 0, results.size() );
		results = session.createQuery( "from X" ).list();
		assertEquals( 0, results.size() );
		results = session.createQuery( "from Y" ).list();
		assertEquals( 0, results.size() );

		results = session.createQuery( "from Preisregelung" ).list();
		assertEquals( 1, results.size() );

		session.getTransaction().commit();
		session.close();

		cleanupData();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				Preisregelung.class,
				Tranche.class,
				Tranchenmodell.class,
				X.class,
				Y.class
		};
	}

}
