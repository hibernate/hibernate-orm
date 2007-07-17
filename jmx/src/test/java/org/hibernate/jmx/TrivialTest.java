package org.hibernate.jmx;

/**
 * Test copied over from o.h.t.legacy.FooBarTest
 *
 * @author Steve Ebersole
 */
public class TrivialTest {
	public void testService() throws Exception {
		HibernateService hs = new HibernateService();
		hs.setJndiName( "SessionFactory" );
		hs.setMapResources( "org/hibernate/jmx/Entity.hbm.xml" );
		hs.setShowSqlEnabled( "true" );
		hs.start();
		hs.stop();
		hs.setProperty( "foo", "bar" );
		hs.start();
		hs.stop();
		try {
			hs.setMapResources( "non-existent" );
			hs.start();
		}
		catch( Throwable t ) {
			// expected behavior
		}
		finally {
			hs.stop();
		}
	}
}
