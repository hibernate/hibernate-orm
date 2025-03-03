/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.archive.spi;

import java.net.URL;

/**
 * Contract for building ArchiveDescriptor instances.
 *
 * @author Steve Ebersole
 */
public interface ArchiveDescriptorFactory {
	/**
	 * Build a descriptor of the archive indicated by the given url
	 *
	 * @param url The url to the archive
	 *
	 * @return The descriptor
	 */
	ArchiveDescriptor buildArchiveDescriptor(URL url);

	/**
	 * Build a descriptor of the archive indicated by the path relative to the given url
	 *
	 * @param url The url to the archive
	 * @param path The path within the given url that refers to the archive
	 *
	 * @return The descriptor
	 */
	ArchiveDescriptor buildArchiveDescriptor(URL url, String path);

	/**
	 * Given a URL which defines an entry within a JAR (really any "bundled archive" such as a jar file, zip, etc)
	 * and an entry within that JAR, find the URL to the JAR itself.
	 *
	 * @param url The URL to an entry within a JAR
	 * @param entry The entry that described the thing referred to by the URL relative to the JAR
	 *
	 * @return The URL to the JAR
	 *
	 * @throws IllegalArgumentException Generally indicates a problem  with malformed urls.
	 */
	URL getJarURLFromURLEntry(URL url, String entry) throws IllegalArgumentException;

}
