/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.internal;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.cache.spi.NaturalIdCacheKey;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.CachedNaturalIdValueSource;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.hibernate.type.TypeHelper;

/**
 * Maintains a {@link org.hibernate.engine.spi.PersistenceContext}-level 2-way cross-reference (xref) between the 
 * identifiers and natural ids of entities associated with the PersistenceContext.  Additionally coordinates
 * actions related to the shared caching of the entity's natural id. 
 * 
 * @author Steve Ebersole
 */
public class NaturalIdXrefDelegate {
	private static final Logger LOG = Logger.getLogger( NaturalIdXrefDelegate.class );

	private final StatefulPersistenceContext persistenceContext;
	private final Map<EntityPersister, NaturalIdResolutionCache> naturalIdResolutionCacheMap = new ConcurrentHashMap<EntityPersister, NaturalIdResolutionCache>();

	public NaturalIdXrefDelegate(StatefulPersistenceContext persistenceContext) {
		this.persistenceContext = persistenceContext;
	}

	protected SessionImplementor session() {
		return persistenceContext.getSession();
	}

	public void cacheNaturalIdResolution(
			EntityPersister persister, 
			final Serializable pk,
			Object[] naturalIdValues,
			CachedNaturalIdValueSource valueSource) {
		persister = locatePersisterForKey( persister );
		validateNaturalId( persister, naturalIdValues );

		Object[] previousNaturalIdValues = valueSource == CachedNaturalIdValueSource.UPDATE
				? persistenceContext.getNaturalIdSnapshot( pk, persister )
				: null;

		NaturalIdResolutionCache entityNaturalIdResolutionCache = naturalIdResolutionCacheMap.get( persister );
		if ( entityNaturalIdResolutionCache == null ) {
			entityNaturalIdResolutionCache = new NaturalIdResolutionCache( persister );
			naturalIdResolutionCacheMap.put( persister, entityNaturalIdResolutionCache );
		}

		final boolean justAddedToLocalCache = entityNaturalIdResolutionCache.cache( pk, naturalIdValues );

		// If second-level caching is enabled cache the resolution there as well
		//		NOTE : the checks using 'justAddedToLocalCache' below protect only the stat journaling, not actually
		//		putting into the shared cache.  we still put into the shared cache because that might have locking
		//		semantics that we need to honor.
		if ( persister.hasNaturalIdCache() ) {
			final NaturalIdRegionAccessStrategy naturalIdCacheAccessStrategy = persister.getNaturalIdCacheAccessStrategy();
			final NaturalIdCacheKey naturalIdCacheKey = new NaturalIdCacheKey( naturalIdValues, persister, session() );

			final SessionFactoryImplementor factory = session().getFactory();

			switch ( valueSource ) {
				case LOAD: {
					final boolean put = naturalIdCacheAccessStrategy.putFromLoad(
							naturalIdCacheKey,
							pk,
							session().getTimestamp(),
							null
					);
	
					if ( put && justAddedToLocalCache && factory.getStatistics().isStatisticsEnabled() ) {
						factory.getStatisticsImplementor()
								.naturalIdCachePut( naturalIdCacheAccessStrategy.getRegion().getName() );
					}

					break;
				}
				case INSERT: {
					naturalIdCacheAccessStrategy.insert( naturalIdCacheKey, pk );

					( (EventSource) session() ).getActionQueue().registerProcess(
							new AfterTransactionCompletionProcess() {
								@Override
								public void doAfterTransactionCompletion(boolean success, SessionImplementor session) {
									final boolean put = naturalIdCacheAccessStrategy.afterInsert( naturalIdCacheKey, pk );

									if ( put && justAddedToLocalCache && factory.getStatistics().isStatisticsEnabled() ) {
										factory.getStatisticsImplementor().naturalIdCachePut(
												naturalIdCacheAccessStrategy.getRegion().getName() );
									}
								}
							}
					);

					break;
				}
				case UPDATE: {
					final NaturalIdCacheKey previousCacheKey = new NaturalIdCacheKey( previousNaturalIdValues, persister, session() );
					final SoftLock removalLock = naturalIdCacheAccessStrategy.lockItem( previousCacheKey, null );
					naturalIdCacheAccessStrategy.remove( previousCacheKey );

					final SoftLock lock = naturalIdCacheAccessStrategy.lockItem( naturalIdCacheKey, null );
					naturalIdCacheAccessStrategy.update( naturalIdCacheKey, pk );

					( (EventSource) session() ).getActionQueue().registerProcess(
							new AfterTransactionCompletionProcess() {
								@Override
								public void doAfterTransactionCompletion(boolean success, SessionImplementor session) {
									naturalIdCacheAccessStrategy.unlockRegion( removalLock );
									final boolean put = naturalIdCacheAccessStrategy.afterUpdate( naturalIdCacheKey, pk, lock );

									if ( put && justAddedToLocalCache && factory.getStatistics().isStatisticsEnabled() ) {
										factory.getStatisticsImplementor().naturalIdCachePut(
												naturalIdCacheAccessStrategy.getRegion().getName() );
									}
								}
							}
					);

					break;
				}
			}
		}
	}

