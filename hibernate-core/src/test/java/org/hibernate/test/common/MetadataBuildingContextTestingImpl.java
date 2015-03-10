/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.common;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.ClassLoaderAccessImpl;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.registry.StandardServiceRegistry;
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

	public MetadataBuildingContextTestingImpl(StandardServiceRegistry serviceRegistry) {
		buildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
		mappingDefaults = new MetadataBuilderImpl.MappingDefaultsImpl( serviceRegistry );
		metadataCollector = new InFlightMetadataCollectorImpl( buildingOptions, new MetadataSources( serviceRegistry ), new TypeResolver() );
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
