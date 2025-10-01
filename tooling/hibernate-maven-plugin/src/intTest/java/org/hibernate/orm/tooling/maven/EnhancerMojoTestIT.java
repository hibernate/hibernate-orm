/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.apache.maven.cli.MavenCli;
import org.hibernate.bytecode.enhance.spi.EnhancementInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.List;

public class EnhancerMojoTestIT {

	public static final String MVN_HOME = "maven.multiModuleProjectDirectory";

	@TempDir
	File projectDir;

	private MavenCli mavenCli;

	@BeforeEach
	public void beforeEach() throws Exception {
		copyJavFiles();
		System.setProperty(MVN_HOME, projectDir.getAbsolutePath());
		mavenCli = new MavenCli();
	}

	@Test
	public void testEnhancementDefault() throws Exception {
		// The default configuration for the enhance goal are as follows:
		//   enableLazyInitialization = 'true'
		//   enableDirtyTracking = 'true'
		//   enableAssociationManagement = 'false'
		//   enableExtendedEnhancement = 'false'
		//   classesDirectory = 'target/classes'
		String configurationElement = "<configuration/>\n";
		preparePomXml(configurationElement);
		executeCompileGoal();
		executeEnhanceGoal();
		// Both Bar and Baz should be enhanced
		assertTrue(isEnhanced( "Bar" ));
		assertTrue(isEnhanced( "Baz" ));
		// Both Bar and Baz contain the method '$$_hibernate_getInterceptor'
		// because of the default setting of 'enableLazyInitialization'
		assertTrue( methodIsPresentInClass("$$_hibernate_getInterceptor", "Bar"));
		assertTrue( methodIsPresentInClass("$$_hibernate_getInterceptor", "Baz"));
		// Both Bar and Baz contain the method '$$_hibernate_hasDirtyAttributes'
		// because of the default setting of 'enableDirtyTracking'
		assertTrue( methodIsPresentInClass("$$_hibernate_hasDirtyAttributes", "Bar"));
		assertTrue( methodIsPresentInClass("$$_hibernate_hasDirtyAttributes", "Baz"));
		// Foo is not an entity and extended enhancement is not enabled so the class is not enhanced
		assertFalse(isEnhanced("Foo"));
		// Association management should not be present
		assertFalse(isAssociationManagementPresent());
	}

	@Test
	public void testEnhancementFileSet() throws Exception {
		// Use the defaults settings for enhancement (see #testEnhancementDefault)
		// The files are read from the specified 'fileset' element:
		//   - the folder specified by 'dir'
		//   - the 'Baz.class' file is excluded
		String configurationElement =
				"<configuration>\n" +
				"  <fileSets>\n"+
				"    <fileset>\n" +
				"      <directory>" + projectDir.getAbsolutePath() + "/target" + "</directory>\n" +
				"      <excludes>\n" +
				"        <exclude>**/Baz.class</exclude>\n" +
				"      </excludes>\n" +
				"    </fileset>\n" +
				"  </fileSets>\n" +
				"</configuration>\n";
		preparePomXml(configurationElement);
		executeCompileGoal();
		executeEnhanceGoal();
		// Bar is enhanced
		assertTrue(isEnhanced( "Bar" ));
		// Baz is not enhanced because it was excluded from the file set
		assertFalse(isEnhanced( "Baz" ));
		// Foo is not enhanced because it is not an entity and extended enhancement was not enabled
		assertFalse( isEnhanced( "Foo" ) );
		// Association management should not be present
		assertFalse(isAssociationManagementPresent());
	}

	@Test
	public void testEnhancementNoLazyInitialization() throws Exception {
		// Change the default setting for 'enableLazyInitialization' to 'false'
		// Otherwise use the settings of #testEnhancementDefault
		String configurationElement =
				"<configuration>\n" +
				"  <enableLazyInitialization>false</enableLazyInitialization>\n"+
				"</configuration>\n";
		preparePomXml(configurationElement);
		executeCompileGoal();
		executeEnhanceGoal();
		// Both Bar and Baz are enhanced, Foo is not
		assertTrue( isEnhanced( "Bar" ));
		assertTrue( isEnhanced( "Baz" ));
		// Foo is not enhanced because it is not an entity and extended enhancement was not enabled
		assertFalse( isEnhanced( "Foo" ) );
		// but $$_hibernate_getInterceptor is not present in the enhanced classes
		// because of the 'false' value of 'enableLazyInitialization'
		assertFalse( methodIsPresentInClass("$$_hibernate_getInterceptor", "Bar"));
		assertFalse( methodIsPresentInClass("$$_hibernate_getInterceptor", "Baz"));
		// Association management should not be present
		assertFalse(isAssociationManagementPresent());
	}

