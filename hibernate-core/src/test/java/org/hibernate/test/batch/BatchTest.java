/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.batch;
import java.math.BigDecimal;

import org.junit.Test;

import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * This is how to do batch processing in Hibernate. Remember to enable JDBC batch updates, or this test will take a
 * VeryLongTime!
 *
 * @author Gavin King
 */
public class BatchTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "batch/DataPoint.hbm.xml" };
	}

	@Override
	public String getCacheConcurrencyStrategy() {
		return null;
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.STATEMENT_BATCH_SIZE, "20" );
	}

	@Test
	public void testBatchInsertUpdate() {
		long start = System.currentTimeMillis();
		final int N = 5000; //26 secs with batch flush, 26 without
		//final int N = 100000; //53 secs with batch flush, OOME without
		//final int N = 250000; //137 secs with batch flush, OOME without
		int batchSize = sessionFactory().getSettings().getJdbcBatchSize();
		doBatchInsertUpdate( N, batchSize );
		System.out.println( System.currentTimeMillis() - start );
	}

	@Test
	public void testBatchInsertUpdateSizeEqJdbcBatchSize() {
		int batchSize = sessionFactory().getSettings().getJdbcBatchSize();
		doBatchInsertUpdate( 50, batchSize );
	}

	@Test
	public void testBatchInsertUpdateSizeLtJdbcBatchSize() {
		int batchSize = sessionFactory().getSettings().getJdbcBatchSize();
		doBatchInsertUpdate( 50, batchSize - 1 );
	}

	@Test
	public void testBatchInsertUpdateSizeGtJdbcBatchSize() {
		long start = System.currentTimeMillis();
		int batchSize = sessionFactory().getSettings().getJdbcBatchSize();
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
			s.save( dp );
			if ( i + 1 % nBeforeFlush == 0 ) {
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
			if ( ++i % nBeforeFlush == 0 ) {
				s.flush();
				s.clear();
			}
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode( CacheMode.IGNORE );
		t = s.beginTransaction();
		i = 0;
		sr = s.createQuery( "from DataPoint dp order by dp.x asc" )
				.scroll( ScrollMode.FORWARD_ONLY );
		while ( sr.next() ) {
			DataPoint dp = ( DataPoint ) sr.get( 0 );
			s.delete( dp );
			if ( ++i % nBeforeFlush == 0 ) {
				s.flush();
				s.clear();
			}
		}
		t.commit();
		s.close();
	}
}

