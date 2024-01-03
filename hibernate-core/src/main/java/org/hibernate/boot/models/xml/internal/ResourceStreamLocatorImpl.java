/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.models.spi.ClassLoading;

/**
 * Adapts {@linkplain ClassLoading} as a {@linkplain ResourceStreamLocator}
 *
 * @author Steve Ebersole
 */
public class ResourceStreamLocatorImpl implements ResourceStreamLocator {
	private final ClassLoading classLoadingAccess;

	public ResourceStreamLocatorImpl(ClassLoading classLoadingAccess) {
		this.classLoadingAccess = classLoadingAccess;
	}

	@Override
	public InputStream locateResourceStream(String resourceName) {
		final URL resource = classLoadingAccess.locateResource( resourceName );
		try {
			return resource.openStream();
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}
}
