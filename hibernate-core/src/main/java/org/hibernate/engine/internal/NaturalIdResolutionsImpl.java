/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.AssertionFailure;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.CachedNaturalIdValueSource;
import org.hibernate.engine.spi.NaturalIdResolutions;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.Resolution;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.monitor.spi.EventMonitor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.results.LoadingLogger;
import org.hibernate.stat.internal.StatsHelper;

import org.jboss.logging.Logger;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableCollection;
import static org.hibernate.engine.internal.CacheHelper.fromSharedCache;
import static org.hibernate.engine.internal.NaturalIdLogging.NATURAL_ID_MESSAGE_LOGGER;

public class NaturalIdResolutionsImpl implements NaturalIdResolutions, Serializable {

	private static final Logger LOG = Logger.getLogger( NaturalIdResolutionsImpl.class );

	private final StatefulPersistenceContext persistenceContext;
	private final ConcurrentHashMap<EntityMappingType, EntityResolutions> resolutionsByEntity = new ConcurrentHashMap<>();

	/**
	 * Constructs a NaturalIdXrefDelegate
	 *
	 * @param persistenceContext The persistence context that owns this delegate
	 */
	public NaturalIdResolutionsImpl(StatefulPersistenceContext persistenceContext) {
		this.persistenceContext = persistenceContext;
	}

	/**
	 * Access to the session (via the PersistenceContext) to which this delegate ultimately belongs.
	 *
	 * @return The session
	 */
	protected SharedSessionContractImplementor session() {
		return persistenceContext.getSession();
	}

	@Override
	public boolean cacheResolution(Object id, Object naturalId, EntityMappingType entityDescriptor) {
		validateNaturalId( entityDescriptor, naturalId );
		return cacheResolutionLocally( id, naturalId, entityDescriptor );
	}

	@Override
	public void cacheResolutionFromLoad(Object id, Object naturalId, EntityMappingType entityDescriptor) {
		NATURAL_ID_MESSAGE_LOGGER.cachingNaturalIdResolutionFromLoad(
				entityDescriptor.getEntityName(),
				naturalId instanceof Object[] array ? Arrays.toString( array ) : naturalId,
				id
		);

		final var naturalIdMapping = entityDescriptor.getNaturalIdMapping();
		if ( naturalIdMapping != null ) {
			// 'justAddedLocally' is meant to handle the case where we would get double stats journaling
			// from a single load event. The first put journal would come from the natural id resolution;
			// the second comes from the entity loading. In this condition, we want to avoid the multiple
			// 'put' stats incrementing.
			final boolean justAddedLocally = cacheResolution( id, naturalId, entityDescriptor );
			if ( justAddedLocally && naturalIdMapping.getCacheAccess() != null ) {
				final var persister = locatePersisterForKey( entityDescriptor.getEntityPersister() );
				manageSharedResolution( persister, id, naturalId, (Object) null, CachedNaturalIdValueSource.LOAD );
			}
		}
	}

	/**
	 * Private, but see {@link #cacheResolution} for public version
	 */
	private boolean cacheResolutionLocally(Object id, Object naturalId, EntityMappingType entityDescriptor) {
		// by the time we get here, we assume that the natural id value has already been validated, so just assert
		assert entityDescriptor.getNaturalIdMapping() != null;
		assert isValidValue( naturalId, entityDescriptor );

		NATURAL_ID_MESSAGE_LOGGER.locallyCachingNaturalIdResolution(
				entityDescriptor.getEntityName(),
				naturalId instanceof Object[] array ? Arrays.toString( array ) : naturalId,
				id
		);

		final var rootEntityDescriptor = entityDescriptor.getRootEntityDescriptor();
		final var previousEntry = resolutionsByEntity.get( rootEntityDescriptor );
		final EntityResolutions resolutions;
		if ( previousEntry != null ) {
			resolutions = previousEntry;
		}
		else {
			resolutions = new EntityResolutions( rootEntityDescriptor, persistenceContext );
			resolutionsByEntity.put( rootEntityDescriptor, resolutions );
		}

		return resolutions.cache( id, naturalId );
	}

