/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.AssertionFailure;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * Maintains a {@link org.hibernate.engine.spi.PersistenceContext}-level 2-way cross-reference (xref) between the 
 * identifiers and natural ids of entities associated with the PersistenceContext.
 * <p/>
 * Most operations resolve the proper {@link NaturalIdResolutionCache} to use based on the persister and 
 * simply delegate calls there.
 * 
 * @author Steve Ebersole
 */
public class NaturalIdXrefDelegate {
	private static final Logger LOG = Logger.getLogger( NaturalIdXrefDelegate.class );

	private final StatefulPersistenceContext persistenceContext;
	private final ConcurrentHashMap<EntityPersister, NaturalIdResolutionCache> naturalIdResolutionCacheMap = new ConcurrentHashMap<>();

	/**
	 * Constructs a NaturalIdXrefDelegate
	 *
	 * @param persistenceContext The persistence context that owns this delegate
	 */
	public NaturalIdXrefDelegate(StatefulPersistenceContext persistenceContext) {
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

	/**
	 * Creates needed cross-reference entries between the given primary (pk) and natural (naturalIdValues) key values
	 * for the given persister.  Returns an indication of whether entries were actually made.  If those values already
	 * existed as an entry, {@code false} would be returned here.
	 *
	 * @param persister The persister representing the entity type.
	 * @param pk The primary key value
	 * @param naturalIdValues The natural id value(s)
	 *
	 * @return {@code true} if a new entry was actually added; {@code false} otherwise.
	 */
	public boolean cacheNaturalIdCrossReference(EntityPersister persister, Serializable pk, Object[] naturalIdValues) {
		validateNaturalId( persister, naturalIdValues );

		NaturalIdResolutionCache entityNaturalIdResolutionCache = naturalIdResolutionCacheMap.get( persister );
		if ( entityNaturalIdResolutionCache == null ) {
			entityNaturalIdResolutionCache = new NaturalIdResolutionCache( persister );
			NaturalIdResolutionCache previousInstance = naturalIdResolutionCacheMap.putIfAbsent( persister, entityNaturalIdResolutionCache );
			if ( previousInstance != null ) {
				entityNaturalIdResolutionCache = previousInstance;
			}
		}
		return entityNaturalIdResolutionCache.cache( pk, naturalIdValues );
	}

	/**
	 * Handle removing cross reference entries for the given natural-id/pk combo
	 *
	 * @param persister The persister representing the entity type.
	 * @param pk The primary key value
	 * @param naturalIdValues The natural id value(s)
	 * 
	 * @return The cached values, if any.  May be different from incoming values.
	 */
	public Object[] removeNaturalIdCrossReference(EntityPersister persister, Serializable pk, Object[] naturalIdValues) {
		persister = locatePersisterForKey( persister );
		validateNaturalId( persister, naturalIdValues );

		final NaturalIdResolutionCache entityNaturalIdResolutionCache = naturalIdResolutionCacheMap.get( persister );
		Object[] sessionCachedNaturalIdValues = null;
		if ( entityNaturalIdResolutionCache != null ) {
			final CachedNaturalId cachedNaturalId = entityNaturalIdResolutionCache.pkToNaturalIdMap
					.remove( pk );
			if ( cachedNaturalId != null ) {
				entityNaturalIdResolutionCache.naturalIdToPkMap.remove( cachedNaturalId );
				sessionCachedNaturalIdValues = cachedNaturalId.getValues();
			}
		}

		if ( persister.hasNaturalIdCache() ) {
			final NaturalIdRegionAccessStrategy naturalIdCacheAccessStrategy = persister
					.getNaturalIdCacheAccessStrategy();
			final Object naturalIdCacheKey = naturalIdCacheAccessStrategy.generateCacheKey( naturalIdValues, persister, session() );
			naturalIdCacheAccessStrategy.evict( naturalIdCacheKey );

			if ( sessionCachedNaturalIdValues != null
					&& !Arrays.equals( sessionCachedNaturalIdValues, naturalIdValues ) ) {
				final Object sessionNaturalIdCacheKey = naturalIdCacheAccessStrategy.generateCacheKey( sessionCachedNaturalIdValues, persister, session() );
				naturalIdCacheAccessStrategy.evict( sessionNaturalIdCacheKey );
			}
		}

		return sessionCachedNaturalIdValues;
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
	public boolean sameAsCached(EntityPersister persister, Serializable pk, Object[] naturalIdValues) {
		final NaturalIdResolutionCache entityNaturalIdResolutionCache = naturalIdResolutionCacheMap.get( persister );
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
	 * @param persister The persister representing the entity type.
	 * @param naturalIdValues The natural id values
	 */
	protected void validateNaturalId(EntityPersister persister, Object[] naturalIdValues) {
		if ( !persister.hasNaturalIdentifier() ) {
			throw new IllegalArgumentException( "Entity did not define a natrual-id" );
		}
		if ( persister.getNaturalIdentifierProperties().length != naturalIdValues.length ) {
			throw new IllegalArgumentException( "Mismatch between expected number of natural-id values and found." );
		}
	}

	/**
	 * Given a persister and primary key, find the locally cross-referenced natural id.
	 *
	 * @param persister The persister representing the entity type.
	 * @param pk The entity primary key
	 * 
	 * @return The corresponding cross-referenced natural id values, or {@code null} if none 
	 */
	public Object[] findCachedNaturalId(EntityPersister persister, Serializable pk) {
		persister = locatePersisterForKey( persister );
		final NaturalIdResolutionCache entityNaturalIdResolutionCache = naturalIdResolutionCacheMap.get( persister );
		if ( entityNaturalIdResolutionCache == null ) {
			return null;
		}

		final CachedNaturalId cachedNaturalId = entityNaturalIdResolutionCache.pkToNaturalIdMap.get( pk );
		if ( cachedNaturalId == null ) {
			return null;
		}

		return cachedNaturalId.getValues();
	}

	/**
	 * Given a persister and natural-id value(s), find the locally cross-referenced primary key.  Will return
	 * {@link PersistenceContext.NaturalIdHelper#INVALID_NATURAL_ID_REFERENCE} if the given natural ids are known to
	 * be invalid (see {@link #stashInvalidNaturalIdReference}).
	 *
	 * @param persister The persister representing the entity type.
	 * @param naturalIdValues The natural id value(s)
	 * 
	 * @return The corresponding cross-referenced primary key, 
	 * 		{@link PersistenceContext.NaturalIdHelper#INVALID_NATURAL_ID_REFERENCE},
	 * 		or {@code null} if none 
	 */
	public Serializable findCachedNaturalIdResolution(EntityPersister persister, Object[] naturalIdValues) {
		persister = locatePersisterForKey( persister );
		validateNaturalId( persister, naturalIdValues );

		NaturalIdResolutionCache entityNaturalIdResolutionCache = naturalIdResolutionCacheMap.get( persister );

		Serializable pk;
		final CachedNaturalId cachedNaturalId = new CachedNaturalId( persister, naturalIdValues );
		if ( entityNaturalIdResolutionCache != null ) {
			pk = entityNaturalIdResolutionCache.naturalIdToPkMap.get( cachedNaturalId );

			// Found in session cache
			if ( pk != null ) {
				if ( LOG.isTraceEnabled() ) {
					LOG.trace(
							"Resolved natural key -> primary key resolution in session cache: " +
									persister.getRootEntityName() + "#[" +
									Arrays.toString( naturalIdValues ) + "]"
					);
				}

				return pk;
			}

			// if we did not find a hit, see if we know about these natural ids as invalid...
			if ( entityNaturalIdResolutionCache.containsInvalidNaturalIdReference( naturalIdValues ) ) {
				return PersistenceContext.NaturalIdHelper.INVALID_NATURAL_ID_REFERENCE;
			}
		}

		// Session cache miss, see if second-level caching is enabled
		if ( !persister.hasNaturalIdCache() ) {
			return null;
		}

		// Try resolution from second-level cache
		final NaturalIdRegionAccessStrategy naturalIdCacheAccessStrategy = persister.getNaturalIdCacheAccessStrategy();
		final Object naturalIdCacheKey = naturalIdCacheAccessStrategy.generateCacheKey( naturalIdValues, persister, session() );

		pk = CacheHelper.fromSharedCache( session(), naturalIdCacheKey, naturalIdCacheAccessStrategy );

		// Found in second-level cache, store in session cache
		final SessionFactoryImplementor factory = session().getFactory();
		if ( pk != null ) {
			if ( factory.getStatistics().isStatisticsEnabled() ) {
				factory.getStatisticsImplementor().naturalIdCacheHit(
						naturalIdCacheAccessStrategy.getRegion().getName()
				);
			}

			if ( LOG.isTraceEnabled() ) {
				// protected to avoid Arrays.toString call unless needed
				LOG.tracef(
						"Found natural key [%s] -> primary key [%s] xref in second-level cache for %s",
						Arrays.toString( naturalIdValues ),
						pk,
						persister.getRootEntityName()
				);
			}

			if ( entityNaturalIdResolutionCache == null ) {
				entityNaturalIdResolutionCache = new NaturalIdResolutionCache( persister );
				NaturalIdResolutionCache existingCache = naturalIdResolutionCacheMap.putIfAbsent( persister, entityNaturalIdResolutionCache );
				if ( existingCache != null ) {
					entityNaturalIdResolutionCache = existingCache;
				}
			}

			entityNaturalIdResolutionCache.pkToNaturalIdMap.put( pk, cachedNaturalId );
			entityNaturalIdResolutionCache.naturalIdToPkMap.put( cachedNaturalId, pk );
		}
		else if ( factory.getStatistics().isStatisticsEnabled() ) {
			factory.getStatisticsImplementor().naturalIdCacheMiss( naturalIdCacheAccessStrategy.getRegion().getName() );
		}

		return pk;
	}

	/**
	 * Return all locally cross-referenced primary keys for the given persister.  Used as part of load
	 * synchronization process.
	 *
	 * @param persister The persister representing the entity type.
	 * 
	 * @return The primary keys
	 * 
	 * @see org.hibernate.NaturalIdLoadAccess#setSynchronizationEnabled
	 */
	public Collection<Serializable> getCachedPkResolutions(EntityPersister persister) {
		persister = locatePersisterForKey( persister );

		Collection<Serializable> pks = null;

		final NaturalIdResolutionCache entityNaturalIdResolutionCache = naturalIdResolutionCacheMap.get( persister );
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
	 * its invalidity until afterQuery the next flush.  This method lets the "load synchronization process" indicate
	 * when it has encountered such changes.
	 *
	 * @param persister The persister representing the entity type.
	 * @param invalidNaturalIdValues The "old" natural id values.
	 *
	 * @see org.hibernate.NaturalIdLoadAccess#setSynchronizationEnabled
	 */
	public void stashInvalidNaturalIdReference(EntityPersister persister, Object[] invalidNaturalIdValues) {
		persister = locatePersisterForKey( persister );

		final NaturalIdResolutionCache entityNaturalIdResolutionCache = naturalIdResolutionCacheMap.get( persister );
		if ( entityNaturalIdResolutionCache == null ) {
			throw new AssertionFailure( "Expecting NaturalIdResolutionCache to exist already for entity " + persister.getEntityName() );
		}

		entityNaturalIdResolutionCache.stashInvalidNaturalIdReference( invalidNaturalIdValues );
	}

	/**
	 * Again, as part of "load synchronization process" we need to also be able to clear references to these
	 * known-invalid natural-ids afterQuery flush.  This method exposes that capability.
	 */
	public void unStashInvalidNaturalIdReferences() {
		for ( NaturalIdResolutionCache naturalIdResolutionCache : naturalIdResolutionCacheMap.values() ) {
			naturalIdResolutionCache.unStashInvalidNaturalIdReferences();
		}
	}

	/**
	 * Used to put natural id values into collections.  Useful mainly to apply equals/hashCode implementations.
	 */
	private static class CachedNaturalId implements Serializable {
		private final EntityPersister persister;
		private final Object[] values;
		private final Type[] naturalIdTypes;
		private int hashCode;

		public CachedNaturalId(EntityPersister persister, Object[] values) {
			this.persister = persister;
			this.values = values;

			final int prime = 31;
			int hashCodeCalculation = 1;
			hashCodeCalculation = prime * hashCodeCalculation + persister.hashCode();

			final int[] naturalIdPropertyIndexes = persister.getNaturalIdentifierProperties();
			naturalIdTypes = new Type[ naturalIdPropertyIndexes.length ];
			int i = 0;
			for ( int naturalIdPropertyIndex : naturalIdPropertyIndexes ) {
				final Type type = persister.getPropertyType( persister.getPropertyNames()[ naturalIdPropertyIndex ] );
				naturalIdTypes[i] = type;
				final int elementHashCode = values[i] == null ? 0 :type.getHashCode( values[i], persister.getFactory() );
				hashCodeCalculation = prime * hashCodeCalculation + elementHashCode;
				i++;
			}

			this.hashCode = hashCodeCalculation;
		}

		public Object[] getValues() {
			return values;
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

			final CachedNaturalId other = (CachedNaturalId) obj;
			return persister.equals( other.persister ) && isSame( other.values );
		}

		private boolean isSame(Object[] otherValues) {
			// lengths have already been verified at this point
			for ( int i = 0; i < naturalIdTypes.length; i++ ) {
				if ( ! naturalIdTypes[i].isEqual( values[i], otherValues[i], persister.getFactory() ) ) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * Represents the persister-specific cross-reference cache.
	 */
	private static class NaturalIdResolutionCache implements Serializable {
		private final EntityPersister persister;
		private final Type[] naturalIdTypes;

		private Map<Serializable, CachedNaturalId> pkToNaturalIdMap = new ConcurrentHashMap<Serializable, CachedNaturalId>();
		private Map<CachedNaturalId, Serializable> naturalIdToPkMap = new ConcurrentHashMap<CachedNaturalId, Serializable>();

		private List<CachedNaturalId> invalidNaturalIdList;

		private NaturalIdResolutionCache(EntityPersister persister) {
			this.persister = persister;

			final int[] naturalIdPropertyIndexes = persister.getNaturalIdentifierProperties();
			naturalIdTypes = new Type[ naturalIdPropertyIndexes.length ];
			int i = 0;
			for ( int naturalIdPropertyIndex : naturalIdPropertyIndexes ) {
				naturalIdTypes[i++] = persister.getPropertyType( persister.getPropertyNames()[ naturalIdPropertyIndex ] );
			}
		}

		public EntityPersister getPersister() {
			return persister;
		}

		public boolean sameAsCached(Serializable pk, Object[] naturalIdValues) {
			if ( pk == null ) {
				return false;
			}
			final CachedNaturalId initial = pkToNaturalIdMap.get( pk );
			if ( initial != null ) {
				if ( initial.isSame( naturalIdValues ) ) {
					return true;
				}
			}
			return false;
		}

		public boolean cache(Serializable pk, Object[] naturalIdValues) {
			if ( pk == null ) {
				return false;
			}
			final CachedNaturalId initial = pkToNaturalIdMap.get( pk );
			if ( initial != null ) {
				if ( initial.isSame( naturalIdValues ) ) {
					return false;
				}
				naturalIdToPkMap.remove( initial );
			}

			final CachedNaturalId cachedNaturalId = new CachedNaturalId( persister, naturalIdValues );
			pkToNaturalIdMap.put( pk, cachedNaturalId );
			naturalIdToPkMap.put( cachedNaturalId, pk );
			
			return true;
		}

		public void stashInvalidNaturalIdReference(Object[] invalidNaturalIdValues) {
			if ( invalidNaturalIdList == null ) {
				invalidNaturalIdList = new ArrayList<CachedNaturalId>();
			}
			invalidNaturalIdList.add( new CachedNaturalId( persister, invalidNaturalIdValues ) );
		}

		public boolean containsInvalidNaturalIdReference(Object[] naturalIdValues) {
			return invalidNaturalIdList != null
					&& invalidNaturalIdList.contains( new CachedNaturalId( persister, naturalIdValues ) );
		}

		public void unStashInvalidNaturalIdReferences() {
			if ( invalidNaturalIdList != null ) {
				invalidNaturalIdList.clear();
			}
		}
	}

	/**
	 * Clear the resolution cache
	 */
	public void clear() {
		naturalIdResolutionCacheMap.clear();
	}
}
