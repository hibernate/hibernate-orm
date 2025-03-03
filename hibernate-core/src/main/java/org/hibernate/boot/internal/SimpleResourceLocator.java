/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import java.net.URL;

import org.hibernate.boot.ResourceLocator;

/**
 * Simple ResourceLocator impl using its own ClassLoader to locate the resource
 *
 * @author Steve Ebersole
 */
public class SimpleResourceLocator implements ResourceLocator {
	/**
	 * Singleton access
	 */
	public static final SimpleResourceLocator INSTANCE = new SimpleResourceLocator();

	@Override
	public URL locateResource(String resourceName) {
		return getClass().getClassLoader().getResource( resourceName );
	}
}
