/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
 */
package org.hibernate.test.perf;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.classic.Session;
import org.hibernate.junit.UnitTestCase;

/**
 * Test of configuration, specifically "cacheable files".
 *
 * @author Max Andersen
 * @author Steve Ebersole
 */
public class ConfigurationPerformanceTest extends UnitTestCase {

	private final String workPackageName = "org.hibernate.test.cfg.work";
	private File compilationBaseDir;
	private File workPackageDir;

	protected void setUp() throws Exception {
		compilationBaseDir = getTestComplileDirectory();
		workPackageDir = new File( compilationBaseDir, workPackageName.replace( '.', '/' ) );
		if ( workPackageDir.exists() ) {
			//noinspection ResultOfMethodCallIgnored
			workPackageDir.delete();
		}
		boolean created = workPackageDir.mkdirs();
		if ( !created ) {
			System.err.println( "Unable to create workPackageDir during setup" );
		}
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public ConfigurationPerformanceTest(String string) {
		super( string );
	}

	public static Test suite() {
		return new TestSuite( ConfigurationPerformanceTest.class );
	}

	public static void main(String[] args) throws Exception {
		TestRunner.run( suite() );
	}

	public void testSessionFactoryCreationTime() throws Throwable {
		generateTestFiles();
		if ( !workPackageDir.exists() ) {
			System.err.println( workPackageDir.getAbsoluteFile() + " not found" );
			return;
		}

		long start = System.currentTimeMillis();
		Configuration configuration = buildConfigurationFromCacheableFiles(
				workPackageDir,
				workPackageDir.list(
						new FilenameFilter() {
							public boolean accept(File dir, String name) {
								return name.endsWith( ".hbm.xml" );
							}
						}
				)
		);
		SessionFactory factory = configuration.buildSessionFactory();
		long initial = System.currentTimeMillis() - start;
		factory.close();

		start = System.currentTimeMillis();
		configuration = buildConfigurationFromCacheableFiles(
				workPackageDir,
				workPackageDir.list(
						new FilenameFilter() {
							public boolean accept(File dir, String name) {
								return name.endsWith( ".hbm.xml" );
							}
						}
				)
		);
		factory = configuration.buildSessionFactory();
		long subsequent = System.currentTimeMillis() - start;

		// Let's make sure the mappings were read in correctly (in termas of they are operational).
		Session session = factory.openSession();
		session.beginTransaction();
		session.createQuery( "from Test1" ).list();
		session.getTransaction().commit();
		session.close();
		factory.close();

		System.err.println( "Initial SessionFactory load time : " + initial );
		System.err.println( "Subsequent SessionFactory load time : " + subsequent );
	}

	private Configuration buildConfigurationFromCacheableFiles(File mappingFileBase, String[] files) {
		long start = System.currentTimeMillis();
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		System.err.println(
				"Created configuration: " + ( System.currentTimeMillis() - start ) / 1000.0 + " sec."
		);

		start = System.currentTimeMillis();
		//noinspection ForLoopReplaceableByForEach
		for ( int i = 0; i < files.length; i++ ) {
			cfg.addCacheableFile( new File( mappingFileBase, files[i] ) );
		}
		System.err.println(
				"Added " + ( files.length ) + " resources: " +
						( System.currentTimeMillis() - start ) / 1000.0 + " sec."
		);
		return cfg;
	}

	public void generateTestFiles() throws Throwable {
		String filesToCompile = "";
		for ( int count = 0; count < 100; count++ ) {
			String name = "Test" + count;
			File javaFile = new File( workPackageDir, name + ".java" );
			File hbmFile = new File( workPackageDir, name + ".hbm.xml" );
			filesToCompile += ( javaFile.getAbsolutePath() + " " );

			System.out.println( "Generating " + javaFile.getAbsolutePath() );
			PrintWriter javaWriter = null;
			PrintWriter hbmWriter = null;
			try {
				javaWriter = new PrintWriter( new FileWriter( javaFile ) );
				hbmWriter = new PrintWriter( new FileWriter( hbmFile ) );

				javaWriter.println( "package " + workPackageName + ";" );
				hbmWriter.println(
						"<?xml version=\"1.0\"?>\r\n" +
								"<!DOCTYPE hibernate-mapping PUBLIC \r\n" +
								"	\"-//Hibernate/Hibernate Mapping DTD 3.0//EN\"\r\n" +
								"	\"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">\r\n"
				);

				hbmWriter.println( "<hibernate-mapping package=\"" + workPackageName + "\">" );

				javaWriter.println( "public class " + name + " {" );
				javaWriter.println( " static { System.out.println(\"" + name + " initialized!\"); }" );
				hbmWriter.println( "<class name=\"" + name + "\">" );

				hbmWriter.println( "<id type=\"long\"><generator class=\"assigned\"/></id>" );
				for ( int propCount = 0; propCount < 100; propCount++ ) {
					String propName = "Prop" + propCount;

					writeJavaProperty( javaWriter, propName );

					hbmWriter.println( "<property name=\"" + propName + "\" type=\"string\"/>" );

				}
				hbmWriter.println( "</class>" );
				javaWriter.println( "}" );
				hbmWriter.println( "</hibernate-mapping>" );
			}
			finally {
				if ( javaWriter != null ) {
					javaWriter.flush();
					javaWriter.close();
				}
				if ( hbmWriter != null ) {
					hbmWriter.flush();
					hbmWriter.close();
				}
			}
		}

		String javac = "javac -version -d " + compilationBaseDir + " " + filesToCompile;
		System.err.println( "JAVAC : " + javac );
		Process process = Runtime.getRuntime().exec( javac );
		process.waitFor();
		System.err.println( "********************* JAVAC OUTPUT **********************" );
		pullStream( process.getInputStream() );
		System.err.println( "---------------------------------------------------------" );
		pullStream( process.getErrorStream() );
		System.err.println( "*********************************************************" );
	}

	private void pullStream(InputStream stream) throws IOException {
		if ( stream == null || stream.available() <= 0 ) {
			return;
		}
		byte[] buffer = new byte[256];
		while ( true ) {
			int read = stream.read( buffer );
			if ( read == -1 ) {
				break;
			}
			System.err.write( buffer, 0, read );
		}
//		System.err.println( "" );
	}

	private void writeJavaProperty(PrintWriter javaWriter, String propName) {
		javaWriter.println( " String " + propName + ";" );
		javaWriter.println( " String get" + propName + "() { return " + propName + "; }" );
		javaWriter.println( " void set" + propName + "(String newVal) { " + propName + "=newVal; }" );
	}

	private File getTestComplileDirectory() {
		String resourceName = "org/hibernate/test/legacy/ABC.hbm.xml";
		String prefix = getClass().getClassLoader().getResource( resourceName ).getFile();
		prefix = prefix.substring( 0, prefix.lastIndexOf( '/' ) );	// ABC.hbm.xml
		prefix = prefix.substring( 0, prefix.lastIndexOf( '/' ) );	// legacy/
		prefix = prefix.substring( 0, prefix.lastIndexOf( '/' ) );	// test/
		prefix = prefix.substring( 0, prefix.lastIndexOf( '/' ) );	// hibernate/
		prefix = prefix.substring( 0, prefix.lastIndexOf( '/' ) );	// org/
		return new File( prefix + '/' );
	}
}
