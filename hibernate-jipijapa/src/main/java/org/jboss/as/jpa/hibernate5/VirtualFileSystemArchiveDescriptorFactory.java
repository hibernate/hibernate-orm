/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.jboss.as.jpa.hibernate5;

import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.boot.archive.internal.StandardArchiveDescriptorFactory;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;

import org.jboss.vfs.VFS;


/**
 * In Hibernate terms, the ArchiveDescriptorFactory contract is used to plug in handling for how to deal
 * with archives in various systems.  For JBoss, that means its VirtualFileSystem API.
 *
 * @author Steve Ebersole
 */
public class VirtualFileSystemArchiveDescriptorFactory extends StandardArchiveDescriptorFactory {
	@SuppressWarnings("WeakerAccess")
	public static final VirtualFileSystemArchiveDescriptorFactory INSTANCE = new VirtualFileSystemArchiveDescriptorFactory();

	@Override
	public ArchiveDescriptor buildArchiveDescriptor(URL url, String entryBase) {
		try {
			return new VirtualFileSystemArchiveDescriptor( VFS.getChild( url.toURI() ), entryBase );
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException( e );
		}
	}
}
