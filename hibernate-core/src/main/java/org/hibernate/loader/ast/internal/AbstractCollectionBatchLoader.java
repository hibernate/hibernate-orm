/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.loader.ast.spi.CollectionBatchLoader;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.sql.results.internal.ResultsHelper;


import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.hasSingleId;
import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.trimIdBatch;
import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER;
import static org.hibernate.pretty.MessageHelper.collectionInfoString;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionBatchLoader implements CollectionBatchLoader {
	private final int domainBatchSize;
	private final PluralAttributeMapping attributeMapping;
	private final LoadQueryInfluencers influencers;
	private final SessionFactoryImplementor sessionFactory;

	private final int keyJdbcCount;

	final CollectionLoaderSingleKey singleKeyLoader;

	public AbstractCollectionBatchLoader(
			int domainBatchSize,
			@Nonnull LoadQueryInfluencers influencers,
			@Nonnull PluralAttributeMapping attributeMapping,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		this.domainBatchSize = domainBatchSize;
		this.attributeMapping = attributeMapping;

		this.keyJdbcCount = attributeMapping.getJdbcTypeCount();
		this.sessionFactory = sessionFactory;
		this.influencers = influencers;

		singleKeyLoader = new CollectionLoaderSingleKey( getLoadable(), getInfluencers(), getSessionFactory() );
	}

	@Override
	public int getDomainBatchSize() {
		return domainBatchSize;
	}

	@Nonnull
	@Override
	public PluralAttributeMapping getLoadable() {
		return attributeMapping;
	}

	@Nonnull
	public LoadQueryInfluencers getInfluencers() {
		return influencers;
	}

	@Nonnull
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	public int getKeyJdbcCount() {
		return keyJdbcCount;
	}

	abstract void initializeKeys(@Nonnull Object key, @Nonnull Object[] keysToInitialize, @Nonnull SharedSessionContractImplementor session);

	@Nonnull
	private CollectionKey collectionKey(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		return session.generateCollectionKey( getLoadable().getCollectionDescriptor(), key );
	}

	@Nonnull
	@Override
	public PersistentCollection<?> load(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.batchFetchingCollection(
					collectionInfoString( getLoadable(), key ) );
		}

		final var keys = resolveKeysToInitialize( key, session );
		if ( hasSingleId( keys ) ) {
			return singleKeyLoader.load( key, session );
		}
		initializeKeys( key, keys, session );
		finishInitializingKeys( keys, session );

		return session.getPersistenceContext().getCollection( collectionKey( key, session ) );
	}

	abstract void finishInitializingKeys(@Nonnull Object[] key, @Nonnull SharedSessionContractImplementor session);

	protected void finishInitializingKey(@Nullable Object key, @Nonnull SharedSessionContractImplementor session) {
		if ( key != null ) {
			if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
				MULTI_KEY_LOAD_LOGGER.finishingInitializingBatchFetchedCollection(
						collectionInfoString( attributeMapping, key ) );
			}

			final var persistenceContext = session.getPersistenceContext();
			final var collection = persistenceContext.getCollection( collectionKey( key, session ) );
			if ( !collection.wasInitialized() ) {
				final var entry = persistenceContext.getCollectionEntry( collection );
				collection.initializeEmptyCollection( entry.getLoadedPersister() );
				ResultsHelper.finalizeCollectionLoading(
						persistenceContext,
						entry.getLoadedPersister(),
						collection,
						key,
						true,
						null
				);
			}
		}
	}

	@Nonnull
	@AllowReflection
	Object[] resolveKeysToInitialize(@Nonnull Object keyBeingLoaded, @Nonnull SharedSessionContractImplementor session) {
		final int length = getDomainBatchSize();
		final Object[] keysToInitialize = new Object[length];
		session.getPersistenceContextInternal().getBatchFetchQueue()
				.collectBatchLoadableCollectionKeys(
						length,
						(index, key) -> keysToInitialize[index] = key,
						keyBeingLoaded,
						getLoadable()
				);
		// now trim down the array to the number of keys we found
		return trimIdBatch( length, keysToInitialize );
	}
}
