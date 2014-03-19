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
import org.hibernate.metamodel.internal.ClassLoaderAccessImpl;
import org.hibernate.metamodel.internal.MetadataBuilderImpl;
import org.hibernate.metamodel.internal.MetadataBuildingProcess;
import org.hibernate.metamodel.reflite.internal.JavaTypeDescriptorRepositoryImpl;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptorRepository;
import org.hibernate.metamodel.source.internal.annotations.JandexAccessImpl;
import org.hibernate.metamodel.spi.BindingContext;
import org.hibernate.metamodel.spi.ClassLoaderAccess;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.TypeFactory;
import org.hibernate.type.TypeResolver;

import org.jboss.jandex.IndexView;

/**
 * @author Steve Ebersole
 */
public class RootBindingContextBuilder {

	public static BindingContext buildBindingContext(StandardServiceRegistry serviceRegistry) {
		return buildBindingContext( serviceRegistry, null );
	}

	public static BindingContext buildBindingContext(StandardServiceRegistry serviceRegistry, IndexView index) {
		final BasicTypeRegistry basicTypeRegistry = new BasicTypeRegistry();
		final MetadataBuilderImpl.Options options = new MetadataBuilderImpl.Options( serviceRegistry );
		final MetadataBuildingProcess.MappingDefaultsImpl  mappingDefaults = new MetadataBuildingProcess.MappingDefaultsImpl(
				options
		);
		final ClassLoaderAccess classLoaderAccess = new ClassLoaderAccessImpl( null, serviceRegistry );
		final JandexAccessImpl jandexAccess = new JandexAccessImpl(
				index,
				classLoaderAccess
		);
		final JavaTypeDescriptorRepository javaTypeDescriptorRepository = new JavaTypeDescriptorRepositoryImpl(
				jandexAccess,
				classLoaderAccess
		);
		final MetadataBuildingProcess.InFlightMetadataCollectorImpl metadataCollector = new MetadataBuildingProcess.InFlightMetadataCollectorImpl(
				options,
				new TypeResolver( basicTypeRegistry, new TypeFactory() )
		);
		return new MetadataBuildingProcess.RootBindingContextImpl(
				options,
				mappingDefaults,
				javaTypeDescriptorRepository,
				jandexAccess,
				classLoaderAccess,
				metadataCollector
		);
	}
}
