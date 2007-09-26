/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Max Andersen, Steve Ebersole
 */
package org.hibernate.test.cfg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
	private File mappingBaseDir;
	private File workPackageDir;

	protected void setUp() throws Exception {
		compilationBaseDir = getTestComplileDirectory();
		mappingBaseDir = new File( compilationBaseDir, "org/hibernate/test" );
		workPackageDir = new File( compilationBaseDir, workPackageName.replace( '.', '/' ) );
		if ( workPackageDir.exists() ) {
			workPackageDir.delete();
		}
		workPackageDir.mkdirs();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	private static final String[] FILES = new String[] {
			"legacy/ABC.hbm.xml",
			"legacy/ABCExtends.hbm.xml",
			"legacy/Baz.hbm.xml",
			"legacy/Blobber.hbm.xml",
			"legacy/Broken.hbm.xml",
			"legacy/Category.hbm.xml",
			"legacy/Circular.hbm.xml",
			"legacy/Commento.hbm.xml",
			"legacy/ComponentNotNullMaster.hbm.xml",
			"legacy/Componentizable.hbm.xml",
			"legacy/Container.hbm.xml",
			"legacy/Custom.hbm.xml",
			"legacy/CustomSQL.hbm.xml",
			"legacy/Eye.hbm.xml",
			"legacy/Fee.hbm.xml",
			"legacy/Fo.hbm.xml",
			"legacy/FooBar.hbm.xml",
			"legacy/Fum.hbm.xml",
			"legacy/Fumm.hbm.xml",
			"legacy/Glarch.hbm.xml",
			"legacy/Holder.hbm.xml",
			"legacy/IJ2.hbm.xml",
			"legacy/Immutable.hbm.xml",
			"legacy/Location.hbm.xml",
			"legacy/Many.hbm.xml",
			"legacy/Map.hbm.xml",
			"legacy/Marelo.hbm.xml",
			"legacy/MasterDetail.hbm.xml",
			"legacy/Middle.hbm.xml",
			"legacy/Multi.hbm.xml",
			"legacy/MultiExtends.hbm.xml",
			"legacy/Nameable.hbm.xml",
			"legacy/One.hbm.xml",
			"legacy/ParentChild.hbm.xml",
			"legacy/Qux.hbm.xml",
			"legacy/Simple.hbm.xml",
			"legacy/SingleSeveral.hbm.xml",
			"legacy/Stuff.hbm.xml",
			"legacy/UpDown.hbm.xml",
			"legacy/Vetoer.hbm.xml",
			"legacy/WZ.hbm.xml",
	};

	public ConfigurationPerformanceTest(String string) {
		super( string );
	}

	public static Test suite() {
		return new TestSuite( ConfigurationPerformanceTest.class );
	}

	public static void main(String[] args) throws Exception {
		TestRunner.run( suite() );
	}

	public void testLoadingAndSerializationOfConfiguration() throws Throwable {
		String prefix = mappingBaseDir.getAbsolutePath() + '/';
		try {
			// first time
			System.err.println( "###FIRST SAVELOAD###" );
			SessionFactory factory = saveAndLoad( prefix, FILES, "hibernate.cfg.bin" );
			factory.close();
			// second time to validate
			System.err.println( "###SECOND SAVELOAD###" );
			factory = saveAndLoad( prefix, FILES, "hibernate.cfg.bin" );
			factory.close();
		}
		finally {
			System.err.println( "###CLEANING UP###" );
			File file = null;
			try {
				// clean up
				file = new File( "hibernate.cfg.bin" );
				file.delete();
			}
			catch ( Throwable t ) {
				System.err.println( "Unable to cleanup [" + file + "] : " + t );
			}

			for ( int i = 0; i < FILES.length; i++ ) {
				try {
					String fileName = FILES[i];
					file = new File( prefix, fileName + ".bin" );
					file.delete();
				}
				catch ( Throwable t ) {
					System.err.println( "Unable to cleanup [" + file + "] : " + t );
				}
			}
		}
	}

	public void testSessionFactoryCreationTime() throws Throwable {
		generateTestFiles();
		if ( !workPackageDir.exists() ) {
			System.err.println( workPackageDir.getAbsoluteFile() + " not found" );
			return;
		}

		SessionFactory factory = saveAndLoad(
				workPackageDir.getAbsolutePath() + '/',
				workPackageDir.list(
					new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return name.endsWith( ".hbm.xml" );
						}
					}
				),
				"hibernateperftest.cfg.bin"
		);

		Session session = factory.openSession();
		session.createQuery( "from Test1" ).list();

		factory.close();
	}

	private SessionFactory saveAndLoad(String prefix, String[] files, String cfgName) throws Throwable {
		System.err.println( "saveAndLoad from " + prefix + " with cfg = " + cfgName );
		File cachedCfgFile = new File( workPackageDir, cfgName );

		Configuration cfg;
		long start;

		if ( !cachedCfgFile.exists() ) {
			start = System.currentTimeMillis();
			cfg = new Configuration();
			cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
			System.err.println( "Created configuration: " + ( System.currentTimeMillis() - start ) / 1000.0 + " sec." );

			start = System.currentTimeMillis();
			for ( int i = 0; i < files.length; i++ ) {
				String file = files[i];
				cfg.addCacheableFile( new File( prefix + file ) );
			}
			System.err.println(
					"Added " + ( files.length ) + " resources: " +
							( System.currentTimeMillis() - start ) / 1000.0 + " sec."
			);

			ObjectOutputStream os = new ObjectOutputStream( new FileOutputStream( cachedCfgFile ) );
			os.writeObject( cfg ); // need to serialize Configuration *before* building sf since it would require non-mappings and cfg types to be serializable
			os.flush();
			os.close();

		}
		else {
			start = System.currentTimeMillis();
			ObjectInputStream is = new ObjectInputStream( new FileInputStream( cachedCfgFile ) );
			cfg = ( Configuration ) is.readObject();
			is.close();
			System.err
					.println( "Loaded serializable configuration:" + ( System.currentTimeMillis() - start ) / 1000.0 + " sec." );
		}
		start = System.currentTimeMillis();
		System.err.println( "Start build of session factory" );
		SessionFactory factory = cfg.buildSessionFactory();
		System.err.println( "Build session factory:" + ( System.currentTimeMillis() - start ) / 1000.0 + " sec." );
		return factory;
	}

	public void generateTestFiles() throws Throwable {
		String filesToCompile = "";
		for ( int count = 0; count < 100; count++ ) {
			String name = "Test" + count;
			File javaFile = new File( workPackageDir, name + ".java" );
			File hbmFile = new File( workPackageDir, name + ".hbm.xml" );
			filesToCompile += ( javaFile.getAbsolutePath() + " " );

			javaFile.getParentFile().mkdirs();
			hbmFile.getParentFile().mkdirs();

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
