/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * A no-op implementation of SessionEventListener.  Intended as a convenient base class for developing
 * SessionEventListener implementations.
 *
 * @author Steve Ebersole
 */
public class BaseSessionEventListener implements SessionEventListener {
	@Override
	public void transactionCompletion(boolean successful) {
	}

	@Override
	public void jdbcConnectionAcquisitionStart() {
	}

	@Override
	public void jdbcConnectionAcquisitionEnd() {
	}

	@Override
	public void jdbcConnectionReleaseStart() {
	}

	@Override
	public void jdbcConnectionReleaseEnd() {
	}

	@Override
	public void jdbcPrepareStatementStart() {
	}

	@Override
	public void jdbcPrepareStatementEnd() {
	}

	@Override
	public void jdbcExecuteStatementStart() {
	}

	@Override
	public void jdbcExecuteStatementEnd() {
	}

	@Override
	public void jdbcExecuteBatchStart() {
	}

	@Override
	public void jdbcExecuteBatchEnd() {
	}

	@Override
	public void cachePutStart() {
	}

	@Override
	public void cachePutEnd() {
	}

	@Override
	public void cacheGetStart() {
	}

	@Override
	public void cacheGetEnd(boolean hit) {
	}

	@Override
	public void flushStart() {
	}

	@Override
	public void flushEnd(int numberOfEntities, int numberOfCollections) {
	}

	@Override
	public void partialFlushStart() {
	}

	@Override
	public void partialFlushEnd(int numberOfEntities, int numberOfCollections) {
	}

	@Override
	public void dirtyCalculationStart() {
	}

	@Override
	public void dirtyCalculationEnd(boolean dirty) {
	}

	@Override
	public void end() {
	}
}
