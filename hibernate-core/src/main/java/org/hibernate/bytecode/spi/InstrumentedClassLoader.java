/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
			if ( originalBytecode == transformedBytecode ) {
				// no transformations took place, so handle it as we would a
				// non-instrumented class
				return getParent().loadClass( name );
			}
			else {
				return defineClass( name, transformedBytecode, 0, transformedBytecode.length );
			}
		}
		catch( Throwable t ) {
			throw new ClassNotFoundException( name + " not found", t );
		}
	}
}
