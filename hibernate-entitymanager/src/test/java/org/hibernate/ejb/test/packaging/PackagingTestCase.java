// $Id$
/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.ejb.test.packaging;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Archives;
import org.jboss.shrinkwrap.api.exporter.ExplodedExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.hibernate.ejb.test.Cat;
import org.hibernate.ejb.test.Distributor;
import org.hibernate.ejb.test.Item;
import org.hibernate.ejb.test.Kitten;
import org.hibernate.ejb.test.pack.cfgxmlpar.Morito;
import org.hibernate.ejb.test.pack.defaultpar.ApplicationServer;
import org.hibernate.ejb.test.pack.defaultpar.IncrementListener;
import org.hibernate.ejb.test.pack.defaultpar.Lighter;
import org.hibernate.ejb.test.pack.defaultpar.Money;
import org.hibernate.ejb.test.pack.defaultpar.Mouse;
import org.hibernate.ejb.test.pack.defaultpar.OtherIncrementListener;
import org.hibernate.ejb.test.pack.defaultpar.Version;
import org.hibernate.ejb.test.pack.defaultpar_1_0.ApplicationServer1;
import org.hibernate.ejb.test.pack.defaultpar_1_0.IncrementListener1;
import org.hibernate.ejb.test.pack.defaultpar_1_0.Lighter1;
import org.hibernate.ejb.test.pack.defaultpar_1_0.Money1;
import org.hibernate.ejb.test.pack.defaultpar_1_0.Mouse1;
import org.hibernate.ejb.test.pack.defaultpar_1_0.Version1;
import org.hibernate.ejb.test.pack.excludehbmpar.Caipirinha;
import org.hibernate.ejb.test.pack.explodedpar.Carpet;
import org.hibernate.ejb.test.pack.explodedpar.Elephant;
import org.hibernate.ejb.test.pack.externaljar.Scooter;
import org.hibernate.ejb.test.pack.spacepar.Bug;
import org.hibernate.ejb.test.pack.various.Airplane;
import org.hibernate.ejb.test.pack.various.Seat;

/**
 * @author Hardy Ferentschik
 */
public abstract class PackagingTestCase extends TestCase {
	protected static ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
	protected static ClassLoader bundleClassLoader;
	protected static File targetDir;

	static {
		// get a URL reference to something we now is part of the classpath (us)
		URL myUrl = originalClassLoader.getResource(
				PackagingTestCase.class.getName().replace( '.', '/' ) + ".class"
		);
		File myPath = new File( myUrl.getFile() );
		// navigate back to '/target'
		targetDir = myPath
				.getParentFile()  // target/classes/org/hibernate/ejb/test/packaging
				.getParentFile()  // target/classes/org/hibernate/ejb/test
				.getParentFile()  // target/classes/org/hibernate/ejb
				.getParentFile()  // target/classes/org/hibernate
				.getParentFile()  // target/classes/org
				.getParentFile()  // target/classes/
				.getParentFile(); // target
		File testPackagesDir = new File( targetDir, "bundles" );
		try {
			bundleClassLoader = new URLClassLoader( new URL[] { testPackagesDir.toURL() }, originalClassLoader );
		}
		catch ( MalformedURLException e ) {
			fail( "Unable to build custom class loader" );
		}
		targetDir = new File( targetDir, "packages" );
		targetDir.mkdirs();
	}