	@Override
	public Object removeResolution(Object id, Object naturalId, EntityMappingType entityDescriptor) {
		final var persister = locatePersisterForKey( entityDescriptor.getEntityPersister() );

//		final NaturalIdMapping naturalIdMapping = persister.getNaturalIdMapping();
		validateNaturalId( persister, naturalId );

		final var entityNaturalIdResolutionCache = resolutionsByEntity.get( persister );
		Object sessionCachedNaturalIdValues = null;
		if ( entityNaturalIdResolutionCache != null ) {
			final var cachedNaturalId = entityNaturalIdResolutionCache.pkToNaturalIdMap.remove( id );
			if ( cachedNaturalId != null ) {
				entityNaturalIdResolutionCache.naturalIdToPkMap.remove( cachedNaturalId );
				sessionCachedNaturalIdValues = cachedNaturalId.getNaturalIdValue();
			}
		}

		return sessionCachedNaturalIdValues;
	}

	@Override
	public void manageLocalResolution(
			Object id,
			Object naturalIdValue,
			EntityMappingType entityDescriptor,
			CachedNaturalIdValueSource source) {
		final var naturalIdMapping = entityDescriptor.getNaturalIdMapping();
		if ( naturalIdMapping != null ) {
			cacheResolutionLocally( id, naturalIdValue, entityDescriptor );
		}
		// else nothing to do
	}

	@Override
	public Object removeLocalResolution(Object id, Object naturalId, EntityMappingType entityDescriptor) {
		final var naturalIdMapping = entityDescriptor.getNaturalIdMapping();
		if ( naturalIdMapping != null ) {
			NATURAL_ID_MESSAGE_LOGGER.removingLocallyCachedNaturalIdResolution(
					entityDescriptor.getEntityName(),
					naturalId instanceof Object[] array ? Arrays.toString( array ) : naturalId,
					id
			);

			final var persister = locatePersisterForKey( entityDescriptor.getEntityPersister() );
			final Object localNaturalIdValues = removeNaturalIdCrossReference( id, naturalId, persister );
			return localNaturalIdValues == null ? naturalId : localNaturalIdValues;
		}
		else {
			return null;
		}
	}

	/**
	 * Removes the cross-reference from both Session cache (Resolution) as well as the
	 * second-level cache (NaturalIdDataAccess).  Returns the natural-id value previously
	 * cached on the Session
	 */
	private Object removeNaturalIdCrossReference(Object id, Object naturalId, EntityPersister persister) {
		validateNaturalId( persister, naturalId );
		final Object sessionCachedNaturalId = removeSessionCachedNaturalIdValue( id, persister );
		if ( persister.hasNaturalIdCache() ) {
			final var session = session();
			evictCachedNaturalId( naturalId, persister, session );
			if ( sessionCachedNaturalId != null
					// TODO: we should not really use equals() to compare instances of Hibernate Types
					&& !Objects.deepEquals( sessionCachedNaturalId, naturalId ) ) {
				evictCachedNaturalId( sessionCachedNaturalId, persister, session );
			}
		}
		return sessionCachedNaturalId;
	}

	private static void evictCachedNaturalId(
			Object naturalId,
			EntityPersister persister,
			SharedSessionContractImplementor session) {
		final NaturalIdDataAccess cacheAccessStrategy = persister.getNaturalIdCacheAccessStrategy();
		cacheAccessStrategy.evict( cacheAccessStrategy.generateCacheKey( naturalId, persister, session ) );
	}

