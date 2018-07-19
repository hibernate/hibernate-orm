/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.cfg.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.cache.cfg.spi.CollectionDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataCachingConfig;
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig;
import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.cfg.spi.NaturalIdDataCachingConfig;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.type.VersionType;

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

		private Map<NavigableRole,EntityDataCachingConfigImpl> entityConfigsByRootName;
		private List<NaturalIdDataCachingConfig> naturalIdConfigs;
		private List<CollectionDataCachingConfig> collectionConfigs;

		/**
		 * Constructs a {@link Builder}
		 *
		 * @param regionName - the unqualified region name
		 */
		public Builder(String regionName) {
			this.regionName = regionName;
		}

		@SuppressWarnings("UnusedReturnValue")
		public Builder addEntityConfig(PersistentClass bootEntityDescriptor, AccessType accessType) {
			if ( entityConfigsByRootName == null ) {
				entityConfigsByRootName = new HashMap<>();
			}

			// todo (5.3) : this is another place where having `BootstrapContext` / `TypeConfiguration` helps
			//		would allow us to delay the attempt to resolve the comparator (usual timing issues wrt Type resolution)
			final NavigableRole rootEntityName = new NavigableRole( bootEntityDescriptor.getRootClass().getEntityName() );
			final EntityDataCachingConfigImpl entityDataCachingConfig = entityConfigsByRootName.computeIfAbsent(
					rootEntityName,
					x -> new EntityDataCachingConfigImpl(
							rootEntityName,
							bootEntityDescriptor.isVersioned()
									? (Supplier<Comparator>) () -> ( (VersionType) bootEntityDescriptor.getVersion().getType() ).getComparator()
									: null,
							bootEntityDescriptor.isMutable(),
							accessType
					)
			);

			if ( bootEntityDescriptor == bootEntityDescriptor.getRootClass() ) {
				entityDataCachingConfig.addCachedType( rootEntityName );
			}
			else {
				entityDataCachingConfig.addCachedType( new NavigableRole( bootEntityDescriptor.getEntityName() ) );
			}

			return this;
		}


		// todo (6.0) : `EntityPersister` and `CollectionPersister` references here should be replaces with `EntityHierarchy` and `PersistentCollectionDescriptor`
		//
		// todo : although ^^, couldn't this just be the boot-time model?  Is there a specific need for it to be the run-time model?
		//		that would alleviate the difference between 5.3 and 6.0 from the SPI POV

		@SuppressWarnings("UnusedReturnValue")
		public Builder addNaturalIdConfig(RootClass rootEntityDescriptor, AccessType accessType) {
			if ( naturalIdConfigs == null ) {
				naturalIdConfigs = new ArrayList<>();
			}

			naturalIdConfigs.add( new NaturalIdDataCachingConfigImpl( rootEntityDescriptor, accessType ) );
			return this;
		}

		@SuppressWarnings("UnusedReturnValue")
		public Builder addCollectionConfig(Collection collectionDescriptor, AccessType accessType) {
			if ( collectionConfigs == null ) {
				collectionConfigs = new ArrayList<>();
			}

			collectionConfigs.add( new CollectionDataCachingConfigImpl( collectionDescriptor, accessType ) );
			return this;
		}

		public DomainDataRegionConfigImpl build() {
			return new DomainDataRegionConfigImpl(
					regionName,
					finalize( entityConfigsByRootName ),
					finalize( naturalIdConfigs ),
					finalize( collectionConfigs )
			);
		}

		@SuppressWarnings("unchecked")
		private <T extends DomainDataCachingConfig> List<T> finalize(Map configs) {
			return configs == null
					? Collections.emptyList()
					: Collections.unmodifiableList( new ArrayList( configs.values() ) );
		}

		private <T extends DomainDataCachingConfig> List<T> finalize(List<T> configs) {
			return configs == null
					? Collections.emptyList()
					: Collections.unmodifiableList( configs );
		}
	}

}
