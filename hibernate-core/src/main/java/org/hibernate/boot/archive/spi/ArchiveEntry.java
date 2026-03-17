/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.spi;


import java.net.URI;

/// Represent an entry in the archive.
///
/// @author Steve Ebersole
public interface ArchiveEntry {
	/// The entry name.
	String getName();

	/// The relative name of the entry within the archive.
	/// Typically, what we are looking for here is the ClassLoader resource lookup name.
	///
	/// @return The name relative to the archive root
	String getNameWithinArchive();

	/// URI reference to the entry.  Useful for externalizing reference to the entry.
	URI getUri();

	/// Get access to the stream for the entry
	///
	/// @return Obtain stream access to the entry
	InputStreamAccess getStreamAccess();
}
