/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
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
