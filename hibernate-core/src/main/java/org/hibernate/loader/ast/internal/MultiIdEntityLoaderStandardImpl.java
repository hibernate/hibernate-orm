/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.loader.entity.CacheEntityLoaderHelper;
import org.hibernate.loader.ast.spi.MultiIdEntityLoader;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.MultiLoadOptions;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.RowTransformerPassThruImpl;
import org.hibernate.sql.results.graph.entity.LoadingEntityEntry;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class MultiIdEntityLoaderStandardImpl<T> implements MultiIdEntityLoader<T> {
	private static final Logger log = Logger.getLogger( MultiIdEntityLoaderStandardImpl.class );

	private final EntityPersister entityDescriptor;
	private final SessionFactoryImplementor sessionFactory;

	private int idJdbcTypeCount = -1;

	public MultiIdEntityLoaderStandardImpl(EntityPersister entityDescriptor, SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;
		this.sessionFactory = sessionFactory;
	}

	@Override
	public EntityPersister getLoadable() {
		return entityDescriptor;
	}

	@Override
	public List<T> load(Object[] ids, MultiLoadOptions loadOptions, SharedSessionContractImplementor session) {
		// todo (6.0) : account for all of the `loadOptions`...
		//		for now just do a simple load

		assert ids != null;

		if ( idJdbcTypeCount < 0 ) {
			// can't do this in the ctor because of chicken-egg between this ctor and the persisters
			idJdbcTypeCount = entityDescriptor.getIdentifierMapping().getJdbcTypeCount( sessionFactory.getTypeConfiguration() );
		}

		if ( loadOptions.isOrderReturnEnabled() ) {
			return performOrderedMultiLoad( ids, session, loadOptions );
		}
		else {
			return performUnorderedMultiLoad( ids, session, loadOptions );
		}
	}

	private List<T> performOrderedMultiLoad(
			Object[] ids,
			SharedSessionContractImplementor session,
			MultiLoadOptions loadOptions) {
		log.tracef( "#performOrderedMultiLoad(`%s`, ..)", entityDescriptor.getEntityName() );

		assert loadOptions.isOrderReturnEnabled();

		final JdbcEnvironment jdbcEnvironment = sessionFactory.getJdbcServices().getJdbcEnvironment();
		final Dialect dialect = jdbcEnvironment.getDialect();

		final List result = CollectionHelper.arrayList( ids.length );

		final LockOptions lockOptions = (loadOptions.getLockOptions() == null)
				? new LockOptions( LockMode.NONE )
				: loadOptions.getLockOptions();

		final int maxBatchSize;
		if ( loadOptions.getBatchSize() != null && loadOptions.getBatchSize() > 0 ) {
			maxBatchSize = loadOptions.getBatchSize();
		}
		else {
			maxBatchSize = dialect.getDefaultBatchLoadSizingStrategy().determineOptimalBatchLoadSize(
					idJdbcTypeCount,
					ids.length
			);
		}

		final List<Object> idsInBatch = new ArrayList<>();
		final List<Integer> elementPositionsLoadedByBatch = new ArrayList<>();

		for ( int i = 0; i < ids.length; i++ ) {
			final Object id = ids[i];
			final EntityKey entityKey = new EntityKey( id, entityDescriptor );

			if ( loadOptions.isSessionCheckingEnabled() || loadOptions.isSecondLevelCacheCheckingEnabled() ) {
				LoadEvent loadEvent = new LoadEvent(
						id,
						entityDescriptor.getMappedClass().getName(),
						lockOptions,
						(EventSource) session,
						getReadOnlyFromLoadQueryInfluencers(session)
				);

				Object managedEntity = null;

				if ( loadOptions.isSessionCheckingEnabled() ) {
					// look for it in the Session first
					CacheEntityLoaderHelper.PersistenceContextEntry persistenceContextEntry = CacheEntityLoaderHelper.INSTANCE
							.loadFromSessionCache(
									loadEvent,
									entityKey,
									LoadEventListener.GET
							);
					managedEntity = persistenceContextEntry.getEntity();

					if ( managedEntity != null && !loadOptions.isReturnOfDeletedEntitiesEnabled() && !persistenceContextEntry
							.isManaged() ) {
						// put a null in the result
						//noinspection unchecked
						result.add( i, null );
						continue;
					}
				}

				if ( managedEntity == null && loadOptions.isSecondLevelCacheCheckingEnabled() ) {
					// look for it in the SessionFactory
					managedEntity = CacheEntityLoaderHelper.INSTANCE.loadFromSecondLevelCache(
							loadEvent,
							entityDescriptor,
							entityKey
					);
				}

				if ( managedEntity != null ) {
					//noinspection unchecked
					result.add( i, managedEntity );
					continue;
				}
			}

			// if we did not hit any of the continues above, then we need to batch
			// load the entity state.
			idsInBatch.add( ids[i] );

			if ( idsInBatch.size() >= maxBatchSize ) {
				// we've hit the allotted max-batch-size, perform an "intermediate load"
				loadEntitiesById( idsInBatch, lockOptions, session );
				idsInBatch.clear();
			}

			// Save the EntityKey instance for use later!
			// todo (6.0) : see below wrt why `elementPositionsLoadedByBatch` probably isn't needed
			result.add( i, entityKey );
			elementPositionsLoadedByBatch.add( i );
		}

		if ( !idsInBatch.isEmpty() ) {
			// we still have ids to load from the processing above since the last max-batch-size trigger,
			// perform a load for them
			loadEntitiesById( idsInBatch, lockOptions, session );
		}

		// todo (6.0) : can't we just walk all elements of the results looking for EntityKey and replacing here?
		//		can't imagine
		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		for ( Integer position : elementPositionsLoadedByBatch ) {
			// the element value at this position in the result List should be
			// the EntityKey for that entity; reuse it!
			final EntityKey entityKey = (EntityKey) result.get( position );
			Object entity = persistenceContext.getEntity( entityKey );
			if ( entity != null && !loadOptions.isReturnOfDeletedEntitiesEnabled() ) {
				// make sure it is not DELETED
				final EntityEntry entry = persistenceContext.getEntry( entity );
				if ( entry.getStatus() == Status.DELETED || entry.getStatus() == Status.GONE ) {
					// the entity is locally deleted, and the options ask that we not return such entities...
					entity = null;
				}
			}
			//noinspection unchecked
			result.set( position, entity );
		}

		//noinspection unchecked
		return (List) result;
	}

	private List<T> loadEntitiesById(
			List<Object> idsInBatch,
			LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		assert idsInBatch != null;
		assert ! idsInBatch.isEmpty();

		assert idJdbcTypeCount > 0;

		final int numberOfIdsInBatch = idsInBatch.size();

		log.tracef( "#loadEntitiesById(`%s`, `%s`, ..)", entityDescriptor.getEntityName(), numberOfIdsInBatch );

		final List<JdbcParameter> jdbcParameters = new ArrayList<>( numberOfIdsInBatch * idJdbcTypeCount);

		final SelectStatement sqlAst = LoaderSelectBuilder.createSelect(
				getLoadable(),
				// null here means to select everything
				null,
				getLoadable().getIdentifierMapping(),
				null,
				numberOfIdsInBatch,
				session.getLoadQueryInfluencers(),
				lockOptions,
				jdbcParameters::add,
				sessionFactory
		);

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory ).translate( sqlAst );

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		final Iterator<JdbcParameter> paramItr = jdbcParameters.iterator();

		for ( int i = 0; i < numberOfIdsInBatch; i++ ) {
			final Object id = idsInBatch.get( i );

			entityDescriptor.getIdentifierMapping().visitJdbcValues(
					id,
					Clause.WHERE,
					(value, type) -> {
						assert paramItr.hasNext();
						final JdbcParameter parameter = paramItr.next();
						jdbcParameterBindings.addBinding(
								parameter,
								new JdbcParameterBinding() {
									@Override
									public JdbcMapping getBindType() {
										return type;
									}

									@Override
									public Object getBindValue() {
										return value;
									}
								}
						);
					},
					session
			);
		}

		// we should have used all of the JdbcParameter references (created bindings for all)
		assert !paramItr.hasNext();

		final LoadingEntityCollector loadingEntityCollector;

		if ( entityDescriptor.hasSubselectLoadableCollections() ) {
			loadingEntityCollector = new LoadingEntityCollector(
					entityDescriptor,
					sqlAst.getQuerySpec(),
					jdbcParameters,
					jdbcParameterBindings,
					session.getPersistenceContext().getBatchFetchQueue()
			);
		}
		else {
			loadingEntityCollector = null;
		}

		return JdbcSelectExecutorStandardImpl.INSTANCE.list(
				jdbcSelect,
				jdbcParameterBindings,
				new ExecutionContext() {
					@Override
					public SharedSessionContractImplementor getSession() {
						return session;
					}

					@Override
					public QueryOptions getQueryOptions() {
						return QueryOptions.NONE;
					}

					@Override
					public QueryParameterBindings getQueryParameterBindings() {
						return QueryParameterBindings.NO_PARAM_BINDINGS;
					}

					@Override
					public Callback getCallback() {
						return null;
					}

					@Override
					public void registerLoadingEntityEntry(EntityKey entityKey, LoadingEntityEntry entry) {
						if ( loadingEntityCollector != null ) {
							loadingEntityCollector.collectLoadingEntityKey( entityKey );
						}
					}
				},
				RowTransformerPassThruImpl.instance(),
				true
		);
	}

	private static class LoadingEntityCollector {
		private final SubselectFetch subselectFetch;
		private final BatchFetchQueue batchFetchQueue;

		LoadingEntityCollector(
				EntityValuedModelPart loadingEntityDescriptor,
				QuerySpec loadingSqlAst,
				List<JdbcParameter> jdbcParameters,
				JdbcParameterBindings jdbcParameterBindings,
				BatchFetchQueue batchFetchQueue) {
			this.batchFetchQueue = batchFetchQueue;
			this.subselectFetch = new SubselectFetch(
					loadingEntityDescriptor,
					loadingSqlAst,
					loadingSqlAst.getFromClause().getRoots().get( 0 ),
					jdbcParameters,
					jdbcParameterBindings,
					new HashSet<>()
			);

		}

		void collectLoadingEntityKey(EntityKey entityKey) {
			subselectFetch.getResultingEntityKeys().add( entityKey );
			batchFetchQueue.addSubselect( entityKey, subselectFetch );
		}
	}

	private List<T> performUnorderedMultiLoad(
			Object[] ids,
			SharedSessionContractImplementor session,
			MultiLoadOptions loadOptions) {
		assert !loadOptions.isOrderReturnEnabled();
		assert ids != null;

		log.tracef( "#performUnorderedMultiLoad(`%s`, ..)", entityDescriptor.getEntityName() );

		final List<T> result = CollectionHelper.arrayList( ids.length );

		final LockOptions lockOptions = (loadOptions.getLockOptions() == null)
				? new LockOptions( LockMode.NONE )
				: loadOptions.getLockOptions();

		if ( loadOptions.isSessionCheckingEnabled() || loadOptions.isSecondLevelCacheCheckingEnabled() ) {
			// the user requested that we exclude ids corresponding to already managed
			// entities from the generated load SQL.  So here we will iterate all
			// incoming id values and see whether it corresponds to an existing
			// entity associated with the PC - if it does we add it to the result
			// list immediately and remove its id from the group of ids to load.
			boolean foundAnyManagedEntities = false;
			final List<Object> nonManagedIds = new ArrayList<>();

			for ( int i = 0; i < ids.length; i++ ) {
				final Object id = ids[ i ];
				final EntityKey entityKey = new EntityKey( id, entityDescriptor );

				LoadEvent loadEvent = new LoadEvent(
						id,
						entityDescriptor.getMappedClass().getName(),
						lockOptions,
						(EventSource) session,
						getReadOnlyFromLoadQueryInfluencers( session )
				);

				Object managedEntity = null;

				// look for it in the Session first
				CacheEntityLoaderHelper.PersistenceContextEntry persistenceContextEntry = CacheEntityLoaderHelper.INSTANCE
						.loadFromSessionCache(
								loadEvent,
								entityKey,
								LoadEventListener.GET
						);
				if ( loadOptions.isSessionCheckingEnabled() ) {
					managedEntity = persistenceContextEntry.getEntity();

					if ( managedEntity != null && !loadOptions.isReturnOfDeletedEntitiesEnabled() && !persistenceContextEntry
							.isManaged() ) {
						foundAnyManagedEntities = true;
						result.add( null );
						continue;
					}
				}

				if ( managedEntity == null && loadOptions.isSecondLevelCacheCheckingEnabled() ) {
					managedEntity = CacheEntityLoaderHelper.INSTANCE.loadFromSecondLevelCache(
							loadEvent,
							entityDescriptor,
							entityKey
					);
				}

				if ( managedEntity != null ) {
					foundAnyManagedEntities = true;
					//noinspection unchecked
					result.add( (T) managedEntity );
				}
				else {
					nonManagedIds.add( id );
				}
			}

			if ( foundAnyManagedEntities ) {
				if ( nonManagedIds.isEmpty() ) {
					// all of the given ids were already associated with the Session
					return result;
				}
				else {
					// over-write the ids to be loaded with the collection of
					// just non-managed ones
					ids = nonManagedIds.toArray(
							(Object[]) Array.newInstance(
									ids.getClass().getComponentType(),
									nonManagedIds.size()
							)
					);
				}
			}
		}

		int numberOfIdsLeft = ids.length;
		final int maxBatchSize;
		if ( loadOptions.getBatchSize() != null && loadOptions.getBatchSize() > 0 ) {
			maxBatchSize = loadOptions.getBatchSize();
		}
		else {
			maxBatchSize = session.getJdbcServices().getJdbcEnvironment().getDialect().getDefaultBatchLoadSizingStrategy().determineOptimalBatchLoadSize(
					entityDescriptor.getIdentifierType().getColumnSpan( session.getFactory() ),
					numberOfIdsLeft
			);
		}

		int idPosition = 0;
		while ( numberOfIdsLeft > 0 ) {
			final int batchSize =  Math.min( numberOfIdsLeft, maxBatchSize );

			final Object[] idsInBatch = new Object[ batchSize ];
			System.arraycopy( ids, idPosition, idsInBatch, 0, batchSize );

			result.addAll(
					loadEntitiesById( Arrays.asList( idsInBatch ), lockOptions, session )
			);

			numberOfIdsLeft = numberOfIdsLeft - batchSize;
			idPosition += batchSize;
		}

		return result;
	}

	private Boolean getReadOnlyFromLoadQueryInfluencers(SharedSessionContractImplementor session) {
		Boolean readOnly = null;
		final LoadQueryInfluencers loadQueryInfluencers = session.getLoadQueryInfluencers();
		if ( loadQueryInfluencers != null ) {
			readOnly = loadQueryInfluencers.getReadOnly();
		}
		return readOnly;
	}
}
