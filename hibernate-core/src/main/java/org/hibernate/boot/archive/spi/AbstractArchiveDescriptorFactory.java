/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.archive.spi;

import java.net.URL;

import org.hibernate.boot.archive.internal.ArchiveHelper;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractArchiveDescriptorFactory implements ArchiveDescriptorFactory {
	@Override
	public ArchiveDescriptor buildArchiveDescriptor(URL url) {
		return buildArchiveDescriptor( url, "" );
	}

	@Override
	public URL getJarURLFromURLEntry(URL url, String entry) throws IllegalArgumentException {
		return ArchiveHelper.getJarURLFromURLEntry( url, entry );
	}

	@Override
	public URL getURLFromPath(String jarPath) {
		return ArchiveHelper.getURLFromPath( jarPath );
	}
}
