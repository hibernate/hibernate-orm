/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.loader.ast.spi.CollectionBatchLoader;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.IdClassEmbeddable;
import org.hibernate.sql.results.internal.ResultsHelper;

import java.lang.reflect.Array;

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
			LoadQueryInfluencers influencers,
			PluralAttributeMapping attributeMapping,
			SessionFactoryImplementor sessionFactory) {
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

	@Override
	public PluralAttributeMapping getLoadable() {
		return attributeMapping;
	}

	public LoadQueryInfluencers getInfluencers() {
		return influencers;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	public int getKeyJdbcCount() {
		return keyJdbcCount;
	}

	abstract void initializeKeys(Object key, Object[] keysToInitialize, SharedSessionContractImplementor session);

	@Override
	public PersistentCollection<?> load(Object key, SharedSessionContractImplementor session) {
		if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.trace( "Batch fetching collection: "
					+ collectionInfoString( getLoadable(), key ) );
		}

		final Object[] keys = resolveKeysToInitialize( key, session );

		if ( hasSingleId( keys ) ) {
			return singleKeyLoader.load( key, session );
		}

		initializeKeys( key, keys, session );

		finishInitializingKeys( keys, session );

		final CollectionKey collectionKey = new CollectionKey( getLoadable().getCollectionDescriptor(), key );
		return session.getPersistenceContext().getCollection( collectionKey );
	}

	abstract void finishInitializingKeys(Object[] key, SharedSessionContractImplementor session);

	protected void finishInitializingKey(Object key, SharedSessionContractImplementor session) {
		if ( key == null ) {
			return;
		}

		if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.trace( "Finishing initializing batch-fetched collection: "
					+ collectionInfoString( attributeMapping, key ) );
		}

		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final CollectionKey collectionKey = new CollectionKey( getLoadable().getCollectionDescriptor(), key );
		final PersistentCollection<?> collection = persistenceContext.getCollection( collectionKey );
		if ( !collection.wasInitialized() ) {
			final CollectionEntry entry = persistenceContext.getCollectionEntry( collection );
			collection.initializeEmptyCollection( entry.getLoadedPersister() );
			ResultsHelper.finalizeCollectionLoading(
					persistenceContext,
					entry.getLoadedPersister(),
					collection,
					key,
					true
			);
		}

	}

	@AllowReflection
	Object[] resolveKeysToInitialize(Object keyBeingLoaded, SharedSessionContractImplementor session) {
		final int length = getDomainBatchSize();
		final Object[] keysToInitialize = (Object[]) Array.newInstance(
				getKeyType( getLoadable().getKeyDescriptor().getKeyPart() ),
				length
		);
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

	protected Class<?> getKeyType(ValuedModelPart keyPart) {
		if ( keyPart instanceof NonAggregatedIdentifierMapping nonAggregatedIdentifierMapping ) {
			final IdClassEmbeddable idClassEmbeddable = nonAggregatedIdentifierMapping.getIdClassEmbeddable();
			if ( idClassEmbeddable != null ) {
				return idClassEmbeddable.getMappedJavaType().getJavaTypeClass();
			}
		}
		return keyPart.getJavaType().getJavaTypeClass();
	}


}
