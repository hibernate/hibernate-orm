/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.internal.util.NullnessUtil;
import org.hibernate.loader.ast.spi.CollectionLoader;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.internal.ResultsHelper;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * A one-time use {@link CollectionLoader} for applying a subselect fetch.
 *
 * @author Steve Ebersole
 */
public class CollectionLoaderSubSelectFetch implements CollectionLoader {
	private final PluralAttributeMapping attributeMapping;
	private final SubselectFetch subselect;

	private final SelectStatement sqlAst;

	public CollectionLoaderSubSelectFetch(
			PluralAttributeMapping attributeMapping,
			DomainResult<?> cachedDomainResult,
			SubselectFetch subselect,
			SharedSessionContractImplementor session) {
		this.attributeMapping = attributeMapping;
		this.subselect = subselect;

		sqlAst = LoaderSelectBuilder.createSubSelectFetchSelect(
				attributeMapping,
				subselect,
				cachedDomainResult,
				session.getLoadQueryInfluencers(),
				new LockOptions(),
				jdbcParameter -> {},
				session.getFactory()
		);

		final var querySpec = sqlAst.getQueryPart().getFirstQuerySpec();
		final var tableGroup = querySpec.getFromClause().getRoots().get( 0 );
		attributeMapping.applySoftDeleteRestrictions( tableGroup, querySpec::applyPredicate );
	}

	@Override
	public PluralAttributeMapping getLoadable() {
		return attributeMapping;
	}

	protected SelectStatement getSqlAst() {
		return sqlAst;
	}

	@Override
	public PersistentCollection<?> load(Object triggerKey, SharedSessionContractImplementor session) {
		final var collectionKey = new CollectionKey( attributeMapping.getCollectionDescriptor(), triggerKey );

		final var sessionFactory = session.getFactory();
		final var jdbcServices = sessionFactory.getJdbcServices();
		final var persistenceContext = session.getPersistenceContext();

		// try to find a registered SubselectFetch
		final var collection = persistenceContext.getCollection( collectionKey );
		attributeMapping.getCollectionDescriptor().getCollectionType().getKeyOfOwner( collection.getOwner(), session );

		final var batchFetchQueue = persistenceContext.getBatchFetchQueue();
		final var ownerEntry = persistenceContext.getEntry( collection.getOwner() );
		List<PersistentCollection<?>> subSelectFetchedCollections = null;
		if ( ownerEntry != null ) {
			final var triggerKeyOwnerKey = ownerEntry.getEntityKey();
			final var registeredFetch = batchFetchQueue.getSubselect( triggerKeyOwnerKey );
			if ( registeredFetch != null ) {
				subSelectFetchedCollections = arrayList( registeredFetch.getResultingEntityKeys().size() );
				// there was one, so we want to make sure to prepare the corresponding collection
				// reference for reading
				for ( var key : registeredFetch.getResultingEntityKeys() ) {
					final var containedCollection = persistenceContext.getCollection( collectionKey( key ) );
					if ( containedCollection != null && containedCollection != collection ) {
						containedCollection.beginRead();
						containedCollection.beforeInitialize( getLoadable().getCollectionDescriptor(), -1 );
						subSelectFetchedCollections.add( containedCollection );
					}
				}
			}
		}

		final var jdbcSelect =
				jdbcServices.getJdbcEnvironment()
						.getSqlAstTranslatorFactory().buildSelectTranslator( sessionFactory, sqlAst )
						.translate( subselect.getLoadingJdbcParameterBindings(), QueryOptions.NONE );

		final var subSelectFetchableKeysHandler = SubselectFetch.createRegistrationHandler(
				batchFetchQueue,
				sqlAst,
				subselect.getLoadingJdbcParameters(),
				subselect.getLoadingJdbcParameterBindings()
		);

		jdbcServices.getJdbcSelectExecutor().list(
				jdbcSelect,
				subselect.getLoadingJdbcParameterBindings(),
				new ExecutionContextWithSubselectFetchHandler( session, subSelectFetchableKeysHandler ),
				RowTransformerStandardImpl.instance(),
				ListResultsConsumer.UniqueSemantic.NONE
		);

		if ( subSelectFetchedCollections != null && ! subSelectFetchedCollections.isEmpty() ) {
			subSelectFetchedCollections.forEach(
					c -> {
						if ( !c.wasInitialized() ) {
							final var persister = getLoadable().getCollectionDescriptor();
							c.initializeEmptyCollection( persister );
							ResultsHelper.finalizeCollectionLoading(
									persistenceContext,
									persister,
									c,
									NullnessUtil.castNonNull( c.getKey() ),
									true
							);
						}
					}
			);
			subSelectFetchedCollections.clear();
		}

		return collection;
	}

	private CollectionKey collectionKey(EntityKey key) {
		return new CollectionKey( attributeMapping.getCollectionDescriptor(), key.getIdentifier() );
	}

}
