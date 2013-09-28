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

import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.batch.internal.BatchingBatch;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.jboss.logging.Logger;
import org.junit.Test;

/**
 * @author Hernan Chanfreau
 */
public class BatchLogTest extends BaseCoreFunctionalTestCase {

	private static final Logger LOGGER = Logger.getLogger(BatchLogTest.class);

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
		cfg.setProperty(Environment.STATEMENT_BATCH_SIZE, "5");

		// setting the log4j config for
		// org.hibernate.engine.jdbc.batch.internal.BatchingBatch to debug would
		// avoid this
		org.apache.log4j.Logger logger = org.apache.log4j.Logger
				.getLogger(BatchingBatch.class);
		logger.setLevel(org.apache.log4j.Level.DEBUG);

	}

	@Test
	public void testBatchLoggingInsertUpdateSizeEqJdbcBatchSize() {
		LOGGER.info("Executing testBatchLoggingInsertUpdateSizeEqJdbcBatchSize()");
		int batchSize = ((SessionFactoryImplementor) sessionFactory())
				.getSettings().getJdbcBatchSize();
		doBatchInsertUpdate(15, batchSize);
		LOGGER.info("Finished testBatchLoggingInsertUpdateSizeEqJdbcBatchSize()");
	}

	@Test
	public void testBatchLoggingInsertUpdateSizeGtJdbcBatchSize() {
		LOGGER.info("Executing testBatchLoggingInsertUpdateSizeGtJdbcBatchSize()");
		int batchSize = ((SessionFactoryImplementor) sessionFactory())
				.getSettings().getJdbcBatchSize();
		doBatchInsertUpdate(15, batchSize + 2);
		LOGGER.info("Finished testBatchLoggingInsertUpdateSizeGtJdbcBatchSize()");
	}

	@Test
	public void testBatchLoggingInsertUpdateSizeLtJdbcBatchSize() {
		LOGGER.info("Executing testBatchLoggingInsertUpdateSizeLtJdbcBatchSize()");
		int batchSize = ((SessionFactoryImplementor) sessionFactory())
				.getSettings().getJdbcBatchSize();
		doBatchInsertUpdate(15, batchSize - 2);
		LOGGER.info("Finished testBatchLoggingInsertUpdateSizeLtJdbcBatchSize()");
	}

	public void doBatchInsertUpdate(int nEntities, int nBeforeFlush) {
		Session s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		Transaction t = s.beginTransaction();
		for (int i = 0; i < nEntities; i++) {
			DataPoint dp = new DataPoint();
			dp.setX(new BigDecimal(i * 0.1d)
					.setScale(19, BigDecimal.ROUND_DOWN));
			dp.setY(new BigDecimal(Math.cos(dp.getX().doubleValue())).setScale(
					19, BigDecimal.ROUND_DOWN));
			s.save(dp);
			if (i + 1 % nBeforeFlush == 0) {
				s.flush();
				s.clear();
			}
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		int i = 0;
		ScrollableResults sr = s.createQuery(
				"from DataPoint dp order by dp.x asc").scroll(
				ScrollMode.FORWARD_ONLY);
		while (sr.next()) {
			DataPoint dp = (DataPoint) sr.get(0);
			dp.setDescription("done!");
			if (++i % nBeforeFlush == 0) {
				s.flush();
				s.clear();
			}
		}
		t.commit();
		s.close();

		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		t = s.beginTransaction();
		i = 0;
		sr = s.createQuery("from DataPoint dp order by dp.x asc").scroll(
				ScrollMode.FORWARD_ONLY);
		while (sr.next()) {
			DataPoint dp = (DataPoint) sr.get(0);
			s.delete(dp);
			if (++i % nBeforeFlush == 0) {
				s.flush();
				s.clear();
			}
		}
		t.commit();
		s.close();
	}
}
