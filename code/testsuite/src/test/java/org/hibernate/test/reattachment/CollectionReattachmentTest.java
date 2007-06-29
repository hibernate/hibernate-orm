package org.hibernate.test.reattachment;

import junit.framework.Test;

import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.Session;

/**
 * Test of collection reattachment semantics
 *
 * @author Steve Ebersole
 */
public class CollectionReattachmentTest extends FunctionalTestCase {
	public CollectionReattachmentTest(String string) {
		super( string );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CollectionReattachmentTest.class );
	}

	public String[] getMappings() {
		return new String[] { "reattachment/Mappings.hbm.xml" };
	}

	public void testUpdateOwnerAfterClear() {
		Session s = openSession();
		s.beginTransaction();
		Parent p = new Parent( "p" );
		p.getChildren().add( new Child( "c" ) );
		s.save( p );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		p = ( Parent ) s.get( Parent.class, "p" );
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

	public void testUpdateOwnerAfterEvict() {
		Session s = openSession();
		s.beginTransaction();
		Parent p = new Parent( "p" );
		p.getChildren().add( new Child( "c" ) );
		s.save( p );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		p = ( Parent ) s.get( Parent.class, "p" );
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
}
