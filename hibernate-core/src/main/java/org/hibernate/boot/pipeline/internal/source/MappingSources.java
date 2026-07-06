/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.internal.source;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.hibernate.boot.pipeline.internal.settings.ResolvedBootstrapSettings;
import org.hibernate.boot.pipeline.internal.settings.ResolvedMappingSettings;
import org.hibernate.boot.pipeline.internal.settings.SettingsResolver;
import org.hibernate.boot.scan.spi.ScanningResult;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import jakarta.persistence.PersistenceConfiguration;

import static java.util.Collections.addAll;
import static org.hibernate.internal.util.StringHelper.qualifier;

/// Mapping sources collected from a bootstrap entry point.
///
/// This type intentionally carries source declarations only: managed classes,
/// discovered class and package names, and XML mappings.  Settings resolution
/// remains a separate concern handled by the orchestration layer, and scanning is
/// performed by entry-point adapters before this descriptor is consumed.
///
/// @since 9.0
/// @author Steve Ebersole
public class MappingSources {
	private final LinkedHashSet<Class<?>> managedClasses = new LinkedHashSet<>();
	private final LinkedHashSet<String> managedClassNames = new LinkedHashSet<>();
	private final LinkedHashSet<String> packageNames = new LinkedHashSet<>();
	private final LinkedHashSet<String> mappingResources = new LinkedHashSet<>();
	private final LinkedHashSet<URI> mappingFileUris = new LinkedHashSet<>();
	private final LinkedHashSet<URL> mappingFileUrls = new LinkedHashSet<>();
	private final LinkedHashSet<XmlMappingSource> xmlMappingSources = new LinkedHashSet<>();
	private boolean includeUnlistedStructuralTypes = true;

	public MappingSources() {
	}

	public MappingSources(
			Collection<Class<?>> managedClasses,
			Collection<String> mappingResources) {
		this( managedClasses, List.of(), List.of(), mappingResources, List.of(), List.of() );
	}

	public MappingSources(
			Collection<Class<?>> managedClasses,
			Collection<String> managedClassNames,
			Collection<String> packageNames,
			Collection<String> mappingResources,
			Collection<URI> mappingFileUris) {
		this( managedClasses, managedClassNames, packageNames, mappingResources, mappingFileUris, List.of() );
	}

	public MappingSources(
			Collection<Class<?>> managedClasses,
			Collection<String> managedClassNames,
			Collection<String> packageNames,
			Collection<String> mappingResources,
			Collection<URI> mappingFileUris,
			Collection<URL> mappingFileUrls) {
		this( managedClasses, managedClassNames, packageNames, mappingResources, mappingFileUris, mappingFileUrls, List.of() );
	}

	public MappingSources(
			Collection<Class<?>> managedClasses,
			Collection<String> managedClassNames,
			Collection<String> packageNames,
			Collection<String> mappingResources,
			Collection<URI> mappingFileUris,
			Collection<URL> mappingFileUrls,
			Collection<XmlMappingSource> xmlMappingSources) {
		this( managedClasses, managedClassNames, packageNames, mappingResources, mappingFileUris, mappingFileUrls, xmlMappingSources, true );
	}

	public MappingSources(
			Collection<Class<?>> managedClasses,
			Collection<String> managedClassNames,
			Collection<String> packageNames,
			Collection<String> mappingResources,
			Collection<URI> mappingFileUris,
			Collection<URL> mappingFileUrls,
			Collection<XmlMappingSource> xmlMappingSources,
			boolean includeUnlistedStructuralTypes) {
		addManagedClasses( managedClasses );
		addManagedClassNames( managedClassNames );
		addPackages( packageNames );
		addMappingResources( mappingResources );
		addMappingUris( mappingFileUris );
		addMappingUrls( mappingFileUrls );
		addXmlMappingSources( xmlMappingSources );
		this.includeUnlistedStructuralTypes = includeUnlistedStructuralTypes;
	}

	/// Add a managed class.
	public MappingSources addManagedClass(Class<?> managedClass) {
		if ( managedClass != null ) {
			managedClasses.add( managedClass );
		}
		return this;
	}

	/// Add managed classes.
	public MappingSources addManagedClasses(Class<?>... managedClasses) {
		if ( managedClasses != null && managedClasses.length > 0 ) {
			addAll( this.managedClasses, managedClasses );
		}
		return this;
	}

	/// Add managed classes.
	public MappingSources addManagedClasses(Collection<Class<?>> managedClasses) {
		if ( managedClasses != null ) {
			managedClasses.forEach( this::addManagedClass );
		}
		return this;
	}

	/// Add a managed class name without loading the class.
	public MappingSources addManagedClassName(String managedClassName) {
		if ( managedClassName != null ) {
			managedClassNames.add( managedClassName );
		}
		return this;
	}

	/// Add managed class names without loading the classes.
	public MappingSources addManagedClassNames(String... managedClassNames) {
		if ( managedClassNames != null && managedClassNames.length > 0 ) {
			addAll( this.managedClassNames, managedClassNames );
		}
		return this;
	}

