/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal.source;

import jakarta.annotation.Nonnull;
import jakarta.persistence.PersistenceConfiguration;
import org.hibernate.InvalidMappingException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.pipeline.internal.settings.ResolvedMappingSettings;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
		boolean includeUnlistedStructuralTypes) {

	public PreparedMappingSources(
			Collection<ClassDetails> managedClassDetails,
			Collection<ClassDetails> packageDetails,
			Collection<Binding<JaxbEntityMappingsImpl>> xmlMappings) {
		this( managedClassDetails, packageDetails, xmlMappings, true );
	}

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

		return new PreparedMappingSources(
				managedClassDetails,
				packageDetailsList,
				xmlBindings,
				!persistenceUnitDescriptor.isExcludeUnlistedClasses()
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
		mappingSources.managedClasses().forEach( (managedClass) -> {
			applyClassDetails(
					classDetailsRegistry.resolveClassDetails( managedClass.getName() ),
					managedClassDetails,
					packageDetailsList
			);
		} );
		mappingSources.managedClassNames().forEach( (managedClassName) -> {
			applyClassDetails(
					classDetailsRegistry.resolveClassDetails( managedClassName ),
					managedClassDetails,
					packageDetailsList
			);
		} );
		mappingSources.packageNames().forEach( (packageName) -> {
			applyPackageDetails( packageName, classDetailsRegistry, packageDetailsList );
		} );

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
				mappingSources.includeUnlistedStructuralTypes()
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

		return new PreparedMappingSources( managedClassDetails, packageDetailsList, xmlBindings );
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

	private static void applyPackageDetails(
			String packageName,
			ClassDetailsRegistry classDetailsRegistry,
			Collection<ClassDetails> packageDetails) {
		try {
			final ClassDetails packageInfoDetails = classDetailsRegistry.resolveClassDetails( packageName + ".package-info" );
			if ( packageInfoDetails.getClassName().endsWith( "package-info" ) ) {
				packageDetails.add( packageInfoDetails );
			}
		}
		catch (ClassLoadingException ignored) {
			// An annotated package name does not require a loadable package-info class.
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
