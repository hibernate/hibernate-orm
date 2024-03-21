/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.internal;

import java.net.URL;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.models.spi.ClassLoading;

/**
 * Adapts {@linkplain ClassLoaderService} to the {@linkplain ClassLoading} contract
 *
 * @author Steve Ebersole
 */
public class ClassLoaderServiceLoading implements ClassLoading {
	private final ClassLoaderService classLoaderService;

	public ClassLoaderServiceLoading(ClassLoaderService classLoaderService) {
		this.classLoaderService = classLoaderService;
	}

	@Override
	public <T> Class<T> classForName(String name) {
		return classLoaderService.classForName( name );
	}

	@Override
	public Package packageForName(String name) {
		return classLoaderService.packageForNameOrNull( name );
	}

	@Override
	public URL locateResource(String resourceName) {
		return classLoaderService.locateResource( resourceName );
	}
}
