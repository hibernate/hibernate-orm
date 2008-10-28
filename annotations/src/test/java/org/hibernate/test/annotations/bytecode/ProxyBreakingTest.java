//$Id$
package org.hibernate.test.annotations.bytecode;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.Hibernate;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.cfg.Configuration;

/**
 * @author Emmanuel Bernard
 */
public class ProxyBreakingTest extends TestCase {

	static {
		System.setProperty( "hibernate.bytecode.provider", "javassist" );
	}

	public void testProxiedBridgeMethod() throws Exception {
		//bridge methods should not be proxied
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Hammer h = new Hammer();
		s.save(h);
		s.flush();
		s.clear();
		assertNotNull( "The proxy creation failure is breaking things", h.getId() );
		h = (Hammer) s.load( Hammer.class, h.getId() );
		assertFalse( Hibernate.isInitialized( h ) );
		tx.rollback();
		s.close();
	}

	public ProxyBreakingTest(String name) {
		super( name );
	}

	protected Class[] getMappings() {
		return new Class[0];
	}

	protected String[] getXmlFiles() {
		return new String[] {
				"org/hibernate/test/annotations/bytecode/Hammer.hbm.xml"
		};
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg.setProperty( "hibernate.bytecode.provider", "javassist" ) );
	}
}
