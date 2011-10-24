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
package org.hibernate.test.util;
import java.io.InputStream;
import java.io.Serializable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.LockMode;
import org.hibernate.bytecode.spi.ByteCodeHelper;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * This is basically a test to assert the expectations of {@link org.hibernate.type.SerializableType}
 * in regards to deserializing bytes from second level caches.
 *
 * @author Steve Ebersole
 */
public class SerializationHelperTest extends BaseUnitTestCase {
	private ClassLoader original;
	private CustomClassLoader custom;

	@Before
	public void setUp() throws Exception {
		original = Thread.currentThread().getContextClassLoader();
		custom = new CustomClassLoader( original );
		Thread.currentThread().setContextClassLoader( custom );
	}

	@After
	public void tearDown() throws Exception {
		Thread.currentThread().setContextClassLoader( original );
	}

	@Test
	public void testSerializeDeserialize() throws Exception {
		Class clazz = Thread.currentThread().getContextClassLoader().loadClass( "org.hibernate.test.util.SerializableThing" );
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

	public void testSerDeserClassUnknownToCustomLoader() throws Exception {
		Object instance = LockMode.OPTIMISTIC;
		assertSame( 
			SerializationHelper.hibernateClassLoader(),
			instance.getClass().getClassLoader() 
		);

		// SerializableType.toBytes() logic, as called from SerializableType.disassemble()
		byte[] bytes = SerializationHelper.serialize( (Serializable) instance );

		// SerializableType.fromBytes() logic, as called from SerializableType.assemble
		// NOTE : specifically we use custom so that LockType.class is not found
		//        until the 3rd loader (because loader1 == loader2, the custom classloader)
		Object instance2 = SerializationHelper.deserialize( bytes, custom );

		assertSame( instance.getClass(), instance2.getClass() );
		assertSame( instance.getClass().getClassLoader(), instance2.getClass().getClassLoader() );
	}


	public static class CustomClassLoader extends ClassLoader {
		public CustomClassLoader(ClassLoader parent) {
			super( parent );
		}

		public Class loadClass(String name) throws ClassNotFoundException {
			if ( name.equals( "org.hibernate.LockMode" ) ) {
				throw new ClassNotFoundException( "Could not find "+ name );
			}
			if ( ! name.equals( "org.hibernate.test.util.SerializableThing" ) ) {
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
