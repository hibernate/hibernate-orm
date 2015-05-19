/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util;

/**
 * This exists purely to allow custom ClassLoaders to be injected and used
 * prior to ServiceRegistry and ClassLoadingService existence.  This should be
 * replaced in Hibernate 5.
 * 
 * @author Brett Meyer
 */
public final class ClassLoaderHelper {
	private ClassLoaderHelper() {
	}

	public static ClassLoader overridenClassLoader;
	
	public static ClassLoader getContextClassLoader() {
		return overridenClassLoader != null ?
			overridenClassLoader : Thread.currentThread().getContextClassLoader();
	}
}
