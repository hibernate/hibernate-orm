/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.boot;

import java.net.URL;

import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.ClassLoaderAccess;

/**
 * @author Steve Ebersole
 */
public class ClassLoaderAccessTestingImpl implements ClassLoaderAccess {
	/**
	 * Singleton access
	 */
	public static final ClassLoaderAccessTestingImpl INSTANCE = new ClassLoaderAccessTestingImpl();

	@Override
	@SuppressWarnings("unchecked")
	public <T> Class<T> classForName(String name) {
		try {
			return (Class<T>) getClass().getClassLoader().loadClass( name );
		}
		catch (ClassNotFoundException e) {
			throw new ClassLoadingException( "Could not load class by name : " + name, e );
		}
	}

	@Override
	public URL locateResource(String resourceName) {
		return getClass().getClassLoader().getResource( resourceName );
	}
}
