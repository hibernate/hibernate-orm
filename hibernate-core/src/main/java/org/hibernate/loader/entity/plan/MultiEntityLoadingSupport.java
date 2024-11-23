/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.entity.plan;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.loader.entity.CacheEntityLoaderHelper;
import org.hibernate.persister.entity.MultiLoadOptions;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class MultiEntityLoadingSupport {
	public static List<?> multiLoad(
			OuterJoinLoadable persister,
			Serializable[] ids,
			SharedSessionContractImplementor session,
			MultiLoadOptions loadOptions) {
		if ( loadOptions.isOrderReturnEnabled() ) {
			return performOrderedMultiLoad( persister, ids, session, loadOptions );
		}
		else {
			return performUnorderedMultiLoad( persister, ids, session, loadOptions );
		}
	}

	@SuppressWarnings("unchecked")
	private static List performOrderedMultiLoad(
			OuterJoinLoadable persister,
			Serializable[] ids,
			SharedSessionContractImplementor session,
			MultiLoadOptions loadOptions) {
		assert loadOptions.isOrderReturnEnabled();

		final List result = CollectionHelper.arrayList( ids.length );

		final LockOptions lockOptions = (loadOptions.getLockOptions() == null)
				? new LockOptions( LockMode.NONE )
				: loadOptions.getLockOptions();

		final int maxBatchSize;
		if ( loadOptions.getBatchSize() != null && loadOptions.getBatchSize() > 0 ) {
			maxBatchSize = loadOptions.getBatchSize();
		}
		else {
			maxBatchSize = session.getJdbcServices().getJdbcEnvironment().getDialect().getDefaultBatchLoadSizingStrategy().determineOptimalBatchLoadSize(
					persister.getIdentifierType().getColumnSpan( session.getFactory() ),
					ids.length
			);
		}

		final List<Serializable> idsInBatch = new ArrayList<>();
		final List<Integer> elementPositionsLoadedByBatch = new ArrayList<>();

		for ( int i = 0; i < ids.length; i++ ) {
			final Serializable id = ids[i];
			final EntityKey entityKey = new EntityKey( id, persister );

			if ( loadOptions.isSessionCheckingEnabled() || loadOptions.isSecondLevelCacheCheckingEnabled() ) {
				LoadEvent loadEvent = new LoadEvent(
						id,
						persister.getMappedClass().getName(),
						lockOptions,
						(EventSource) session,
						null
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
						result.add( i, null );
						continue;
					}
				}

				if ( managedEntity == null && loadOptions.isSecondLevelCacheCheckingEnabled() ) {
					// look for it in the SessionFactory
					managedEntity = CacheEntityLoaderHelper.INSTANCE.loadFromSecondLevelCache(
							loadEvent,
							persister,
							entityKey
					);
				}

				if ( managedEntity != null ) {
					result.add( i, managedEntity );
					continue;
				}
			}

			// if we did not hit any of the continues above, then we need to batch
			// load the entity state.
			idsInBatch.add( ids[i] );

			if ( idsInBatch.size() >= maxBatchSize ) {
				performOrderedBatchLoad( idsInBatch, lockOptions, persister, session );
			}

			// Save the EntityKey instance for use later!
			result.add( i, entityKey );
			elementPositionsLoadedByBatch.add( i );
		}

		if ( !idsInBatch.isEmpty() ) {
			performOrderedBatchLoad( idsInBatch, lockOptions, persister, session );
		}

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
			result.set( position, entity );
		}

		return result;
	}

	private static void performOrderedBatchLoad(
			List<Serializable> idsInBatch,
			LockOptions lockOptions,
			OuterJoinLoadable persister,
			SharedSessionContractImplementor session) {
		final EntityLoader entityLoader = EntityLoader.forEntity( persister )
				.withInfluencers( session.getLoadQueryInfluencers() )
				.withLockOptions( lockOptions )
				.withBatchSize( idsInBatch.size() ).byPrimaryKey();

		entityLoader.loadEntityBatch(
				idsInBatch.toArray( new Serializable[0] ),
				persister,
				lockOptions,
				session
		);

		idsInBatch.clear();
	}

	@SuppressWarnings("unchecked")
	protected static List performUnorderedMultiLoad(
			OuterJoinLoadable persister,
			Serializable[] ids,
			SharedSessionContractImplementor session,
			MultiLoadOptions loadOptions) {
		assert !loadOptions.isOrderReturnEnabled();

		final List result = CollectionHelper.arrayList( ids.length );

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
			final List<Serializable> nonManagedIds = new ArrayList<Serializable>();
			for ( Serializable id : ids ) {
				final EntityKey entityKey = new EntityKey( id, persister );

				LoadEvent loadEvent = new LoadEvent(
						id,
						persister.getMappedClass().getName(),
						lockOptions,
						(EventSource) session,
						null
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
							persister,
							entityKey
					);
				}

				if ( managedEntity != null ) {
					foundAnyManagedEntities = true;
					result.add( managedEntity );
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
							(Serializable[]) Array.newInstance(
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
					persister.getIdentifierType().getColumnSpan( session.getFactory() ),
					numberOfIdsLeft
			);
		}

		int idPosition = 0;
		while ( numberOfIdsLeft > 0 ) {
			int batchSize =  Math.min( numberOfIdsLeft, maxBatchSize );

			final EntityLoader entityLoader = EntityLoader.forEntity( persister )
					.withInfluencers( session.getLoadQueryInfluencers() )
					.withLockOptions( lockOptions )
					.withBatchSize( batchSize ).byPrimaryKey();

			Serializable[] idsInBatch = new Serializable[batchSize];
			System.arraycopy( ids, idPosition, idsInBatch, 0, batchSize );

			final List<?> batchResults = entityLoader.loadEntityBatch(
					idsInBatch,
					persister,
					lockOptions,
					session
			);
			result.addAll( batchResults );

			numberOfIdsLeft = numberOfIdsLeft - batchSize;
			idPosition += batchSize;
		}

		return result;
	}

	public static QueryParameters buildMultiLoadQueryParameters(
			OuterJoinLoadable persister,
			Serializable[] ids,
			LockOptions lockOptions) {
		Type[] types = new Type[ids.length];
		Arrays.fill( types, persister.getIdentifierType() );

		QueryParameters qp = new QueryParameters();
		qp.setOptionalEntityName( persister.getEntityName() );
		qp.setPositionalParameterTypes( types );
		qp.setPositionalParameterValues( ids );
		qp.setLockOptions( lockOptions );
		qp.setOptionalObject( null );
		qp.setOptionalId( null );
		return qp;
	}
}
