/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.result.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.CallbackImpl;
import org.hibernate.sql.exec.spi.Callback;

public class OutputsExecutionContext extends BaseExecutionContext {
	private final Callback callback = new CallbackImpl();

	public OutputsExecutionContext(SharedSessionContractImplementor session) {
		super( session );
	}

	@Override
	public QueryOptions getQueryOptions() {
		return QueryOptions.READ_WRITE;
	}

	@Override
	public Callback getCallback() {
		return callback;
	}

}
