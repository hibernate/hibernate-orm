/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.internal;

import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MappingDefaults;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;

/**
 * @author Steve Ebersole
 */
public class MetadataBuildingContextRootImpl implements MetadataBuildingContext {
	private final MetadataBuildingOptions options;
	private final MappingDefaults mappingDefaults;
	private final ClassLoaderAccess classLoaderAccess;
	private final InFlightMetadataCollector metadataCollector;
	private final ObjectNameNormalizer objectNameNormalizer;

	public MetadataBuildingContextRootImpl(
			MetadataBuildingOptions options,
			ClassLoaderAccess classLoaderAccess,
			InFlightMetadataCollector metadataCollector) {
		this.options = options;
		this.mappingDefaults = options.getMappingDefaults();
		this.classLoaderAccess = classLoaderAccess;
		this.metadataCollector = metadataCollector;
		this.objectNameNormalizer = new ObjectNameNormalizer() {
			@Override
			protected MetadataBuildingContext getBuildingContext() {
				return MetadataBuildingContextRootImpl.this;
			}
		};
	}

	@Override
	public MetadataBuildingOptions getBuildingOptions() {
		return options;
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