	/// Add managed class names without loading the classes.
	public MappingSources addManagedClassNames(Collection<String> managedClassNames) {
		if ( managedClassNames != null ) {
			managedClassNames.forEach( this::addManagedClassName );
		}
		return this;
	}

	/// Add package-level metadata by package name.
	public MappingSources addPackage(String packageName) {
		if ( packageName != null ) {
			packageNames.add( packageName );
		}
		return this;
	}

	/// Add package-level metadata by package reference.
	public MappingSources addPackage(Package packageRef) {
		return packageRef == null ? this : addPackage( packageRef.getName() );
	}

	/// Add package-level metadata by package name.
	public MappingSources addPackages(Collection<String> packageNames) {
		if ( packageNames != null ) {
			packageNames.forEach( this::addPackage );
		}
		return this;
	}

	/// Add a classpath mapping resource name.
	public MappingSources addMappingResource(String mappingResource) {
		if ( mappingResource != null ) {
			mappingResources.add( mappingResource );
		}
		return this;
	}

	/// Add classpath mapping resource names.
	public MappingSources addMappingResources(String... mappingResources) {
		if ( mappingResources != null && mappingResources.length > 0 ) {
			addAll( this.mappingResources, mappingResources );
		}
		return this;
	}

	/// Add classpath mapping resource names.
	public MappingSources addMappingResources(Collection<String> mappingResources) {
		if ( mappingResources != null ) {
			mappingResources.forEach( this::addMappingResource );
		}
		return this;
	}

	/// Add a mapping file path.
	public MappingSources addMappingFile(Path mappingFile) {
		if ( mappingFile != null ) {
			mappingFileUris.add( mappingFile.toUri() );
		}
		return this;
	}

	/// Add a mapping file.
	public MappingSources addMappingFile(File mappingFile) {
		return mappingFile == null ? this : addMappingFile( mappingFile.toPath() );
	}

	/// Add a mapping file URI.
	public MappingSources addMappingUri(URI mappingFileUri) {
		if ( mappingFileUri != null ) {
			mappingFileUris.add( mappingFileUri );
		}
		return this;
	}

	/// Add mapping file URIs.
	public MappingSources addMappingUris(Collection<URI> mappingFileUris) {
		if ( mappingFileUris != null ) {
			mappingFileUris.forEach( this::addMappingUri );
		}
		return this;
	}

	/// Add a mapping file URL.
	public MappingSources addMappingUrl(URL mappingFileUrl) {
		if ( mappingFileUrl != null ) {
			mappingFileUrls.add( mappingFileUrl );
		}
		return this;
	}

	/// Add mapping file URLs.
	public MappingSources addMappingUrls(Collection<URL> mappingFileUrls) {
		if ( mappingFileUrls != null ) {
			mappingFileUrls.forEach( this::addMappingUrl );
		}
		return this;
	}

	/// Add a lazy XML mapping source.
	public MappingSources addXmlMappingSource(XmlMappingSource xmlMappingSource) {
		if ( xmlMappingSource != null ) {
			xmlMappingSources.add( xmlMappingSource );
		}
		return this;
	}

	/// Add lazy XML mapping sources.
	public MappingSources addXmlMappingSources(Collection<XmlMappingSource> xmlMappingSources) {
		if ( xmlMappingSources != null ) {
			xmlMappingSources.forEach( this::addXmlMappingSource );
		}
		return this;
	}

	public MappingSources includeUnlistedStructuralTypes(boolean includeUnlistedStructuralTypes) {
		this.includeUnlistedStructuralTypes = includeUnlistedStructuralTypes;
		return this;
	}

	/// Managed Java classes explicitly contributed by the entry point.
	public List<Class<?>> managedClasses() {
		return List.copyOf( managedClasses );
	}

	/// Managed Java class names contributed without loading the classes.
	public List<String> managedClassNames() {
		return List.copyOf( managedClassNames );
	}

	/// Package names contributed for package-level metadata.
	public List<String> packageNames() {
		return List.copyOf( packageNames );
	}

	/// XML mapping resources contributed by name.
	public List<String> mappingResources() {
		return List.copyOf( mappingResources );
	}

	/// XML mapping files contributed by URI.
	public List<URI> mappingFileUris() {
		return List.copyOf( mappingFileUris );
	}

	/// XML mapping files contributed by URL.
	public List<URL> mappingFileUrls() {
		return List.copyOf( mappingFileUrls );
	}

	/// XML mapping sources which bind lazily during source resolution.
	public List<XmlMappingSource> xmlMappingSources() {
		return List.copyOf( xmlMappingSources );
	}

	public boolean includeUnlistedStructuralTypes() {
		return includeUnlistedStructuralTypes;
	}

	public List<Class<?>> getManagedClasses() {
		return managedClasses();
	}

	public List<String> getManagedClassNames() {
		return managedClassNames();
	}

	public List<String> getPackageNames() {
		return packageNames();
	}

	public List<String> getMappingResources() {
		return mappingResources();
	}

	public List<URI> getMappingFileUris() {
		return mappingFileUris();
	}

