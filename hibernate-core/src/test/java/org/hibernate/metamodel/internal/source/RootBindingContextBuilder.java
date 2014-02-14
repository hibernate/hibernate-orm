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
package org.hibernate.metamodel.internal.source;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.metamodel.internal.MetadataBuilderImpl;
import org.hibernate.metamodel.internal.MetadataBuildingProcess;
import org.hibernate.metamodel.reflite.internal.RepositoryImpl;
import org.hibernate.metamodel.reflite.spi.Repository;
import org.hibernate.metamodel.spi.BindingContext;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.TypeFactory;
import org.hibernate.type.TypeResolver;

/**
 * @author Steve Ebersole
 */
public class RootBindingContextBuilder {

	public static BindingContext buildBindingContext(StandardServiceRegistry serviceRegistry) {
		final BasicTypeRegistry basicTypeRegistry = new BasicTypeRegistry();
		final MetadataBuilderImpl.Options options = new MetadataBuilderImpl.Options( serviceRegistry );
		final MetadataBuildingProcess.MappingDefaultsImpl  mappingDefaults = new MetadataBuildingProcess.MappingDefaultsImpl(
				options
		);
		final Repository typeRepository = new RepositoryImpl( null, serviceRegistry );
		final MetadataBuildingProcess.InFlightMetadataCollectorImpl metadataCollector = new MetadataBuildingProcess.InFlightMetadataCollectorImpl(
				options,
				new TypeResolver( basicTypeRegistry, new TypeFactory() )
		);
		return new MetadataBuildingProcess.RootBindingContextImpl(
				options,
				mappingDefaults,
				typeRepository,
				metadataCollector
		);
	}
}
