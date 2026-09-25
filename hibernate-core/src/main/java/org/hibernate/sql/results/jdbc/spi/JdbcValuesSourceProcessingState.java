package org.hibernate.sql.results.jdbc.spi;

import java.util.List;

import jakarta.annotation.Nullable;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.loader.ast.internal.ToOneVisibilityLoader;
import org.hibernate.sql.results.spi.LoadContexts;
import org.hibernate.sql.results.graph.collection.LoadingCollectionEntry;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.LoadedValuesCollector;

/**
 * Provides a context for processing the processing of the complete
 * set of rows from a JdbcValuesSource.  Holds in-flight state
 * and provides access to environmental information needed to perform the
 * processing.
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface JdbcValuesSourceProcessingState {
	ExecutionContext getExecutionContext();

	SharedSessionContractImplementor getSession();

	/**
	 * Execution-local visibility plans, or {@code null} when the implementation does not cache them.
	 * The cache lives for the result set, including across calls to {@link #finishUp()} while scrolling.
	 */
	default @Nullable ToOneVisibilityLoader.Cache getToOneVisibilityPlanCache() {
		return null;
	}

	default QueryOptions getQueryOptions() {
		return getExecutionContext().getQueryOptions();
	}

	JdbcValuesSourceProcessingOptions getProcessingOptions();

	default LoadedValuesCollector getLoadedValuesCollector() {
		return null;
	}

	PreLoadEvent getPreLoadEvent();
	PostLoadEvent getPostLoadEvent();

	void registerLoadingEntityHolder(EntityHolder holder);

	List<EntityHolder> getLoadingEntityHolders();

	void registerReloadedEntityHolder(EntityHolder holder);

	List<EntityHolder> getReloadedEntityHolders();

	/**
	 * Find a LoadingCollectionEntry locally to this context.
	 *
	 * @see LoadContexts#findLoadingCollectionEntry(CollectionKey)
	 */
	LoadingCollectionEntry findLoadingCollectionLocally(CollectionKey key);

	/**
	 * Registers a LoadingCollectionEntry locally to this context
	 */
	void registerLoadingCollection(
			CollectionKey collectionKey,
			LoadingCollectionEntry loadingCollectionEntry);

	void registerSubselects();

	void finishUp();
}
