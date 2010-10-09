/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.util;

import java.io.InputStream;
import java.io.Serializable;

import junit.framework.TestCase;

import org.hibernate.bytecode.util.ByteCodeHelper;

/**
 * This is basically a test to assert the expectations of {@link org.hibernate.type.SerializableType}
 * in regards to deserializing bytes from second level caches.
 *
 * @author Steve Ebersole
 */
public class SerializationHelperTest extends TestCase {
	private ClassLoader original;
	private CustomClassLoader custom;

	protected void setUp() throws Exception {
		original = Thread.currentThread().getContextClassLoader();
		custom = new CustomClassLoader( original );
		Thread.currentThread().setContextClassLoader( custom );

	}

	protected void tearDown() throws Exception {
		Thread.currentThread().setContextClassLoader( original );
	}

	public void testSerializeDeserialize() throws Exception {
		Class clazz = Thread.currentThread().getContextClassLoader().loadClass( "org.hibernate.util.SerializableThing" );
		Object instance = clazz.newInstance();

		// SerializableType.toBytes() logic, as called from SerializableType.disassemble()
		byte[] bytes = SerializationHelper.serialize( (Serializable) instance );

		// SerializableType.fromBytes() logic, as called from SerializableType.assemble
		//		NOTE : specifically we use Serializable.class.getClassLoader for the CL in many cases
		//			which are the problematic cases
		Object instance2 = SerializationHelper.deserialize( bytes, Serializable.class.getClassLoader() );

		assertEquals( instance.getClass(), instance2.getClass() );
		assertEquals( instance.getClass().getClassLoader(), instance2.getClass().getClassLoader() );
		assertEquals( custom, instance2.getClass().getClassLoader() );
	}

	public static class CustomClassLoader extends ClassLoader {
		public CustomClassLoader(ClassLoader parent) {
			super( parent );
		}

		public Class loadClass(String name) throws ClassNotFoundException {
			if ( ! name.equals( "org.hibernate.util.SerializableThing" ) ) {
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
				byte[] bytecode = ByteCodeHelper.readByteCode( is );
				return defineClass( name, bytecode, 0, bytecode.length );
			}
			catch( Throwable t ) {
				throw new ClassNotFoundException( name + " not found", t );
			}
		}
	}
}
