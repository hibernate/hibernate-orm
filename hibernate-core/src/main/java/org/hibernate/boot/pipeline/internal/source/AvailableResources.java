/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal.source;

import jakarta.annotation.Nonnull;
import jakarta.persistence.PersistenceConfiguration;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.pipeline.internal.settings.ResolvedMappingSettings;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.models.spi.ClassDetails;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/// Pipeline implementation of [org.hibernate.boot.models.AvailableResources].
///
/// @apiNote Hibernate hbm.xml bindings are intentionally ignored here. 9.0 will
/// drop support for them altogether.
///
/// @since 9.0
/// @author Steve Ebersole
public record AvailableResources(
		Collection<ClassDetails> managedClassDetails,
		Collection<ClassDetails> packageDetails,
		Collection<Binding<JaxbEntityMappingsImpl>> xmlMappings)
		implements org.hibernate.boot.models.AvailableResources {

	@Nonnull
	public Collection<ClassDetails> managedClassDetails() {
		return managedClassDetails == null ? Collections.emptyList() : managedClassDetails;
	}

	@Nonnull
	public Collection<ClassDetails> packageDetails() {
		return packageDetails == null ? Collections.emptyList() : packageDetails;
	}

	@Nonnull
	public Collection<Binding<JaxbEntityMappingsImpl>> xmlMappings() {
		return xmlMappings == null ? Collections.emptyList() : xmlMappings;
	}

	/// Creates available resources from Hibernate's descriptor for persistence-unit
	/// information.
	///
	/// Managed class names are resolved through the supplied model context. Mapping
	/// file names are located through the bootstrap class-loading service and bound
	/// immediately.
	///
	/// @param persistenceUnitDescriptor The persistence-unit wrapper
	/// @param context Context used to resolve model details and load resources
	public static AvailableResources from(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			AvailableResourcesContext context) {
		return from(
				persistenceUnitDescriptor,
				context,
				SettingsResolver.resolveMappingSettings(
						SettingsResolver.resolveBootstrapSettings( persistenceUnitDescriptor, Map.of() ),
						persistenceUnitDescriptor.getDefaultToOneFetchType()
				)
		);
	}

	/// Creates available resources from Hibernate's descriptor for persistence-unit
	/// information.
	///
	/// Managed class names are resolved through the supplied model context. Mapping
	/// file names are located through the bootstrap class-loading service and bound
	/// immediately when XML mappings are enabled.
	///
	/// @param persistenceUnitDescriptor The persistence-unit wrapper
	/// @param context Context used to resolve model details and load resources
	/// @param mappingSettings Resolved mapping settings used during source collection
	public static AvailableResources from(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			AvailableResourcesContext context,
			ResolvedMappingSettings mappingSettings) {
		var classLoading = context.getClassLoaderService();
		var classDetailsRegistry = context.modelsContext().getClassDetailsRegistry();

		var managedClassDetails = new ArrayList<ClassDetails>();
		var packageDetailsList = new ArrayList<ClassDetails>();
		persistenceUnitDescriptor.getManagedClassNames().forEach( (managedClassName) -> {
			var classDetails = classDetailsRegistry.resolveClassDetails( managedClassName );
			if ( StringHelper.isEmpty( classDetails.getClassName() ) ) {
				managedClassDetails.add( classDetails );
			}
			else {
				applyClassDetails( classDetails, managedClassDetails, packageDetailsList );
			}
		} );

		final List<Binding<JaxbEntityMappingsImpl>> xmlBindings;
		if ( !mappingSettings.xmlMappingEnabled()
				|| persistenceUnitDescriptor.getMappingFileNames().isEmpty() ) {
			xmlBindings = Collections.emptyList();
		}
		else {
			xmlBindings = new ArrayList<>();

			var mappingFileBinder = context.createMappingBinder();
			persistenceUnitDescriptor.getMappingFileNames().forEach( (mappingFile) -> {
				try (var mappingFileStream = classLoading.locateResourceStream( mappingFile )) {
					xmlBindings.add( mappingFileBinder.bind(
							mappingFileStream,
							new Origin( SourceType.RESOURCE, mappingFile )
					) );
				}
				catch (IOException e) {
					throw new RuntimeException( "Error accessing mapping file - " + mappingFile, e );
				}
			} );
		}

		return new AvailableResources( managedClassDetails, packageDetailsList, xmlBindings );
	}

	/// Creates available resources from Hibernate's JPA
	/// {@link HibernatePersistenceConfiguration} extension.
	///
	/// The configuration is first adapted to [MappingSourceContributions], which
	/// includes archive scanning based on [HibernatePersistenceConfiguration#rootUrl()]
	/// and [HibernatePersistenceConfiguration#jarFileUrls()].
	///
	/// @param persistenceConfiguration The PersistenceConfiguration
	/// @param context Context used to resolve model details and load resources
	public static AvailableResources from(
			HibernatePersistenceConfiguration persistenceConfiguration,
			AvailableResourcesContext context) {
		return from(
				persistenceConfiguration,
				context,
				SettingsResolver.resolveMappingSettings(
						SettingsResolver.resolveBootstrapSettings( persistenceConfiguration ),
						persistenceConfiguration.defaultToOneFetchType()
				)
		);
	}

	/// Creates available resources from Hibernate's JPA
	/// {@link HibernatePersistenceConfiguration} extension.
	///
	/// The configuration is first adapted to [MappingSourceContributions], which
	/// includes archive scanning based on [HibernatePersistenceConfiguration#rootUrl()]
	/// and [HibernatePersistenceConfiguration#jarFileUrls()].
	///
	/// @param persistenceConfiguration The PersistenceConfiguration
	/// @param context Context used to resolve model details and load resources
	/// @param mappingSettings Resolved mapping settings used during source collection
	public static AvailableResources from(
			HibernatePersistenceConfiguration persistenceConfiguration,
			AvailableResourcesContext context,
			ResolvedMappingSettings mappingSettings) {
		final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings( persistenceConfiguration );
		return from(
				MappingSourceContributions.from(
						persistenceConfiguration,
						bootstrapSettings,
						mappingSettings,
						context.getClassLoaderService()
				),
				context,
				mappingSettings
		);
	}

	/// Creates available resources from neutral bootstrap source contributions.
	///
	/// Explicit and discovered managed classes, package metadata, and mapping files
	/// are included.  Source discovery, including archive scanning, is expected to
	/// have already happened before these contributions are consumed.
	///
	/// @param sourceContributions Source declarations from a bootstrap entry point
	/// @param context Context used to resolve model details and load resources
	/// @param mappingSettings Resolved mapping settings used during source collection
	public static AvailableResources from(
			MappingSourceContributions sourceContributions,
			AvailableResourcesContext context,
			ResolvedMappingSettings mappingSettings) {
		final var classLoading = context.getClassLoaderService();
		final var classDetailsRegistry = context.modelsContext().getClassDetailsRegistry();
		final var mappingFileBinder = context.createMappingBinder();

		var managedClassDetails = new ArrayList<ClassDetails>();
		var packageDetailsList = new ArrayList<ClassDetails>();
		sourceContributions.managedClasses().forEach( (managedClass) -> {
			applyClassDetails(
					classDetailsRegistry.resolveClassDetails( managedClass.getName() ),
					managedClassDetails,
					packageDetailsList
			);
		} );
		sourceContributions.managedClassNames().forEach( (managedClassName) -> {
			applyClassDetails(
					classDetailsRegistry.resolveClassDetails( managedClassName ),
					managedClassDetails,
					packageDetailsList
			);
		} );
		sourceContributions.packageNames().forEach( (packageName) -> {
			applyClassDetails(
					classDetailsRegistry.resolveClassDetails( packageName + ".package-info" ),
					managedClassDetails,
					packageDetailsList
			);
		} );

		final var xmlBindings = new ArrayList<Binding<JaxbEntityMappingsImpl>>();
		if ( mappingSettings.xmlMappingEnabled() ) {
			sourceContributions.mappingResources().forEach( (mappingResource) -> {
				try (var mappingFileStream = classLoading.locateResourceStream( mappingResource )) {
					xmlBindings.add( mappingFileBinder.bind(
							mappingFileStream,
							new Origin( SourceType.RESOURCE, mappingResource )
					) );
				}
				catch (IOException e) {
					throw new RuntimeException( "Error accessing mapping resource - " + mappingResource, e );
				}
			} );
			sourceContributions.mappingFileUris().forEach( (mappingFileUri) -> {
				xmlBindings.add( bindMappingFile( mappingFileUri, mappingFileBinder ) );
			} );
			sourceContributions.mappingFileUrls().forEach( (mappingFileUrl) -> {
				xmlBindings.add( bindMappingFile( mappingFileUrl, mappingFileBinder ) );
			} );
		}

		return new AvailableResources(
				managedClassDetails,
				packageDetailsList,
				xmlBindings
		);
	}

	/// Creates available resources from JPA {@link PersistenceConfiguration}.
	///
	/// Explicit managed classes and mapping files are included.  Archive scanning is
	/// not applied here.
	///
	/// @param persistenceConfiguration The PersistenceConfiguration
	/// @param context Context used to resolve model details and load resources
	public static AvailableResources from(
			PersistenceConfiguration persistenceConfiguration,
			AvailableResourcesContext context) {
		var classLoading = context.getClassLoaderService();
		var classDetailsRegistry = context.modelsContext().getClassDetailsRegistry();

		var managedClassDetails = new ArrayList<ClassDetails>();
		var packageDetailsList = new ArrayList<ClassDetails>();
		persistenceConfiguration.managedClasses().forEach( (managedClass) -> {
			var classDetails = classDetailsRegistry.resolveClassDetails( managedClass.getName() );
			if ( StringHelper.isEmpty( classDetails.getClassName() ) ) {
				managedClassDetails.add( classDetails );
			}
			else {
				applyClassDetails( classDetails, managedClassDetails, packageDetailsList );
			}
		} );

		final List<Binding<JaxbEntityMappingsImpl>> xmlBindings;
		if ( persistenceConfiguration.mappingFiles().isEmpty() ) {
			xmlBindings = Collections.emptyList();
		}
		else {
			xmlBindings = new ArrayList<>();

			var mappingFileBinder = context.createMappingBinder();
			persistenceConfiguration.mappingFiles().forEach( (mappingFile) -> {
				try (var mappingFileStream = classLoading.locateResourceStream( mappingFile )) {
					xmlBindings.add( mappingFileBinder.bind(
							mappingFileStream,
							new Origin( SourceType.RESOURCE, mappingFile )
					) );
				}
				catch (IOException e) {
					throw new RuntimeException( "Error accessing mapping file - " + mappingFile, e );
				}
			} );
		}

		return new AvailableResources( managedClassDetails, packageDetailsList, xmlBindings );
	}

	/// Creates available resources from Hibernate's native source accumulator.
	///
	/// Annotated classes, annotated class names, and package names are resolved
	/// through the supplied model context.  Mapping XML bindings already collected
	/// by {@code metadataSources} are carried forward directly.
	///
	/// @param metadataSources The native source accumulator
	/// @param context Context used to resolve model details
	public static AvailableResources from(
			MetadataSources metadataSources,
			AvailableResourcesContext context) {
		var classDetailsRegistry = context.modelsContext().getClassDetailsRegistry();

		var managedClassDetails = new ArrayList<ClassDetails>();
		var packageDetailsList = new ArrayList<ClassDetails>();
		metadataSources.getAnnotatedClasses().forEach( (annotatedClass) -> {
			applyClassDetails(
					classDetailsRegistry.resolveClassDetails( annotatedClass.getName() ),
					managedClassDetails,
					packageDetailsList
			);
		} );
		metadataSources.getAnnotatedClassNames().forEach( (annotatedClassName) -> {
			applyClassDetails(
					classDetailsRegistry.resolveClassDetails( annotatedClassName ),
					managedClassDetails,
					packageDetailsList
			);
		} );
		metadataSources.getAnnotatedPackages().forEach( (packageName) -> {
			applyClassDetails(
					classDetailsRegistry.resolveClassDetails( packageName + ".package-info" ),
					managedClassDetails,
					packageDetailsList
			);
		} );

		final var xmlBindings = new ArrayList<>( metadataSources.getMappingXmlBindings() );
		return new AvailableResources( managedClassDetails, packageDetailsList, xmlBindings );
	}

	private static void applyClassDetails(
			ClassDetails classDetails,
			Collection<ClassDetails> managedClassDetails,
			Collection<ClassDetails> packageDetails) {
		if ( StringHelper.isEmpty( classDetails.getClassName() ) ) {
			managedClassDetails.add( classDetails );
		}
		else if ( classDetails.getClassName().endsWith( "package-info" ) ) {
			packageDetails.add( classDetails );
		}
		else {
			managedClassDetails.add( classDetails );
		}
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
