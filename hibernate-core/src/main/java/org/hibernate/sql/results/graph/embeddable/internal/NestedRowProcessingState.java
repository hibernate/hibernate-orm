/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.sql.results.spi.RowReader;

public class NestedRowProcessingState implements RowProcessingState {
	private final AggregateEmbeddableInitializer aggregateEmbeddableInitializer;
	final RowProcessingState processingState;

	public NestedRowProcessingState(
			AggregateEmbeddableInitializer aggregateEmbeddableInitializer,
			RowProcessingState processingState) {
		this.aggregateEmbeddableInitializer = aggregateEmbeddableInitializer;
		this.processingState = processingState;
	}

	public static NestedRowProcessingState wrap(
			AggregateEmbeddableInitializer aggregateEmbeddableInitializer,
			RowProcessingState processingState) {
		if ( processingState instanceof NestedRowProcessingState ) {
			return new NestedRowProcessingState(
					aggregateEmbeddableInitializer,
					( (NestedRowProcessingState) processingState ).processingState
			);
		}
		return new NestedRowProcessingState( aggregateEmbeddableInitializer, processingState );
	}

	@Override
	public Object getJdbcValue(int position) {
		final Object[] jdbcValue = aggregateEmbeddableInitializer.getJdbcValues( processingState );
		return jdbcValue == null ? null : jdbcValue[position];
	}

	// -- delegate the rest

	@Override
	public JdbcValuesSourceProcessingState getJdbcValuesSourceProcessingState() {
		return processingState.getJdbcValuesSourceProcessingState();
	}

	@Override
	public RowReader<?> getRowReader() {
		return processingState.getRowReader();
	}

	@Override
	public void registerNonExists(EntityFetch fetch) {
		processingState.registerNonExists( fetch );
	}

	@Override
	public boolean isQueryCacheHit() {
		return processingState.isQueryCacheHit();
	}

	@Override
	public void finishRowProcessing() {
		processingState.finishRowProcessing();
	}

	@Override
	public Initializer resolveInitializer(NavigablePath path) {
		return processingState.resolveInitializer( path );
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return processingState.getSession();
	}

	@Override
	public QueryOptions getQueryOptions() {
		return processingState.getQueryOptions();
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return processingState.getQueryParameterBindings();
	}

	@Override
	public Callback getCallback() {
		return processingState.getCallback();
	}
}
