/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
