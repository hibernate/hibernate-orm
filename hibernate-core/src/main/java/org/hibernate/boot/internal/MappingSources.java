/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import static java.util.Collections.addAll;

/// Internal collector for ORM mapping source declarations.
///
/// This collects declarations only; it does not bind XML, categorize the domain
/// model, build metadata, or construct a SessionFactory.  The user-facing native
/// bootstrap API is [org.hibernate.boot.HibernateBootstrap].
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

	/// Add a managed class.
	public MappingSources addManagedClass(Class<?> managedClass) {
		managedClasses.add( managedClass );
		return this;
	}

	/// Add managed classes.
	public MappingSources addManagedClasses(Class<?>... managedClasses) {
		if ( managedClasses != null && managedClasses.length > 0 ) {
			addAll( this.managedClasses, managedClasses );
		}
		return this;
	}

	/// Add a managed class name without loading the class.
	public MappingSources addManagedClassName(String managedClassName) {
		managedClassNames.add( managedClassName );
		return this;
	}

	/// Add managed class names without loading the classes.
	public MappingSources addManagedClassNames(String... managedClassNames) {
		if ( managedClassNames != null && managedClassNames.length > 0 ) {
			addAll( this.managedClassNames, managedClassNames );
		}
		return this;
	}

	/// Add package-level metadata by package name.
	public MappingSources addPackage(String packageName) {
		packageNames.add( packageName );
		return this;
	}

	/// Add package-level metadata by package reference.
	public MappingSources addPackage(Package packageRef) {
		return addPackage( packageRef.getName() );
	}

	/// Add a classpath mapping resource name.
	public MappingSources addMappingResource(String mappingResource) {
		mappingResources.add( mappingResource );
		return this;
	}

	/// Add classpath mapping resource names.
	public MappingSources addMappingResources(String... mappingResources) {
		if ( mappingResources != null && mappingResources.length > 0 ) {
			addAll( this.mappingResources, mappingResources );
		}
		return this;
	}

	/// Add a mapping file path.
	public MappingSources addMappingFile(Path mappingFile) {
		mappingFileUris.add( mappingFile.toUri() );
		return this;
	}

	/// Add a mapping file.
	public MappingSources addMappingFile(File mappingFile) {
		return addMappingFile( mappingFile.toPath() );
	}

	/// Add a mapping file URI.
	public MappingSources addMappingUri(URI mappingFileUri) {
		mappingFileUris.add( mappingFileUri );
		return this;
	}

	/// Add a mapping file URL.
	public MappingSources addMappingUrl(URL mappingFileUrl) {
		mappingFileUrls.add( mappingFileUrl );
		return this;
	}

	/// Managed Java classes explicitly contributed by the entry point.
	public Collection<Class<?>> getManagedClasses() {
		return List.copyOf( managedClasses );
	}

	/// Managed Java class names contributed without loading the classes.
	public Collection<String> getManagedClassNames() {
		return List.copyOf( managedClassNames );
	}

	/// Package names contributed for package-level metadata.
	public Collection<String> getPackageNames() {
		return List.copyOf( packageNames );
	}

	/// XML mapping resources contributed by name.
	public Collection<String> getMappingResources() {
		return List.copyOf( mappingResources );
	}

	/// XML mapping files contributed by URI.
	public Collection<URI> getMappingFileUris() {
		return List.copyOf( mappingFileUris );
	}

	/// XML mapping files contributed by URL.
	public Collection<URL> getMappingFileUrls() {
		return List.copyOf( mappingFileUrls );
	}
}
