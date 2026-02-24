/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.internal;

import org.hibernate.procedure.UpdateCountOutput;

/**
 * Implementation of UpdateCountOutput
 *
 * @author Steve Ebersole
 */
public class UpdateCountOutputImpl implements UpdateCountOutput {
	private final int updateCount;

	public UpdateCountOutputImpl(int updateCount) {
		this.updateCount = updateCount;
	}

	@Override
	public int getUpdateCount() {
		return updateCount;
	}

	@Override
	public boolean isResultSet() {
		return false;
	}
}
