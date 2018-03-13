/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.AbstractDomainDataRegion;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class DomainDataRegionImpl extends AbstractDomainDataRegion {
	private static final Logger log = Logger.getLogger( DomainDataRegionImpl.class );

	private final CacheKeysFactory effectiveKeysFactory;

	public DomainDataRegionImpl(
			DomainDataRegionConfig regionConfig,
			CachingRegionFactory regionFactory,
			DomainDataRegionBuildingContext buildingContext) {
		super( regionConfig, regionFactory, buildingContext );

		this.effectiveKeysFactory = buildingContext.getEnforcedCacheKeysFactory() != null
				? buildingContext.getEnforcedCacheKeysFactory()
				: regionFactory.getCacheKeysFactory();
	}

	public CacheKeysFactory getEffectiveKeysFactory() {
		return effectiveKeysFactory;
	}

	@Override
	public EntityDataAccess generateEntityAccess(EntityDataCachingConfig entityAccessConfig) {
		final NavigableRole namedEntityRole = entityAccessConfig.getNavigableRole();
		final AccessType accessType = entityAccessConfig.getAccessType();

		log.debugf( "Generating entity cache access [%s] : %s", accessType.getExternalName(), namedEntityRole );

		switch ( accessType ) {
			case READ_ONLY: {
				return new EntityReadOnlyAccess( this, entityAccessConfig );
			}
			case TRANSACTIONAL: {
				return new EntityTransactionalAccess( this, entityAccessConfig );
			}
			case READ_WRITE: {
				return new EntityReadWriteAccess( this, entityAccessConfig );
			}
			case NONSTRICT_READ_WRITE: {
				return new EntityNonStrictReadWriteAccess( this, entityAccessConfig );
			}
			default: {
				throw new IllegalArgumentException( "Unrecognized cache AccessType - " + accessType );
			}
		}
	}

	@Override
	public NaturalIdDataAccess generateNaturalIdAccess(NaturalIdDataCachingConfig naturalIdDataCachingConfig) {
		final NavigableRole namedEntityRole = naturalIdDataCachingConfig.getNavigableRole();
		final AccessType accessType = naturalIdDataCachingConfig.getAccessType();

		log.debugf( "Generating entity natural-id access [%s] : %s", accessType.getExternalName(), namedEntityRole );

		switch ( accessType ) {
			case READ_ONLY: {
				return new NaturalIdReadOnlyAccess( this, naturalIdDataCachingConfig );
			}
			case TRANSACTIONAL: {
				return new NaturalIdTransactionalAccess( this, naturalIdDataCachingConfig );
			}
			case READ_WRITE: {
				return new NaturalIdReadWriteAccess( this, naturalIdDataCachingConfig );
			}
			case NONSTRICT_READ_WRITE: {
				return new NaturalIdNonStrictReadWriteAccess( this, naturalIdDataCachingConfig );
			}
			default: {
				throw new IllegalArgumentException( "Unrecognized cache AccessType - " + accessType );
			}
		}
	}

	@Override
	public CollectionDataAccess generateCollectionAccess(CollectionDataCachingConfig config) {
		final NavigableRole namedCollectionRole = config.getNavigableRole();

		log.debugf( "Generating collection cache access : %s", namedCollectionRole );

		switch ( config.getAccessType() ) {
			case READ_ONLY: {
				return new CollectionReadOnlyAccess( this, config );
			}
			case TRANSACTIONAL: {
				return new CollectionTransactionAccess( this, config );
			}
			case READ_WRITE: {
				return new CollectionReadWriteAccess( this, config );
			}
			case NONSTRICT_READ_WRITE: {
				return new CollectionNonStrictReadWriteAccess( this, config );
			}
			default: {
				throw new IllegalArgumentException( "Unrecognized cache AccessType - " + config.getAccessType() );
			}
		}
	}
}
