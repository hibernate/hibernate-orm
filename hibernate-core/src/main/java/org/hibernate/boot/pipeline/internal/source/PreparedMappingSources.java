/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal.source;

import jakarta.persistence.PersistenceConfiguration;
import org.hibernate.InvalidMappingException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.pipeline.internal.settings.ResolvedMappingSettings;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.boot.model.process.internal.ManagedClassDetails;
import org.hibernate.boot.model.process.internal.ManagedResourceValidation;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModuleDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/// Prepared mapping sources ready for XML pre-processing and domain-model categorization.
/// This is the prepared form of [MappingSources].
///
/// @since 9.0
/// @author Steve Ebersole
public record PreparedMappingSources(
		Collection<ClassDetails> managedClassDetails,
		Collection<ClassDetails> packageDetails,
		Collection<Binding<JaxbEntityMappingsImpl>> xmlMappings,
		boolean includeUnlistedStructuralTypes,
		Collection<ModuleDetails> moduleDetails) {

	public PreparedMappingSources {
		managedClassDetails = managedClassDetails == null ? List.of() : List.copyOf( managedClassDetails );
		packageDetails = packageDetails == null ? List.of() : List.copyOf( packageDetails );
		xmlMappings = xmlMappings == null ? List.of() : List.copyOf( xmlMappings );
		moduleDetails = moduleDetails == null ? List.of() : List.copyOf( moduleDetails );
	}

	public PreparedMappingSources(
			Collection<ClassDetails> managedClassDetails,
			Collection<ClassDetails> packageDetails,
			Collection<Binding<JaxbEntityMappingsImpl>> xmlMappings,
			boolean includeUnlistedStructuralTypes) {
		this( managedClassDetails, packageDetails, xmlMappings, includeUnlistedStructuralTypes, List.of() );
	}

	public PreparedMappingSources(
			Collection<ClassDetails> managedClassDetails,
			Collection<ClassDetails> packageDetails,
			Collection<Binding<JaxbEntityMappingsImpl>> xmlMappings) {
		this( managedClassDetails, packageDetails, xmlMappings, true );
	}

	/// Creates prepared mapping sources from Hibernate's descriptor for persistence-unit
	/// information.
	///
	/// Managed class names are resolved through the supplied model context. Mapping
	/// file names are located through the bootstrap class-loading service and bound
	/// immediately.
	///
	/// @param persistenceUnitDescriptor The persistence-unit wrapper
	/// @param context Context used to resolve model details and load resources
	public static PreparedMappingSources from(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			MappingSourcePreparationContext context) {
		return from(
				persistenceUnitDescriptor,
				context,
				SettingsResolver.resolveMappingSettings(
						SettingsResolver.resolveBootstrapSettings( persistenceUnitDescriptor, Map.of() ),
						persistenceUnitDescriptor.getDefaultToOneFetchType()
				)
		);
	}

	/// Creates prepared mapping sources from Hibernate's descriptor for persistence-unit
	/// information.
	///
	/// Managed class names are resolved through the supplied model context. Mapping
	/// file names are located through the bootstrap class-loading service and bound
	/// immediately when XML mappings are enabled.
	///
	/// @param persistenceUnitDescriptor The persistence-unit wrapper
	/// @param context Context used to resolve model details and load resources
	/// @param mappingSettings Resolved mapping settings used during source collection
	public static PreparedMappingSources from(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			MappingSourcePreparationContext context,
			ResolvedMappingSettings mappingSettings) {
		return from( MappingSources.from( persistenceUnitDescriptor ), context, mappingSettings );
	}

	/// Creates prepared mapping sources from Hibernate's JPA
	/// {@link HibernatePersistenceConfiguration} extension.
	///
	/// The configuration is first adapted to [MappingSources], which
	/// includes archive scanning based on [HibernatePersistenceConfiguration#rootUrl()]
	/// and [HibernatePersistenceConfiguration#jarFileUrls()].
	///
	/// @param persistenceConfiguration The PersistenceConfiguration
	/// @param context Context used to resolve model details and load resources
	public static PreparedMappingSources from(
			HibernatePersistenceConfiguration persistenceConfiguration,
			MappingSourcePreparationContext context) {
		return from(
				persistenceConfiguration,
				context,
				SettingsResolver.resolveMappingSettings(
						SettingsResolver.resolveBootstrapSettings( persistenceConfiguration ),
						persistenceConfiguration.defaultToOneFetchType()
				)
		);
	}

	/// Creates prepared mapping sources from Hibernate's JPA
	/// {@link HibernatePersistenceConfiguration} extension.
	///
	/// The configuration is first adapted to [MappingSources], which
	/// includes archive scanning based on [HibernatePersistenceConfiguration#rootUrl()]
	/// and [HibernatePersistenceConfiguration#jarFileUrls()].
	///
	/// @param persistenceConfiguration The PersistenceConfiguration
	/// @param context Context used to resolve model details and load resources
	/// @param mappingSettings Resolved mapping settings used during source collection
	public static PreparedMappingSources from(
			HibernatePersistenceConfiguration persistenceConfiguration,
			MappingSourcePreparationContext context,
			ResolvedMappingSettings mappingSettings) {
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings( persistenceConfiguration );
		return from(
				MappingSources.from(
						persistenceConfiguration,
						bootstrapSettings,
						mappingSettings,
						new ContributionDiscoveryContext( context.getClassLoaderService() )
				),
				context,
				mappingSettings
		);
	}

	/// Creates prepared mapping sources from neutral bootstrap source declarations.
	///
	/// Explicit and discovered managed classes, package metadata, and mapping files
	/// are included.  Source discovery, including archive scanning, is expected to
	/// have already happened before these contributions are consumed.
	///
	/// @param mappingSources Source declarations from a bootstrap entry point
	/// @param context Context used to resolve model details and load resources
	/// @param mappingSettings Resolved mapping settings used during source collection
	public static PreparedMappingSources from(
			MappingSources mappingSources,
			MappingSourcePreparationContext context,
			ResolvedMappingSettings mappingSettings) {
		final var classLoading = context.getClassLoaderService();
		final var classDetailsRegistry = context.modelsContext().getClassDetailsRegistry();
		final var mappingFileBinder = context.createMappingBinder();

		var managedClassDetails = new ArrayList<ClassDetails>();
		var packageDetailsList = new ArrayList<ClassDetails>();
		mappingSources.managedClassDetails().forEach( details -> ManagedClassDetails.register( details, classDetailsRegistry ) );
		mappingSources.modules().forEach( context.modelsContext().getModuleDetailsRegistry()::resolveModuleDetails );
		mappingSources.packageNames().forEach( name -> applyPackageDetails( name, classDetailsRegistry, packageDetailsList ) );
		mappingSources.managedClasses().forEach( (managedClass) -> {
			applyClassDetails(
					ManagedClassDetails.resolve( managedClass, context.modelsContext() ),
					managedClassDetails
			);
		} );
		mappingSources.managedClassNames().forEach( (managedClassName) -> {
			applyClassDetails(
					classDetailsRegistry.resolveClassDetails( managedClassName ),
					managedClassDetails
			);
		} );
		mappingSources.managedClassDetails().forEach( details -> applyClassDetails( details, managedClassDetails ) );

		final var xmlBindings = new ArrayList<Binding<JaxbEntityMappingsImpl>>();
		if ( mappingSettings.xmlMappingEnabled() ) {
				mappingSources.mappingResources().forEach( (mappingResource) -> {
					final var origin = new Origin( SourceType.RESOURCE, mappingResource );
					try (var mappingFileStream = classLoading.locateResourceStream( mappingResource )) {
						xmlBindings.add( mappingFileBinder.bind(
								mappingFileStream,
								origin
						) );
					}
					catch (org.hibernate.boot.MappingException e) {
						throw new InvalidMappingException(
								"Could not parse mapping document: " + mappingResource,
								origin.getType().getLegacyTypeText(),
								origin.getName(),
								e
						);
					}
					catch (IOException e) {
						throw new RuntimeException( "Error accessing mapping resource - " + mappingResource, e );
					}
			} );
			mappingSources.mappingFileUris().forEach( (mappingFileUri) -> {
				xmlBindings.add( bindMappingFile( mappingFileUri, mappingFileBinder ) );
			} );
			mappingSources.mappingFileUrls().forEach( (mappingFileUrl) -> {
				xmlBindings.add( bindMappingFile( mappingFileUrl, mappingFileBinder ) );
			} );
			mappingSources.xmlMappingSources().forEach( (xmlMappingSource) ->
					xmlMappingSource.bind( mappingFileBinder, classLoading, xmlBindings::add ) );
		}

		return new PreparedMappingSources(
				managedClassDetails,
				packageDetailsList,
				xmlBindings,
				mappingSources.includeUnlistedStructuralTypes(),
			mappingSources.moduleNames().stream()
					.map( context.modelsContext().getModuleDetailsRegistry()::resolveModuleDetails )
					.toList()
		);
	}

	/// Creates resolved mapping sources from JPA {@link PersistenceConfiguration}.
	///
	/// Explicit managed classes and mapping files are included.  Archive scanning is
	/// not applied here.
	///
	/// @param persistenceConfiguration The PersistenceConfiguration
	/// @param context Context used to resolve model details and load resources
	public static PreparedMappingSources from(
			PersistenceConfiguration persistenceConfiguration,
			MappingSourcePreparationContext context) {
		final var settings = SettingsResolver.resolveBootstrapSettings( persistenceConfiguration.properties(), true );
		return from( MappingSources.from( persistenceConfiguration ), context,
				SettingsResolver.resolveMappingSettings( settings, persistenceConfiguration.defaultToOneFetchType() ) );
	}

	private static void applyClassDetails(
			ClassDetails classDetails,
			Collection<ClassDetails> managedClassDetails) {
		ManagedResourceValidation.validateClassName( classDetails.getName() );
		if ( !managedClassDetails.contains( classDetails ) ) {
			managedClassDetails.add( classDetails );
		}
	}

	private static void applyPackageDetails(
			String packageName,
			ClassDetailsRegistry classDetailsRegistry,
			Collection<ClassDetails> packageDetails) {
		packageDetails.add( classDetailsRegistry.resolveExplicitPackageDetails( packageName ) );
	}

	private static Binding<JaxbEntityMappingsImpl> bindMappingFile(
			URI mappingFile,
			MappingBinder mappingFileBinder) {
		try {
			return org.hibernate.boot.jaxb.internal.UrlXmlSource.fromUrl( mappingFile.toURL(), mappingFileBinder );
		}
		catch (MalformedURLException e) {
			throw new RuntimeException( "Error accessing mapping file - " + mappingFile, e );
		}
	}

	private static Binding<JaxbEntityMappingsImpl> bindMappingFile(
			URL mappingFile,
			MappingBinder mappingFileBinder) {
		return org.hibernate.boot.jaxb.internal.UrlXmlSource.fromUrl( mappingFile, mappingFileBinder );
	}
}