	private Object removeSessionCachedNaturalIdValue(Object id, EntityPersister persister) {
		final var entityResolutions = resolutionsByEntity.get( persister );
		if ( entityResolutions != null ) {
			final var cachedNaturalId = entityResolutions.pkToNaturalIdMap.remove( id );
			if ( cachedNaturalId != null ) {
				entityResolutions.naturalIdToPkMap.remove( cachedNaturalId );
				return cachedNaturalId.getNaturalIdValue();
			}
		}
		return null;
	}

	@Override
	public void manageSharedResolution(
			final Object id,
			Object naturalId,
			Object previousNaturalId,
			EntityMappingType entityDescriptor,
			CachedNaturalIdValueSource source) {
		final var naturalIdMapping = entityDescriptor.getNaturalIdMapping();
		if ( naturalIdMapping != null && naturalIdMapping.getCacheAccess() != null ) {
			manageSharedResolution(
					entityDescriptor.getEntityPersister(),
					id,
					naturalId,
					previousNaturalId,
					source
			);
		}
	}

	private void manageSharedResolution(
			EntityPersister persister,
			final Object id,
			Object naturalIdValues,
			Object previousNaturalIdValues,
			CachedNaturalIdValueSource source) {
		final var cacheAccess = persister.getNaturalIdMapping().getCacheAccess();
		if ( cacheAccess != null ) {
			final var rootEntityDescriptor = persister.getRootEntityDescriptor();
			final var rootEntityPersister = rootEntityDescriptor.getEntityPersister();
			final Object cacheKey = cacheAccess.generateCacheKey( naturalIdValues, rootEntityPersister, session() );
			switch ( source ) {
				case LOAD:
					cacheFromLoad(
							persister,
							id,
							cacheKey,
							cacheAccess,
							rootEntityDescriptor,
							rootEntityPersister
					);
					break;
				case INSERT:
					cacheFromInsert(
							id,
							cacheAccess,
							cacheKey,
							rootEntityDescriptor,
							rootEntityPersister
					);
					break;
				case UPDATE:
					cacheFromUpdate(
							id,
							previousNaturalIdValues,
							cacheAccess,
							rootEntityPersister,
							cacheKey,
							rootEntityDescriptor
					);

					break;
				default:
					if ( LOG.isDebugEnabled() ) {
						LOG.debug( "Unexpected CachedNaturalIdValueSource [" + source + "]" );
					}
			}
		}
	}

	private void cacheFromUpdate(
			Object id,
			Object previousNaturalIdValues,
			NaturalIdDataAccess cacheAccess,
			EntityPersister rootEntityPersister,
			Object cacheKey,
			EntityMappingType rootEntityDescriptor) {
		final var session = session();

		final Object previousCacheKey =
				cacheAccess.generateCacheKey( previousNaturalIdValues, rootEntityPersister, session );
		if ( cacheKey.equals( previousCacheKey ) ) {
			// prevent identical re-caching, solves HHH-7309
			return;
		}

		final SoftLock removalLock = cacheAccess.lockItem( session, previousCacheKey, null );
		cacheAccess.remove( session, previousCacheKey);
		final SoftLock lock = cacheAccess.lockItem( session, cacheKey, null );

		final var statistics = session.getFactory().getStatistics();
		final var eventMonitor = session.getEventMonitor();
		boolean put = false;
		final var cachePutEvent = eventMonitor.beginCachePutEvent();
		try {
			put = cacheAccess.update( session, cacheKey, id );
			if ( put && statistics.isStatisticsEnabled() ) {
				statistics.naturalIdCachePut(
						rootEntityDescriptor.getNavigableRole(),
						cacheAccess.getRegion().getName()
				);
			}
		}
		finally {
			eventMonitor.completeCachePutEvent(
					cachePutEvent,
					session(),
					cacheAccess,
					rootEntityPersister,
					put,
					true,
					EventMonitor.CacheActionDescription.ENTITY_UPDATE
			);
		}

		session.asEventSource().getActionQueue().registerProcess(
				(success, sess) -> {
					cacheAccess.unlockItem( sess, previousCacheKey, removalLock );
					if (success) {
						boolean putAfterUpdate = false;
						final var cachePutEventAfterUpdate = eventMonitor.beginCachePutEvent();
						try {
							putAfterUpdate = cacheAccess.afterUpdate( sess, cacheKey, id, lock );
							if ( putAfterUpdate && statistics.isStatisticsEnabled() ) {
								statistics.naturalIdCachePut(
										rootEntityDescriptor.getNavigableRole(),
										cacheAccess.getRegion().getName()
								);
							}
						}
						finally {
							eventMonitor.completeCachePutEvent(
									cachePutEventAfterUpdate,
									session(),
									cacheAccess,
									rootEntityPersister,
									putAfterUpdate,
									true,
									EventMonitor.CacheActionDescription.ENTITY_AFTER_UPDATE
							);
						}
					}
					else {
						cacheAccess.unlockItem( sess, cacheKey, lock );
					}
				}
		);
	}

