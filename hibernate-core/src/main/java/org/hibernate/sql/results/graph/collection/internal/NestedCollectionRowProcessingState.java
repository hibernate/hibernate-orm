package org.hibernate.sql.results.graph.collection.internal;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Nullable;
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

/**
 * A {@link RowProcessingState} serving the values of one row of a
 * {@linkplain NestedCollectionFetch nested collection} to the initializers of the
 * collection element, and delegating everything else to the row of the owner.
 * <p>
 * The element initializers are not known to the {@link RowReader}, so their
 * data is kept here.
 *
 * @see NestedCollectionAssemblerCreationState
 * @see org.hibernate.sql.results.graph.embeddable.internal.NestedRowProcessingState
 */
public class NestedCollectionRowProcessingState extends BaseExecutionContext implements RowProcessingState {
	private final RowProcessingState processingState;
	private final Map<Integer, InitializerData> initializerData = new HashMap<>();
	private @Nullable Object[] currentRow;

	public NestedCollectionRowProcessingState(RowProcessingState processingState) {
		super( processingState.getSession() );
		this.processingState = processingState;
	}

	public void setCurrentRow(@Nullable Object[] currentRow) {
		this.currentRow = currentRow;
	}

	@Override
	public @Nullable Object getJdbcValue(int position) {
		return currentRow == null ? null : currentRow[position];
	}

	@Override
	public <T extends InitializerData> T getInitializerData(int initializerId) {
		//noinspection unchecked
		return NestedCollectionAssemblerCreationState.isElementInitializerId( initializerId )
				? (T) initializerData.get( initializerId )
				: processingState.getInitializerData( initializerId );
	}

	@Override
	public void setInitializerData(int initializerId, InitializerData state) {
		if ( NestedCollectionAssemblerCreationState.isElementInitializerId( initializerId ) ) {
			initializerData.put( initializerId, state );
		}
		else {
			processingState.setInitializerData( initializerId, state );
		}
	}

	@Override
	public RowProcessingState unwrap() {
		return processingState;
	}

	// -- delegate the rest

	@Override
	public boolean next() {
		return processingState.next();
	}

	@Override
	public boolean previous() {
		return processingState.previous();
	}

	@Override
	public boolean scroll(int numberOfRows) {
		return processingState.scroll( numberOfRows );
	}

	@Override
	public boolean position(int position) {
		return processingState.position( position );
	}

	@Override
	public int getPosition() {
		return processingState.getPosition();
	}

	@Override
	public void beforeFirst() {
		processingState.beforeFirst();
	}

	@Override
	public boolean first() {
		return processingState.first();
	}

	@Override
	public boolean last() {
		return processingState.last();
	}

	@Override
	public void afterLast() {
		processingState.afterLast();
	}

	@Override
	public boolean isFirst() {
		return processingState.isFirst();
	}

	@Override
	public boolean isLast() {
		return processingState.isLast();
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