	@Override
	protected void setUp() throws Exception {
		// add the bundle class loader in order for ShrinkWrap to build the test package
		Thread.currentThread().setContextClassLoader( bundleClassLoader );
		super.setUp();
	}

	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		// reset the classloader
		Thread.currentThread().setContextClassLoader( originalClassLoader );
	}

	protected void addPackageToClasspath(File... files) throws MalformedURLException {
		List<URL> urlList = new ArrayList<URL>();
		for ( File file : files ) {
			urlList.add( file.toURL() );
		}
		URLClassLoader classLoader = new URLClassLoader(
				urlList.toArray( new URL[urlList.size()] ), originalClassLoader
		);
		Thread.currentThread().setContextClassLoader( classLoader );
	}

	protected void addPackageToClasspath(URL... urls) throws MalformedURLException {
		List<URL> urlList = new ArrayList<URL>();
		urlList.addAll( Arrays.asList( urls ) );
		URLClassLoader classLoader = new URLClassLoader(
				urlList.toArray( new URL[urlList.size()] ), originalClassLoader
		);
		Thread.currentThread().setContextClassLoader( classLoader );
	}

	protected File buildDefaultPar() {
		String fileName = "defaultpar.par";
		JavaArchive archive = Archives.create( fileName, JavaArchive.class );
		archive.addClasses(
				ApplicationServer.class,
				Lighter.class,
				Money.class,
				Mouse.class,
				OtherIncrementListener.class,
				IncrementListener.class,
				Version.class
		);
		ArchivePath path = ArchivePaths.create( "META-INF/orm.xml" );
		archive.addResource( "defaultpar/META-INF/orm.xml", path );

		path = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addResource( "defaultpar/META-INF/persistence.xml", path );

		path = ArchivePaths.create( "org/hibernate/ejb/test/pack/defaultpar/Mouse.hbm.xml" );
		archive.addResource( "defaultpar/org/hibernate/ejb/test/pack/defaultpar/Mouse.hbm.xml", path );

		path = ArchivePaths.create( "org/hibernate/ejb/test/pack/defaultpar/package-info.class" );
		archive.addResource( "org/hibernate/ejb/test/pack/defaultpar/package-info.class", path );


		File testPackage = new File( targetDir, fileName );
		archive.as( ZipExporter.class ).exportZip( testPackage, true );
		return testPackage;
	}

	protected File buildDefaultPar_1_0() {
		String fileName = "defaultpar_1_0.par";
		JavaArchive archive = Archives.create( fileName, JavaArchive.class );
		archive.addClasses(
				ApplicationServer1.class,
				Lighter1.class,
				Money1.class,
				Mouse1.class,
				IncrementListener1.class,
				Version1.class
		);
		ArchivePath path = ArchivePaths.create( "META-INF/orm.xml" );
		archive.addResource( "defaultpar_1_0/META-INF/orm.xml", path );

		path = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addResource( "defaultpar_1_0/META-INF/persistence.xml", path );

		path = ArchivePaths.create( "org/hibernate/ejb/test/pack/defaultpar_1_0/Mouse.hbm.xml" );
		archive.addResource( "defaultpar_1_0/org/hibernate/ejb/test/pack/defaultpar_1_0/Mouse1.hbm.xml", path );

		path = ArchivePaths.create( "org/hibernate/ejb/test/pack/defaultpar_1_0/package-info.class" );
		archive.addResource( "org/hibernate/ejb/test/pack/defaultpar_1_0/package-info.class", path );


		File testPackage = new File( targetDir, fileName );
		archive.as( ZipExporter.class ).exportZip( testPackage, true );
		return testPackage;
	}

	protected File buildExplicitPar() {
		String fileName = "explicitpar.par";
		JavaArchive archive = Archives.create( fileName, JavaArchive.class );
		archive.addClasses(
				Airplane.class,
				Seat.class,
				Cat.class,
				Kitten.class,
				Distributor.class,
				Item.class
		);

		ArchivePath path = ArchivePaths.create( "META-INF/orm.xml" );
		archive.addResource( "explicitpar/META-INF/orm.xml", path );

		path = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addResource( "explicitpar/META-INF/persistence.xml", path );

		File testPackage = new File( targetDir, fileName );
		archive.as( ZipExporter.class ).exportZip( testPackage, true );
		return testPackage;
	}

	protected File buildExplodedPar() {
		String fileName = "explodedpar";
		JavaArchive archive = Archives.create( fileName, JavaArchive.class );
		archive.addClasses(
				Elephant.class,
				Carpet.class
		);

		ArchivePath path = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addResource( "explodedpar/META-INF/persistence.xml", path );

		path = ArchivePaths.create( "org/hibernate/ejb/test/pack/explodedpar/Elephant.hbm.xml" );
		archive.addResource( "explodedpar/org/hibernate/ejb/test/pack/explodedpar/Elephant.hbm.xml", path );

		path = ArchivePaths.create( "org/hibernate/ejb/test/pack/explodedpar/package-info.class" );
		archive.addResource( "org/hibernate/ejb/test/pack/explodedpar/package-info.class", path );

		File testPackage = new File( targetDir, fileName );
		archive.as( ExplodedExporter.class ).exportExploded( targetDir );
		return testPackage;
	}

	protected File buildExcludeHbmPar() {
		String fileName = "excludehbmpar.par";
		JavaArchive archive = Archives.create( fileName, JavaArchive.class );
		archive.addClasses(
				Caipirinha.class
		);

		ArchivePath path = ArchivePaths.create( "META-INF/orm2.xml" );
		archive.addResource( "excludehbmpar/META-INF/orm2.xml", path );

		path = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addResource( "excludehbmpar/META-INF/persistence.xml", path );

		path = ArchivePaths.create( "org/hibernate/ejb/test/pack/excludehbmpar/Mouse.hbm.xml" );
		archive.addResource( "excludehbmpar/org/hibernate/ejb/test/pack/excludehbmpar/Mouse.hbm.xml", path );

		File testPackage = new File( targetDir, fileName );
		archive.as( ZipExporter.class ).exportZip( testPackage, true );
		return testPackage;
	}

	protected File buildCfgXmlPar() {
		String fileName = "cfgxmlpar.par";
		JavaArchive archive = Archives.create( fileName, JavaArchive.class );
		archive.addClasses(
				Morito.class,
				Item.class
		);

		ArchivePath path = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addResource( "cfgxmlpar/META-INF/persistence.xml", path );

		path = ArchivePaths.create( "org/hibernate/ejb/test/pack/cfgxmlpar/hibernate.cfg.xml" );
		archive.addResource( "cfgxmlpar/org/hibernate/ejb/test/pack/cfgxmlpar/hibernate.cfg.xml", path );

		File testPackage = new File( targetDir, fileName );
		archive.as( ZipExporter.class ).exportZip( testPackage, true );
		return testPackage;
	}

	protected File buildSpacePar() {
		String fileName = "space par.par";
		JavaArchive archive = Archives.create( fileName, JavaArchive.class );
		archive.addClasses(
				Bug.class
		);

		ArchivePath path = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addResource( "space par/META-INF/persistence.xml", path );

		File testPackage = new File( targetDir, fileName );
		archive.as( ZipExporter.class ).exportZip( testPackage, true );
		return testPackage;
	}

	protected File buildOverridenPar() {
		String fileName = "overridenpar.jar";
		JavaArchive archive = Archives.create( fileName, JavaArchive.class );
		archive.addClasses(
				org.hibernate.ejb.test.pack.overridenpar.Bug.class
		);

		ArchivePath path = ArchivePaths.create( "META-INF/persistence.xml" );
		archive.addResource( "overridenpar/META-INF/persistence.xml", path );

		path = ArchivePaths.create( "overridenpar.properties" );
		archive.addResource( "overridenpar/overridenpar.properties", path );

		File testPackage = new File( targetDir, fileName );
		archive.as( ZipExporter.class ).exportZip( testPackage, true );
		return testPackage;
	}

	protected File buildExternalJar() {
		String fileName = "externaljar.jar";
		JavaArchive archive = Archives.create( fileName, JavaArchive.class );
		archive.addClasses(
				Scooter.class
		);

		ArchivePath path = ArchivePaths.create( "META-INF/orm.xml" );
		archive.addResource( "externaljar/META-INF/orm.xml", path );

		File testPackage = new File( targetDir, fileName );
		archive.as( ZipExporter.class ).exportZip( testPackage, true );
		return testPackage;
	}

	protected File buildWar() {
		String fileName = "war.war";
		WebArchive archive = Archives.create( fileName, WebArchive.class );
		archive.addClasses(
				org.hibernate.ejb.test.pack.war.ApplicationServer.class,
				org.hibernate.ejb.test.pack.war.IncrementListener.class,
				org.hibernate.ejb.test.pack.war.Lighter.class,
				org.hibernate.ejb.test.pack.war.Money.class,
				org.hibernate.ejb.test.pack.war.Mouse.class,
				org.hibernate.ejb.test.pack.war.OtherIncrementListener.class,
				org.hibernate.ejb.test.pack.war.Version.class
		);

		ArchivePath path = ArchivePaths.create( "WEB-INF/classes/META-INF/orm.xml" );
		archive.addResource( "war/WEB-INF/classes/META-INF/orm.xml", path );

		path = ArchivePaths.create( "WEB-INF/classes/META-INF/persistence.xml" );
		archive.addResource( "war/WEB-INF/classes/META-INF/persistence.xml", path );

		path = ArchivePaths.create( "WEB-INF/classes/org/hibernate/ejb/test/pack/war/Mouse.hbm.xml" );
		archive.addResource( "war/WEB-INF/classes/org/hibernate/ejb/test/pack/war/Mouse.hbm.xml", path );

		File testPackage = new File( targetDir, fileName );
		archive.as( ZipExporter.class ).exportZip( testPackage, true );
		return testPackage;
	}

	protected File buildNestedEar(File includeFile) {
		String fileName = "nestedjar.ear";
		JavaArchive archive = Archives.create( fileName, JavaArchive.class );
		archive.addResource( includeFile );

		File testPackage = new File( targetDir, fileName );
		archive.as( ZipExporter.class ).exportZip( testPackage, true );
		return testPackage;
	}

	protected File buildNestedEarDir(File includeFile) {
		String fileName = "nesteddir.ear";
		JavaArchive archive = Archives.create( fileName, JavaArchive.class );
		archive.addResource( includeFile );

		File testPackage = new File( targetDir, fileName );
		archive.as( ExplodedExporter.class ).exportExploded( targetDir );
		return testPackage;
	}
}


