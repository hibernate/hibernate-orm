//$Id$
package org.hibernate.test.annotations;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

/**
 * @author Emmanuel Bernard
 */
public class ConfigurationTest extends junit.framework.TestCase {
	public void testDeclarativeMix() throws Exception {
		AnnotationConfiguration cfg = new AnnotationConfiguration();
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

	public void testIgnoringHbm() throws Exception {
		AnnotationConfiguration cfg = new AnnotationConfiguration();
		cfg.configure( "org/hibernate/test/annotations/hibernate.cfg.xml" );
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		cfg.setProperty( AnnotationConfiguration.ARTEFACT_PROCESSING_ORDER, "class" );
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

	public void testPrecedenceHbm() throws Exception {
		AnnotationConfiguration cfg = new AnnotationConfiguration();
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

	public void testPrecedenceAnnotation() throws Exception {
		AnnotationConfiguration cfg = new AnnotationConfiguration();
		cfg.configure( "org/hibernate/test/annotations/hibernate.cfg.xml" );
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		cfg.setProperty( AnnotationConfiguration.ARTEFACT_PROCESSING_ORDER, "class, hbm" );
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

	public void testHbmWithSubclassExtends() throws Exception {
		AnnotationConfiguration cfg = new AnnotationConfiguration();
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

	public void testAnnReferencesHbm() throws Exception {
		AnnotationConfiguration cfg = new AnnotationConfiguration();
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
