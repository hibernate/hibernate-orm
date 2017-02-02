/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.spi;

import java.io.InputStream;

/**
 * A specialized ClassLoader which performs bytecode enhancement on class definitions as they are loaded
 * into the ClassLoader scope.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class InstrumentedClassLoader extends ClassLoader {
	private final ClassTransformer classTransformer;

	/**
	 * Constructs an InstrumentedClassLoader.
	 *
	 * @param parent The parent ClassLoader
	 * @param classTransformer The transformer to use for applying enhancement
	 */
	public InstrumentedClassLoader(ClassLoader parent, ClassTransformer classTransformer) {
		super( parent );
		this.classTransformer = classTransformer;
	}

	@Override
	public Class loadClass(String name) throws ClassNotFoundException {
		if ( name.startsWith( "java." ) || classTransformer == null ) {
			return getParent().loadClass( name );
		}

		final Class c = findLoadedClass( name );
		if ( c != null ) {
			return c;
		}

		final InputStream is = this.getResourceAsStream( name.replace( '.', '/' ) + ".class" );
		if ( is == null ) {
			throw new ClassNotFoundException( name + " not found" );
		}

		try {
			final byte[] originalBytecode = ByteCodeHelper.readByteCode( is );
			final byte[] transformedBytecode = classTransformer.transform( getParent(), name, null, null, originalBytecode );
			if ( transformedBytecode == null ) {
				// no transformations took place, so handle it as we would a
				// non-instrumented class
				return getParent().loadClass( name );
			}
			else {
				return defineClass( name, transformedBytecode, 0, transformedBytecode.length );
			}
		}
		catch ( Throwable t ) {
			throw new ClassNotFoundException( name + " not found", t );
		}
	}
}
