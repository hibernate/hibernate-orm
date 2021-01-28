/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.AssertionFailure;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.CachedNaturalIdValueSource;
import org.hibernate.engine.spi.NaturalIdResolutions;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.Resolution;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.stat.internal.StatsHelper;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.jboss.logging.Logger;

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
	public boolean cacheResolution(Object pk, Object naturalIdValues, EntityMappingType entityDescriptor) {
		validateNaturalId( entityDescriptor, naturalIdValues );

		EntityResolutions entityNaturalIdResolutionCache = resolutionsByEntity.get( entityDescriptor );
		if ( entityNaturalIdResolutionCache == null ) {
			entityNaturalIdResolutionCache = new EntityResolutions( entityDescriptor, persistenceContext );
			EntityResolutions previousInstance = resolutionsByEntity.putIfAbsent( entityDescriptor, entityNaturalIdResolutionCache );
			if ( previousInstance != null ) {
				entityNaturalIdResolutionCache = previousInstance;
			}
		}
		return entityNaturalIdResolutionCache.cache( pk, naturalIdValues );
	}

	@Override
	public Object removeResolution(Object id, Object naturalId, EntityMappingType entityDescriptor) {
		final EntityPersister persister = locatePersisterForKey( entityDescriptor.getEntityPersister() );

		final NaturalIdMapping naturalIdMapping = persister.getNaturalIdMapping();
		validateNaturalId( persister, naturalId );

		final EntityResolutions entityNaturalIdResolutionCache = resolutionsByEntity.get( persister );
		Object sessionCachedNaturalIdValues = null;
		if ( entityNaturalIdResolutionCache != null ) {
			final Resolution cachedNaturalId = entityNaturalIdResolutionCache.pkToNaturalIdMap.remove( id );
			if ( cachedNaturalId != null ) {
				entityNaturalIdResolutionCache.naturalIdToPkMap.remove( cachedNaturalId );
				sessionCachedNaturalIdValues = cachedNaturalId.getNaturalIdValue();
			}
		}

		if ( persister.hasNaturalIdCache() ) {
			final NaturalIdDataAccess naturalIdCacheAccessStrategy = persister.getNaturalIdCacheAccessStrategy();
			final Object naturalIdCacheKey = naturalIdCacheAccessStrategy.generateCacheKey( naturalId, persister, session() );
			naturalIdCacheAccessStrategy.evict( naturalIdCacheKey );

			if ( sessionCachedNaturalIdValues != null
					&& ! naturalIdMapping.areEqual( sessionCachedNaturalIdValues, naturalId, session() ) ) {
				final Object sessionNaturalIdCacheKey = naturalIdCacheAccessStrategy.generateCacheKey( sessionCachedNaturalIdValues, persister, session() );
				naturalIdCacheAccessStrategy.evict( sessionNaturalIdCacheKey );
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
		final NaturalIdMapping naturalIdMapping = entityDescriptor.getNaturalIdMapping();

		if ( naturalIdMapping == null ) {
			// nothing to do
			return;
		}

		// cache
		cacheNaturalIdCrossReference( naturalIdValue, id, entityDescriptor.getRootEntityDescriptor() );
	}

	@Override
	public Object removeLocalResolution(Object id, Object naturalId, EntityMappingType entityDescriptor) {
		final NaturalIdMapping naturalIdMapping = entityDescriptor.getNaturalIdMapping();

		if ( naturalIdMapping == null ) {
			// nothing to do
			return null;
		}

		final EntityPersister persister = locatePersisterForKey( entityDescriptor.getEntityPersister() );

		final Object localNaturalIdValues = removeNaturalIdCrossReference(
				id,
				naturalId,
				persister
		);

		return localNaturalIdValues != null ? localNaturalIdValues : naturalId;
	}

	private Object removeNaturalIdCrossReference(Object id, Object naturalIdValue, EntityPersister persister) {
		validateNaturalId( persister, naturalIdValue );

		final EntityResolutions entityResolutions = this.resolutionsByEntity.get( persister );
		Object sessionCachedNaturalIdValues = null;
		if ( entityResolutions != null ) {
			final Resolution cachedNaturalId = entityResolutions.pkToNaturalIdMap.remove( id );
			if ( cachedNaturalId != null ) {
				entityResolutions.naturalIdToPkMap.remove( cachedNaturalId );
				sessionCachedNaturalIdValues = cachedNaturalId.getNaturalIdValue();
			}
		}

		if ( persister.hasNaturalIdCache() ) {
			final NaturalIdDataAccess naturalIdCacheAccessStrategy = persister
					.getNaturalIdCacheAccessStrategy();
			final Object naturalIdCacheKey = naturalIdCacheAccessStrategy.generateCacheKey( naturalIdValue, persister, session() );
			naturalIdCacheAccessStrategy.evict( naturalIdCacheKey );

			if ( sessionCachedNaturalIdValues != null
					&& ! Objects.deepEquals( sessionCachedNaturalIdValues, naturalIdValue ) ) {
				final Object sessionNaturalIdCacheKey = naturalIdCacheAccessStrategy.generateCacheKey( sessionCachedNaturalIdValues, persister, session() );
				naturalIdCacheAccessStrategy.evict( sessionNaturalIdCacheKey );
			}
		}

		return sessionCachedNaturalIdValues;
	}

	@Override
	public void cacheResolutionFromLoad(
			EntityMappingType entityDescriptor,
			Object id,
			Object naturalIdValues) {
		final EntityPersister persister = locatePersisterForKey( entityDescriptor.getEntityPersister() );

		if ( !persister.hasNaturalIdentifier() ) {
			// nothing to do
			return;
		}

		// 'justAddedLocally' is meant to handle the case where we would get double stats journaling
		//	from a single load event.  The first put journal would come from the natural id resolution;
		// the second comes from the entity loading.  In this condition, we want to avoid the multiple
		// 'put' stats incrementing.
		final boolean justAddedLocally = cacheResolution( id, naturalIdValues, entityDescriptor );

		if ( justAddedLocally && persister.hasNaturalIdCache() ) {
			manageSharedResolution( persister, id, naturalIdValues, (Object) null, CachedNaturalIdValueSource.LOAD );
		}
	}

	@Override
	public void manageSharedResolution(
			final Object id,
			Object naturalId,
			Object previousNaturalId,
			EntityMappingType entityDescriptor,
			CachedNaturalIdValueSource source) {
		final NaturalIdMapping naturalIdMapping = entityDescriptor.getNaturalIdMapping();
		if ( naturalIdMapping == null ) {
			// nothing to do
			return;
		}

		if ( naturalIdMapping.getCacheAccess() == null ) {
			// nothing to do
			return;
		}

		entityDescriptor = locatePersisterForKey( entityDescriptor.getEntityPersister() );
		final Object naturalIdValues = naturalId;

//			final Object previousNaturalIdValues = previousNaturalId == null ? null : extractNaturalIdValues( previousNaturalId, entityDescriptor );
		final Object previousNaturalIdValues = previousNaturalId;

		manageSharedResolution(
				entityDescriptor.getEntityPersister(),
				id,
				naturalIdValues,
				previousNaturalIdValues,
				source
		);
	}

	private void manageSharedResolution(
			EntityPersister persister,
			final Object id,
			Object naturalIdValues,
			Object previousNaturalIdValues,
			CachedNaturalIdValueSource source) {
		final NaturalIdDataAccess naturalIdCacheAccessStrategy = persister.getNaturalIdCacheAccessStrategy();
		final Object naturalIdCacheKey = naturalIdCacheAccessStrategy.generateCacheKey( naturalIdValues, persister, session() );

		final SessionFactoryImplementor factory = session().getFactory();
		final StatisticsImplementor statistics = factory.getStatistics();

		switch ( source ) {
			case LOAD: {
				if ( CacheHelper.fromSharedCache( session(), naturalIdCacheKey, naturalIdCacheAccessStrategy ) != null ) {
					// prevent identical re-cachings
					return;
				}
				final boolean put = naturalIdCacheAccessStrategy.putFromLoad(
						session(),
						naturalIdCacheKey,
						id,
						null
				);

				if ( put && statistics.isStatisticsEnabled() ) {
					statistics.naturalIdCachePut(
							StatsHelper.INSTANCE.getRootEntityRole( persister ),
							naturalIdCacheAccessStrategy.getRegion().getName()
					);
				}

				break;
			}
			case INSERT: {
				final boolean put = naturalIdCacheAccessStrategy.insert( session(), naturalIdCacheKey, id );
				if ( put && statistics.isStatisticsEnabled() ) {
					statistics.naturalIdCachePut(
							StatsHelper.INSTANCE.getRootEntityRole( persister ),
							naturalIdCacheAccessStrategy.getRegion().getName()
					);
				}

				( (EventSource) session() ).getActionQueue().registerProcess(
						new AfterTransactionCompletionProcess() {
							@Override
							public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
								if ( success ) {
									final boolean put = naturalIdCacheAccessStrategy.afterInsert( session, naturalIdCacheKey, id );
									if ( put && statistics.isStatisticsEnabled() ) {
										statistics.naturalIdCachePut(
												StatsHelper.INSTANCE.getRootEntityRole( persister ),
												naturalIdCacheAccessStrategy.getRegion().getName()
										);
									}
								}
								else {
									naturalIdCacheAccessStrategy.evict( naturalIdCacheKey );
								}
							}
						}
				);

				break;
			}
			case UPDATE: {
				final Object previousCacheKey = naturalIdCacheAccessStrategy.generateCacheKey( previousNaturalIdValues, persister, session() );
				if ( naturalIdCacheKey.equals( previousCacheKey ) ) {
					// prevent identical re-caching, solves HHH-7309
					return;
				}
				final SoftLock removalLock = naturalIdCacheAccessStrategy.lockItem( session(), previousCacheKey, null );
				naturalIdCacheAccessStrategy.remove( session(), previousCacheKey);

				final SoftLock lock = naturalIdCacheAccessStrategy.lockItem( session(), naturalIdCacheKey, null );
				final boolean put = naturalIdCacheAccessStrategy.update( session(), naturalIdCacheKey, id );
				if ( put && statistics.isStatisticsEnabled() ) {
					statistics.naturalIdCachePut(
							StatsHelper.INSTANCE.getRootEntityRole( persister ),
							naturalIdCacheAccessStrategy.getRegion().getName()
					);
				}

				( (EventSource) session() ).getActionQueue().registerProcess(
						new AfterTransactionCompletionProcess() {
							@Override
							public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor session) {
								naturalIdCacheAccessStrategy.unlockItem( session(), previousCacheKey, removalLock );
								if (success) {
									final boolean put = naturalIdCacheAccessStrategy.afterUpdate(
											session(),
											naturalIdCacheKey,
											id,
											lock
									);

									if ( put && statistics.isStatisticsEnabled() ) {
										statistics.naturalIdCachePut(
												StatsHelper.INSTANCE.getRootEntityRole( persister ),
												naturalIdCacheAccessStrategy.getRegion().getName()
										);
									}
								}
								else {
									naturalIdCacheAccessStrategy.unlockItem( session(), naturalIdCacheKey, lock );
								}
							}
						}
				);

				break;
			}
			default: {
				if ( LOG.isDebugEnabled() ) {
					LOG.debug( "Unexpected CachedNaturalIdValueSource [" + source + "]" );
				}
			}
		}
	}

	@Override
	public void removeSharedResolution(EntityMappingType entityDescriptor, Object id, Object naturalIdValues) {
		final NaturalIdMapping naturalIdMapping = entityDescriptor.getNaturalIdMapping();
		if ( naturalIdMapping == null || naturalIdMapping.getCacheAccess() == null ) {
			// nothing to do
			return;
		}

		// todo : couple of things wrong here:
		//		1) should be using access strategy, not plain evict..
		//		2) should prefer session-cached values if any (requires interaction from removeLocalNaturalIdCrossReference)

		final EntityPersister persister = locatePersisterForKey( entityDescriptor.getEntityPersister() );

		final NaturalIdDataAccess naturalIdCacheAccessStrategy = persister.getNaturalIdCacheAccessStrategy();
		final Object naturalIdCacheKey = naturalIdCacheAccessStrategy.generateCacheKey( naturalIdValues, persister, session() );
		naturalIdCacheAccessStrategy.evict( naturalIdCacheKey );

//			if ( sessionCachedNaturalIdValues != null
//					&& !Arrays.equals( sessionCachedNaturalIdValues, deletedNaturalIdValues ) ) {
//				final NaturalIdCacheKey sessionNaturalIdCacheKey = new NaturalIdCacheKey( sessionCachedNaturalIdValues, persister, session );
//				naturalIdCacheAccessStrategy.evict( sessionNaturalIdCacheKey );
//			}
	}

	@Override
	public void removeSharedResolution(Object id, Object naturalId, EntityMappingType entityDescriptor) {

	}

	@Override
	public void handleSynchronization(Object pk, Object entity, EntityMappingType entityDescriptor) {
		final NaturalIdMapping naturalIdMapping = entityDescriptor.getNaturalIdMapping();
		if ( naturalIdMapping == null ) {
			// nothing to do
			return;
		}
		final EntityPersister persister = locatePersisterForKey( entityDescriptor.getEntityPersister() );

		final Object naturalIdValuesFromCurrentObjectState = naturalIdMapping.extractNaturalIdValues( entity, session() );
		final boolean changed = ! sameAsCached(
				persister,
				pk,
				naturalIdValuesFromCurrentObjectState
		);

		if ( changed ) {
			final Object cachedNaturalIdValues = findCachedNaturalId( pk, persister );
			cacheResolution( pk, naturalIdValuesFromCurrentObjectState, persister );
			stashInvalidNaturalIdReference( persister, cachedNaturalIdValues );

			removeSharedResolution(
					persister,
					pk,
					cachedNaturalIdValues
			);
		}
	}

	@Override
	public void cleanupFromSynchronizations() {
		unStashInvalidNaturalIdReferences();
	}

	@Override
	public void handleEviction(Object id, Object object, EntityMappingType entityDescriptor) {
		removeResolution(
				id,
				findCachedNaturalId( id, entityDescriptor ),
				entityDescriptor
		);
	}

	public boolean cacheNaturalIdCrossReference(Object naturalIdValue, Object pk, EntityMappingType entityDescriptor) {
		validateNaturalId( entityDescriptor, naturalIdValue );

		EntityResolutions entityNaturalIdResolutionCache = resolutionsByEntity.get( entityDescriptor.getRootEntityDescriptor() );
		if ( entityNaturalIdResolutionCache == null ) {
			entityNaturalIdResolutionCache = new EntityResolutions( entityDescriptor.getRootEntityDescriptor(), persistenceContext );
			EntityResolutions previousInstance = resolutionsByEntity.putIfAbsent( entityDescriptor.getRootEntityDescriptor(), entityNaturalIdResolutionCache );
			if ( previousInstance != null ) {
				entityNaturalIdResolutionCache = previousInstance;
			}
		}
		return entityNaturalIdResolutionCache.cache( pk, naturalIdValue );
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
		final EntityResolutions entityNaturalIdResolutionCache = resolutionsByEntity.get( persister );
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
		return persistenceContext.getSession().getFactory().getEntityPersister( persister.getRootEntityName() );
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
		final NaturalIdMapping naturalIdMapping = entityDescriptor.getNaturalIdMapping();

		if ( naturalIdMapping == null ) {
			throw new IllegalArgumentException( "Entity did not define a natural-id" );
		}

		naturalIdMapping.validateInternalForm( naturalIdValues, persistenceContext.getSession() );
	}

	@Override
	public Object findCachedNaturalId(Object id, EntityMappingType entityDescriptor) {
		final EntityPersister persister = locatePersisterForKey( entityDescriptor.getEntityPersister() );
		final EntityResolutions entityNaturalIdResolutionCache = resolutionsByEntity.get( persister );
		if ( entityNaturalIdResolutionCache == null ) {
			return null;
		}

		final Resolution cachedNaturalId = entityNaturalIdResolutionCache.pkToNaturalIdMap.get( id );
		if ( cachedNaturalId == null ) {
			return null;
		}

		return cachedNaturalId.getNaturalIdValue();
	}

	@Override
	public Object findCachedNaturalIdResolution(Object naturalId, EntityMappingType entityDescriptor) {
		final EntityPersister persister = locatePersisterForKey( entityDescriptor.getEntityPersister() );
		validateNaturalId( persister, naturalId );

		EntityResolutions entityNaturalIdResolutionCache = resolutionsByEntity.get( persister );

		Object pk;
		final Resolution cachedNaturalId = new ResolutionImpl( persister, naturalId, persistenceContext );
		if ( entityNaturalIdResolutionCache != null ) {
			pk = entityNaturalIdResolutionCache.naturalIdToPkMap.get( cachedNaturalId );

			// Found in session cache
			if ( pk != null ) {
				if ( LOG.isTraceEnabled() ) {
					LOG.tracef(
							"Resolved natural key (%s) -> primary key (%s) resolution in session cache for `%s`:",
							naturalId,
							pk,
							entityDescriptor.getEntityName()
					);
				}

				return pk;
			}

			// if we did not find a hit, see if we know about these natural ids as invalid...
			if ( entityNaturalIdResolutionCache.containsInvalidNaturalIdReference( naturalId ) ) {
				return NaturalIdResolutions.INVALID_NATURAL_ID_REFERENCE;
			}
		}

		// Session cache miss, see if second-level caching is enabled
		if ( !persister.hasNaturalIdCache() ) {
			return null;
		}

		// Try resolution from second-level cache
		final NaturalIdDataAccess naturalIdCacheAccessStrategy = persister.getNaturalIdCacheAccessStrategy();
		final SharedSessionContractImplementor session = session();
		final Object naturalIdCacheKey = naturalIdCacheAccessStrategy.generateCacheKey( naturalId, persister, session );

		pk = CacheHelper.fromSharedCache( session, naturalIdCacheKey, naturalIdCacheAccessStrategy );

		// Found in second-level cache, store in session cache
		final SessionFactoryImplementor factory = session.getFactory();
		final StatisticsImplementor statistics = factory.getStatistics();
		final boolean statisticsEnabled = statistics.isStatisticsEnabled();
		if ( pk != null ) {
			if ( statisticsEnabled ) {
				statistics.naturalIdCacheHit(
						StatsHelper.INSTANCE.getRootEntityRole( persister ),
						naturalIdCacheAccessStrategy.getRegion().getName()
				);
			}

			if ( LOG.isTraceEnabled() ) {
				// protected to avoid Arrays.toString call unless needed
				LOG.tracef(
						"Found natural key [%s] -> primary key [%s] xref in second-level cache for %s",
						naturalId,
						pk,
						persister.getRootEntityName()
				);
			}

			if ( entityNaturalIdResolutionCache == null ) {
				entityNaturalIdResolutionCache = new EntityResolutions( persister, persistenceContext );
				EntityResolutions existingCache = resolutionsByEntity.putIfAbsent( persister, entityNaturalIdResolutionCache );
				if ( existingCache != null ) {
					entityNaturalIdResolutionCache = existingCache;
				}
			}

			entityNaturalIdResolutionCache.pkToNaturalIdMap.put( pk, cachedNaturalId );
			entityNaturalIdResolutionCache.naturalIdToPkMap.put( cachedNaturalId, pk );
		}
		else if ( statisticsEnabled ) {
			statistics.naturalIdCacheMiss(
					StatsHelper.INSTANCE.getRootEntityRole( persister ),
					naturalIdCacheAccessStrategy.getRegion().getName()
			);
		}

		return pk;
	}

	@Override
	public Collection<?> getCachedPkResolutions(EntityMappingType entityDescriptor) {
		final EntityPersister persister = locatePersisterForKey( entityDescriptor.getEntityPersister() );

		Collection<Object> pks = null;

		final EntityResolutions entityNaturalIdResolutionCache = resolutionsByEntity.get( persister );
		if ( entityNaturalIdResolutionCache != null ) {
			pks = entityNaturalIdResolutionCache.pkToNaturalIdMap.keySet();
		}

		if ( pks == null || pks.isEmpty() ) {
			return java.util.Collections.emptyList();
		}
		else {
			return java.util.Collections.unmodifiableCollection( pks );
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

		final EntityResolutions entityNaturalIdResolutionCache = resolutionsByEntity.get( persister );
		if ( entityNaturalIdResolutionCache == null ) {
			throw new AssertionFailure( "Expecting NaturalIdResolutionCache to exist already for entity " + persister.getEntityName() );
		}

		entityNaturalIdResolutionCache.stashInvalidNaturalIdReference( invalidNaturalIdValues );
	}

	/**
	 * Again, as part of "load synchronization process" we need to also be able to clear references to these
	 * known-invalid natural-ids after flush.  This method exposes that capability.
	 */
	public void unStashInvalidNaturalIdReferences() {
		for ( EntityResolutions naturalIdResolutionCache : resolutionsByEntity.values() ) {
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
	 * Represents the persister-specific cross-reference cache.
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
			if ( initial != null ) {
				if ( initial.isSame( naturalIdValues ) ) {
					return true;
				}
			}
			return false;
		}

		public boolean cache(Object pk, Object naturalIdValues) {
			if ( pk == null ) {
				return false;
			}
			final Resolution initial = pkToNaturalIdMap.get( pk );
			if ( initial != null ) {
				if ( initial.isSame( naturalIdValues ) ) {
					return false;
				}
				naturalIdToPkMap.remove( initial );
			}

			final Resolution cachedNaturalId = new ResolutionImpl( getEntityDescriptor(), naturalIdValues, persistenceContext );
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
			hashCodeCalculation = prime * hashCodeCalculation + entityDescriptor.getNaturalIdMapping().calculateHashCode( naturalIdValue, persistenceContext.getSession() );

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
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}

			final ResolutionImpl other = (ResolutionImpl) obj;
			return entityDescriptor.equals( other.entityDescriptor ) && isSame( other.naturalIdValue );
		}

		@Override
		public boolean isSame(Object otherValue) {
			return entityDescriptor.getNaturalIdMapping().areEqual( naturalIdValue, otherValue, persistenceContext.getSession() );
		}
	}
}
