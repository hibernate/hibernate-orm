/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.scan.spi;

import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;
import org.hibernate.boot.scan.internal.ScanningResultImpl;

import java.net.URI;
import java.net.URL;
import java.util.Set;

/// Defines the result from archive scanning.
///
/// @see Scanner#scan(URL...)
/// @see Scanner#jpaScan(ArchiveDescriptor, JaxbPersistenceImpl.JaxbPersistenceUnitImpl)
///
/// @author Steve Ebersole
public interface ScanningResult {
	/// Singleton access for "no results".
	ScanningResult NONE = new ScanningResultImpl();

	/// All discovered package names (without `package-info`).
	Set<String> discoveredPackages();

	/// All discovered class names.
	Set<String> discoveredClasses();

	/// All `META-INF/orm.xml` files discovered across all unit archives.
	Set<URI> mappingFiles();
}
