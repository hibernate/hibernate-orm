/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.archive.spi;

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
	public ArchiveDescriptor buildArchiveDescriptor(URL url);

	/**
	 * Build a descriptor of the archive indicated by the path relative to the given url
	 *
	 * @param url The url to the archive
	 * @param path The path within the given url that refers to the archive
	 *
	 * @return The descriptor
	 */
	public ArchiveDescriptor buildArchiveDescriptor(URL url, String path);

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
	public URL getJarURLFromURLEntry(URL url, String entry) throws IllegalArgumentException;

	/**
	 * Not used!
	 *
	 * @param jarPath The jar path
	 *
	 * @return The url from the path?
	 *
	 * @deprecated Not used!
	 */
	@Deprecated
	@SuppressWarnings("UnusedDeclaration")
	public URL getURLFromPath(String jarPath);
}
