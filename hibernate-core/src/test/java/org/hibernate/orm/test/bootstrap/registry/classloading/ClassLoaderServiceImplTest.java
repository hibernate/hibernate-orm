/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.registry.classloading;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.internal.TcclLookupPrecedence;

import org.hibernate.testing.orm.junit.JiraKey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Cédric Tabin
 */
public class ClassLoaderServiceImplTest {
	protected ClassLoader originalClassLoader;

	@BeforeEach
	public void setUp(){
		originalClassLoader = Thread.currentThread().getContextClassLoader();
	}

	@AfterEach
	public void tearDown(){
		Thread.currentThread().setContextClassLoader( originalClassLoader );
	}

	@Test
	public void testNullTCCL() {
		Thread.currentThread().setContextClassLoader( null );

		ClassLoaderServiceImpl csi1 = new ClassLoaderServiceImpl( null,
																TcclLookupPrecedence.BEFORE );
		Class<ClassLoaderServiceImplTest> clazz1 = csi1.classForName(
				ClassLoaderServiceImplTest.class.getName() );
		assertEquals( ClassLoaderServiceImplTest.class, clazz1 );
		csi1.stop();

		ClassLoaderServiceImpl csi2 = new ClassLoaderServiceImpl( null,
																TcclLookupPrecedence.AFTER );
		Class<ClassLoaderServiceImplTest> clazz2 = csi2.classForName(
				ClassLoaderServiceImplTest.class.getName() );
		assertEquals( ClassLoaderServiceImplTest.class, clazz2 );
		csi2.stop();

		ClassLoaderServiceImpl csi3 = new ClassLoaderServiceImpl( null,
																TcclLookupPrecedence.NEVER );
		Class<ClassLoaderServiceImplTest> clazz3 = csi3.classForName(
				ClassLoaderServiceImplTest.class.getName() );
		assertEquals( ClassLoaderServiceImplTest.class, clazz3 );
		csi3.stop();
	}

	@Test
	public void testLookupBefore() {
		InternalClassLoader icl = new InternalClassLoader();
		Thread.currentThread().setContextClassLoader( icl );

		ClassLoaderServiceImpl csi = new ClassLoaderServiceImpl( null,
																TcclLookupPrecedence.BEFORE );
		Class<ClassLoaderServiceImplTest> clazz = csi.classForName(
				ClassLoaderServiceImplTest.class.getName() );
		assertEquals( ClassLoaderServiceImplTest.class, clazz );
		assertEquals( 1, icl.getAccessCount() );
		csi.stop();
	}

	@Test
	public void testLookupAfterAvoided() {
		InternalClassLoader icl = new InternalClassLoader();
		Thread.currentThread().setContextClassLoader( icl );

		ClassLoaderServiceImpl csi = new ClassLoaderServiceImpl( null,
																TcclLookupPrecedence.AFTER );
		Class<ClassLoaderServiceImplTest> clazz = csi.classForName(
				ClassLoaderServiceImplTest.class.getName() );
		assertEquals( ClassLoaderServiceImplTest.class, clazz );
		assertEquals( 0, icl.getAccessCount() );
		csi.stop();
	}

	@Test
	public void testLookupAfter() {
		InternalClassLoader icl = new InternalClassLoader();
		Thread.currentThread().setContextClassLoader( icl );

		ClassLoaderServiceImpl csi = new ClassLoaderServiceImpl( null,
																TcclLookupPrecedence.AFTER );
		try {
			csi.classForName( "test.class.name" );
			assertTrue( false );
		}
		catch ( Exception e ) {
		}
		assertEquals( 0, icl.getAccessCount() );
		csi.stop();
	}

	@Test
	public void testLookupAfterNotFound() {
		InternalClassLoader icl = new InternalClassLoader();
		Thread.currentThread().setContextClassLoader( icl );

		ClassLoaderServiceImpl csi = new ClassLoaderServiceImpl( null,
																TcclLookupPrecedence.BEFORE );
		try {
			csi.classForName( "test.class.not.found" );
			assertTrue( false );
		}
		catch ( Exception e ) {
		}
		assertEquals( 0, icl.getAccessCount() );
		csi.stop();
	}

	@Test
	public void testLookupNever() {
		InternalClassLoader icl = new InternalClassLoader();
		Thread.currentThread().setContextClassLoader( icl );

		ClassLoaderServiceImpl csi = new ClassLoaderServiceImpl( null,
																TcclLookupPrecedence.NEVER );
		try {
			csi.classForName( "test.class.name" );
			assertTrue( false );
		}
		catch ( Exception e ) {
		}
		assertEquals( 0, icl.getAccessCount() );
		csi.stop();
	}

	@Test
	@JiraKey(value = "HHH-13551")
	public void testServiceFromIncompatibleClassLoader() {
		ClassLoaderServiceImpl classLoaderService = new ClassLoaderServiceImpl(
				Arrays.asList(
						getClass().getClassLoader(),
						/*
						 * This classloader will return instances of MyService where MyService
						 * is a different object than the one we manipulate in the current classloader.
						 * This used to throw an exception that triggered a boot failure in ORM,
						 * but should now be ignored.
						 */
						new IsolatedClassLoader( getClass().getClassLoader() )
				),
				TcclLookupPrecedence.AFTER
		);

		Collection<MyService> loadedServices = classLoaderService.loadJavaServices( MyService.class );

		assertEquals( 1, loadedServices.size() );
	}

	private static class InternalClassLoader extends ClassLoader {
		private List<String> names = new ArrayList<>(  );

		public InternalClassLoader() {
			super( null );
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			if ( name.startsWith( "org.hibernate" ) ) {
				names.add( name );
			}
			return super.loadClass( name );
		}

		@Override
		protected URL findResource(String name) {
			if ( name.startsWith( "org.hibernate" ) ) {
				names.add( name );
			}
			return super.findResource( name );
		}

		public int getAccessCount() {
			return names.size();
		}
	}
}
