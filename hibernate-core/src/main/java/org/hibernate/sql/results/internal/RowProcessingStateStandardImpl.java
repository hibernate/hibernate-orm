/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.List;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesCacheHit;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesSourceProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.sql.results.spi.RowReader;

/**
 * Standard RowProcessingState implementation
 */
public class RowProcessingStateStandardImpl implements RowProcessingState {
	private static final Initializer[] NO_INITIALIZERS = new Initializer[0];

	private final JdbcValuesSourceProcessingStateStandardImpl resultSetProcessingState;

	private final Initializer[] initializers;

	private final RowReader<?> rowReader;
	private final JdbcValues jdbcValues;
	private final ExecutionContext executionContext;
	public final boolean hasCollectionInitializers;

	public RowProcessingStateStandardImpl(
			JdbcValuesSourceProcessingStateStandardImpl resultSetProcessingState,
			ExecutionContext executionContext,
			RowReader<?> rowReader,
			JdbcValues jdbcValues) {
		this.resultSetProcessingState = resultSetProcessingState;
		this.executionContext = executionContext;
		this.rowReader = rowReader;
		this.jdbcValues = jdbcValues;

		final List<Initializer> initializers = rowReader.getInitializers();
		if ( initializers == null || initializers.isEmpty() ) {
			this.initializers = NO_INITIALIZERS;
			hasCollectionInitializers = false;
		}
		else {
			//noinspection ToArrayCallWithZeroLengthArrayArgument
			this.initializers = initializers.toArray( new Initializer[initializers.size()] );
			hasCollectionInitializers = hasCollectionInitializers(this.initializers);
		}
	}

	private static boolean hasCollectionInitializers(Initializer[] initializers) {
		for ( int i = 0; i < initializers.length; i++ ) {
			if ( initializers[i] instanceof CollectionInitializer ) {
				return true;
			}
		}
		return false;
	}

	public boolean hasCollectionInitializers(){
		return this.hasCollectionInitializers;
	}

	@Override
	public JdbcValuesSourceProcessingState getJdbcValuesSourceProcessingState() {
		return resultSetProcessingState;
	}

	@Override
	public RowReader<?> getRowReader() {
		return rowReader;
	}

	public boolean next() {
		return jdbcValues.next( this );
	}

	public boolean previous() {
		return jdbcValues.previous( this );
	}

	public boolean scroll(int i) {
		return jdbcValues.scroll( i, this );
	}

	public boolean position(int i) {
		return jdbcValues.position( i, this );
	}

	public int getPosition() {
		return jdbcValues.getPosition();
	}

	public boolean isBeforeFirst() {
		return jdbcValues.isBeforeFirst( this );
	}

	public void beforeFirst() {
		jdbcValues.beforeFirst( this );
	}

	public boolean isFirst() {
		return jdbcValues.isFirst( this );
	}

	public boolean first() {
		return jdbcValues.first( this );
	}

	public boolean last() {
		return jdbcValues.last( this );
	}

	public boolean isLast() {
		return jdbcValues.isLast( this );
	}

	public void afterLast() {
		jdbcValues.afterLast( this );
	}

	public boolean isAfterLast() {
		return jdbcValues.isAfterLast( this );
	}

	@Override
	public Object getJdbcValue(int position) {
		return jdbcValues.getCurrentRowValuesArray()[ position ];
	}

	@Override
	public void registerNonExists(EntityFetch fetch) {
	}

	@Override
	public boolean isQueryCacheHit() {
		return jdbcValues instanceof JdbcValuesCacheHit;
	}

	@Override
	public void finishRowProcessing() {
	}

	@Override
	public SharedSessionContractImplementor getSession() {
		return getJdbcValuesSourceProcessingState().getExecutionContext().getSession();
	}

	@Override
	public QueryOptions getQueryOptions() {
		return executionContext.getQueryOptions();
	}

	@Override
	public QueryParameterBindings getQueryParameterBindings() {
		return getJdbcValuesSourceProcessingState().getExecutionContext().getQueryParameterBindings();
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
	public Initializer resolveInitializer(NavigablePath path) {
		for ( Initializer initializer : initializers ) {
			if ( initializer.getNavigablePath().equals( path ) ) {
				return initializer;
			}
		}

		return null;
	}
}
