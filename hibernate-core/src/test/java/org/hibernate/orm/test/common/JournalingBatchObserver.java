/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.common;

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
