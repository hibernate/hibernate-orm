/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PreLoadEvent;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.spi.LoadingEntityEntry;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class JdbcValuesSourceProcessingStateStandardImpl implements JdbcValuesSourceProcessingState {
	private static final Logger log = Logger.getLogger( JdbcValuesSourceProcessingStateStandardImpl.class );

	private final ExecutionContext executionContext;
	private final JdbcValuesSourceProcessingOptions processingOptions;

	private Map<EntityKey,LoadingEntityEntry> loadingEntityMap;
	private Map<Object,EntityKey> hydratedEntityKeys;

	private final PreLoadEvent preLoadEvent;
	private final PostLoadEvent postLoadEvent;

	// todo (6.0) : "loading collections" as well?

	public JdbcValuesSourceProcessingStateStandardImpl(
			ExecutionContext executionContext,
			JdbcValuesSourceProcessingOptions processingOptions) {
		this.executionContext = executionContext;
		this.processingOptions = processingOptions;

		preLoadEvent  = new PreLoadEvent( (EventSource) executionContext.getSession() );
		postLoadEvent  = new PostLoadEvent( (EventSource) executionContext.getSession() );
	}

	@Override
	public QueryOptions getQueryOptions() {
		return executionContext.getQueryOptions();
	}

	@Override
	public JdbcValuesSourceProcessingOptions getProcessingOptions() {
		return processingOptions;
	}

	@Override
	public PreLoadEvent getPreLoadEvent() {
		return preLoadEvent;
	}

	@Override
	public PostLoadEvent getPostLoadEvent() {
		return postLoadEvent;
	}

	@Override
	public LoadingEntityEntry registerLoadingEntity(
			EntityKey entityKey,
			Function<EntityKey,LoadingEntityEntry> entryProducer) {
		if ( loadingEntityMap == null ) {
			loadingEntityMap = new HashMap<>();
		}

		final LoadingEntityEntry loadingEntity = loadingEntityMap.compute(
				entityKey,
				(key, existingValue) -> {
					if ( existingValue == null ) {
						log.debugf(
								"Generating LoadingEntity registration : %s[id=%s]",
								entityKey.getEntityName(),
								entityKey.getIdentifier()
						);
						return entryProducer.apply( key );
					}
					else {
						log.debugf(
								"Attempt to add duplicate LoadingEntity registration for same EntityKey [%s]",
								entityKey
						);
						return existingValue;
					}
				}
		);

		if ( hydratedEntityKeys == null ) {
			hydratedEntityKeys = new HashMap<>();
		}

		hydratedEntityKeys.put( loadingEntity.getEntityInstance(), entityKey );

		return loadingEntity;
	}

	@Override
	public SharedSessionContractImplementor getPersistenceContext() {
		return executionContext.getSession();
	}

	@Override
	public void finishUp() {
		// for arrays, we should end the collection load beforeQuery resolving the entities, since the
		// actual array instances are not instantiated during loading
		finishLoadingArrays();

		// now finish loading the entities (2-phase load)
		performTwoPhaseLoad();

		// now we can finalize loading collections
		finishLoadingCollections();
	}

	private void finishLoadingArrays() {
//		for ( CollectionReferenceInitializer arrayReferenceInitializer : arrayReferenceInitializers ) {
//			arrayReferenceInitializer.endLoading( context );
//		}
	}


	private void performTwoPhaseLoad() {
		if ( loadingEntityMap == null ) {
			return;
		}

		log.tracev( "Total objects hydrated: {0}", loadingEntityMap.size() );
	}

	@SuppressWarnings("SimplifiableIfStatement")
	private boolean isReadOnly() {
		if ( getQueryOptions().isReadOnly() != null ) {
			return getQueryOptions().isReadOnly();
		}

		if ( executionContext.getSession() instanceof EventSource ) {
			return executionContext.getSession().isDefaultReadOnly();
		}

		return false;
	}


	private void finishLoadingCollections() {
//		for ( InitializerCollection initializer : initializers ) {
//			initializer.endLoading( context );
//		}

		// todo (6.0) : need something like org.hibernate.engine.loading.internal.LoadingCollectionEntry

//		throw new NotYetImplementedFor6Exception(  );
	}

}
