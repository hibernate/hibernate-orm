/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.cache;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.AbstractDomainDataRegion;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

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
		log.debugf( "Generating entity cache access : %s", entityAccessConfig.getEntityHierarchy().getRootEntityType().getEntityName() );
		switch ( entityAccessConfig.getAccessType() ) {
			case READ_ONLY: {
				return new EntityReadOnlyAccess( this, entityAccessConfig.getEntityHierarchy() );
			}
			case TRANSACTIONAL: {
				return new EntityTransactionalAccess( this, entityAccessConfig.getEntityHierarchy() );
			}
			case READ_WRITE: {
				return new EntityReadWriteAccess( this, entityAccessConfig.getEntityHierarchy() );
			}
			case NONSTRICT_READ_WRITE: {
				return new EntityNonStrictReadWriteAccess( this, entityAccessConfig.getEntityHierarchy() );
			}
			default: {
				throw new IllegalArgumentException( "Unrecognized cache AccessType - " + entityAccessConfig.getAccessType() );
			}
		}
	}

	@Override
	public NaturalIdDataAccess generateNaturalIdAccess(NaturalIdDataCachingConfig naturalIdDataCachingConfig) {
		log.debugf( "Generating entity natural-id cache access : %s", naturalIdDataCachingConfig.getEntityHierarchy().getRootEntityType().getEntityName() );

		switch ( naturalIdDataCachingConfig.getAccessType() ) {
			case READ_ONLY: {
				return new NaturalIdReadOnlyAccess( this, naturalIdDataCachingConfig.getEntityHierarchy() );
			}
			case TRANSACTIONAL: {
				return new NaturalIdTransactionalAccess( this, naturalIdDataCachingConfig.getEntityHierarchy() );
			}
			case READ_WRITE: {
				return new NaturalIdReadWriteAccess( this, naturalIdDataCachingConfig.getEntityHierarchy() );
			}
			case NONSTRICT_READ_WRITE: {
				return new NaturalIdNonStrictReadWriteAccess( this, naturalIdDataCachingConfig.getEntityHierarchy() );
			}
			default: {
				throw new IllegalArgumentException( "Unrecognized cache AccessType - " + naturalIdDataCachingConfig.getAccessType() );
			}
		}
	}

	@Override
	public CollectionDataAccess generateCollectionAccess(CollectionDataCachingConfig config) {
		log.debugf( "Generating collection cache access : %s", config.getCollectionDescriptor().getNavigableRole().getFullPath() );

		switch ( config.getAccessType() ) {
			case READ_ONLY: {
				return new CollectionReadOnlyAccess( this, config.getCollectionDescriptor() );
			}
			case TRANSACTIONAL: {
				return new CollectionTransactionAccess( this, config.getCollectionDescriptor() );
			}
			case READ_WRITE: {
				return new CollectionReadWriteAccess( this, config.getCollectionDescriptor() );
			}
			case NONSTRICT_READ_WRITE: {
				return new CollectionNonStrictReadWriteAccess( this, config.getCollectionDescriptor() );
			}
			default: {
				throw new IllegalArgumentException( "Unrecognized cache AccessType - " + config.getAccessType() );
			}
		}
	}
}
