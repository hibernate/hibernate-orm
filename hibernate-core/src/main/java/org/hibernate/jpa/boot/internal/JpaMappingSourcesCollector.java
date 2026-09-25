/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.convert.internal.ConverterDescriptors;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.process.internal.ManagedResourceValidation;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.jpa.boot.spi.PersistenceConfigurationDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import jakarta.persistence.AttributeConverter;

import static org.hibernate.boot.BootLogging.BOOT_LOGGER;
import static org.hibernate.internal.util.StringHelper.split;

/// Collects JPA declarations and discoveries through the common [MetadataSources] machinery.
/// A collector belongs to one preparation operation. It suppresses automatic XML mappings
/// already registered explicitly, while preserving repeated explicit registrations.
/// Configuration-file mappings are applied separately, after metadata-builder contributors.
///
/// @since 8.0
/// @author Steve Ebersole
final class JpaMappingSourcesCollector {
	private final PersistenceUnitDescriptor persistenceUnit;
	private final StandardServiceRegistry standardServiceRegistry;
	private final Map<String, Object> configurationValues;
	private final boolean xmlMappingEnabled;
	private final Set<String> automaticMappingUrls = new HashSet<>();

	JpaMappingSourcesCollector(
			PersistenceUnitDescriptor persistenceUnit,
			StandardServiceRegistry standardServiceRegistry,
			Map<String, Object> configurationValues,
			boolean xmlMappingEnabled) {
		this.persistenceUnit = persistenceUnit;
		this.standardServiceRegistry = standardServiceRegistry;
		this.configurationValues = configurationValues;
		this.xmlMappingEnabled = xmlMappingEnabled;
	}

	/// Registers sources and returns the converters to apply before contributor callbacks.
	List<ConverterDescriptor<?, ?>> collect(MetadataSources sources, ScanningResult discovery) {
		applyMappingResources( sources );
		applyDiscoveredMappings( discovery, sources );
		return getConverterDescriptors( sources );
	}

	private void applyMappingResources(MetadataSources metadataSources) {
		assert persistenceUnit != null;
		if ( persistenceUnit instanceof PersistenceConfigurationDescriptor configuration ) {
			configuration.getManagedClasses().forEach( metadataSources::addAnnotatedClass );
		}

		persistenceUnit.getAllClassNames().forEach( name -> {
			ManagedResourceValidation.validateClassName(
					name, "getAllClassNames() entry '{class}'", "getAllPackageDescriptors() returning \"{package}\"", "getAllModuleDescriptors()" );
			metadataSources.addAnnotatedClassName( name );
		} );
		persistenceUnit.getAllPackageDescriptors().forEach( metadataSources::addPackageDescriptor );
		persistenceUnit.getAllModuleDescriptors().forEach( metadataSources::addModuleDescriptor );

		if ( !xmlMappingEnabled ) {
			BOOT_LOGGER.ignoringXmlMappings(
					persistenceUnit.getMappingFileNames().size(),
					MappingSettings.XML_MAPPING_ENABLED
			);
		}
		else {
			persistenceUnit.getMappingFileNames().forEach( name -> {
				metadataSources.addResource( name );
				final var explicitUrl = standardServiceRegistry.requireService( ClassLoaderService.class ).locateResource( name );
				if ( explicitUrl != null ) {
					automaticMappingUrls.add( explicitUrl.toExternalForm() );
				}
			} );
			addStandardMappings( metadataSources );

			// add any explicit hbm.xml references passed in
			final String explicitHbmXmls =
					(String) configurationValues.remove( AvailableSettings.HBM_XML_FILES );
			if ( explicitHbmXmls != null ) {
				for ( String hbmXml : split( ", ", explicitHbmXmls ) ) {
					metadataSources.addResource( hbmXml );
				}
			}
		}
	}

	private void addStandardMappings(MetadataSources metadataSources) {
		var ormXmlUrls = standardServiceRegistry.requireService( ClassLoaderService.class ).locateResources( "META-INF/orm.xml" );
		ormXmlUrls.forEach( url -> addAutomaticMapping( url, metadataSources ) );
	}

	private void addAutomaticMapping(URL url, MetadataSources sources) {
		if ( automaticMappingUrls.add( url.toExternalForm() ) ) {
			sources.addURL( url );
		}
	}

	private List<ConverterDescriptor<?, ?>> getConverterDescriptors(MetadataSources metadataSources) {
		final Object loadedClasses = configurationValues.remove( AvailableSettings.LOADED_CLASSES );
		if ( loadedClasses instanceof List<?> loadedAnnotatedClasses) {
			List<ConverterDescriptor<?, ?>> converterDescriptors = null;
			for ( Object annotatedClass : loadedAnnotatedClasses ) {
				if ( annotatedClass instanceof Class<?> converterClass ) {
					if ( AttributeConverter.class.isAssignableFrom( converterClass ) ) {
						if ( converterDescriptors == null ) {
							converterDescriptors = new ArrayList<>();
						}
						@SuppressWarnings("unchecked") // Safe, because we just checked!
						final var attributeConverterType = (Class<? extends AttributeConverter<?, ?>>) converterClass;
						converterDescriptors.add( ConverterDescriptors.of( attributeConverterType ) );
					}
					else {
						metadataSources.addAnnotatedClass( converterClass );
					}
				}
			}
			return converterDescriptors;
		}
		else {
			return null;
		}
	}

	private void applyDiscoveredMappings(ScanningResult scanningResult, MetadataSources metadataSources) {
		if ( !xmlMappingEnabled ) {
			return;
		}
		scanningResult.mappingFiles().forEach( (mappingFileUri) -> {
			try {
				addAutomaticMapping( mappingFileUri.toURL(), metadataSources );
			}
			catch (MalformedURLException e) {
				throw new HibernateException( "Unable to handle discovered mapping file : " + mappingFileUri, e );
			}
		} );
	}

}
