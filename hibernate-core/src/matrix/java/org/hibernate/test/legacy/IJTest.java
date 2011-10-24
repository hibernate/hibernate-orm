//$Id: IJTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.legacy;

import java.io.Serializable;

import org.junit.Test;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.dialect.HSQLDialect;

import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class IJTest extends LegacyTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "legacy/IJ.hbm.xml" };
	}

	@Test
	public void testFormulaDiscriminator() throws Exception {
		if ( getDialect() instanceof HSQLDialect ) return;
		Session s = sessionFactory().openSession();
		s.beginTransaction();
		I i = new I();
		i.setName( "i" );
		i.setType( 'a' );
		J j = new J();
		j.setName( "j" );
		j.setType( 'x' );
		j.setAmount( 1.0f );
		Serializable iid = s.save(i);
		Serializable jid = s.save(j);
		s.getTransaction().commit();
		s.close();

		sessionFactory().getCache().evictEntityRegion( I.class );

		s = sessionFactory().openSession();
		s.beginTransaction();
		j = (J) s.get(I.class, jid);
		i = (I) s.get(I.class, iid);
		assertTrue( i.getClass()==I.class );
		j.setAmount( 0.5f );
		s.lock(i, LockMode.UPGRADE);
		s.getTransaction().commit();
		s.close();

		s = sessionFactory().openSession();
		s.beginTransaction();
		j = (J) s.get(I.class, jid, LockMode.UPGRADE);
		i = (I) s.get(I.class, iid, LockMode.UPGRADE);
		s.getTransaction().commit();
		s.close();

		s = sessionFactory().openSession();
		s.beginTransaction();
		assertTrue( s.createQuery( "from I" ).list().size()==2 );
		assertTrue( s.createQuery( "from J" ).list().size()==1 );
		assertTrue( s.createQuery( "from I i where i.class = 0" ).list().size()==1 );
		assertTrue( s.createQuery( "from I i where i.class = 1" ).list().size()==1 );
		s.getTransaction().commit();
		s.close();

		s = sessionFactory().openSession();
		s.beginTransaction();
		j = (J) s.get(J.class, jid);
		i = (I) s.get(I.class, iid);
		s.delete(j);
		s.delete(i);
		s.getTransaction().commit();
		s.close();

	}
}
