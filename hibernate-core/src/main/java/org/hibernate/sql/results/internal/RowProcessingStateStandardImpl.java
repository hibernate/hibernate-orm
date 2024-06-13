/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.InitializerData;
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
public class RowProcessingStateStandardImpl extends BaseExecutionContext implements RowProcessingState {

	private final JdbcValuesSourceProcessingStateStandardImpl resultSetProcessingState;

	private final RowReader<?> rowReader;
	private final JdbcValues jdbcValues;
	private final ExecutionContext executionContext;
	private final boolean needsResolveState;

	private final InitializerData[] initializerData;

	public RowProcessingStateStandardImpl(
			JdbcValuesSourceProcessingStateStandardImpl resultSetProcessingState,
			ExecutionContext executionContext,
			RowReader<?> rowReader,
			JdbcValues jdbcValues) {
		super( resultSetProcessingState.getSession() );
		this.resultSetProcessingState = resultSetProcessingState;
		this.executionContext = executionContext;
		this.rowReader = rowReader;
		this.jdbcValues = jdbcValues;
		this.needsResolveState = !isQueryCacheHit()
				&& getQueryOptions().isResultCachingEnabled() == Boolean.TRUE;
		this.initializerData = new InitializerData[rowReader.getInitializerCount()];
	}

	@Override
	public JdbcValuesSourceProcessingState getJdbcValuesSourceProcessingState() {
		return resultSetProcessingState;
	}

	@Override
	public LockMode determineEffectiveLockMode(String alias) {
		if ( jdbcValues.usesFollowOnLocking() ) {
			// If follow-on locking is used, we must omit the lock options here,
			// because these lock options are only for Initializers.
			// If we wouldn't omit this, the follow-on lock requests would be no-ops,
			// because the EntityEntrys would already have the desired lock mode
			return LockMode.NONE;
		}
		final LockMode effectiveLockMode = resultSetProcessingState.getQueryOptions().getLockOptions()
				.getEffectiveLockMode( alias );
		return effectiveLockMode == LockMode.NONE
				? jdbcValues.getValuesMapping().determineDefaultLockMode( alias, effectiveLockMode )
				: effectiveLockMode;
	}

	@Override
	public boolean needsResolveState() {
		return needsResolveState;
	}

	@Override
	public <T extends InitializerData> T getInitializerData(int initializerId) {
		return (T) initializerData[initializerId];
	}

	@Override
	public void setInitializerData(int initializerId, InitializerData state) {
		initializerData[initializerId] = state;
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
		return jdbcValues.getCurrentRowValue( position );
	}

	@Override
	public void registerNonExists(EntityFetch fetch) {
	}

	@Override
	public boolean isQueryCacheHit() {
		return jdbcValues instanceof JdbcValuesCacheHit;
	}

	@Override
	public void finishRowProcessing(boolean wasAdded) {
		jdbcValues.finishRowProcessing( this, wasAdded );
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
	public boolean isScrollResult(){
		return executionContext.isScrollResult();
	}

	@Override
	public Callback getCallback() {
		return executionContext.getCallback();
	}

	@Override
	public boolean hasCallbackActions() {
		return executionContext.hasCallbackActions();
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
	public String getEntityUniqueKeyAttributePath() {
		return executionContext.getEntityUniqueKeyAttributePath();
	}

	@Override
	public Object getEntityUniqueKey() {
		return executionContext.getEntityUniqueKey();
	}

	@Override
	public EntityMappingType getRootEntityDescriptor() {
		return executionContext.getRootEntityDescriptor();
	}

	@Override
	public void registerLoadingEntityHolder(EntityHolder holder) {
		executionContext.registerLoadingEntityHolder( holder );
	}

	@Override
	public void afterStatement(LogicalConnectionImplementor logicalConnection) {
		executionContext.afterStatement( logicalConnection );
	}

	@Override
	public boolean hasQueryExecutionToBeAddedToStatistics() {
		return executionContext.hasQueryExecutionToBeAddedToStatistics();
	}

	@Override
	public boolean upgradeLocks() {
		return executionContext.upgradeLocks();
	}
}
