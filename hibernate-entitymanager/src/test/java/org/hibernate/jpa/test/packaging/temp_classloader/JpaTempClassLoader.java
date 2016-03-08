/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.jpa.test.packaging.temp_classloader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Janario Oliveira
 */
public class JpaTempClassLoader extends ClassLoader {
	private final ClassLoader delegate;

	public JpaTempClassLoader(ClassLoader delegate) {
		this.delegate = delegate;
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if ( name.startsWith( "java." ) || name.startsWith( "javax." ) ) {
			return Class.forName(name, resolve, delegate);
		}

		return findClass( name );
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> loaded = findLoadedClass( name );
		if ( loaded != null ) {
			return loaded;
		}

		String pathResource = name.replace( '.', '/' ) + ".class";
		try (InputStream input = delegate.getResourceAsStream( pathResource )) {
			if ( input == null ) {
				throw new ClassNotFoundException( name );
			}


			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int nRead;
			byte[] data = new byte[1024];
			while ( ( nRead = input.read( data, 0, data.length ) ) != -1 ) {
				baos.write( data, 0, nRead );
			}
			data = baos.toByteArray();


			return defineClass( name, data, 0, data.length );
		}
		catch (IOException e) {
			throw new ClassNotFoundException( name, e );
		}
	}
}
