/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.boot;

import org.hibernate.boot.internal.ClassLoaderAccessImpl;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MappingDefaults;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.type.TypeResolver;

/**
* @author Steve Ebersole
*/
public class MetadataBuildingContextTestingImpl implements MetadataBuildingContext {
	private final MetadataBuildingOptions buildingOptions;
	private final MappingDefaults mappingDefaults;
	private final InFlightMetadataCollector metadataCollector;
	private final ClassLoaderAccessImpl classLoaderAccess;

	private final ObjectNameNormalizer objectNameNormalizer;

	public MetadataBuildingContextTestingImpl() {
		this( new StandardServiceRegistryBuilder().build() );
	}

	public MetadataBuildingContextTestingImpl(StandardServiceRegistry serviceRegistry) {
		buildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		mappingDefaults = new MetadataBuilderImpl.MappingDefaultsImpl( serviceRegistry );
		metadataCollector = new InFlightMetadataCollectorImpl( buildingOptions, new TypeResolver() );
		classLoaderAccess = new ClassLoaderAccessImpl( null, serviceRegistry );

		objectNameNormalizer = new ObjectNameNormalizer() {
			@Override
			protected MetadataBuildingContext getBuildingContext() {
				return MetadataBuildingContextTestingImpl.this;
			}
		};
	}

	@Override
	public MetadataBuildingOptions getBuildingOptions() {
		return buildingOptions;
	}

	@Override
	public MappingDefaults getMappingDefaults() {
		return mappingDefaults;
	}

	@Override
	public InFlightMetadataCollector getMetadataCollector() {
		return metadataCollector;
	}

	@Override
	public ClassLoaderAccess getClassLoaderAccess() {
		return classLoaderAccess;
	}

	@Override
	public ObjectNameNormalizer getObjectNameNormalizer() {
		return objectNameNormalizer;
	}
}
