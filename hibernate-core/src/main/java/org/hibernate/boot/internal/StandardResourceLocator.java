/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import java.net.URL;

import org.hibernate.boot.ResourceLocator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;

/**
 * Standard implementation of ResourceLocator delegating to the ClassLoaderService
 *
 * @author Steve Ebersole
 */
public class StandardResourceLocator implements ResourceLocator {
	private final ClassLoaderService classLoaderService;

	public StandardResourceLocator(ClassLoaderService classLoaderService) {
		this.classLoaderService = classLoaderService;
	}

	@Override
	public URL locateResource(String resourceName) {
		return classLoaderService.locateResource( resourceName );
	}
}
