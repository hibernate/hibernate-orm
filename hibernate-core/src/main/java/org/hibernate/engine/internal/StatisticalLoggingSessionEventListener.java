/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.internal;

import org.hibernate.BaseSessionEventListener;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class StatisticalLoggingSessionEventListener extends BaseSessionEventListener {
	private static final Logger log = Logger.getLogger( StatisticalLoggingSessionEventListener.class );

	/**
	 * Used by SettingsFactory (in conjunction with stats being enabled) to determine whether to apply this listener
	 *
	 * @return {@code true} if logging is enabled for this listener.
	 */
	public static boolean isLoggingEnabled() {
		return log.isInfoEnabled();
	}

	// cumulative state ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private int jdbcConnectionAcquisitionCount;
	private long jdbcConnectionAcquisitionTime;

	private int jdbcConnectionReleaseCount;
	private long jdbcConnectionReleaseTime;

	private int jdbcPrepareStatementCount;
	private long jdbcPrepareStatementTime;

	private int jdbcExecuteStatementCount;
	private long jdbcExecuteStatementTime;

	private int jdbcExecuteBatchCount;
	private long jdbcExecuteBatchTime;

	private int cachePutCount;
	private long cachePutTime;

	private int cacheHitCount;
	private long cacheHitTime;

	private int cacheMissCount;
	private long cacheMissTime;

	private int flushCount;
	private long flushEntityCount;
	private long flushCollectionCount;
	private long flushTime;

	private int partialFlushCount;
	private long partialFlushEntityCount;
	private long partialFlushCollectionCount;
	private long partialFlushTime;


	// JDBC Connection acquisition ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private long jdbcConnectionAcquisitionStart = -1;

	@Override
	public void jdbcConnectionAcquisitionStart() {
		assert jdbcConnectionAcquisitionStart < 0 : "Nested calls to jdbcConnectionAcquisitionStart";
		jdbcConnectionAcquisitionStart = System.nanoTime();
	}

	@Override
	public void jdbcConnectionAcquisitionEnd() {
		assert jdbcConnectionAcquisitionStart > 0:
				"Unexpected call to jdbcConnectionAcquisitionEnd; expecting jdbcConnectionAcquisitionStart";

		jdbcConnectionAcquisitionCount++;
		jdbcConnectionAcquisitionTime += ( System.nanoTime() - jdbcConnectionAcquisitionStart );
		jdbcConnectionAcquisitionStart = -1;
	}


	// JDBC Connection release ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private long jdbcConnectionReleaseStart = -1;

	@Override
	public void jdbcConnectionReleaseStart() {
		assert jdbcConnectionReleaseStart < 0 : "Nested calls to jdbcConnectionReleaseStart";
		jdbcConnectionReleaseStart = System.nanoTime();
	}

	@Override
	public void jdbcConnectionReleaseEnd() {
		assert jdbcConnectionReleaseStart > 0:
				"Unexpected call to jdbcConnectionReleaseEnd; expecting jdbcConnectionReleaseStart";

		jdbcConnectionReleaseCount++;
		jdbcConnectionReleaseTime += ( System.nanoTime() - jdbcConnectionReleaseStart );
		jdbcConnectionReleaseStart = -1;
	}


	// JDBC statement preparation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private long jdbcPrepStart = -1;

	@Override
	public void jdbcPrepareStatementStart() {
		assert jdbcPrepStart < 0 : "Nested calls to jdbcPrepareStatementStart";
		jdbcPrepStart = System.nanoTime();
	}

	@Override
	public void jdbcPrepareStatementEnd() {
		assert jdbcPrepStart > 0 : "Unexpected call to jdbcPrepareStatementEnd; expecting jdbcPrepareStatementStart";

		jdbcPrepareStatementCount++;
		jdbcPrepareStatementTime += ( System.nanoTime() - jdbcPrepStart );
		jdbcPrepStart = -1;
	}


	// JDBC statement execution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private long jdbcExecutionStart = -1;

	@Override
	public void jdbcExecuteStatementStart() {
		assert jdbcExecutionStart < 0 : "Nested calls to jdbcExecuteStatementStart";
		jdbcExecutionStart = System.nanoTime();
	}

	@Override
	public void jdbcExecuteStatementEnd() {
		assert jdbcExecutionStart > 0 : "Unexpected call to jdbcExecuteStatementEnd; expecting jdbcExecuteStatementStart";

		jdbcExecuteStatementCount++;
		jdbcExecuteStatementTime += ( System.nanoTime() - jdbcExecutionStart );
		jdbcExecutionStart = -1;
	}


	// JDBC batch execution ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private long jdbcBatchExecutionStart = -1;

	@Override
	public void jdbcExecuteBatchStart() {
		assert jdbcBatchExecutionStart < 0 : "Nested calls to jdbcExecuteBatchStart";
		jdbcBatchExecutionStart = System.nanoTime();
	}

	@Override
	public void jdbcExecuteBatchEnd() {
		assert jdbcBatchExecutionStart > 0 : "Unexpected call to jdbcExecuteBatchEnd; expecting jdbcExecuteBatchStart";

		jdbcExecuteBatchCount++;
		jdbcExecuteBatchTime += ( System.nanoTime() - jdbcBatchExecutionStart );
		jdbcBatchExecutionStart = -1;
	}


	// Caching - put  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private long cachePutStart = -1;

	@Override
	public void cachePutStart() {
		assert cachePutStart < 0 : "Nested calls to cachePutStart";
		cachePutStart = System.nanoTime();
	}

	@Override
	public void cachePutEnd() {
		assert cachePutStart > 0 : "Unexpected call to cachePutEnd; expecting cachePutStart";

		cachePutCount++;
		cachePutTime += ( System.nanoTime() - cachePutStart );
		cachePutStart = -1;
	}


	// Caching - get  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private long cacheGetStart = -1;

	@Override
	public void cacheGetStart() {
		assert cacheGetStart < 0 : "Nested calls to cacheGetStart";
		cacheGetStart = System.nanoTime();
	}

	@Override
	public void cacheGetEnd(boolean hit) {
		assert cacheGetStart > 0 : "Unexpected call to cacheGetEnd; expecting cacheGetStart";

		if ( hit ) {
			cacheHitCount++;
			cacheHitTime += ( System.nanoTime() - cacheGetStart );
		}
		else {
			cacheMissCount++;
			cacheMissTime += ( System.nanoTime() - cacheGetStart );
		}
		cacheGetStart = -1;
	}


	// Flushing  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private long flushStart = -1;

	@Override
	public void flushStart() {
		assert flushStart < 0 : "Nested calls to flushStart";
		flushStart = System.nanoTime();
	}

	@Override
	public void flushEnd(int numberOfEntities, int numberOfCollections) {
		assert flushStart > 0 : "Unexpected call to flushEnd; expecting flushStart";

		flushCount++;
		flushEntityCount += numberOfEntities;
		flushCollectionCount += numberOfCollections;
		flushTime += ( System.nanoTime() - flushStart );
		flushStart = -1;
	}


	// Partial-flushing  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private long partialFlushStart = -1;

	@Override
	public void partialFlushStart() {
		assert partialFlushStart < 0 : "Nested calls to partialFlushStart";
		partialFlushStart = System.nanoTime();
	}

	@Override
	public void partialFlushEnd(int numberOfEntities, int numberOfCollections) {
		assert partialFlushStart > 0 : "Unexpected call to partialFlushEnd; expecting partialFlushStart";

		partialFlushCount++;
		partialFlushEntityCount += numberOfEntities;
		partialFlushCollectionCount += numberOfCollections;
		partialFlushTime += ( System.nanoTime() - partialFlushStart );
		partialFlushStart = -1;
	}

	@Override
	public void end() {
		log.infof(
				"Session Metrics {\n" +
						"    %s nanoseconds spent acquiring %s JDBC connections;\n" +
						"    %s nanoseconds spent releasing %s JDBC connections;\n" +
						"    %s nanoseconds spent preparing %s JDBC statements;\n" +
						"    %s nanoseconds spent executing %s JDBC statements;\n" +
						"    %s nanoseconds spent executing %s JDBC batches;\n" +
						"    %s nanoseconds spent performing %s L2C puts;\n" +
						"    %s nanoseconds spent performing %s L2C hits;\n" +
						"    %s nanoseconds spent performing %s L2C misses;\n" +
						"    %s nanoseconds spent executing %s flushes (flushing a total of %s entities and %s collections);\n" +
						"    %s nanoseconds spent executing %s partial-flushes (flushing a total of %s entities and %s collections)\n" +
						"}",
				jdbcConnectionAcquisitionTime,
				jdbcConnectionAcquisitionCount,
				jdbcConnectionReleaseTime,
				jdbcConnectionReleaseCount,
				jdbcPrepareStatementTime,
				jdbcPrepareStatementCount,
				jdbcExecuteStatementTime,
				jdbcExecuteStatementCount,
				jdbcExecuteBatchTime,
				jdbcExecuteBatchCount,
				cachePutTime,
				cachePutCount,
				cacheHitTime,
				cacheHitCount,
				cacheMissTime,
				cacheMissCount,
				flushTime,
				flushCount,
				flushEntityCount,
				flushCollectionCount,
				partialFlushTime,
				partialFlushCount,
				partialFlushEntityCount,
				partialFlushCollectionCount
		);
	}
}
