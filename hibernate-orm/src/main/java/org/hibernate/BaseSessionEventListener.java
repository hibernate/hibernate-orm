/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

/**
 * A no-op implementation of SessionEventListener.  Intended as a convenient base class for developing
 * SessionEventListener implementations.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("UnusedDeclaration")
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
