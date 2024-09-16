/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import org.hibernate.LockOptions;
import org.hibernate.query.spi.QueryOptionsAdapter;

/**
 * @author Christian Beikov
 */
public class SimpleQueryOptions extends QueryOptionsAdapter {

	private final LockOptions lockOptions;
	private final Boolean readOnlyEnabled;

	public SimpleQueryOptions(LockOptions lockOptions, Boolean readOnlyEnabled) {
		this.lockOptions = lockOptions;
		this.readOnlyEnabled = readOnlyEnabled;
	}

	@Override
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Override
	public Boolean isReadOnly() {
		return readOnlyEnabled;
	}
}
