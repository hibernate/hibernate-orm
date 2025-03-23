/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
