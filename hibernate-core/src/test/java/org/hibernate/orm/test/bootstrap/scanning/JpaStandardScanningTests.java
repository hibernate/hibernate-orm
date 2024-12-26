/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.scanning;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.jpa.pack.scan.CheeseBall;
import org.hibernate.orm.test.jpa.pack.scan.CheeseSpread;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * Tests for bootstrapping a SessionFactory using standard JPA behavior,
 * especially with regard to finding XML descriptors
 *
 * @author Steve Ebersole
 */
@RequiresDialect(value = H2Dialect.class, comment = "Nothing dialect/database sensitive here, so limit to H2 job")
public class JpaStandardScanningTests extends BaseSessionFactoryFunctionalTest {

	@Test
	void testJustPersistenceXml() throws Exception {
		File standaloneJar = buildStandalonePersistenceUnit();
		addPackageToClasspath( standaloneJar );

		try (EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpa-scanning-standalone" )) {
			try (EntityManager entityManager = emf.createEntityManager()) {
				final List<CheeseBall> balls = entityManager.createQuery( "from CheeseBall", CheeseBall.class ).getResultList();
			}
		}
	}

	private File buildStandalonePersistenceUnit() {
		final String archiveFileName = "scan-standalone.jar";

		final JavaArchive archive = ShrinkWrap.create(  JavaArchive.class, archiveFileName );

		archive.addClasses( CheeseBall.class );

		final ArchivePath persistenceXmlPath = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addAsResource( "scan/META-INF/standalone-persistence.xml", persistenceXmlPath );

		final File archiveFile = new File( packageTargetDir, archiveFileName );
		archive.as( ZipExporter.class ).exportTo ( archiveFile, true );
		return archiveFile;
	}

	@Test
	void testPersistenceXmlAndOrmXml() throws Exception {
		final File combinedJr = buildCombinedPersistenceUnit();
		addPackageToClasspath( combinedJr );

		try (EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpa-scanning-combined" )) {
			try (EntityManager entityManager = emf.createEntityManager()) {
				final List<CheeseBall> balls = entityManager.createQuery( "from CheeseBall", CheeseBall.class ).getResultList();
				final List<CheeseSpread> spreads = entityManager.createQuery( "from CheeseSpread", CheeseSpread.class ).getResultList();
			}
		}
	}

	private File buildCombinedPersistenceUnit() {
		final String archiveFileName = "scan-combined.jar";

		final JavaArchive archive = ShrinkWrap.create(  JavaArchive.class, archiveFileName );

		archive.addClasses( CheeseSpread.class );

		final ArchivePath persistenceXmlPath = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addAsResource( "scan/META-INF/combined-persistence.xml", persistenceXmlPath );

		final ArchivePath ormXmlPath = ArchivePaths.create( "META-INF/orm.xml" );
		archive.addAsResource( "scan/META-INF/combined-orm.xml", ormXmlPath );

		final File archiveFile = new File( packageTargetDir, archiveFileName );
		archive.as( ZipExporter.class ).exportTo ( archiveFile, true );
		return archiveFile;
	}

	protected void addPackageToClasspath(File... files) throws MalformedURLException {
		List<URL> urlList = new ArrayList<>();
		for ( File file : files ) {
			urlList.add( file.toURL() );
		}
		URLClassLoader classLoader = new URLClassLoader(
				urlList.toArray( new URL[urlList.size()] ), originalClassLoader
		);
		thread.setContextClassLoader( classLoader );
	}

	protected static ClassLoader originalClassLoader;
	private static Thread thread;

	protected static ClassLoader bundleClassLoader;
	protected static File packageTargetDir;

	static {
		thread = Thread.currentThread();
		originalClassLoader = thread.getContextClassLoader();
		// get a URL reference to something we now is part of the classpath (us)
		URL myUrl = originalClassLoader.getResource(
				PackagingTestCase.class.getName().replace( '.', '/' ) + ".class"
		);

		if ( myUrl == null ) {
			fail( "Unable to setup packaging test : could not resolve 'known class' url" );
		}

		int index = -1;
		if ( myUrl.getFile().contains( "target" ) ) {
			// assume there's normally a /target
			index = myUrl.getFile().lastIndexOf( "target" );
		}
		else if ( myUrl.getFile().contains( "bin" ) ) {
			// if running in some IDEs, may be in /bin instead
			index = myUrl.getFile().lastIndexOf( "bin" );
		}
		else if ( myUrl.getFile().contains( "out/test" ) ) {
			// intellij... intellij sets up project outputs little different
			int outIndex = myUrl.getFile().lastIndexOf( "out/test" );
			index = myUrl.getFile().lastIndexOf( '/', outIndex+1 );
		}

		if ( index < 0 ) {
			fail( "Unable to setup packaging test : could not interpret url" );
		}

		String baseDirPath = myUrl.getFile().substring( 0, index );
		File baseDir = new File( baseDirPath );

		File testPackagesDir = new File( baseDir, "target/bundles" );
		try {
			bundleClassLoader = new URLClassLoader( new URL[] { testPackagesDir.toURL() }, originalClassLoader );
		}
		catch ( MalformedURLException e ) {
			fail( "Unable to build custom class loader" );
		}
		packageTargetDir = new File( baseDir, "target/packages" );
		packageTargetDir.mkdirs();
	}

	@BeforeEach
	public void prepareTCCL() {
		// add the bundle class loader in order for ShrinkWrap to build the test package
		thread.setContextClassLoader( bundleClassLoader );
	}

	@AfterEach
	public void resetTCCL() {
		// reset the classloader
		thread.setContextClassLoader( originalClassLoader );
	}
}