	private void cacheFromInsert(
			Object id,
			NaturalIdDataAccess cacheAccess,
			Object cacheKey,
			EntityMappingType rootEntityDescriptor,
			EntityPersister rootEntityPersister) {
		final var session = session();

		final var statistics = session.getFactory().getStatistics();
		final var eventMonitor = session.getEventMonitor();
		boolean put = false;
		final var cachePutEvent = eventMonitor.beginCachePutEvent();
		try {
			put = cacheAccess.insert( session, cacheKey, id );
			if ( put && statistics.isStatisticsEnabled() ) {
				statistics.naturalIdCachePut(
						rootEntityDescriptor.getNavigableRole(),
						cacheAccess.getRegion().getName()
				);
			}
		}
		finally {
			eventMonitor.completeCachePutEvent(
					cachePutEvent,
					session(),
					cacheAccess,
					rootEntityPersister,
					put,
					true,
					EventMonitor.CacheActionDescription.ENTITY_INSERT
			);
		}

		session.asEventSource().getActionQueue().registerProcess(
				(success, sess) -> {
					if ( success ) {
						final boolean changed = cacheAccess.afterInsert( sess, cacheKey, id );
						if ( changed && statistics.isStatisticsEnabled() ) {
							statistics.naturalIdCachePut(
									rootEntityDescriptor.getNavigableRole(),
									cacheAccess.getRegion().getName()
							);
						}
					}
					else {
						cacheAccess.evict( cacheKey );
					}
				}
		);
	}

	private void cacheFromLoad(
			EntityPersister persister,
			Object id,
			Object cacheKey,
			NaturalIdDataAccess cacheAccess,
			EntityMappingType rootEntityDescriptor,
			EntityPersister rootEntityPersister) {
		final var session = session();

		if ( fromSharedCache( session, cacheKey, persister, cacheAccess ) != null ) {
			// prevent identical re-caching
			return;
		}

		final var statistics = session.getFactory().getStatistics();
		final var eventMonitor = session.getEventMonitor();
		boolean put = false;
		final var cachePutEvent = eventMonitor.beginCachePutEvent();
		try {
			put = cacheAccess.putFromLoad( session, cacheKey, id, null );
			if ( put && statistics.isStatisticsEnabled() ) {
				statistics.naturalIdCachePut(
						rootEntityDescriptor.getNavigableRole(),
						cacheAccess.getRegion().getName()
				);
			}
		}
		finally {
			eventMonitor.completeCachePutEvent(
					cachePutEvent,
					session,
					cacheAccess,
					rootEntityPersister,
					put,
					true,
					EventMonitor.CacheActionDescription.ENTITY_LOAD
			);
		}
	}

	@Override
	public void removeSharedResolution(Object id, Object naturalId, EntityMappingType entityDescriptor) {
		removeSharedResolution( id, naturalId, entityDescriptor, false );
	}

