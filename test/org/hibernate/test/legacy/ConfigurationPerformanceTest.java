/*
 * Created on 01-05-2004
 *
  */
package org.hibernate.test.legacy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.junit.UnitTestCase;
import org.hibernate.cfg.Configuration;
import org.hibernate.classic.Session;

/**
 * @author MAX
 *
 */
public class ConfigurationPerformanceTest extends UnitTestCase {

	String[] files = new String[] {
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

	boolean keepFilesAround = false 	; // set to true to be able to "coldstart" start.

	public ConfigurationPerformanceTest(String string) {
		super( string );
	}

	public void testLoadingAndSerializationOfConfiguration() throws HibernateException, FileNotFoundException, IOException, ClassNotFoundException {

		String prefix = "./test/org/hibernate/test/";
		try {
			// first time
			System.err.println("###FIRST SAVELOAD###");
			saveAndLoad(prefix,files, "hibernate.cfg.bin");
			// second time to validate
			System.err.println("###SECOND SAVELOAD###");
			saveAndLoad(prefix,files, "hibernate.cfg.bin");
		}
		finally {
			System.err.println( "###CLEANING UP###" );
			if(!keepFilesAround) {
				File file = null;
				try {
				// clean up
					file = new File("hibernate.cfg.bin");
					file.delete();
				}
				catch( Throwable t ) {
					System.err.println( "Unable to cleanup [" + file + "] : " + t );
				}

				for (int i = 0; i < files.length; i++) {
					try {
						String fileName = files[i];
						file = new File( prefix, fileName + ".bin" );
						file.delete();
					}
					catch( Throwable t ) {
						System.err.println( "Unable to cleanup [" + file + "] : " + t );
					}
				}
			}
		}
		
	}
	
	// this method requires generation of test files (can be done with generateTestFiles) + their compile
	public void xtestSessionFactoryCreationTime() throws FileNotFoundException, IOException, ClassNotFoundException {
		File perfs = new File("perfsrc");
		generateTestFiles(perfs, "perftest");
		if(perfs.exists()) {
			SessionFactory factory = saveAndLoad("perfsrc/perftest/", new File(perfs, "perftest").list(new FilenameFilter() {
			
				public boolean accept(File dir, String name) {
					return name.endsWith(".hbm.xml");
				}
			
			}), "hibernateperftest.cfg.bin");
			
			Session session = factory.openSession();
			Object o = session.load("perftest.Test1", new Long(42));
			System.out.println(o);
		} else {
			System.err.println(perfs.getAbsoluteFile() + " not found");
		}
	}

	private SessionFactory saveAndLoad(String prefix, String[] files, String cfgName) throws IOException, FileNotFoundException, ClassNotFoundException {
		long start = System.currentTimeMillis();
		
		Configuration cfg = new Configuration();
		System.err.println("Created configuration: " + (System.currentTimeMillis() - start) / 1000.0 + " sec.");
		
		System.err.println("saveAndLoad from " + prefix + " with cfg = " + cfgName);
		if(!new File(cfgName).exists()) {
			start = System.currentTimeMillis();
			/*for (int i=0; i<files.length; i++) {
			 if ( !files[i].startsWith("net/") ) files[i] = "test/org/hibernate/test/" + files[i];
			 cfg.addFile(files[i]);
			 //cfg.addLazyFile(files[i]);
			  }*/
			
			for (int i = 0; i < files.length; i++) {
				String file = files[i];
				cfg.addCacheableFile(new File(prefix + file));
			}
			
			System.err.println("Added " + (files.length) + " resources: " + (System.currentTimeMillis() - start) / 1000.0 + " sec.");
			
			ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(cfgName));
			os.writeObject(cfg); // need to serialize Configuration *before* building sf since it would require non-mappings and cfg types to be serializable
			os.flush();
			os.close();
			
		} else {
			start = System.currentTimeMillis();
			ObjectInputStream is = new ObjectInputStream(new FileInputStream(cfgName));
			cfg = (Configuration) is.readObject();
			is.close();
			System.err.println("Loaded serializable configuration:" + (System.currentTimeMillis() - start) / 1000.0 + " sec.");
		}
		start = System.currentTimeMillis();
		System.err.println("Start build of session factory");
		SessionFactory factory = cfg.buildSessionFactory();
		System.err.println("Build session factory:" + (System.currentTimeMillis() - start) / 1000.0 + " sec.");
		return factory;
	}


	public static Test suite() {
		return new TestSuite(ConfigurationPerformanceTest.class);
	}

	public static void main(String[] args) throws Exception {
		TestRunner.run( suite() );
	}
	
	public void generateTestFiles(File basedir, String pkgName) throws IOException {
		
		for(int count=0;count<100;count++) {
			String name = "Test" + count;
			File javaFile = new File(new File(basedir, pkgName), name + ".java");
			File hbmFile = new File(new File(basedir, pkgName), name + ".hbm.xml");
			
			javaFile.getParentFile().mkdirs();
			hbmFile.getParentFile().mkdirs();
			
			System.out.println("Generating " + javaFile.getAbsolutePath());
			PrintWriter javaWriter = null;
			PrintWriter hbmWriter = null;
			try {
				javaWriter = new PrintWriter(new FileWriter(javaFile));
				hbmWriter = new PrintWriter(new FileWriter(hbmFile));
				
				javaWriter.println("package " + pkgName + ";");
				hbmWriter.println("<?xml version=\"1.0\"?>\r\n" + 
						"<!DOCTYPE hibernate-mapping PUBLIC \r\n" + 
						"	\"-//Hibernate/Hibernate Mapping DTD 3.0//EN\"\r\n" + 
				"	\"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">\r\n");
				
				hbmWriter.println("<hibernate-mapping package=\"" + pkgName + "\">");
				
				javaWriter.println("public class " + name + " {");
				javaWriter.println(" static { System.out.println(\"" + name + " initialized!\"); }");
				hbmWriter.println("<class name=\"" + name + "\">");
				
				hbmWriter.println("<id type=\"long\"><generator class=\"assigned\"/></id>");
				for(int propCount=0;propCount<100;propCount++) {
					String propName = "Prop" + propCount;
					
					writeJavaProperty(javaWriter, propName);
					
					hbmWriter.println("<property name=\"" + propName + "\" type=\"string\"/>");
					
				}
				hbmWriter.println("</class>");
				javaWriter.println("}");
				hbmWriter.println("</hibernate-mapping>");
			} finally {
				if(javaWriter!=null) {
					javaWriter.flush();
					javaWriter.close();
				} 
				if(hbmWriter!=null) {
					hbmWriter.flush();
					hbmWriter.close();
				}
			}		
		}
		
	}


	private void writeJavaProperty(PrintWriter javaWriter, String propName) {
		javaWriter.println(" String " + propName + ";");
		javaWriter.println(" String get" + propName + "() { return " + propName + "; }");
		javaWriter.println(" void set" + propName + "(String newVal) { " + propName + "=newVal; }");
	}
}
