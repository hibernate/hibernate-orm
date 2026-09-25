package org.hibernate.boot.scan.spi;

import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;
import org.hibernate.boot.scan.internal.ScanningResultImpl;

import java.net.URI;
import java.net.URL;
import java.util.Set;

/// Categorized discoveries from archive scanning, excluding explicit declarations.
/// All collections are non-null and duplicate-free. Hibernate-produced results
/// are structurally immutable snapshots.
///
/// @see Scanner#scan(URL...)
/// @see Scanner#jpaScan(ArchiveDescriptor, JaxbPersistenceImpl.JaxbPersistenceUnitImpl)
///
/// @author Steve Ebersole
public interface ScanningResult {
	/// Singleton access for "no results".
	ScanningResult NONE = new ScanningResultImpl();

	/// All discovered module names (from `module-info.class` entries).
	Set<String> discoveredModules();

	/// All discovered package names (without `package-info`).
	Set<String> discoveredPackages();

	/// Discovered ordinary type names, never package-info or module-info.
	Set<String> discoveredClasses();

	/// All `META-INF/orm.xml` files discovered across all unit archives.
	Set<URI> mappingFiles();
}
