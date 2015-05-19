/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class ConfigurationTest {
    @Test
	public void testDeclarativeMix() throws Exception {
		Configuration cfg = new Configuration();
		cfg.configure( "org/hibernate/test/annotations/hibernate.cfg.xml" );
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		SessionFactory sf = cfg.buildSessionFactory();
		assertNotNull( sf );
		Session s = sf.openSession();
		Transaction tx = s.beginTransaction();
		Query q = s.createQuery( "from Boat" );
		assertEquals( 0, q.list().size() );
		q = s.createQuery( "from Plane" );
		assertEquals( 0, q.list().size() );
		tx.commit();
		s.close();
		sf.close();
	}
     @Test
	public void testIgnoringHbm() throws Exception {
		Configuration cfg = new Configuration();
		cfg.configure( "org/hibernate/test/annotations/hibernate.cfg.xml" );
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		cfg.setProperty( Configuration.ARTEFACT_PROCESSING_ORDER, "class" );
		SessionFactory sf = cfg.buildSessionFactory();
		assertNotNull( sf );
		Session s = sf.openSession();
		Transaction tx = s.beginTransaction();
		Query q;
		try {
			s.createQuery( "from Boat" ).list();
			fail( "Boat should not be mapped" );
		}
		catch (HibernateException e) {
			//all good
		}
		q = s.createQuery( "from Plane" );
		assertEquals( 0, q.list().size() );
		tx.commit();
		s.close();
		sf.close();
	}
    @Test
	public void testPrecedenceHbm() throws Exception {
		Configuration cfg = new Configuration();
		cfg.configure( "org/hibernate/test/annotations/hibernate.cfg.xml" );
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		cfg.addAnnotatedClass( Boat.class );
		SessionFactory sf = cfg.buildSessionFactory();
		assertNotNull( sf );
		Session s = sf.openSession();
		s.getTransaction().begin();
		Boat boat = new Boat();
		boat.setSize( 12 );
		boat.setWeight( 34 );
		s.persist( boat );
		s.getTransaction().commit();
		s.clear();
		Transaction tx = s.beginTransaction();
		boat = (Boat) s.get( Boat.class, boat.getId() );
		assertTrue( "Annotation has precedence", 34 != boat.getWeight() );
		s.delete( boat );
		//s.getTransaction().commit();
		tx.commit();
		s.close();
		sf.close();
	}
     @Test
	public void testPrecedenceAnnotation() throws Exception {
		Configuration cfg = new Configuration();
		cfg.configure( "org/hibernate/test/annotations/hibernate.cfg.xml" );
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		cfg.setProperty( Configuration.ARTEFACT_PROCESSING_ORDER, "class, hbm" );
		cfg.addAnnotatedClass( Boat.class );
		SessionFactory sf = cfg.buildSessionFactory();
		assertNotNull( sf );
		Session s = sf.openSession();
		s.getTransaction().begin();
		Boat boat = new Boat();
		boat.setSize( 12 );
		boat.setWeight( 34 );
		s.persist( boat );
		s.getTransaction().commit();
		s.clear();
		Transaction tx = s.beginTransaction();
		boat = (Boat) s.get( Boat.class, boat.getId() );
		assertTrue( "Annotation has precedence", 34 == boat.getWeight() );
		s.delete( boat );
		tx.commit();
		s.close();
		sf.close();
	}
     @Test
	public void testHbmWithSubclassExtends() throws Exception {
		Configuration cfg = new Configuration();
		cfg.configure( "org/hibernate/test/annotations/hibernate.cfg.xml" );
		cfg.addClass( Ferry.class );
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		SessionFactory sf = cfg.buildSessionFactory();
		assertNotNull( sf );
		Session s = sf.openSession();
		Transaction tx = s.beginTransaction();
		Query q = s.createQuery( "from Ferry" );
		assertEquals( 0, q.list().size() );
		q = s.createQuery( "from Plane" );
		assertEquals( 0, q.list().size() );
		tx.commit();
		s.close();
		sf.close();
	}
      @Test
	public void testAnnReferencesHbm() throws Exception {
		Configuration cfg = new Configuration();
		cfg.configure( "org/hibernate/test/annotations/hibernate.cfg.xml" );
		cfg.addAnnotatedClass( Port.class );
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		SessionFactory sf = cfg.buildSessionFactory();
		assertNotNull( sf );
		Session s = sf.openSession();
		Transaction tx = s.beginTransaction();
		Query q = s.createQuery( "from Boat" );
		assertEquals( 0, q.list().size() );
		q = s.createQuery( "from Port" );
		assertEquals( 0, q.list().size() );
		tx.commit();
		s.close();
		sf.close();
	}
}
