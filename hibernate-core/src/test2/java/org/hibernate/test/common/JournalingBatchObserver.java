/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.common;

import org.hibernate.engine.jdbc.batch.spi.BatchObserver;

/**
 * @author Steve Ebersole
 */
public class JournalingBatchObserver implements BatchObserver {
	private int implicitExecutionCount;
	private int explicitExecutionCount;

	@Override
	public void batchExplicitlyExecuted() {
		explicitExecutionCount++;
	}

	@Override
	public void batchImplicitlyExecuted() {
		implicitExecutionCount++;
	}

	public int getImplicitExecutionCount() {
		return implicitExecutionCount;
	}

	public int getExplicitExecutionCount() {
		return explicitExecutionCount;
	}

	public void reset() {
		explicitExecutionCount = 0;
		implicitExecutionCount = 0;
	}
}
