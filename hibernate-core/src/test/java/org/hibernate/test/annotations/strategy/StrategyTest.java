//$Id$
package org.hibernate.test.annotations.strategy;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.DefaultComponentSafeNamingStrategy;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class StrategyTest extends TestCase {

	public void testComponentSafeStrategy() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Location start = new Location();
		start.setCity( "Paris" );
		start.setCountry( "France" );
		Location end = new Location();
		end.setCity( "London" );
		end.setCountry( "UK" );
		Storm storm = new Storm();
		storm.setEnd( end );
		storm.setStart( start );
		s.persist( storm );
		s.flush();
		tx.rollback();
		s.close();
	}

	protected void configure(Configuration cfg) {
		cfg.setNamingStrategy( DefaultComponentSafeNamingStrategy.INSTANCE );
		//cfg.getSessionEventListenerConfig().setFlushEventListener( new EJB3FlushEventListener() );
		//cfg.getSessionEventListenerConfig().setAutoFlushEventListener( new EJB3AutoFlushEventListener() );
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Storm.class
		};
	}
}
