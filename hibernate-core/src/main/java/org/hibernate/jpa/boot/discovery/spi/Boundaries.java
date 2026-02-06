/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.discovery.spi;

import org.hibernate.jpa.boot.discovery.archive.spi.ArchiveDescriptorFactory;

import java.net.URL;
import java.util.List;

/// Describes the boundaries for discovery.
///
/// @author Steve Ebersole
public interface Boundaries {
	/// List of URLs to use for discovery.
	///
	/// @see ArchiveDescriptorFactory#buildArchiveDescriptor
	List<URL> getUrls();

	/// Mapping files to be searched for class names.
	List<String> getMappingFiles();
}
