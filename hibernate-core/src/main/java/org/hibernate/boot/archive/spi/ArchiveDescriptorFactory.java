/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.spi;

import java.net.URL;

/// Contract for building ArchiveDescriptor instances.
/// Generally, only the [#buildArchiveDescriptor(URL)] form should be used.
///
/// @author Steve Ebersole
public interface ArchiveDescriptorFactory {

	/// Build a descriptor of the archive indicated by the given url
	///
	/// @param url The url to the archive
	///
	/// @return The descriptor
	ArchiveDescriptor buildArchiveDescriptor(URL url);

	/// Build a descriptor of the archive indicated by the path relative to the given url
	///
	/// @param url The url to the archive
	/// @param path The path within the given url that refers to the archive
	///
	/// @return The descriptor
	ArchiveDescriptor buildArchiveDescriptor(URL url, String path);
}
