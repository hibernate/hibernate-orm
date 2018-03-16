/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.jcache.internal;

import javax.cache.Cache;

import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.support.AbstractDomainDataRegion;
import org.hibernate.cache.spi.support.CollectionNonStrictReadWriteAccess;
import org.hibernate.cache.spi.support.CollectionReadOnlyAccess;
import org.hibernate.cache.spi.support.CollectionReadWriteAccess;
import org.hibernate.cache.spi.support.EntityNonStrictReadWriteAccess;
import org.hibernate.cache.spi.support.EntityReadOnlyAccess;
import org.hibernate.cache.spi.support.EntityReadWriteAccess;
import org.hibernate.cache.spi.support.NaturalIdNonStrictReadWriteAccess;
import org.hibernate.cache.spi.support.NaturalIdReadOnlyAccess;
import org.hibernate.cache.spi.support.NaturalIdReadWriteAccess;
import org.hibernate.metamodel.model.domain.NavigableRole;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class DomainDataRegionImpl extends AbstractDomainDataRegion {
	private static final Logger log = Logger.getLogger( DomainDataRegionImpl.class );

	private final CacheKeysFactory effectiveKeysFactory;

	public DomainDataRegionImpl(
			DomainDataRegionConfig regionConfig,
			JCacheRegionFactory regionFactory,
			Cache underlyingCache,
			DomainDataRegionBuildingContext buildingContext) {
		super(
				regionConfig,
				regionFactory,
				new DomainDataJCacheAccessImpl( underlyingCache ),
				buildingContext
		);

		this.effectiveKeysFactory = regionFactory.determineKeysFactoryToUse( buildingContext );
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
				return new EntityReadOnlyAccess(
						this,
						effectiveKeysFactory,
						getStorageAccess(),
						entityAccessConfig
				);
			}
			case READ_WRITE: {
				return new EntityReadWriteAccess(
						this,
						effectiveKeysFactory,
						getStorageAccess(),
						entityAccessConfig
				);
			}
			case NONSTRICT_READ_WRITE: {
				return new EntityNonStrictReadWriteAccess(
						this,
						effectiveKeysFactory,
						getStorageAccess(),
						entityAccessConfig
				);
			}
			case TRANSACTIONAL: {
				return generateTransactionalEntityDataAccess( this, entityAccessConfig );
			}
			default: {
				throw new IllegalArgumentException( "Unrecognized cache AccessType - " + accessType );
			}
		}
	}

	protected EntityDataAccess generateTransactionalEntityDataAccess(
			DomainDataRegionImpl domainDataRegion,
			EntityDataCachingConfig entityAccessConfig) {
		throw generateTransactionalNotSupportedException();
	}

	private UnsupportedOperationException generateTransactionalNotSupportedException() {
		return new UnsupportedOperationException( "Cache provider [" + getRegionFactory() + "] does not support `" + AccessType.TRANSACTIONAL.getExternalName() + "` access" );
	}

	@Override
	public NaturalIdDataAccess generateNaturalIdAccess(NaturalIdDataCachingConfig naturalIdDataCachingConfig) {
		final NavigableRole namedEntityRole = naturalIdDataCachingConfig.getNavigableRole();
		final AccessType accessType = naturalIdDataCachingConfig.getAccessType();

		log.debugf( "Generating entity natural-id access [%s] : %s", accessType.getExternalName(), namedEntityRole );

		switch ( accessType ) {
			case READ_ONLY: {
				return new NaturalIdReadOnlyAccess(
						this,
						effectiveKeysFactory,
						getStorageAccess(),
						naturalIdDataCachingConfig
				);
			}
			case READ_WRITE: {
				return new NaturalIdReadWriteAccess(
						this,
						effectiveKeysFactory,
						getStorageAccess(),
						naturalIdDataCachingConfig
				);
			}
			case NONSTRICT_READ_WRITE: {
				return new NaturalIdNonStrictReadWriteAccess(
						this,
						effectiveKeysFactory,
						getStorageAccess(),
						naturalIdDataCachingConfig
				);
			}
			case TRANSACTIONAL: {
				return generateTransactionalNaturalIdDataAccess( this, naturalIdDataCachingConfig );
			}
			default: {
				throw new IllegalArgumentException( "Unrecognized cache AccessType - " + accessType );
			}
		}
	}

	protected NaturalIdDataAccess generateTransactionalNaturalIdDataAccess(
			DomainDataRegionImpl domainDataRegion,
			NaturalIdDataCachingConfig config) {
		throw generateTransactionalNotSupportedException();
	}

	@Override
	public CollectionDataAccess generateCollectionAccess(CollectionDataCachingConfig config) {
		final NavigableRole namedCollectionRole = config.getNavigableRole();

		log.debugf( "Generating collection cache access : %s", namedCollectionRole );

		switch ( config.getAccessType() ) {
			case READ_ONLY: {
				return new CollectionReadOnlyAccess(
						this,
						effectiveKeysFactory,
						getStorageAccess(),
						config
				);
			}
			case READ_WRITE: {
				return new CollectionReadWriteAccess(
						this,
						effectiveKeysFactory,
						getStorageAccess(),
						config
				);
			}
			case NONSTRICT_READ_WRITE: {
				return new CollectionNonStrictReadWriteAccess(
						this,
						effectiveKeysFactory,
						getStorageAccess(),
						config
				);
			}
			case TRANSACTIONAL: {
				return generateTransactionalCollectionDataAccess( this, config );
			}
			default: {
				throw new IllegalArgumentException( "Unrecognized cache AccessType - " + config.getAccessType() );
			}
		}
	}

	protected CollectionDataAccess generateTransactionalCollectionDataAccess(
			DomainDataRegionImpl domainDataRegion,
			CollectionDataCachingConfig entityAccessConfig) {
		throw generateTransactionalNotSupportedException();
	}
}
