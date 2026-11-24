package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;

import java.util.List;
import java.util.Objects;

import org.hibernate.LockOptions;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.internal.util.NullnessUtil;
import org.hibernate.loader.ast.spi.CollectionLoader;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
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
			@Nonnull PluralAttributeMapping attributeMapping,
			@Nullable DomainResult<?> cachedDomainResult,
			@Nonnull SubselectFetch subselect,
			@Nonnull SharedSessionContractImplementor session) {
		this.attributeMapping = attributeMapping;
		this.subselect = subselect;

		final var sqlAliasBaseGenerator = new SqlAliasBaseManager();

		sqlAst = LoaderSelectBuilder.createSubSelectFetchSelect(
				attributeMapping,
				subselect,
				cachedDomainResult,
				session.getLoadQueryInfluencers(),
				new LockOptions(),
				jdbcParameter -> {},
				sqlAliasBaseGenerator,
				session.getFactory()
		);

	}

	@Nonnull
	@Override
	public PluralAttributeMapping getLoadable() {
		return attributeMapping;
	}

	@Nonnull
	protected SelectStatement getSqlAst() {
		return sqlAst;
	}

	@Nonnull
	@Override
	public PersistentCollection<?> load(@Nonnull Object triggerKey, @Nonnull SharedSessionContractImplementor session) {
		final var collectionKey = session.generateCollectionKey( attributeMapping.getCollectionDescriptor(), triggerKey );

		final var sessionFactory = session.getFactory();
		final var jdbcServices = sessionFactory.getJdbcServices();
		final var persistenceContext = session.getPersistenceContext();

		final var collection = persistenceContext.getCollection( collectionKey );
		final var batchFetchQueue = persistenceContext.getBatchFetchQueue();
		final var subSelectFetchedCollections = prepareCollections( collection, collectionKey.getChangesetId(), persistenceContext, session );

		final var jdbcSelect =
				jdbcServices.getJdbcEnvironment()
						.getSqlAstTranslatorFactory().buildTranslator( new SqlAstTranslationRequest.Select( sessionFactory, sqlAst ) )
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

		for ( var fetched : subSelectFetchedCollections ) {
			if ( !fetched.wasInitialized() ) {
				final var persister = attributeMapping.getCollectionDescriptor();
				fetched.initializeEmptyCollection( persister );
				ResultsHelper.finalizeCollectionLoading(
						persistenceContext, persister, fetched, NullnessUtil.castNonNull( fetched.getKey() ), true, null );
			}
		}

		return collection;
	}

	private List<PersistentCollection<?>> prepareCollections(
			PersistentCollection<?> trigger,
			@Nullable Object changesetId,
			PersistenceContext persistenceContext,
			SharedSessionContractImplementor session) {
		final var persister = attributeMapping.getCollectionDescriptor();
		final var ownerKeys = subselect.getResultingEntityKeys();
		final List<PersistentCollection<?>> collections = arrayList( ownerKeys.size() );
		if ( persister.getCollectionType().useLHSPrimaryKey() ) {
			for ( var ownerKey : ownerKeys ) {
				final var key = session.generateCollectionKey( persister, ownerKey.getIdentifier() );
				prepareCollection( persistenceContext.getCollection( key ), trigger, collections );
			}
		}
		else {
			// The owner identifier is not the collection key. Use the registered wrappers
			// so that preparing an empty collection does not fetch the owner's lazy key.
			persistenceContext.getCollectionsByKey().forEach( (key, collection) -> {
				if ( key.getRole().equals( persister.getRole() ) && Objects.equals( changesetId, key.getChangesetId() ) ) {
					final var entry = persistenceContext.getEntry( collection.getOwner() );
					if ( entry != null && ownerKeys.contains( entry.getEntityKey() ) ) {
						prepareCollection( collection, trigger, collections );
					}
				}
			} );
		}
		return collections;
	}

	private void prepareCollection(
			@Nullable PersistentCollection<?> collection,
			PersistentCollection<?> trigger,
			List<PersistentCollection<?>> collections) {
		if ( collection != null && collection != trigger && !collection.wasInitialized() ) {
			collection.beginRead();
			collection.beforeInitialize( attributeMapping.getCollectionDescriptor(), -1 );
			collections.add( collection );
		}
	}
}