	public List<URL> getMappingFileUrls() {
		return mappingFileUrls();
	}

	public List<XmlMappingSource> getXmlMappingSources() {
		return xmlMappingSources();
	}

	public static MappingSources from(MappingSources mappingSources) {
		return new MappingSources(
				mappingSources.managedClasses(),
				mappingSources.managedClassNames(),
				mappingSources.packageNames(),
				mappingSources.mappingResources(),
				mappingSources.mappingFileUris(),
				mappingSources.mappingFileUrls(),
				mappingSources.xmlMappingSources(),
				mappingSources.includeUnlistedStructuralTypes()
		);
	}

	/// Adapts Jakarta Persistence's programmatic bootstrap configuration to
	/// neutral mapping sources.
	public static MappingSources from(PersistenceConfiguration persistenceConfiguration) {
		if ( persistenceConfiguration instanceof HibernatePersistenceConfiguration hibernatePersistenceConfiguration ) {
			final var bootstrapSettings = SettingsResolver.resolveBootstrapSettings( hibernatePersistenceConfiguration );
			return from(
					hibernatePersistenceConfiguration,
					bootstrapSettings,
					SettingsResolver.resolveMappingSettings(
							bootstrapSettings,
							hibernatePersistenceConfiguration.defaultToOneFetchType()
					),
					(ContributionDiscoveryContext) null
			);
		}
		return new MappingSources(
				persistenceConfiguration.managedClasses(),
				persistenceConfiguration.mappingFiles()
		);
	}

	/// Adapts Hibernate's persistence-unit descriptor abstraction to neutral
	/// mapping sources.
	public static MappingSources from(PersistenceUnitDescriptor persistenceUnitDescriptor) {
		return from( persistenceUnitDescriptor, null, null );
	}

	/// Adapts Hibernate's persistence-unit descriptor abstraction to neutral
	/// mapping sources.
	public static MappingSources from(
			PersistenceUnitDescriptor persistenceUnitDescriptor,
			ResolvedBootstrapSettings bootstrapSettings,
			ContributionDiscoveryContext context) {
		final var managedClassNames = new ArrayList<String>();
		final var packageNames = new ArrayList<String>();
		for ( var className : persistenceUnitDescriptor.getAllClassNames() ) {
			if ( className.endsWith( ".package-info" ) ) {
				packageNames.add( qualifier( className ) );
			}
			else {
				managedClassNames.add( className );
			}
		}
		final var classLoaderService = context == null ? null : context.classLoaderService();
		final var scanningResult = classLoaderService == null || bootstrapSettings == null
				? ScanningResult.NONE
				: HibernatePersistenceConfigurationScanner.performScanning(
						persistenceUnitDescriptor,
						bootstrapSettings,
						classLoaderService
				);
		managedClassNames.addAll( scanningResult.discoveredClasses() );
		packageNames.addAll( scanningResult.discoveredPackages() );

		final var mappingResources = new ArrayList<>( persistenceUnitDescriptor.getMappingFileNames() );

		final var mappingFileUrls = classLoaderService == null
				? List.<URL>of()
				: classLoaderService.locateResources( "META-INF/orm.xml" );

		return new MappingSources(
				List.of(),
				managedClassNames,
				packageNames,
				mappingResources,
				scanningResult.mappingFiles(),
				mappingFileUrls,
				List.of(),
				!persistenceUnitDescriptor.isExcludeUnlistedClasses()
		);
	}

	/// Adapts Hibernate's programmatic JPA bootstrap configuration to neutral
	/// mapping sources, including archive scanning.
	public static MappingSources from(
			HibernatePersistenceConfiguration persistenceConfiguration,
			ResolvedBootstrapSettings bootstrapSettings,
			ResolvedMappingSettings mappingSettings,
			ContributionDiscoveryContext context) {
		if ( context == null ) {
			return new MappingSources(
					persistenceConfiguration.managedClasses(),
					persistenceConfiguration.managedClassNames(),
					persistenceConfiguration.packageNames(),
					persistenceConfiguration.mappingFiles(),
					persistenceConfiguration.mappingFileUris(),
					persistenceConfiguration.mappingFileUrls()
			);
		}
		final ScanningResult scanningResult = HibernatePersistenceConfigurationScanner.performScanning(
				persistenceConfiguration,
				bootstrapSettings,
				mappingSettings,
				context.classLoaderService()
		);
		final var managedClassNames = new ArrayList<>( persistenceConfiguration.managedClassNames() );
		managedClassNames.addAll( scanningResult.discoveredClasses() );
		final var packageNames = new ArrayList<>( persistenceConfiguration.packageNames() );
		packageNames.addAll( scanningResult.discoveredPackages() );
		final var mappingFileUris = new ArrayList<>( persistenceConfiguration.mappingFileUris() );
		mappingFileUris.addAll( scanningResult.mappingFiles() );
		return new MappingSources(
				persistenceConfiguration.managedClasses(),
				managedClassNames,
				packageNames,
				persistenceConfiguration.mappingFiles(),
				mappingFileUris,
				persistenceConfiguration.mappingFileUrls()
		);
	}
}