	@Override
	public void removeSharedResolution(Object id, Object naturalId, EntityMappingType entityDescriptor, boolean delayToAfterTransactionCompletion) {
		final var naturalIdMapping = entityDescriptor.getNaturalIdMapping();
		if ( naturalIdMapping == null ) {
			// nothing to do
			return;
		}

		final var cacheAccess = naturalIdMapping.getCacheAccess();
		if ( cacheAccess == null ) {
			// nothing to do
			return;
		}

		// todo : couple of things wrong here:
		//		1) should be using access strategy, not plain evict..
		//		2) should prefer session-cached values if any (requires interaction from removeLocalNaturalIdCrossReference)

		final var session = session();
		final var persister = locatePersisterForKey( entityDescriptor.getEntityPersister() );
		final Object naturalIdCacheKey = cacheAccess.generateCacheKey( naturalId, persister, session );
		if ( delayToAfterTransactionCompletion ) {
			session.asEventSource().getActionQueue().registerProcess(
				(success, sess) -> {
					if ( success ) {
						cacheAccess.evict( naturalIdCacheKey );
					}
				}
			);
		}
		else {
			cacheAccess.evict( naturalIdCacheKey );
		}

//			if ( sessionCachedNaturalIdValues != null
//					&& !Arrays.equals( sessionCachedNaturalIdValues, deletedNaturalIdValues ) ) {
//				final NaturalIdCacheKey sessionNaturalIdCacheKey = new NaturalIdCacheKey( sessionCachedNaturalIdValues, persister, session );
//				cacheAccess.evict( sessionNaturalIdCacheKey );
//			}
	}

	@Override
	public void handleSynchronization(Object pk, Object entity, EntityMappingType entityDescriptor) {
		final var naturalIdMapping = entityDescriptor.getNaturalIdMapping();
		if ( naturalIdMapping != null ) {
			final var persister = locatePersisterForKey( entityDescriptor.getEntityPersister() );
			final Object naturalIdValuesFromCurrentObjectState = naturalIdMapping.extractNaturalIdFromEntity( entity );
			final boolean changed = !sameAsCached( persister, pk, naturalIdValuesFromCurrentObjectState );
			if ( changed ) {
				final Object cachedNaturalIdValues = findCachedNaturalIdById( pk, persister );
				cacheResolution( pk, naturalIdValuesFromCurrentObjectState, persister );
				stashInvalidNaturalIdReference( persister, cachedNaturalIdValues );
				removeSharedResolution( pk, cachedNaturalIdValues, persister, false );
			}
		}
	}

	@Override
	public void cleanupFromSynchronizations() {
		unStashInvalidNaturalIdReferences();
	}

	@Override
	public void handleEviction(Object id, Object object, EntityMappingType entityDescriptor) {
		removeResolution( id, findCachedNaturalIdById( id, entityDescriptor ), entityDescriptor );
	}

	/**
	 * Are the naturals id values cached here (if any) for the given persister+pk combo the same as the given values?
	 *
	 * @param persister The persister representing the entity type.
	 * @param pk The primary key value
	 * @param naturalIdValues The natural id value(s) to check
	 *
	 * @return {@code true} if the given naturalIdValues match the current cached values; {@code false} otherwise.
	 */
	public boolean sameAsCached(EntityPersister persister, Object pk, Object naturalIdValues) {
		final var entityNaturalIdResolutionCache = resolutionsByEntity.get( persister );
		return entityNaturalIdResolutionCache != null
			&& entityNaturalIdResolutionCache.sameAsCached( pk, naturalIdValues );
	}

	/**
	 * It is only valid to define natural ids at the root of an entity hierarchy.  This method makes sure we are
	 * using the root persister.
	 *
	 * @param persister The persister representing the entity type.
	 *
	 * @return The root persister.
	 */
	protected EntityPersister locatePersisterForKey(EntityPersister persister) {
		return persister.getRootEntityDescriptor().getEntityPersister();
	}

