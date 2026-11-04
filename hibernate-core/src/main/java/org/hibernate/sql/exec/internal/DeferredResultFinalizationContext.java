package org.hibernate.sql.exec.internal;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.spi.DelegatingQueryOptions;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;

/// Withholds only list transformation while retaining the invocation's original
/// execution behavior and caller-owned query options.
///
/// @author Steve Ebersole
final class DeferredResultFinalizationContext implements ExecutionContext {
	private final ExecutionContext delegate;
	private final QueryOptions options;

	DeferredResultFinalizationContext(ExecutionContext delegate) {
		this.delegate = delegate;
		options = new DelegatingQueryOptions( delegate.getQueryOptions() ) {
			@Override
			public ResultListTransformer<?> getResultListTransformer() {
				return null;
			}
		};
	}

	@Override
	public QueryOptions getQueryOptions() {
		return options;
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return delegate.getSession();
	}

	@Override
	public boolean isTransactionActive() {
		return delegate.isTransactionActive();
	}

	@Override
	public boolean isScrollResult() {
		return delegate.isScrollResult();
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return delegate.getLoadQueryInfluencers();
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return delegate.getQueryParameterBindings();
	}

	@Override
	public Callback getCallback() {
		return delegate.getCallback();
	}

	@Override
	public boolean hasCallbackActions() {
		return delegate.hasCallbackActions();
	}

	@Override
	public String getQueryIdentifier(String sql) {
		return delegate.getQueryIdentifier( sql );
	}

	@Override
	public CollectionKey getCollectionKey() {
		return delegate.getCollectionKey();
	}

	@Override
	public Object getEntityInstance() {
		return delegate.getEntityInstance();
	}

	@Override
	public Object getEntityId() {
		return delegate.getEntityId();
	}

	@Override
	public String getEntityUniqueKeyAttributePath() {
		return delegate.getEntityUniqueKeyAttributePath();
	}

	@Override
	public Object getEntityUniqueKey() {
		return delegate.getEntityUniqueKey();
	}

	@Override
	public EntityMappingType getRootEntityDescriptor() {
		return delegate.getRootEntityDescriptor();
	}

	@Override
	public void registerLoadingEntityHolder(EntityHolder holder) {
		delegate.registerLoadingEntityHolder( holder );
	}

	@Override
	public void afterStatement(LogicalConnectionImplementor connection) {
		delegate.afterStatement( connection );
	}

	@Override
	public boolean hasQueryExecutionToBeAddedToStatistics() {
		return delegate.hasQueryExecutionToBeAddedToStatistics();
	}

	@Override
	public boolean upgradeLocks() {
		return delegate.upgradeLocks();
	}
}
