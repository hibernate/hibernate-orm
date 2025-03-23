/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.query.internal.SimpleQueryOptions;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.internal.BaseExecutionContext;

class ExecutionContextWithSubselectFetchHandler extends BaseExecutionContext {

	private final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler;
	private final boolean readOnly;
	private final QueryOptions queryOptions;

	public ExecutionContextWithSubselectFetchHandler(
			SharedSessionContractImplementor session,
			SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler) {
		super( session );
		this.subSelectFetchableKeysHandler = subSelectFetchableKeysHandler;
		this.readOnly = false;
		this.queryOptions = QueryOptions.NONE;
	}

	public ExecutionContextWithSubselectFetchHandler(
			SharedSessionContractImplementor session,
			SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler,
			boolean readOnly,
			LockOptions lockOptions) {
		super( session );
		this.subSelectFetchableKeysHandler = subSelectFetchableKeysHandler;
		this.readOnly = readOnly;
		this.queryOptions = determineQueryOptions( readOnly, lockOptions );
	}

	private QueryOptions determineQueryOptions(boolean readOnly, LockOptions lockOptions) {
		return new SimpleQueryOptions( lockOptions, readOnly ? true : null );
	}

	@Override
	public void registerLoadingEntityHolder(EntityHolder holder) {
		if ( subSelectFetchableKeysHandler != null ) {
			subSelectFetchableKeysHandler.addKey( holder );
		}
	}

	@Override
	public QueryOptions getQueryOptions() {
		return queryOptions;
	}

	@Override
	public boolean upgradeLocks() {
		return true;
	}
}