	/**
	 * Invariant validate of the natural id.  Checks include<ul>
	 *     <li>that the entity defines a natural id</li>
	 *     <li>the number of natural id values matches the expected number</li>
	 * </ul>
	 *
	 * @param entityDescriptor The entity type descriptor
	 * @param naturalIdValues The natural id values
	 */
	protected void validateNaturalId(EntityMappingType entityDescriptor, Object naturalIdValues) {
		final var naturalIdMapping = entityDescriptor.getNaturalIdMapping();
		if ( naturalIdMapping == null ) {
			throw new IllegalArgumentException( "Entity did not define a natural id" );
		}
		naturalIdMapping.validateInternalForm( naturalIdValues );
	}

	private boolean isValidValue(Object naturalIdValues, EntityMappingType entityDescriptor) {
		final var naturalIdMapping = entityDescriptor.getNaturalIdMapping();
		if ( naturalIdMapping == null ) {
			throw new IllegalArgumentException( "Entity did not define a natural id" );
		}
		naturalIdMapping.validateInternalForm( naturalIdValues );
		// validateInternalForm would have thrown an exception if not
		return true;
	}

	@Override
	public Object findCachedNaturalIdById(Object id, EntityMappingType entityDescriptor) {
		if ( LoadingLogger.TRACE_ENABLED ) {
			LoadingLogger.LOGGER.tracef(
					"Starting NaturalIdResolutionsImpl.#findCachedNaturalIdById( `%s`, `%s` )",
					entityDescriptor.getEntityName(),
					id
			);
		}

		final var persister = locatePersisterForKey( entityDescriptor.getEntityPersister() );
		final var entityNaturalIdResolutionCache = resolutionsByEntity.get( persister );
		if ( entityNaturalIdResolutionCache == null ) {
			return null;
		}
		else {
			final var cachedNaturalId = entityNaturalIdResolutionCache.pkToNaturalIdMap.get( id );
			return cachedNaturalId == null ? null : cachedNaturalId.getNaturalIdValue();
		}
	}

	@Override
	public Object findCachedIdByNaturalId(Object naturalId, EntityMappingType entityDescriptor) {
		if ( LoadingLogger.TRACE_ENABLED ) {
			LoadingLogger.LOGGER.tracef(
					"Starting NaturalIdResolutionsImpl.#findCachedIdByNaturalId( `%s`, `%s` )",
					entityDescriptor.getEntityName(),
					naturalId
			);
		}

		final var persister = locatePersisterForKey( entityDescriptor.getEntityPersister() );
		validateNaturalId( persister, naturalId );

		final var resolutionCache = resolutionsByEntity.get( persister );

		final Resolution cachedNaturalId = new ResolutionImpl( persister, naturalId, persistenceContext );
		if ( resolutionCache != null ) {
			// Try the session cache
			final Object identifier = resolutionCache.naturalIdToPkMap.get( cachedNaturalId );
			if ( identifier != null ) {
				// Found in session cache
				if ( NATURAL_ID_MESSAGE_LOGGER.isTraceEnabled() ) {
					NATURAL_ID_MESSAGE_LOGGER.resolvedNaturalIdInSessionCache( naturalId, identifier,
							entityDescriptor.getEntityName() );
				}
				return identifier;
			}

			// if we did not find a hit, see if we know about these natural ids as invalid
			if ( resolutionCache.containsInvalidNaturalIdReference( naturalId ) ) {
				return INVALID_NATURAL_ID_REFERENCE;
			}
		}

		// Session cache miss, see if second-level caching is enabled
		if ( !persister.hasNaturalIdCache() ) {
			return null;
		}
		else {
			// Try the second-level cache
			final var cacheAccessStrategy = persister.getNaturalIdCacheAccessStrategy();
			final var session = session();
			final Object cacheKey = cacheAccessStrategy.generateCacheKey( naturalId, persister, session );
			final Object id = fromSharedCache( session, cacheKey, persister, true, cacheAccessStrategy );
			final var statistics = session.getFactory().getStatistics();
			final boolean statisticsEnabled = statistics.isStatisticsEnabled();
			if ( id != null ) {
				// Found in second-level cache, store in session cache
				if ( statisticsEnabled ) {
					statistics.naturalIdCacheHit(
							StatsHelper.getRootEntityRole( persister ),
							cacheAccessStrategy.getRegion().getName()
					);
				}
				if ( NATURAL_ID_MESSAGE_LOGGER.isTraceEnabled() ) {
					// protected to avoid Arrays.toString call unless needed
					NATURAL_ID_MESSAGE_LOGGER.foundNaturalIdInSecondLevelCache( naturalId, id,
							persister.getRootEntityName() );
				}
				storeInResolutionCache( resolutionCache, persister, id, cachedNaturalId );
				return id;
			}
			else {
				// Not found anywhere
				if ( statisticsEnabled ) {
					statistics.naturalIdCacheMiss(
							StatsHelper.getRootEntityRole( persister ),
							cacheAccessStrategy.getRegion().getName()
					);
				}
				return null;
			}
		}
	}

