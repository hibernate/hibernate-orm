/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.entity.LoadingEntityEntry;

/**
 * @author Christian Beikov
 */
public class DelegatingExecutionContext implements ExecutionContext {

	private final ExecutionContext executionContext;

	public DelegatingExecutionContext(ExecutionContext executionContext) {
		this.executionContext = executionContext;
	}

	@Override
	public String getQueryIdentifier(String sql) {
		return executionContext.getQueryIdentifier( sql );
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return executionContext.getSession();
	}

	@Override
	public QueryOptions getQueryOptions() {
		return executionContext.getQueryOptions();
	}

	@Override
	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return executionContext.getLoadQueryInfluencers();
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return executionContext.getQueryParameterBindings();
	}

	@Override
	public Callback getCallback() {
		return executionContext.getCallback();
	}

	@Override
	public CollectionKey getCollectionKey() {
		return executionContext.getCollectionKey();
	}

	@Override
	public Object getEntityInstance() {
		return executionContext.getEntityInstance();
	}

	@Override
	public Object getEntityId() {
		return executionContext.getEntityId();
	}

	@Override
	public void registerLoadingEntityEntry(
			EntityKey entityKey,
			LoadingEntityEntry entry) {
		executionContext.registerLoadingEntityEntry( entityKey, entry );
	}

	@Override
	public void afterStatement(LogicalConnectionImplementor logicalConnection) {
		executionContext.afterStatement( logicalConnection );
	}

}
