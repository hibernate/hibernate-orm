/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
