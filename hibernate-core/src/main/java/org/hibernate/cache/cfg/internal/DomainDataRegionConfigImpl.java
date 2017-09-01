/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.cfg.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * DomainDataRegionConfig implementation
 *
 * @author Steve Ebersole
 */
public class DomainDataRegionConfigImpl implements DomainDataRegionConfig {
	private final String regionName;

	private final List<EntityDataCachingConfig> entityConfigs;
	private final List<NaturalIdDataCachingConfig> naturalIdConfigs;
	private final List<CollectionDataCachingConfig> collectionConfigs;

	private DomainDataRegionConfigImpl(
			String regionName,
			List<EntityDataCachingConfig> entityConfigs,
			List<NaturalIdDataCachingConfig> naturalIdConfigs,
			List<CollectionDataCachingConfig> collectionConfigs) {
		this.regionName = regionName;
		this.entityConfigs = entityConfigs;
		this.naturalIdConfigs = naturalIdConfigs;
		this.collectionConfigs = collectionConfigs;
	}

	@Override
	public String getRegionName() {
		return regionName;
	}

	@Override
	public List<EntityDataCachingConfig> getEntityCaching() {
		return entityConfigs;
	}

	@Override
	public List<NaturalIdDataCachingConfig> getNaturalIdCaching() {
		return naturalIdConfigs;
	}

	@Override
	public List<CollectionDataCachingConfig> getCollectionCaching() {
		return collectionConfigs;
	}

	public static class Builder {
		private final String regionName;

		private List<EntityDataCachingConfig> entityConfigs;
		private List<NaturalIdDataCachingConfig> naturalIdConfigs;
		private List<CollectionDataCachingConfig> collectionConfigs;

		public Builder(String regionName) {
			this.regionName = regionName;
		}

		public Builder addEntityConfig(EntityHierarchy hierarchy, AccessType accessType) {
			if ( entityConfigs == null ) {
				entityConfigs = new ArrayList<>();
			}

			entityConfigs.add( new EntityDataCachingConfigImpl( hierarchy, accessType ) );
			return this;
		}

		public Builder addNaturalIdConfig(EntityHierarchy hierarchy, AccessType accessType) {
			if ( naturalIdConfigs == null ) {
				naturalIdConfigs = new ArrayList<>();
			}

			naturalIdConfigs.add( new NaturalIdDataCachingConfigImpl( hierarchy, accessType ) );
			return this;
		}

		public Builder addCollectionConfig(PersistentCollectionDescriptor collectionDescriptor, AccessType accessType) {
			if ( collectionConfigs == null ) {
				collectionConfigs = new ArrayList<>();
			}

			collectionConfigs.add( new CollectionDataCachingConfigImpl( collectionDescriptor, accessType ) );
			return this;
		}

		public DomainDataRegionConfigImpl build() {
			return new DomainDataRegionConfigImpl(
					regionName,
					finalize( entityConfigs ),
					finalize( naturalIdConfigs ),
					finalize( collectionConfigs )
			);
		}

		private <T extends DomainDataCachingConfig> List<T> finalize(List<T> configs) {
			return configs == null
					? Collections.emptyList()
					: Collections.unmodifiableList( configs );
		}
	}

}
