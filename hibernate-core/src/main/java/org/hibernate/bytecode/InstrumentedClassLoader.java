/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.bytecode;

import org.hibernate.bytecode.util.ByteCodeHelper;

import java.io.InputStream;

/**
 * A specialized classloader which performs bytecode enhancement on class
 * definitions as they are loaded into the classloader scope.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class InstrumentedClassLoader extends ClassLoader {

	private ClassTransformer classTransformer;

	public InstrumentedClassLoader(ClassLoader parent, ClassTransformer classTransformer) {
		super( parent );
		this.classTransformer = classTransformer;
	}

	public Class loadClass(String name) throws ClassNotFoundException {
		if ( name.startsWith( "java." ) || classTransformer == null ) {
			return getParent().loadClass( name );
		}

		Class c = findLoadedClass( name );
		if ( c != null ) {
			return c;
		}

		InputStream is = this.getResourceAsStream( name.replace( '.', '/' ) + ".class" );
		if ( is == null ) {
			throw new ClassNotFoundException( name + " not found" );
		}

		try {
			byte[] originalBytecode = ByteCodeHelper.readByteCode( is );
			byte[] transformedBytecode = classTransformer.transform( getParent(), name, null, null, originalBytecode );
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
