/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.enhance;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnhancementTaskTest {

	@TempDir
	private File projectDir;

	@BeforeEach
	public void beforeEach() throws Exception {
		copyJavFiles();
		prepareDestFolder();
	}

	@Test
	public void testEnhancementDefault() throws Exception {
		// The default settings for the enhancement task are as follows:
		//   enableLazyInitialization = 'true'
		//   enableDirtyTracking = 'true'
		//   enableAssociationManagement = 'false'
		//   enableExtendedEnhancement = 'false'
		// The files are read from folder 'dir' which needs to be a subfolder of 'base'
		// The property 'base' is mandatory
		// If 'dir' is not specified, a 'fileset' element can be used (see #testEnhancementFileSet)
		String enhanceTag =
				"<enhance base='${basedir}/dest' dir='${basedir}/dest'/>\n";
		Project project = createProject(enhanceTag);
		executeCompileTarget( project );
		executeEnhanceTarget( project );
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
		// The property 'base' is mandatory
		// The files are read from the specified 'fileset' element:
		//   - the folder specified by 'dir'
		//   - the 'Baz.class' file is excluded
		String enhanceTag =
				"<enhance base='${basedir}/dest'>\n" +
				"  <fileset dir='${basedir}/dest'>\n" +
				"    <exclude name='Baz.class' />\n" +
				"  </fileset>\n" +
				"</enhance>\n";
		Project project = createProject(enhanceTag);
		executeCompileTarget(project);
		executeEnhanceTarget(project);
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
		String enhanceTag =
				"<enhance \n" +
				"    base='${basedir}/dest'\n" +
				"    dir='${basedir}/dest'\n" +
				"    enableLazyInitialization='false'/>\n";
		Project project = createProject(enhanceTag);
		executeCompileTarget(project);
		executeEnhanceTarget(project);
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
		String enhanceTag =
				"<enhance \n" +
				"    base='${basedir}/dest'\n" +
				"    dir='${basedir}/dest'\n" +
				"    enableDirtyTracking='false'/>\n";
		Project project = createProject(enhanceTag);
		executeCompileTarget(project);
		executeEnhanceTarget(project);
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
		String enhanceTag =
				"<enhance \n" +
				"    base='${basedir}/dest'\n" +
				"    dir='${basedir}/dest'\n" +
				"    enableAssociationManagement='true'/>\n";
		Project project = createProject(enhanceTag);
		executeCompileTarget(project);
		executeEnhanceTarget(project);
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
		String enhanceTag =
				"<enhance \n" +
				"    base='${basedir}/dest'\n" +
				"    dir='${basedir}/dest'\n" +
				"    enableExtendedEnhancement='true'/>\n";
		Project project = createProject(enhanceTag);
		executeCompileTarget(project);
		executeEnhanceTarget(project);
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
		String enhanceTag =
				"<enhance \n" +
				"    base='${basedir}/dest'\n" +
				"    dir='${basedir}/dest'\n" +
				"    enableLazyInitialization='false'\n" +
				"    enableDirtyTracking='false'\n" +
				"    enableAssociationManagement='false'\n" +
				"    enableExtendedEnhancement='false'/>\n";
		Project project = createProject(enhanceTag);
		executeCompileTarget(project);
		executeEnhanceTarget(project);
		// None of the classes should be enhanced
		assertFalse( isEnhanced( "Bar" ));
		assertFalse( isEnhanced( "Baz" ));
		assertFalse( isEnhanced( "Foo" ) );
		// No association management is in place;
		assertFalse(isAssociationManagementPresent());
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

	private Project createProject(String enhanceTag) throws Exception {
		File buildXmlFile = createBuildXmlFile(enhanceTag);
		Project result = new Project();
		result.setBaseDir(projectDir);
		result.addBuildListener(createConsoleLogger());
		ProjectHelper.getProjectHelper().parse(result, buildXmlFile);
		return result;
	}

	private void executeCompileTarget(Project project) {
		// The class files should not exist
		assertFalse(fileExists("dest/Bar.class"));
		assertFalse(fileExists("dest/Baz.class"));
		assertFalse(fileExists("dest/Foo.class"));
		// Execute the 'compile' target
		project.executeTarget( "compile" );
		// The class files should exist now
		assertTrue( fileExists( "dest/Bar.class" ) );
		assertTrue( fileExists( "dest/Baz.class" ) );
		assertTrue( fileExists( "dest/Foo.class" ) );
	}

	private void executeEnhanceTarget(Project project) throws Exception {
		// The class files should not be enhanced at this point
		assertFalse( isEnhanced( "Bar" ));
		assertFalse( isEnhanced( "Baz" ));
		assertFalse( isEnhanced( "Foo" ));
		// Execute the 'enhance' target
		project.executeTarget( "enhance" );
		// The results are verified in the respective tests
	}

	private File createBuildXmlFile(String enhanceTag) throws Exception {
		File result = new File( projectDir, "build.xml" );
		assertFalse(result.exists());
		Files.writeString(
				result.toPath(),
				BUILD_XML_TEMPLATE.replace( "@enhanceTag@", enhanceTag ) );
		assertTrue( result.exists() );
		assertTrue(result.isFile());
		return result;
	}

	private DefaultLogger createConsoleLogger() {
		DefaultLogger consoleLogger = new DefaultLogger();
		consoleLogger.setErrorPrintStream(System.err);
		consoleLogger.setOutputPrintStream(System.out);
		consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
		return consoleLogger;
	}

	private boolean fileExists(String relativePath) {
		return new File( projectDir, relativePath ).exists();
	}

	private void prepareDestFolder() {
		File destFolder = new File(projectDir, "dest");
		assertFalse( destFolder.exists() );
		assertTrue( destFolder.mkdir() );
		assertTrue( destFolder.exists() );
		assertTrue( destFolder.isDirectory() );
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

	private ClassLoader getTestClassLoader() throws Exception {
		return new URLClassLoader( new URL[] { new File(projectDir, "dest").toURI().toURL() } );
	}

	private void copyJavFiles() throws Exception {
		File srcDir = new File(projectDir, "src");
		srcDir.mkdir();
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

	private static String BUILD_XML_TEMPLATE =
			"<project>\n" +
			"  <taskdef \n" +
			"      name='enhance'\n" +
			"      classname='org.hibernate.tool.enhance.EnhancementTask'/>\n" +
			"  <target name='compile'>\n" +
			"    <javac srcdir='src' destdir='dest' includeantruntime='true' />\n" +
			"  </target>\n" +
			"  <target name='enhance'>\n" +
			"    @enhanceTag@\n" +
			"  </target>\n" +
			"</project>";
}