	private void storeInResolutionCache(
			EntityResolutions resolutionCache,
			EntityPersister persister,
			Object pk,
			Resolution cachedNaturalId) {

		if ( resolutionCache == null ) {
			resolutionCache = new EntityResolutions( persister, persistenceContext );
			final var existingCache = resolutionsByEntity.putIfAbsent( persister, resolutionCache );
			if ( existingCache != null ) {
				resolutionCache = existingCache;
			}
		}

		resolutionCache.pkToNaturalIdMap.put( pk, cachedNaturalId );
		resolutionCache.naturalIdToPkMap.put( cachedNaturalId, pk );
	}

	@Override
	public Collection<?> getCachedPkResolutions(EntityMappingType entityDescriptor) {
		final var persister = locatePersisterForKey( entityDescriptor.getEntityPersister() );
		final var entityNaturalIdResolutionCache = resolutionsByEntity.get( persister );
		if ( entityNaturalIdResolutionCache != null ) {
			final var pks = entityNaturalIdResolutionCache.pkToNaturalIdMap.keySet();
			return pks.isEmpty() ? emptyList() : unmodifiableCollection( pks );
		}
		else {
			return emptyList();
		}
	}

	/**
	 * As part of "load synchronization process", if a particular natural id is found to have changed we need to track
	 * its invalidity until after the next flush.  This method lets the "load synchronization process" indicate
	 * when it has encountered such changes.
	 *
	 * @param persister The persister representing the entity type.
	 * @param invalidNaturalIdValues The "old" natural id values.
	 *
	 * @see org.hibernate.NaturalIdLoadAccess#setSynchronizationEnabled
	 */
	public void stashInvalidNaturalIdReference(EntityPersister persister, Object invalidNaturalIdValues) {
		persister = locatePersisterForKey( persister );
		final var entityNaturalIdResolutionCache = resolutionsByEntity.get( persister );
		if ( entityNaturalIdResolutionCache == null ) {
			throw new AssertionFailure( "Expecting NaturalIdResolutionCache to exist already for entity "
										+ persister.getEntityName() );
		}
		entityNaturalIdResolutionCache.stashInvalidNaturalIdReference( invalidNaturalIdValues );
	}

	/**
	 * Again, as part of "load synchronization process" we need to also be able to clear references to these
	 * known-invalid natural-ids after flush.  This method exposes that capability.
	 */
	public void unStashInvalidNaturalIdReferences() {
		for ( var naturalIdResolutionCache : resolutionsByEntity.values() ) {
			naturalIdResolutionCache.unStashInvalidNaturalIdReferences();
		}
	}

	/**
	 * Clear the resolution cache
	 */
	public void clear() {
		resolutionsByEntity.clear();
	}

