/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.math.BigDecimal;

import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * This is how to do batch processing in Hibernate. Remember to enable JDBC batch updates, or this test will take a
 * VeryLongTime!
 *
 * @author Gavin King
 */
public class BatchTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public String[] getMappings() {
		return new String[] { "batch/DataPoint.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.STATEMENT_BATCH_SIZE, 20 );
	}

	@Test
	public void testBatchInsertUpdate() {
		long start = System.currentTimeMillis();
		final int N = 5000; //26 secs with batch flush, 26 without
		//final int N = 100000; //53 secs with batch flush, OOME without
		//final int N = 250000; //137 secs with batch flush, OOME without
		int batchSize = sessionFactory().getSessionFactoryOptions().getJdbcBatchSize();
		doBatchInsertUpdate( N, batchSize );
		System.out.println( System.currentTimeMillis() - start );
	}

	@Test
	public void testBatchInsertUpdateSizeEqJdbcBatchSize() {
		int batchSize = sessionFactory().getSessionFactoryOptions().getJdbcBatchSize();
		doBatchInsertUpdate( 50, batchSize );
	}

	@Test
	public void testBatchInsertUpdateSizeLtJdbcBatchSize() {
		int batchSize = sessionFactory().getSessionFactoryOptions().getJdbcBatchSize();
		doBatchInsertUpdate( 50, batchSize - 1 );
	}

	@Test
	public void testBatchInsertUpdateSizeGtJdbcBatchSize() {
		int batchSize = sessionFactory().getSessionFactoryOptions().getJdbcBatchSize();
		doBatchInsertUpdate( 50, batchSize + 1 );
	}

	public void doBatchInsertUpdate(int nEntities, int nBeforeFlush) {
		Session s = openSession();
		s.setCacheMode( CacheMode.IGNORE );
		Transaction t = s.beginTransaction();
		for ( int i = 0; i < nEntities; i++ ) {
			DataPoint dp = new DataPoint();
			dp.setX( new BigDecimal( i * 0.1d ).setScale( 19, BigDecimal.ROUND_DOWN ) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ).setScale( 19, BigDecimal.ROUND_DOWN ) );
			s.persist( dp );
			if ( ( i + 1 ) % nBeforeFlush == 0 ) {
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
		try (ScrollableResults sr = s.createQuery( "from DataPoint dp order by dp.x asc" )
				.scroll( ScrollMode.FORWARD_ONLY )) {
			while ( sr.next() ) {
				DataPoint dp = (DataPoint) sr.get();
				dp.setDescription( "done!" );
				if ( ++i % nBeforeFlush == 0 ) {
					s.flush();
					s.clear();
				}
			}
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode( CacheMode.IGNORE );
		t = s.beginTransaction();
		i = 0;
		try (ScrollableResults sr = s.createQuery( "from DataPoint dp order by dp.x asc" )
				.scroll( ScrollMode.FORWARD_ONLY )) {
			while ( sr.next() ) {
				DataPoint dp = (DataPoint) sr.get();
				s.remove( dp );
				if ( ++i % nBeforeFlush == 0 ) {
					s.flush();
					s.clear();
				}
			}
		}
		t.commit();
		s.close();
	}
}
