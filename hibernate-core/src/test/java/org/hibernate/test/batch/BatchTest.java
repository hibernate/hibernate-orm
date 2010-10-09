//$Id: BatchTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.batch;

import java.math.BigDecimal;

import junit.framework.Test;

import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

/**
 * This is how to do batch processing in Hibernate.
 * Remember to enable JDBC batch updates, or this
 * test will take a Very Long Time!
 *
 * @author Gavin King
 */
public class BatchTest extends FunctionalTestCase {

	public BatchTest(String str) {
		super( str );
	}

	public String[] getMappings() {
		return new String[] { "batch/DataPoint.hbm.xml" };
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.STATEMENT_BATCH_SIZE, "20" );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( BatchTest.class );
	}

	public void testBatchInsertUpdate() {
		long start = System.currentTimeMillis();
		final int N = 5000; //26 secs with batch flush, 26 without
		//final int N = 100000; //53 secs with batch flush, OOME without
		//final int N = 250000; //137 secs with batch flush, OOME without

		Session s = openSession();
		s.setCacheMode( CacheMode.IGNORE );
		Transaction t = s.beginTransaction();
		for ( int i = 0; i < N; i++ ) {
			DataPoint dp = new DataPoint();
			dp.setX( new BigDecimal( i * 0.1d ).setScale( 19, BigDecimal.ROUND_DOWN ) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale( 19, BigDecimal.ROUND_DOWN ) );
			s.save( dp );
			if ( i % 20 == 0 ) {
				s.flush();
				s.clear();
			}
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode( CacheMode.IGNORE );
		t = s.beginTransaction();
		int i = 0;
		ScrollableResults sr = s.createQuery( "from DataPoint dp order by dp.x asc" )
				.scroll( ScrollMode.FORWARD_ONLY );
		while ( sr.next() ) {
			DataPoint dp = ( DataPoint ) sr.get( 0 );
			dp.setDescription( "done!" );
			if ( ++i % 20 == 0 ) {
				s.flush();
				s.clear();
			}
		}
		t.commit();
		s.close();
		System.out.println( System.currentTimeMillis() - start );
	}

}

