/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.Iterator;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.internal.util.NullnessUtil;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.loader.ast.spi.CollectionLoader;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.internal.ResultsHelper;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

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

		final QuerySpec querySpec = sqlAst.getQueryPart().getFirstQuerySpec();
		final TableGroup tableGroup = querySpec.getFromClause().getRoots().get( 0 );
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
		final CollectionKey collectionKey = new CollectionKey( attributeMapping.getCollectionDescriptor(), triggerKey );

		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();
		final PersistenceContext persistenceContext = session.getPersistenceContext();

		// try to find a registered SubselectFetch
		final PersistentCollection<?> collection = persistenceContext.getCollection( collectionKey );
		attributeMapping.getCollectionDescriptor().getCollectionType().getKeyOfOwner( collection.getOwner(), session );

		final BatchFetchQueue batchFetchQueue = persistenceContext.getBatchFetchQueue();
		final EntityEntry ownerEntry = persistenceContext.getEntry( collection.getOwner() );
		List<PersistentCollection<?>> subSelectFetchedCollections = null;
		if ( ownerEntry != null ) {
			final EntityKey triggerKeyOwnerKey = ownerEntry.getEntityKey();
			final SubselectFetch registeredFetch = batchFetchQueue.getSubselect( triggerKeyOwnerKey );
			if ( registeredFetch != null ) {
				subSelectFetchedCollections = CollectionHelper.arrayList(
						registeredFetch.getResultingEntityKeys().size() );

				// there was one, so we want to make sure to prepare the corresponding collection
				// reference for reading
				final Iterator<EntityKey> itr = registeredFetch.getResultingEntityKeys().iterator();
				while ( itr.hasNext() ) {
					final EntityKey key = itr.next();

					final PersistentCollection<?> containedCollection = persistenceContext.getCollection(
							new CollectionKey( attributeMapping.getCollectionDescriptor(), key.getIdentifier() )
					);

					if ( containedCollection != null && containedCollection != collection ) {
						containedCollection.beginRead();
						containedCollection.beforeInitialize( getLoadable().getCollectionDescriptor(), -1 );

						subSelectFetchedCollections.add( containedCollection );
					}
				}
			}
		}

		final JdbcOperationQuerySelect jdbcSelect = sqlAstTranslatorFactory
				.buildSelectTranslator( sessionFactory, sqlAst )
				.translate( this.subselect.getLoadingJdbcParameterBindings(), QueryOptions.NONE );

		final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler = SubselectFetch.createRegistrationHandler(
				batchFetchQueue,
				sqlAst,
				this.subselect.getLoadingJdbcParameters(),
				this.subselect.getLoadingJdbcParameterBindings()
		);

		jdbcServices.getJdbcSelectExecutor().list(
				jdbcSelect,
				this.subselect.getLoadingJdbcParameterBindings(),
				new ExecutionContextWithSubselectFetchHandler( session, subSelectFetchableKeysHandler ),
				RowTransformerStandardImpl.instance(),
				ListResultsConsumer.UniqueSemantic.NONE
		);

		if ( subSelectFetchedCollections != null && ! subSelectFetchedCollections.isEmpty() ) {
			subSelectFetchedCollections.forEach(
					c -> {
						if ( c.wasInitialized() ) {
							return;
						}

						c.initializeEmptyCollection( getLoadable().getCollectionDescriptor() );
						ResultsHelper.finalizeCollectionLoading(
								persistenceContext,
								getLoadable().getCollectionDescriptor(),
								c,
								NullnessUtil.castNonNull( c.getKey() ),
								true
						);
					}
			);

			subSelectFetchedCollections.clear();
		}

		return collection;
	}

}
