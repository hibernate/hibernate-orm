/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.registry.classloading.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.hibernate.bytecode.spi.ByteCodeHelper;

/**
 * A classloader isolated from the application classloader,
 * simulating a separate classloader that can create duplicate classes.
 * This can result in a Service implementation extending
 * a Service interface that is essentially the same as the one manipulated by ORM,
 * but is a different Class instance and is thus deemed different by the JVM.
 */
class IsolatedClassLoader extends ClassLoader {
	/**
	 * Another classloader from which resources will be read.
	 * Classes available in that classloader will be duplicated in the isolated classloader.
	 */
	private final ClassLoader resourceSource;

	IsolatedClassLoader(ClassLoader resourceSource) {
		super( getTopLevelClassLoader( resourceSource ) );
		this.resourceSource = resourceSource;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		InputStream is = getResourceAsStream( name.replace( '.', '/' ) + ".class" );
		if ( is == null ) {
			throw new ClassNotFoundException( name + " not found" );
		}

		try {
			byte[] bytecode = ByteCodeHelper.readByteCode( is );
			return defineClass( name, bytecode, 0, bytecode.length );
		}
		catch( Throwable t ) {
			throw new ClassNotFoundException( name + " not found", t );
		}
	}

	@Override
	public URL getResource(String name) {
		return resourceSource.getResource( name );
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		return resourceSource.getResources( name );
	}

	private static ClassLoader getTopLevelClassLoader(ClassLoader classFileSource) {
		ClassLoader result = classFileSource;
		while ( result.getParent() != null ) {
			result = result.getParent();
		}
		return result;
	}
}
