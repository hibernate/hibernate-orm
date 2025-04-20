/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.embeddable.internal;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.sql.results.spi.RowReader;

public class NestedRowProcessingState extends BaseExecutionContext implements RowProcessingState {
	private final AggregateEmbeddableInitializerImpl aggregateEmbeddableInitializer;
	final RowProcessingState processingState;

	public NestedRowProcessingState(
			AggregateEmbeddableInitializerImpl aggregateEmbeddableInitializer,
			RowProcessingState processingState) {
		super( processingState.getSession() );
		this.aggregateEmbeddableInitializer = aggregateEmbeddableInitializer;
		this.processingState = processingState;
	}

	public static NestedRowProcessingState wrap(
			AggregateEmbeddableInitializerImpl aggregateEmbeddableInitializer,
			RowProcessingState processingState) {
		if ( processingState instanceof NestedRowProcessingState nestedRowProcessingState ) {
			return new NestedRowProcessingState(
					aggregateEmbeddableInitializer,
					nestedRowProcessingState.processingState
			);
		}
		return new NestedRowProcessingState( aggregateEmbeddableInitializer, processingState );
	}

	@Override
	public Object getJdbcValue(int position) {
		final Object[] jdbcValue = aggregateEmbeddableInitializer.getJdbcValues( processingState );
		return jdbcValue == null ? null : jdbcValue[position];
	}

	@Override
	public RowProcessingState unwrap() {
		return processingState;
	}

	// -- delegate the rest

	@Override
	public <T extends InitializerData> T getInitializerData(int initializerId) {
		return processingState.getInitializerData( initializerId );
	}

	@Override
	public void setInitializerData(int initializerId, InitializerData state) {
		processingState.setInitializerData( initializerId, state );
	}

	@Override
	public JdbcValuesSourceProcessingState getJdbcValuesSourceProcessingState() {
		return processingState.getJdbcValuesSourceProcessingState();
	}

	@Override
	public LockMode determineEffectiveLockMode(String alias) {
		return processingState.determineEffectiveLockMode( alias );
	}

	@Override
	public boolean needsResolveState() {
		return processingState.needsResolveState();
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
	public void finishRowProcessing(boolean wasAdded) {
		processingState.finishRowProcessing( wasAdded );
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
	public boolean isScrollResult(){
		return processingState.isScrollResult();
	}

	@Override
	public Callback getCallback() {
		return processingState.getCallback();
	}

	@Override
	public boolean hasCallbackActions() {
		return processingState.hasCallbackActions();
	}

	@Override
	public CollectionKey getCollectionKey() {
		return processingState.getCollectionKey();
	}

	@Override
	public Object getEntityInstance() {
		return processingState.getEntityInstance();
	}

	@Override
	public Object getEntityId() {
		return processingState.getEntityId();
	}

	@Override
	public String getEntityUniqueKeyAttributePath() {
		return processingState.getEntityUniqueKeyAttributePath();
	}

	@Override
	public Object getEntityUniqueKey() {
		return processingState.getEntityUniqueKey();
	}

	@Override
	public EntityMappingType getRootEntityDescriptor() {
		return processingState.getRootEntityDescriptor();
	}

	@Override
	public void registerLoadingEntityHolder(EntityHolder holder) {
		processingState.registerLoadingEntityHolder( holder );
	}

	@Override
	public void afterStatement(LogicalConnectionImplementor logicalConnection) {
		processingState.afterStatement( logicalConnection );
	}

	@Override
	public boolean hasQueryExecutionToBeAddedToStatistics() {
		return processingState.hasQueryExecutionToBeAddedToStatistics();
	}
}
