/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.osgi;

import java.net.URL;

import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveDescriptorFactory;

import org.osgi.framework.Bundle;

/**
 * ArchiveDescriptorFactory implementation for OSGi environments
 *
 * @author Brett Meyer
 * @author Tim Ward
 */
public class OsgiArchiveDescriptorFactory implements ArchiveDescriptorFactory {
	private Bundle persistenceBundle;

	/**
	 * Creates a OsgiArchiveDescriptorFactory
	 *
	 * @param persistenceBundle The OSGi bundle being scanned
	 */
	public OsgiArchiveDescriptorFactory(Bundle persistenceBundle) {
		this.persistenceBundle = persistenceBundle;
	}

	@Override
	public ArchiveDescriptor buildArchiveDescriptor(URL url) {
		return buildArchiveDescriptor( url, "" );
	}

	@Override
	public ArchiveDescriptor buildArchiveDescriptor(URL url, String entry) {
		return new OsgiArchiveDescriptor( persistenceBundle );
	}

	@Override
	public URL getJarURLFromURLEntry(URL url, String entry) throws IllegalArgumentException {
		// not used
		return null;
	}

	@Override
	public URL getURLFromPath(String jarPath) {
		// not used
		return null;
	}
}
