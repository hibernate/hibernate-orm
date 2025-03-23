/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.registry.classloading;

import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.envers.boot.spi.EnversMetadataBuildingContext;

/**
 * Utility class that facilitates loading of a class.
 *
 * @author Chris Cranford
 */
public final class ClassLoaderAccessHelper {

	/**
	 * Loads a class by name.
	 *
	 * @param metadataBuildingContext the metadata building context
	 * @param className the class to be loaded
	 * @return the loaded class instance
	 * @throws ClassLoadingException if there was an error loading the named class
	 */
	public static Class<?> loadClass(EnversMetadataBuildingContext metadataBuildingContext, String className) {
		ClassLoaderAccess classLoaderAccess = metadataBuildingContext.getBootstrapContext().getClassLoaderAccess();
		return classLoaderAccess.classForName( className );
	}

	private ClassLoaderAccessHelper() {
	}
}
