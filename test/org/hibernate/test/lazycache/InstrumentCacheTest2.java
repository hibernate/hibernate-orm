//$Id$
package org.hibernate.test.lazycache;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.intercept.FieldInterceptionHelper;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class InstrumentCacheTest2 extends FunctionalTestCase {
	
	public InstrumentCacheTest2(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "lazycache/Documents.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( InstrumentCacheTest2.class );
	}

	public static boolean isRunnable() {
		return FieldInterceptionHelper.isInstrumented( new Document() );
	}

	public void testInitFromCache() {
		Session s;
		Transaction tx;
		
		s = getSessions().openSession();
		tx = s.beginTransaction();
		s.persist( new Document("HiA", "Hibernate book", "Hibernate is....") );
		tx.commit();
		s.close();
		
		s = getSessions().openSession();
		tx = s.beginTransaction();
		s.createQuery("from Document fetch all properties").uniqueResult();
		tx.commit();
		s.close();
		
		getSessions().getStatistics().clear();
		
		s = getSessions().openSession();
		tx = s.beginTransaction();
		Document d = (Document) s.createCriteria(Document.class).uniqueResult();
		assertFalse( Hibernate.isPropertyInitialized(d, "text") );
		assertFalse( Hibernate.isPropertyInitialized(d, "summary") );
		assertEquals( "Hibernate is....", d.getText() );
		assertTrue( Hibernate.isPropertyInitialized(d, "text") );
		assertTrue( Hibernate.isPropertyInitialized(d, "summary") );
		tx.commit();
		s.close();
		
		assertEquals( 1, getSessions().getStatistics().getPrepareStatementCount() );

		s = getSessions().openSession();
		tx = s.beginTransaction();
		d = (Document) s.get(Document.class, d.getId());
		assertTrue( Hibernate.isPropertyInitialized(d, "text") );
		assertTrue( Hibernate.isPropertyInitialized(d, "summary") );
		tx.commit();
		s.close();
	}

}