	@Test
	public void testEnhancementNoDirtyTracking() throws Exception {
		// Change the default setting for 'enableDirtyTracking' to 'false'
		// Otherwise use the settings of #testEnhancementDefault
		String configurationElement =
				"<configuration>\n" +
				"  <enableDirtyTracking>false</enableDirtyTracking>\n"+
				"</configuration>\n";
		preparePomXml(configurationElement);
		executeCompileGoal();
		executeEnhanceGoal();
		// Both Bar and Baz should be enhanced
		assertTrue( isEnhanced( "Bar" ));
		assertTrue( isEnhanced( "Baz" ));
		// Foo is not enhanced because it is not an entity and extended enhancement was not enabled
		assertFalse( isEnhanced( "Foo" ) );
		// $$_hibernate_hasDirtyAttributes is not present in the enhanced classes
		// because of the 'false' value of 'enableLazyInitialization'
		assertFalse( methodIsPresentInClass("$$_hibernate_hasDirtyAttributes", "Bar"));
		assertFalse( methodIsPresentInClass("$$_hibernate_hasDirtyAttributes", "Baz"));
		// Association management should not be present
		assertFalse(isAssociationManagementPresent());
	}

	@Test
	public void testEnhancementEnableAssociationManagement() throws Exception {
		// Change the default setting for 'enableAssociationManagement' to 'true'
		// Otherwise use the settings of #testEnhancementDefault
		String configurationElement =
				"<configuration>\n" +
				"  <enableAssociationManagement>true</enableAssociationManagement>\n"+
				"</configuration>\n";
		preparePomXml(configurationElement);
		executeCompileGoal();
		executeEnhanceGoal();
		// Both Bar and Baz are enhanced, Foo is not
		assertTrue( isEnhanced( "Bar" ));
		assertTrue( isEnhanced( "Baz" ));
		assertFalse( isEnhanced( "Foo" ) );
		// Now verify that the association management is in place;
		assertTrue(isAssociationManagementPresent());
	}

	@Test
	public void testEnhancementEnableExtendedEnhancement() throws Exception {
		// Change the default setting for 'enableExtendedEnhancement' to 'true'
		// Otherwise use the settings of #testEnhancementDefault
		String configurationElement =
				"<configuration>\n" +
				"  <enableExtendedEnhancement>true</enableExtendedEnhancement>\n"+
				"</configuration>\n";
		preparePomXml(configurationElement);
		executeCompileGoal();
		executeEnhanceGoal();
		// Both Bar and Baz are enhanced because they are entities
		assertTrue( isEnhanced( "Bar" ));
		assertTrue( isEnhanced( "Baz" ));
		// Though Foo is not an entity, it is enhanced because of the setting of 'enableExtendedEnhancement'
		assertTrue( isEnhanced( "Foo" ) );
		// No association management is in place;
		assertFalse(isAssociationManagementPresent());
	}


	@Test
	public void testNoEnhancement() throws Exception {
		// Setting the values of all the settings to 'false' has the effect
		// of not executing the enhancement at all.
		// The setting of 'enableAssociationManagement' and 'enableExtendedEnhancement' to
		// false is not really needed in this case as that's what their default is
		String configurationElement =
				"<configuration>\n" +
				"  <enableLazyInitialization>false</enableLazyInitialization>\n"+
				"  <enableDirtyTracking>false</enableDirtyTracking>\n"+
				"  <enableAssociationManagement>false</enableAssociationManagement>\n"+
				"  <enableExtendedEnhancement>false</enableExtendedEnhancement>\n"+
				"</configuration>\n";
		preparePomXml(configurationElement);
		executeCompileGoal();
		executeEnhanceGoal();
		// None of the classes should be enhanced
		assertFalse( isEnhanced( "Bar" ));
		assertFalse( isEnhanced( "Baz" ));
		assertFalse( isEnhanced( "Foo" ) );
		// No association management is in place;
		assertFalse(isAssociationManagementPresent());
	}

	private void executeCompileGoal() {
		// The class files should not exist
		assertFalse(fileExists("target/classes/Bar.class"));
		assertFalse(fileExists("target/classes/Baz.class"));
		assertFalse(fileExists("target/classes/Foo.class"));
		// Execute the 'compile' target
		new MavenCli().doMain(
				new String[]{"compile"},
				projectDir.getAbsolutePath(),
				null,
				null);
		// The class files should exist now
		assertTrue( fileExists( "target/classes/Bar.class" ) );
		assertTrue( fileExists( "target/classes/Baz.class" ) );
		assertTrue( fileExists( "target/classes/Foo.class" ) );
	}

