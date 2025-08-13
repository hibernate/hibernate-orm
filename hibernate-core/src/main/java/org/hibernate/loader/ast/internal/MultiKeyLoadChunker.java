/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;


import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.Bindable;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ManagedResultConsumer;

/**
 * When the number of ids to initialize exceeds a certain threshold, IN-predicate based
 * {@linkplain org.hibernate.loader.ast.spi.MultiKeyLoader multi-key loaders} will break
 * the initialization into "chunks".
 *
 * @author Steve Ebersole
 */
public class MultiKeyLoadChunker<K> {
	@FunctionalInterface
	interface SqlExecutionContextCreator {
		ExecutionContext createContext(
				JdbcParameterBindings parameterBindings,
				SharedSessionContractImplementor session);
	}

	@FunctionalInterface
	interface KeyCollector<K> {
		void collect(K key, int relativePosition, int absolutePosition);
	}

	@FunctionalInterface
	interface ChunkStartListener {
		void chunkStartNotification(int startIndex);
	}

	@FunctionalInterface
	interface ChunkBoundaryListener {
		void chunkBoundaryNotification(int startIndex, int nonNullElementCount);
	}

	private final int chunkSize;
	private final int keyColumnCount;
	private final Bindable bindable;

	private final JdbcParametersList jdbcParameters;
	private final SelectStatement sqlAst;
	private final JdbcOperationQuerySelect jdbcSelect;

	public MultiKeyLoadChunker(
			int chunkSize,
			int keyColumnCount,
			Bindable bindable,
			JdbcParametersList jdbcParameters,
			SelectStatement sqlAst,
			JdbcOperationQuerySelect jdbcSelect) {
		this.chunkSize = chunkSize;
		this.keyColumnCount = keyColumnCount;
		this.bindable = bindable;
		this.jdbcParameters = jdbcParameters;
		this.sqlAst = sqlAst;
		this.jdbcSelect = jdbcSelect;
	}

	/**
	 * Process the chunks
	 *
	 * @param keys The group of keys to be initialized
	 * @param nonNullElementCount The number of non-null values in {@code keys}, which will be
	 * 		less-than-or-equal-to the number of {@code keys}
	 * @param startListener Notifications that processing a chunk has starting
	 * @param keyCollector Called for each key as it is processed
	 * @param boundaryListener Notifications that processing a chunk has completed
	 */
	public void processChunks(
			K[] keys,
			int nonNullElementCount,
			SqlExecutionContextCreator sqlExecutionContextCreator,
			KeyCollector<K> keyCollector,
			ChunkStartListener startListener,
			ChunkBoundaryListener boundaryListener,
			SharedSessionContractImplementor session) {
		int numberOfKeysLeft = nonNullElementCount;
		int start = 0;
		while ( numberOfKeysLeft > 0 ) {
			processChunk( keys, start, sqlExecutionContextCreator, keyCollector, startListener, boundaryListener, session );

			start += chunkSize;
			numberOfKeysLeft -= chunkSize;
		}
	}

	private void processChunk(
			K[] keys,
			int startIndex,
			SqlExecutionContextCreator sqlExecutionContextCreator,
			KeyCollector<K> keyCollector,
			ChunkStartListener startListener,
			ChunkBoundaryListener boundaryListener,
			SharedSessionContractImplementor session) {
		startListener.chunkStartNotification( startIndex );

		final int parameterCount = chunkSize * keyColumnCount;
		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( parameterCount );

		int nonNullCounter = 0;
		int bindCount = 0;
		for ( int i = 0; i < chunkSize; i++ ) {
			// the position within `K[] keys`
			final int keyPosition = i + startIndex;

			final K value;
			if ( keyPosition >= keys.length ) {
				value = null;
			}
			else {
				value = keys[keyPosition];
			}

			keyCollector.collect( value, i, keyPosition );

			if ( value != null ) {
				nonNullCounter++;
			}

			bindCount += jdbcParameterBindings.registerParametersForEachJdbcValue(
					value,
					bindCount,
					bindable,
					jdbcParameters,
					session
			);
		}
		assert bindCount == jdbcParameters.size();

		if ( nonNullCounter == 0 ) {
			// there are no non-null keys in the chunk
			return;
		}

		session.getFactory().getJdbcServices().getJdbcSelectExecutor().executeQuery(
				jdbcSelect,
				jdbcParameterBindings,
				sqlExecutionContextCreator.createContext( jdbcParameterBindings, session ),
				RowTransformerStandardImpl.instance(),
				null,
				nonNullCounter,
				ManagedResultConsumer.INSTANCE
		);

		boundaryListener.chunkBoundaryNotification( startIndex, nonNullCounter );
	}

}
