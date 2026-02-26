/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.scan.spi;

import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;

import java.net.URL;

/// Coordinates discovery of managed classes.
///
/// @author Steve Ebersole
public interface Scanner {
	/// Performs scanning for a number of boundaries.  This form used from
	/// [org.hibernate.jpa.HibernatePersistenceConfiguration].
	///
	/// @param boundaries The boundaries for scanning.
	ScanningResult scan(URL... boundaries);

	/// Performs scanning using the expectations defined by the spec.
	///
	/// ```java
	/// var archiveDescriptor = factory.buildArchiveDescriptor(rootUrl);
	/// var persistenceXml = archiveDescriptor.findEntry("META-INF/persistence.xml");
	/// var stream = persistenceXmlEntry.getStreamAccess().accessInputStream();
	/// var persistenceXmlBinding = new ConfigurationBinder(...).bind(stream, ...);
	/// var jaxbPersistence = xmlBinding.getRoot();
	/// var jaxbPersistenceUnit = findUnit(jaxbPersistence, unitName);
	/// var scanResult = scanner.jpaScan(archiveDescriptor,jaxbPersistenceUnit);
	/// ```
	///
	/// todo (jpa4) : ideally, consider something like:
	///
	/// ```java
	/// var scanning = ...;
	/// var archive = scanning.buildArchiveDescriptor(rootUrl);
	/// var jaxbPersistenceUnit = scanning.locatePersistenceUnit(archive, unitName);
	/// var scanResult = scanner.jpaScan(archiveDescriptor,jaxbPersistenceUnit);
	/// ```
	///
	/// @param archiveDescriptor The [persistence root][jakarta.persistence.spi.PersistenceUnitInfo#getPersistenceUnitRootUrl]
	/// @param jaxbUnit The `<persistence-unit/>` to process.
	///
	/// @see org.hibernate.boot.archive.spi.ArchiveDescriptorFactory
	ScanningResult jpaScan(ArchiveDescriptor archiveDescriptor, JaxbPersistenceImpl.JaxbPersistenceUnitImpl jaxbUnit);
}
