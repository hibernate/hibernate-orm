/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.javassist;

import java.io.IOException;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import org.hibernate.HibernateException;

/**
 * A ClassLoader implementation applying Class transformations as they are being loaded.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("UnusedDeclaration")
public class TransformingClassLoader extends ClassLoader {
	private ClassLoader parent;
	private ClassPool classPool;

	TransformingClassLoader(ClassLoader parent, String[] classpaths) {
		this.parent = parent;
		this.classPool = new ClassPool( true );
		for ( String classpath : classpaths ) {
			try {
				classPool.appendClassPath( classpath );
			}
			catch (NotFoundException e) {
				throw new HibernateException(
						"Unable to resolve requested classpath for transformation [" +
								classpath + "] : " + e.getMessage()
				);
			}
		}
	}

	@Override
	protected Class findClass(String name) throws ClassNotFoundException {
		try {
			final CtClass cc = classPool.get( name );
			// todo : modify the class definition if not already transformed...
			final byte[] b = cc.toBytecode();
			return defineClass( name, b, 0, b.length );
		}
		catch (NotFoundException e) {
			throw new ClassNotFoundException();
		}
		catch (IOException e) {
			throw new ClassNotFoundException();
		}
		catch (CannotCompileException e) {
			throw new ClassNotFoundException();
		}
	}

	/**
	 * Used to release resources.  Call when done with the ClassLoader
	 */
	public void release() {
		classPool = null;
		parent = null;
	}
}
