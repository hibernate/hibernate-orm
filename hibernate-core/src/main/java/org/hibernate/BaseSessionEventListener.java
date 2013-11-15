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