	/**
	 * Represents the entity-specific cross-reference cache.
	 */
	private static class EntityResolutions implements Serializable {
		private final PersistenceContext persistenceContext;

		private final EntityMappingType entityDescriptor;

		private final Map<Object, Resolution> pkToNaturalIdMap = new ConcurrentHashMap<>();
		private final Map<Resolution, Object> naturalIdToPkMap = new ConcurrentHashMap<>();

		private List<Resolution> invalidNaturalIdList;

		private EntityResolutions(EntityMappingType entityDescriptor, PersistenceContext persistenceContext) {
			this.entityDescriptor = entityDescriptor;
			this.persistenceContext = persistenceContext;
		}

		public EntityMappingType getEntityDescriptor() {
			return entityDescriptor;
		}

		public EntityPersister getPersister() {
			return getEntityDescriptor().getEntityPersister();
		}

		public boolean sameAsCached(Object pk, Object naturalIdValues) {
			if ( pk == null ) {
				return false;
			}
			final Resolution initial = pkToNaturalIdMap.get( pk );
			return initial != null && initial.isSame( naturalIdValues );
		}

		public boolean cache(Object pk, Object naturalIdValues) {
			if ( pk == null ) {
				return false;
			}
			final var initial = pkToNaturalIdMap.get( pk );
			if ( initial != null ) {
				if ( initial.isSame( naturalIdValues ) ) {
					return false;
				}
				naturalIdToPkMap.remove( initial );
			}

			final var cachedNaturalId = new ResolutionImpl( getEntityDescriptor(), naturalIdValues, persistenceContext );
			pkToNaturalIdMap.put( pk, cachedNaturalId );
			naturalIdToPkMap.put( cachedNaturalId, pk );

			return true;
		}

		public void stashInvalidNaturalIdReference(Object invalidNaturalIdValues) {
			if ( invalidNaturalIdList == null ) {
				invalidNaturalIdList = new ArrayList<>();
			}
			invalidNaturalIdList.add( new ResolutionImpl( getEntityDescriptor(), invalidNaturalIdValues, persistenceContext ) );
		}

		public boolean containsInvalidNaturalIdReference(Object naturalIdValues) {
			return invalidNaturalIdList != null
				&& invalidNaturalIdList.contains( new ResolutionImpl( getEntityDescriptor(), naturalIdValues, persistenceContext ) );
		}

		public void unStashInvalidNaturalIdReferences() {
			if ( invalidNaturalIdList != null ) {
				invalidNaturalIdList.clear();
			}
		}
	}


	private static class ResolutionImpl implements Resolution, Serializable {
		private final PersistenceContext persistenceContext;

		private final EntityMappingType entityDescriptor;
		private final Object naturalIdValue;

		private final int hashCode;

		public ResolutionImpl(EntityMappingType entityDescriptor, Object naturalIdValue, PersistenceContext persistenceContext) {
			this.entityDescriptor = entityDescriptor;
			this.naturalIdValue = naturalIdValue;
			this.persistenceContext = persistenceContext;

			final int prime = 31;
			int hashCodeCalculation = 1;
			hashCodeCalculation = prime * hashCodeCalculation + entityDescriptor.hashCode();
			hashCodeCalculation = prime * hashCodeCalculation + entityDescriptor.getNaturalIdMapping().calculateHashCode( naturalIdValue );

			this.hashCode = hashCodeCalculation;
		}

		@Override
		public Object getNaturalIdValue() {
			return naturalIdValue;
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			else if ( !(obj instanceof ResolutionImpl other) ) {
				return false;
			}
			else {
				return entityDescriptor.equals( other.entityDescriptor )
					&& isSame( other.naturalIdValue );
			}
		}

		@Override
		public boolean isSame(Object otherValue) {
			return entityDescriptor.getNaturalIdMapping()
					.areEqual( naturalIdValue, otherValue, persistenceContext.getSession() );
		}
	}
}
