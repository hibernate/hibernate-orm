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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.model.process.internal.ManagedResourceValidation;
import org.hibernate.models.spi.ClassDetails;

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
	private final LinkedHashMap<String, ClassDetails> managedDetails = new LinkedHashMap<>();
	private final LinkedHashMap<String, Module> modules = new LinkedHashMap<>();
	private final LinkedHashMap<String, Class<?>> managedClasses = new LinkedHashMap<>();
	private final LinkedHashSet<String> managedClassNames = new LinkedHashSet<>();
	private final LinkedHashSet<String> moduleNames = new LinkedHashSet<>();
	private final LinkedHashSet<String> packageNames = new LinkedHashSet<>();
	private final List<XmlMappingSource> xmlMappingSources = new ArrayList<>();
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

	public MappingSources addClassDetails(ClassDetails details) {
		ManagedResourceValidation.validateClassName( details.getName() );
		final var previous = managedDetails.putIfAbsent( details.getName(), details );
		if ( previous != null && previous != details ) {
			throw new MappingException( "Conflicting ClassDetails for '" + details.getName() + "'" );
		}
		return this;
	}

	public List<ClassDetails> managedClassDetails() {
		return List.copyOf( managedDetails.values() );
	}

	public List<Module> modules() {
		return List.copyOf( modules.values() );
	}

	public MappingSources addPackageDescriptor(String name) {
		return addPackage( name );
	}

	public MappingSources addModuleDescriptor(String name) {
		return addModule( name );
	}

	/// Add a managed class.
	public MappingSources addManagedClass(Class<?> managedClass) {
		if ( managedClass != null ) {
			ManagedResourceValidation.validateClassName( managedClass.getName() );
			final var previous = managedClasses.putIfAbsent( managedClass.getName(), managedClass );
			if ( previous != null && previous != managedClass ) {
				throw new MappingException( "Conflicting class handle for '" + managedClass.getName() + "'" );
			}
		}
		return this;
	}

	/// Add managed classes.
	public MappingSources addManagedClasses(Class<?>... managedClasses) {
		if ( managedClasses != null && managedClasses.length > 0 ) {
			java.util.Arrays.stream( managedClasses ).forEach( this::addManagedClass );
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
			ManagedResourceValidation.validateClassName( managedClassName );
			managedClassNames.add( managedClassName );
		}
		return this;
	}

	/// Add managed class names without loading the classes.
	public MappingSources addManagedClassNames(String... managedClassNames) {
		if ( managedClassNames != null && managedClassNames.length > 0 ) {
			java.util.Arrays.stream( managedClassNames ).forEach( this::addManagedClassName );
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
			packageNames.add( packageName.endsWith( "." ) ? packageName.substring( 0, packageName.length() - 1 ) : packageName );
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

	/// Add metadata from a named module.
	public MappingSources addModule(Module module) {
		if ( module == null || !module.isNamed() ) {
			throw new IllegalArgumentException( "An explicit module descriptor requires a named module" );
		}
		final var previous = modules.putIfAbsent( module.getName(), module );
		if ( previous != null && previous != module ) {
			throw new MappingException( "Conflicting module handle for '" + module.getName() + "'" );
		}
		return addModule( module.getName() );
	}

	/// Add metadata from a module name.
	public MappingSources addModule(String moduleName) {
		if ( moduleName != null ) {
			moduleNames.add( moduleName );
		}
		return this;
	}

	public MappingSources addModules(Collection<String> names) {
		if ( names != null ) {
			names.forEach( this::addModule );
		}
		return this;
	}

	public List<String> moduleNames() {
		return List.copyOf( moduleNames );
	}

	/// Add a classpath mapping resource name.
	public MappingSources addMappingResource(String mappingResource) {
		if ( mappingResource != null ) {
			xmlMappingSources.add( XmlMappingSource.fromResource( mappingResource ) );
		}
		return this;
	}

	/// Add classpath mapping resource names.
	public MappingSources addMappingResources(String... mappingResources) {
		if ( mappingResources != null && mappingResources.length > 0 ) {
			java.util.Arrays.stream( mappingResources ).forEach( this::addMappingResource );
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
			addMappingUri( mappingFile.toUri() );
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
			xmlMappingSources.add( XmlMappingSource.fromUri( mappingFileUri ) );
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
			xmlMappingSources.add( XmlMappingSource.fromUrl( mappingFileUrl ) );
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
		return List.copyOf( managedClasses.values() );
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
		return xmlMappingSources.stream().filter( XmlMappingSource.Resource.class::isInstance )
				.map( XmlMappingSource.Resource.class::cast ).map( XmlMappingSource.Resource::name ).toList();
	}

	/// XML mapping files contributed by URI.
	public List<URI> mappingFileUris() {
		return xmlMappingSources.stream().filter( XmlMappingSource.Uri.class::isInstance )
				.map( XmlMappingSource.Uri.class::cast ).map( XmlMappingSource.Uri::uri ).toList();
	}

	/// XML mapping files contributed by URL.
	public List<URL> mappingFileUrls() {
		return xmlMappingSources.stream().filter( XmlMappingSource.Url.class::isInstance )
				.map( XmlMappingSource.Url.class::cast ).map( XmlMappingSource.Url::url ).toList();
	}

	/// XML mapping sources which bind lazily during source resolution.
	public List<XmlMappingSource> xmlMappingSources() {
		return xmlMappingSources.stream().sorted( java.util.Comparator.comparingInt( XmlMappingSource::bindingOrder ) ).toList();
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
		final var copy = new MappingSources(
				mappingSources.managedClasses(),
				mappingSources.managedClassNames(),
				mappingSources.packageNames(),
				List.of(),
				List.of(),
				List.of(),
				mappingSources.xmlMappingSources(),
				mappingSources.includeUnlistedStructuralTypes()
		).addModules( mappingSources.moduleNames() );
		mappingSources.managedClassDetails().forEach( copy::addClassDetails );
		mappingSources.modules().forEach( copy::addModule );
		return copy;
	}

}
