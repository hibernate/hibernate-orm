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
