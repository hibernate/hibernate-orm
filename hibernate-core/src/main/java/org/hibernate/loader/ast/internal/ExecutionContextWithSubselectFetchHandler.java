package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.query.internal.SimpleQueryOptions;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.internal.BaseExecutionContext;

class ExecutionContextWithSubselectFetchHandler extends BaseExecutionContext {

	@Nullable
	private final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler;
	private final boolean readOnly;
	private final QueryOptions queryOptions;

	public ExecutionContextWithSubselectFetchHandler(
			@Nonnull SharedSessionContractImplementor session,
			@Nullable SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler) {
		super( session );
		this.subSelectFetchableKeysHandler = subSelectFetchableKeysHandler;
		this.readOnly = false;
		this.queryOptions = QueryOptions.NONE;
	}

	public ExecutionContextWithSubselectFetchHandler(
			@Nonnull SharedSessionContractImplementor session,
			@Nullable SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler,
			boolean readOnly,
			@Nonnull LockOptions lockOptions) {
		super( session );
		this.subSelectFetchableKeysHandler = subSelectFetchableKeysHandler;
		this.readOnly = readOnly;
		this.queryOptions = determineQueryOptions( readOnly, lockOptions );
	}

	@Nonnull
	private QueryOptions determineQueryOptions(boolean readOnly, @Nonnull LockOptions lockOptions) {
		return new SimpleQueryOptions( lockOptions, readOnly ? true : null );
	}

	@Override
	public void registerLoadingEntityHolder(@Nonnull EntityHolder holder) {
		if ( subSelectFetchableKeysHandler != null ) {
			subSelectFetchableKeysHandler.addKey( holder );
		}
	}

	@Nonnull
	@Override
	public QueryOptions getQueryOptions() {
		return queryOptions;
	}

	@Override
	public boolean upgradeLocks() {
		return true;
	}
}
