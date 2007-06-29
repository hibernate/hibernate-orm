package org.hibernate.test.reattachment;

import junit.framework.Test;

import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.Session;

/**
 * Test of proxy reattachment semantics
 *
 * @author Steve Ebersole
 */
public class ProxyReattachmentTest extends FunctionalTestCase {
	public ProxyReattachmentTest(String string) {
		super( string );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( ProxyReattachmentTest.class );
	}

	public String[] getMappings() {
		return new String[] { "reattachment/Mappings.hbm.xml" };
	}

	public void testUpdateAfterEvict() {
		Session s = openSession();
		s.beginTransaction();
		Parent p = new Parent( "p" );
		s.save( p );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		p = ( Parent ) s.load( Parent.class, "p" );
		// evict...
		s.evict( p );
		// now try to reattach...
		s.update( p );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( p );
		s.getTransaction().commit();
		s.close();
	}

	public void testUpdateAfterClear() {
		Session s = openSession();
		s.beginTransaction();
		Parent p = new Parent( "p" );
		s.save( p );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		p = ( Parent ) s.load( Parent.class, "p" );
		// clear...
		s.clear();
		// now try to reattach...
		s.update( p );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( p );
		s.getTransaction().commit();
		s.close();
	}
}
