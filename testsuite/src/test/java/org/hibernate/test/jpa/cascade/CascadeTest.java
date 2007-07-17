package org.hibernate.test.jpa.cascade;

import org.hibernate.test.jpa.AbstractJPATest;
import org.hibernate.Session;
import org.hibernate.TransientObjectException;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.engine.Status;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.event.FlushEntityEventListener;
import org.hibernate.event.FlushEntityEvent;
import org.hibernate.cfg.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import junit.framework.Test;

/**
 * According to the JPA spec, persist()ing an entity should throw an exception
 * when said entity contains a reference to a transient entity through a mapped
 * association where that association is not marked for cascading the persist
 * operation.
 * <p/>
 * This test-case tests that requirement in the various association style
 * scenarios such as many-to-one, one-to-one, many-to-one (property-ref),
 * one-to-one (property-ref).  Additionally, it performs each of these tests
 * in both generated and assigned identifier usages...
 *
 * @author Steve Ebersole
 */
public class CascadeTest extends AbstractJPATest {

	public static final Log log = LogFactory.getLog( CascadeTest.class );

	public CascadeTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] { "jpa/cascade/ParentChild.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CascadeTest.class );
	}

	public void testManyToOneGeneratedIdsOnSave() {
		// NOTES: Child defines a many-to-one back to its Parent.  This
		// association does not define persist cascading (which is natural;
		// a child should not be able to create its parent).
		try {
			Session s = openSession();
			s.beginTransaction();
			Parent p = new Parent( "parent" );
			Child c = new Child( "child" );
			c.setParent( p );
			s.save( c );
			try {
				s.getTransaction().commit();
				fail( "expecting TransientObjectException on flush" );
			}
			catch( TransientObjectException e ) {
				// expected result
				log.trace( "handled expected exception", e );
				s.getTransaction().rollback();
			}
			finally {
				s.close();
			}
		}
		finally {
			cleanupData();
		}
	}

	public void testManyToOneGeneratedIds() {
		// NOTES: Child defines a many-to-one back to its Parent.  This
		// association does not define persist cascading (which is natural;
		// a child should not be able to create its parent).
		try {
			Session s = openSession();
			s.beginTransaction();
			Parent p = new Parent( "parent" );
			Child c = new Child( "child" );
			c.setParent( p );
			s.persist( c );
			try {
				s.getTransaction().commit();
				fail( "expecting TransientObjectException on flush" );
			}
			catch( TransientObjectException e ) {
				// expected result
				log.trace( "handled expected exception", e );
				s.getTransaction().rollback();
			}
			finally {
				s.close();
			}
		}
		finally {
			cleanupData();
		}
	}

	public void testManyToOneAssignedIds() {
		// NOTES: Child defines a many-to-one back to its Parent.  This
		// association does not define persist cascading (which is natural;
		// a child should not be able to create its parent).
		try {
			Session s = openSession();
			s.beginTransaction();
			ParentAssigned p = new ParentAssigned( new Long( 1 ), "parent" );
			ChildAssigned c = new ChildAssigned( new Long( 2 ), "child" );
			c.setParent( p );
			s.persist( c );
			try {
				s.getTransaction().commit();
				fail( "expecting TransientObjectException on flush" );
			}
			catch( TransientObjectException e ) {
				// expected result
				log.trace( "handled expected exception", e );
				s.getTransaction().rollback();
			}
			finally {
				s.close();
			}
		}
		finally {
			cleanupData();
		}
	}

	public void testOneToOneGeneratedIds() {
		try {
			Session s = openSession();
			s.beginTransaction();
			Parent p = new Parent( "parent" );
			ParentInfo info = new ParentInfo( "xyz" );
			p.setInfo( info );
			info.setOwner( p );
			s.persist( p );
			try {
				s.getTransaction().commit();
				fail( "expecting TransientObjectException on flush" );
			}
			catch( TransientObjectException e ) {
				// expected result
				log.trace( "handled expected exception", e );
				s.getTransaction().rollback();
			}
			finally {
				s.close();
			}
		}
		finally {
			cleanupData();
		}
	}

	public void testOneToOneAssignedIds() {
		try {
			Session s = openSession();
			s.beginTransaction();
			ParentAssigned p = new ParentAssigned( new Long( 1 ), "parent" );
			ParentInfoAssigned info = new ParentInfoAssigned( "something secret" );
			p.setInfo( info );
			info.setOwner( p );
			s.persist( p );
			try {
				s.getTransaction().commit();
				fail( "expecting TransientObjectException on flush" );
			}
			catch( TransientObjectException e ) {
				// expected result
				log.trace( "handled expected exception", e );
				s.getTransaction().rollback();
			}
			finally {
				s.close();
			}
		}
		finally {
			cleanupData();
		}
	}

	public void testManyToOnePropertyRefGeneratedIds() {
		try {
			Session s = openSession();
			s.beginTransaction();
			Parent p = new Parent( "parent" );
			Other other = new Other();
			other.setOwner( p );
			s.persist( other );
			try {
				s.getTransaction().commit();
				fail( "expecting TransientObjectException on flush" );
			}
			catch( TransientObjectException e ) {
				// expected result
				log.trace( "handled expected exception", e );
				s.getTransaction().rollback();
			}
			finally {
				s.close();
			}
		}
		finally {
			cleanupData();
		}
	}

	public void testManyToOnePropertyRefAssignedIds() {
		try {
			Session s = openSession();
			s.beginTransaction();
			ParentAssigned p = new ParentAssigned( new Long( 1 ), "parent" );
			OtherAssigned other = new OtherAssigned( new Long( 2 ) );
			other.setOwner( p );
			s.persist( other );
			try {
				s.getTransaction().commit();
				fail( "expecting TransientObjectException on flush" );
			}
			catch( TransientObjectException e ) {
				// expected result
				log.trace( "handled expected exception", e );
				s.getTransaction().rollback();
			}
			finally {
				s.close();
			}
		}
		finally {
			cleanupData();
		}
	}

	public void testOneToOnePropertyRefGeneratedIds() {
		try {
			Session s = openSession();
			s.beginTransaction();
			Child c2 = new Child( "c2" );
			ChildInfo info = new ChildInfo( "blah blah blah" );
			c2.setInfo( info );
			info.setOwner( c2 );
			s.persist( c2 );
			try {
				s.getTransaction().commit();
				fail( "expecting TransientObjectException on flush" );
			}
			catch( TransientObjectException e ) {
				// expected result
				log.trace( "handled expected exception : " + e );
				s.getTransaction().rollback();
			}
			finally {
				s.close();
			}
		}
		finally {
			cleanupData();
		}
	}

	public void testOneToOnePropertyRefAssignedIds() {
		try {
			Session s = openSession();
			s.beginTransaction();
			ChildAssigned c2 = new ChildAssigned( new Long( 3 ), "c3" );
			ChildInfoAssigned info = new ChildInfoAssigned( new Long( 4 ), "blah blah blah" );
			c2.setInfo( info );
			info.setOwner( c2 );
			s.persist( c2 );
			try {
				s.getTransaction().commit();
				fail( "expecting TransientObjectException on flush" );
			}
			catch( TransientObjectException e ) {
				// expected result
				log.trace( "handled expected exception : " + e );
				s.getTransaction().rollback();
			}
			finally {
				s.close();
			}
		}
		finally {
			cleanupData();
		}
	}


	private void cleanupData() {
		Session s = null;
		try {
			s = openSession();
			s.beginTransaction();
			s.createQuery( "delete ChildInfoAssigned" ).executeUpdate();
			s.createQuery( "delete ChildAssigned" ).executeUpdate();
			s.createQuery( "delete ParentAssigned" ).executeUpdate();
			s.createQuery( "delete ChildInfoAssigned" ).executeUpdate();
			s.createQuery( "delete ChildAssigned" ).executeUpdate();
			s.createQuery( "delete ParentAssigned" ).executeUpdate();
			s.getTransaction().commit();
		}
		catch( Throwable t ) {
			log.warn( "unable to cleanup test data [" + fullTestName() + "] : " + t );
		}
		finally {
			if ( s != null ) {
				try {
					s.close();
				}
				catch( Throwable ignore ) {
				}
			}
		}
	}
}