	protected EntityPersister locatePersisterForKey(EntityPersister persister) {
		return persistenceContext.getSession().getFactory().getEntityPersister( persister.getRootEntityName() );
	}

	protected void validateNaturalId(EntityPersister persister, Object[] naturalIdValues) {
		if ( !persister.hasNaturalIdentifier() ) {
			throw new IllegalArgumentException( "Entity did not define a natrual-id" );
		}
		if ( persister.getNaturalIdentifierProperties().length != naturalIdValues.length ) {
			throw new IllegalArgumentException( "Mismatch between expected number of natural-id values and found." );
		}
	}

	public void evictNaturalIdResolution(EntityPersister persister, final Serializable pk, Object[] deletedNaturalIdValues) {
		persister = locatePersisterForKey( persister );
		validateNaturalId( persister, deletedNaturalIdValues );

		NaturalIdResolutionCache entityNaturalIdResolutionCache = naturalIdResolutionCacheMap.get( persister );
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
			final NaturalIdCacheKey naturalIdCacheKey = new NaturalIdCacheKey( deletedNaturalIdValues, persister, session() );
			naturalIdCacheAccessStrategy.evict( naturalIdCacheKey );

			if ( sessionCachedNaturalIdValues != null
					&& !Arrays.equals( sessionCachedNaturalIdValues, deletedNaturalIdValues ) ) {
				final NaturalIdCacheKey sessionNaturalIdCacheKey = new NaturalIdCacheKey( sessionCachedNaturalIdValues, persister, session() );
				naturalIdCacheAccessStrategy.evict( sessionNaturalIdCacheKey );
			}
		}
	}

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
		}

		// Session cache miss, see if second-level caching is enabled
		if ( !persister.hasNaturalIdCache() ) {
			return null;
		}

		// Try resolution from second-level cache
		final NaturalIdCacheKey naturalIdCacheKey = new NaturalIdCacheKey( naturalIdValues, persister, session() );

		final NaturalIdRegionAccessStrategy naturalIdCacheAccessStrategy = persister.getNaturalIdCacheAccessStrategy();
		pk = (Serializable) naturalIdCacheAccessStrategy.get( naturalIdCacheKey, session().getTimestamp() );

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
				naturalIdResolutionCacheMap.put( persister, entityNaturalIdResolutionCache );
			}

			entityNaturalIdResolutionCache.pkToNaturalIdMap.put( pk, cachedNaturalId );
			entityNaturalIdResolutionCache.naturalIdToPkMap.put( cachedNaturalId, pk );
		}
		else if ( factory.getStatistics().isStatisticsEnabled() ) {
			factory.getStatisticsImplementor().naturalIdCacheMiss( naturalIdCacheAccessStrategy.getRegion().getName() );
		}

		return pk;
	}


	private static class CachedNaturalId {
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
				int elementHashCode = values[i] == null ? 0 :type.getHashCode( values[i], persister.getFactory() );
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
			return persister.equals( other.persister ) && areSame( values, other.values );
		}

		private boolean areSame(Object[] values, Object[] otherValues) {
			// lengths have already been verified at this point
			for ( int i = 0; i < naturalIdTypes.length; i++ ) {
				if ( ! naturalIdTypes[i].isEqual( values[i], otherValues[i], persister.getFactory() ) ) {
					return false;
				}
			}
			return true;
		}
	}

	private static class NaturalIdResolutionCache implements Serializable {
		private final EntityPersister persister;
		private final Type[] naturalIdTypes;

		private Map<Serializable, CachedNaturalId> pkToNaturalIdMap = new ConcurrentHashMap<Serializable, CachedNaturalId>();
		private Map<CachedNaturalId, Serializable> naturalIdToPkMap = new ConcurrentHashMap<CachedNaturalId, Serializable>();

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

		public boolean cache(Serializable pk, Object[] naturalIdValues) {
			final CachedNaturalId initial = pkToNaturalIdMap.get( pk );
			if ( initial != null ) {
				if ( areSame( naturalIdValues, initial.getValues() ) ) {
					return false;
				}
				naturalIdToPkMap.remove( initial );
			}

			final CachedNaturalId cachedNaturalId = new CachedNaturalId( persister, naturalIdValues );
			pkToNaturalIdMap.put( pk, cachedNaturalId );
			naturalIdToPkMap.put( cachedNaturalId, pk );
			
			return true;
		}

		private boolean areSame(Object[] naturalIdValues, Object[] values) {
			// lengths have already been verified at this point
			for ( int i = 0; i < naturalIdTypes.length; i++ ) {
				if ( ! naturalIdTypes[i].isEqual( naturalIdValues[i], values[i] ) ) {
					return false;
				}
			}
			return true;
		}
	}
}