	private void executeEnhanceGoal() throws Exception {
		// The class files should not be enhanced at this point
		assertFalse( isEnhanced( "Bar" ));
		assertFalse( isEnhanced( "Baz" ));
		assertFalse( isEnhanced( "Foo" ));
		// Execute the 'enhance' target
		mavenCli.doMain(
				new String[]{"process-classes"},
				projectDir.getAbsolutePath(),
				null,
				null);
		// The results are verified in the respective tests
	}

	private void preparePomXml(String configurationElement) throws Exception {
		URL url = getClass().getClassLoader().getResource("pom.xm_");
		File source = new File(url.toURI());
		assertFalse( fileExists( "pom.xml" ));
		String pomXmlContents = new String(Files.readAllBytes( source.toPath() ));
		pomXmlContents = pomXmlContents.replace( "@hibernate-version@", System.getenv("hibernateVersion"));
		pomXmlContents = pomXmlContents.replace(  "@configuration@", configurationElement);
		File destination = new File(projectDir, "pom.xml");
		Files.writeString(destination.toPath(), pomXmlContents);
		assertTrue( fileExists( "pom.xml" ) );
	}

	private void copyJavFiles() throws Exception {
		File srcDir = new File(projectDir, "src/main/java");
		srcDir.mkdirs();
		String[] javFileNames = {"Bar.jav_", "Baz.jav_", "Foo.jav_"};
		for (String javFileName : javFileNames) {
			copyJavFile( javFileName, srcDir );
		}
	}

	private void copyJavFile(String javFileName, File toFolder) throws Exception {
		URL url = getClass().getClassLoader().getResource( javFileName );
		assert url != null;
		File source = new File(url.toURI());
		File destination = new File(toFolder, javFileName.replace( '_', 'a' ));
		assertTrue(source.exists());
		assertTrue(source.isFile());
		Files.copy(source.toPath(), destination.toPath());
		assertTrue(destination.exists());
		assertTrue(destination.isFile());
	}

	private ClassLoader getTestClassLoader() throws Exception {
		return new URLClassLoader( new URL[] { new File(projectDir, "target/classes").toURI().toURL() } );
	}

	private boolean isEnhanced(String className) throws Exception {
		return getTestClassLoader().loadClass( className ).isAnnotationPresent( EnhancementInfo.class );
	}

	private boolean methodIsPresentInClass(String methodName, String className) throws Exception {
		Class<?> classToCheck = getTestClassLoader().loadClass( className );
		try {
			Object m = classToCheck.getMethod( methodName, new Class[] {} );
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

	private boolean isAssociationManagementPresent() throws Exception {
		// Some dynamic programming
		ClassLoader loader = getTestClassLoader();
		// Obtain the class objects for 'Baz' and 'Bar'
		Class<?> bazClass = loader.loadClass( "Baz" );
		Class<?> barClass = loader.loadClass( "Bar" );
		// Create an instance of both 'Baz' and 'Bar'
		Object bazObject = bazClass.getDeclaredConstructor().newInstance();
		Object barObject = barClass.getDeclaredConstructor().newInstance();
		// Lookup the 'bars' field of class 'Baz' (an ArrayList of 'Bar' objects)
		Field bazBarsField = bazClass.getDeclaredField( "bars" );
		bazBarsField.setAccessible( true );
		// Obtain the 'bars' list of the 'Baz' object; it should be empty
		List<?> bazBarsList = (List<?>) bazBarsField.get( bazObject );   // baz.bars
		assertTrue(bazBarsList.isEmpty());
		// Lookup the 'setBaz' method of class 'Bar' and invoke it on the 'Bar' object
		Method barSetBazMethod = barClass.getDeclaredMethod( "setBaz", new Class[] { bazClass } );
		barSetBazMethod.invoke( barObject, bazObject );                  // bar.setBaz(baz)
		// Reobtain the 'bars' list of the 'Baz' object
		bazBarsList = (List<?>) bazBarsField.get( bazObject );
		// If there is association management, the 'bars' list should contain the 'Bar' object
		return bazBarsList.contains( barObject );                        // baz.bars.contains(bar)
	}

	private boolean fileExists(String relativePath) {
		return new File( projectDir, relativePath ).exists();
	}

}
