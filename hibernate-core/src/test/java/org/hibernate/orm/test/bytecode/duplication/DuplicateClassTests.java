/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.duplication;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;


/**
 * @author Steve Ebersole
 */
@BaseUnitTest
@RequiresDialect( H2Dialect.class )
public class DuplicateClassTests {
	private ClassLoader originalTccl;

	@BeforeEach
	void storeOriginalTccl() {
		originalTccl = Thread.currentThread().getContextClassLoader();
	}

	@AfterEach
	void restoreOriginalTccl() {
		Thread.currentThread().setContextClassLoader( originalTccl );
	}

	@Test
	void testSimple(@TempDir File tempDir)
			throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		ArchivePath path = ArchivePaths.create( "META-INF/persistence.xml" );

		JavaArchive jar1 = ShrinkWrap.create( JavaArchive.class, "jar1.jar" );
		jar1.addClasses( SimpleEntity.class );
		jar1.addAsResource( "units/bytecode/duplication/pu1.xml", path );
		File jar1File = new File( tempDir, "jar1.jar" );
		jar1.as( ZipExporter.class ).exportTo( jar1File, true );

		JavaArchive jar2 = ShrinkWrap.create( JavaArchive.class, "jar2.jar" );
		jar2.addClasses( SimpleEntity.class );
		jar2.addAsResource( "units/bytecode/duplication/pu2.xml", path );
		File jar2File = new File( tempDir, "jar2.jar" );
		jar2.as( ZipExporter.class ).exportTo( jar2File, true );

		URL[] jarUrls = new URL[] {
				jar1File.toURI().toURL(),
				jar2File.toURI().toURL()
		};
		final URLClassLoader classLoader = new URLClassLoader( jarUrls, getClass().getClassLoader() );
		Thread.currentThread().setContextClassLoader( classLoader );

		try (EntityManagerFactory emf1 = Persistence.createEntityManagerFactory( "unit-1" )) {
			executeUseCases( classLoader, emf1 );
		}
		try (EntityManagerFactory emf2 = Persistence.createEntityManagerFactory( "unit-2" )) {
			executeUseCases( classLoader, emf2 );
		}

		{
			JavaArchive jar3 = ShrinkWrap.create( JavaArchive.class, "jar3.jar" );
			jar3.addClasses( SimpleEntity.class );
			jar3.addAsResource( "units/bytecode/duplication/pu1.xml", path );
			File jar3File = new File( tempDir, "jar3.jar" );
			jar3.as( ZipExporter.class ).exportTo( jar3File, true );

			URL[] secondaryUrls = new URL[] { jar3File.toURI().toURL() };
			final URLClassLoader secondaryClassLoader = new URLClassLoader( secondaryUrls, getClass().getClassLoader() );
			Thread.currentThread().setContextClassLoader( secondaryClassLoader );

			try (EntityManagerFactory emf3 = Persistence.createEntityManagerFactory( "unit-1" )) {
				executeUseCases( classLoader, emf3 );
			}
		}
	}

	private void executeUseCases(URLClassLoader classLoader, EntityManagerFactory entityManagerFactory)
			throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		final Class<?> clientClass = classLoader.loadClass( "org.hibernate.orm.test.bytecode.duplication.Client" );
		final Method executeMethod = clientClass.getDeclaredMethod( "execute", EntityManagerFactory.class );
		final Method cleanupMethod = clientClass.getDeclaredMethod( "cleanup", EntityManagerFactory.class );

		try {
			executeMethod.invoke( null, entityManagerFactory );
		}
		finally {
			cleanupMethod.invoke( null, entityManagerFactory );
		}

	}
}
