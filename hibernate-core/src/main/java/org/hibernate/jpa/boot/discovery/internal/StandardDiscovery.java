/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.discovery.internal;

import org.hibernate.HibernateException;
import org.hibernate.boot.ResourceLocator;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveDescriptorFactory;
import org.hibernate.jpa.boot.discovery.spi.Boundaries;
import org.hibernate.jpa.boot.discovery.spi.Discovery;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

import static org.hibernate.boot.jaxb.internal.MappingBinder.NON_VALIDATING;

/// Standard implementation of [Discovery].
///
/// @author Steve Ebersole
public class StandardDiscovery implements Discovery {
	protected final ArchiveDescriptorFactory archiveDescriptorFactory;
	protected final ClassLoaderService classLoaderService;
	private final ResourceLocator resourceLocator;

	public StandardDiscovery(ArchiveDescriptorFactory archiveDescriptorFactory, ClassLoaderService classLoaderService) {
		this.archiveDescriptorFactory = archiveDescriptorFactory;
		this.classLoaderService = classLoaderService;
		this.resourceLocator = new ResourceLocatorImpl( classLoaderService );
	}

	@Override
	public void discoverClassNames(Boundaries boundaries, Consumer<String> classNameConsumer) {
		if ( !CollectionHelper.isNotEmpty( boundaries.getMappingFiles() ) ) {
			processMappingFiles( boundaries.getMappingFiles(), classNameConsumer );
		}

		// by default, we do not scan url boundaries here.  we leave that for other impls.
	}

	protected void processMappingFiles(List<String> mappingFiles, Consumer<String> classNameConsumer) {
		var binder = new MappingBinder( classLoaderService, NON_VALIDATING );

		mappingFiles.forEach( mappingFile -> {
			final URL mappingFileUrl = resourceLocator.locateResource( mappingFile );
			try (final InputStream mappingFileStream = mappingFileUrl.openStream()) {
				final Binding<JaxbBindableMappingDescriptor> binding = binder.bind(
						mappingFileStream,
						new Origin( SourceType.URL, mappingFileUrl.toExternalForm() )
				);
				processMappingFile( (JaxbEntityMappingsImpl) binding.getRoot(), classNameConsumer );
			}
			catch (IOException e) {
				throw new HibernateException( "Unable to open stream for " + mappingFileUrl, e );
			}
		} );
	}

	protected static void processMappingFile(JaxbEntityMappingsImpl root, Consumer<String> classNameConsumer) {
		root.getEntities().forEach( (jaxbEntity) -> {
			if ( StringHelper.isNotEmpty( jaxbEntity.getClazz() ) ) {
				classNameConsumer.accept( jaxbEntity.getClazz() );
			}
		} );

		root.getMappedSuperclasses().forEach( (jaxbMappedSuperclass) -> {
			classNameConsumer.accept( jaxbMappedSuperclass.getClazz() );
		} );

		root.getEmbeddables().forEach( (jaxbEmbeddable) -> {
			if ( StringHelper.isNotEmpty( jaxbEmbeddable.getClazz() ) ) {
				classNameConsumer.accept( jaxbEmbeddable.getClazz() );
			}
		} );

		root.getConverters().forEach( (jaxbConverter) -> {
			classNameConsumer.accept( jaxbConverter.getClazz() );
		} );

		root.getConverterRegistrations().forEach( (jaxbConverterRegistration) -> {
			classNameConsumer.accept( jaxbConverterRegistration.getClazz() );
		} );

		root.getJavaTypeRegistrations().forEach( (jaxbJavaTypeRegistration) -> {
			classNameConsumer.accept( jaxbJavaTypeRegistration.getClazz() );
		} );

		root.getJdbcTypeRegistrations().forEach( (jaxbJdbcTypeRegistration) -> {
			classNameConsumer.accept( jaxbJdbcTypeRegistration.getDescriptor() );
		} );

		root.getUserTypeRegistrations().forEach( (jaxbUserTypeRegistration) -> {
			classNameConsumer.accept( jaxbUserTypeRegistration.getDescriptor() );
		} );

		root.getCompositeUserTypeRegistrations().forEach( (jaxbCompositeUserTypeRegistration) -> {
			classNameConsumer.accept( jaxbCompositeUserTypeRegistration.getDescriptor() );
		} );

		root.getCollectionUserTypeRegistrations().forEach( (jaxbCollectionTypeRegistration) -> {
			classNameConsumer.accept( jaxbCollectionTypeRegistration.getDescriptor() );
		} );
	}

	private record ResourceLocatorImpl(ClassLoaderService classLoaderService) implements ResourceLocator {
		@Override
			public URL locateResource(String resourceName) {
				// first try it as a full path file name
				try {
					var asFile = new File( resourceName );
					if ( asFile.exists() ) {
						return asFile.toURI().toURL();
					}
				}
				catch (MalformedURLException e) {
					throw new HibernateException( "Unable to locate resource " + resourceName, e );
				}

				// otherwise, try it as classpath resource name
				var classpathResource = classLoaderService.locateResource( resourceName );
				if ( classpathResource != null ) {
					return classpathResource;
				}

				throw new HibernateException( "Unable to locate resource " + resourceName );
			}
		}
}
