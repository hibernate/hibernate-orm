package org.hibernate.test.instrument.cases;
import java.util.Iterator;

import junit.framework.Assert;

import org.hibernate.Session;
import org.hibernate.bytecode.instrumentation.internal.FieldInterceptionHelper;
import org.hibernate.test.instrument.domain.Problematic;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class TestLazyPropertyCustomTypeExecutable extends AbstractExecutable {

	protected String[] getResources() {
		return new String[] { "org/hibernate/test/instrument/domain/Problematic.hbm.xml" };
	}

	public void execute() throws Exception {
		Session s = getFactory().openSession();
		Problematic p = new Problematic();
		try {
			s.beginTransaction();
			p.setName( "whatever" );
			p.setBytes( new byte[] { 1, 0, 1, 1, 0 } );
			s.save( p );
			s.getTransaction().commit();
		} catch (Exception e) {
			s.getTransaction().rollback();
			throw e;
		} finally {
			s.close();
		}

		// this access should be ok because p1 is not a lazy proxy 
		s = getFactory().openSession();
		try {
			s.beginTransaction();
			Problematic p1 = (Problematic) s.get( Problematic.class, p.getId() );
			Assert.assertTrue( FieldInterceptionHelper.isInstrumented( p1 ) );
			p1.getRepresentation();
			s.getTransaction().commit();
		} catch (Exception e) {
			s.getTransaction().rollback();
			throw e;
		} finally {
			s.close();
		}
		
		s = getFactory().openSession();
		try {
			s.beginTransaction();
			Problematic p1 = (Problematic) s.createQuery( "from Problematic" ).setReadOnly(true ).list().get( 0 );
			p1.getRepresentation();
			s.getTransaction().commit();
		} catch (Exception e) {
			s.getTransaction().rollback();
			throw e;
		} finally {
			s.close();
		}
		
		s = getFactory().openSession();
		try {
			s.beginTransaction();
			Problematic p1 = (Problematic) s.load( Problematic.class, p.getId() );
			Assert.assertFalse( FieldInterceptionHelper.isInstrumented( p1 ) );
			p1.setRepresentation( p.getRepresentation() );
			s.getTransaction().commit();
		} catch (Exception e) {
			s.getTransaction().rollback();
			throw e;
		} finally {
			s.close();
		}
	}

	protected void cleanup() {
		Session s = getFactory().openSession();
		s.beginTransaction();
		Iterator itr = s.createQuery( "from Problematic" ).list().iterator();
		while ( itr.hasNext() ) {
			Problematic p = (Problematic) itr.next();
			s.delete( p );
		}
		s.getTransaction().commit();
		s.close();
	}
}
