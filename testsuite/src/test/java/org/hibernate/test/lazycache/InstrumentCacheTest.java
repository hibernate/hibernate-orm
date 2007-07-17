//$Id: InstrumentCacheTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.lazycache;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.intercept.FieldInterceptionHelper;
import org.hibernate.junit.functional.DatabaseSpecificFunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class InstrumentCacheTest extends DatabaseSpecificFunctionalTestCase {

	public InstrumentCacheTest(String str) {
		super(str);
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( InstrumentCacheTest.class );
	}

	private boolean isRunnable() {
		// skip this test if document is not instrumented/enhanced
		return FieldInterceptionHelper.isInstrumented( new Document() );
	}

	public boolean appliesTo(Dialect dialect) {
		return isRunnable();
	}

	public String[] getMappings() {
		return new String[] { "lazycache/Documents.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
	}

	public boolean overrideCacheStrategy() {
		return false;
	}

	public void testInitFromCache() {
		if ( !isRunnable() ) {
			reportSkip( "classes not instrumented", "instrumentation tests" );
			return;
		}
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

		assertEquals( 2, getSessions().getStatistics().getPrepareStatementCount() );

		s = getSessions().openSession();
		tx = s.beginTransaction();
		d = (Document) s.get(Document.class, d.getId());
		assertFalse( Hibernate.isPropertyInitialized(d, "text") );
		assertFalse( Hibernate.isPropertyInitialized(d, "summary") );
		tx.commit();
		s.close();
	}

	public void testInitFromCache2() {
		if ( !isRunnable() ) {
			reportSkip( "classes not instrumented", "instrumentation tests" );
			return;
		}
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

