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
package org.hibernate.bytecode.internal.javassist;

import java.io.IOException;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import org.hibernate.HibernateException;

/**
 * @author Steve Ebersole
 */
public class TransformingClassLoader extends ClassLoader {
	private ClassLoader parent;
	private ClassPool classPool;

	/*package*/ TransformingClassLoader(ClassLoader parent, String[] classpath) {
		this.parent = parent;
		classPool = new ClassPool( true );
		for ( int i = 0; i < classpath.length; i++ ) {
			try {
				classPool.appendClassPath( classpath[i] );
			}
			catch ( NotFoundException e ) {
				throw new HibernateException(
						"Unable to resolve requested classpath for transformation [" +
						classpath[i] + "] : " + e.getMessage()
				);
			}
		}
	}

	protected Class findClass(String name) throws ClassNotFoundException {
        try {
            CtClass cc = classPool.get( name );
	        // todo : modify the class definition if not already transformed...
            byte[] b = cc.toBytecode();
            return defineClass( name, b, 0, b.length );
        }
        catch ( NotFoundException e ) {
            throw new ClassNotFoundException();
        }
        catch ( IOException e ) {
            throw new ClassNotFoundException();
        }
        catch ( CannotCompileException e ) {
            throw new ClassNotFoundException();
        }
    }

	public void release() {
		classPool = null;
		parent = null;
	}
}
