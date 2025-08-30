/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.model.fileset.FileSet;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.persistence.Entity;

public class EnhancerMojoTest {

	@TempDir
	File tempDir;

	private List<String> logMessages = new ArrayList<String>();

	private Field classesDirectoryField;
	private Field fileSetsField;
	private Field sourceSetField;
	private Field enhancerField;

	private File classesDirectory;  // folder '${tempDir}/classes'
	private File fooFolder;         // folder '${classesDirectory}/org/foo'
	private File barFolder;         // folder '${classesDirectory}/bar'
	private File barClassFile;      // file '${fooFolder}/Bar.class'
	private File fooTxtFile;        // file '${barFolder}/Foo.txt'

	private EnhancerMojo enhanceMojo;

	@BeforeEach
	void beforeEach() throws Exception {
		classesDirectoryField = EnhancerMojo.class.getDeclaredField("classesDirectory");
		classesDirectoryField.setAccessible(true);
		fileSetsField = EnhancerMojo.class.getDeclaredField("fileSets");
		fileSetsField.setAccessible(true);
		sourceSetField = EnhancerMojo.class.getDeclaredField("sourceSet");
		sourceSetField.setAccessible(true);
		enhancerField = EnhancerMojo.class.getDeclaredField("enhancer");
		enhancerField.setAccessible(true);
		enhanceMojo = new EnhancerMojo();
		enhanceMojo.setLog(createLog());
		classesDirectory = new File(tempDir, "classes");
		classesDirectory.mkdirs();
		classesDirectoryField.set(enhanceMojo, classesDirectory);
		fooFolder = new File(classesDirectory, "org/foo");
		fooFolder.mkdirs();
		barFolder = new File(classesDirectory, "bar");
		barFolder.mkdirs();
		barClassFile = new File(fooFolder, "Bar.class");
		barClassFile.createNewFile();
		fooTxtFile = new File (barFolder, "Foo.txt");
		fooTxtFile.createNewFile();
	}

	@Test
	void testAssembleSourceSet() throws Exception {
		Method assembleSourceSetMethod = EnhancerMojo.class.getDeclaredMethod("assembleSourceSet");
		assembleSourceSetMethod.setAccessible(true);
		FileSet[] fileSets = new FileSet[1];
		fileSets[0] = new FileSet();
		fileSets[0].setDirectory(classesDirectory.getAbsolutePath());
		fileSetsField.set(enhanceMojo, fileSets);
		List<?> sourceSet = (List<?>)sourceSetField.get(enhanceMojo);
		assertTrue(sourceSet.isEmpty());
		assembleSourceSetMethod.invoke(enhanceMojo);
		assertFalse(sourceSet.isEmpty());
		assertTrue(sourceSet.contains(barClassFile));
		assertFalse(sourceSet.contains(fooTxtFile));
		assertEquals(1, sourceSet.size());
		// verify the log messages
		assertEquals(7, logMessages.size());
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.STARTING_ASSEMBLY_OF_SOURCESET));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.PROCESSING_FILE_SET));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.USING_BASE_DIRECTORY.formatted(classesDirectory)));
		assertTrue(logMessages.contains( INFO + EnhancerMojo.ADDED_FILE_TO_SOURCE_SET.formatted(barClassFile)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.SKIPPING_NON_CLASS_FILE.formatted(fooTxtFile)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.FILESET_PROCESSED_SUCCESFULLY));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.ENDING_ASSEMBLY_OF_SOURCESET));
	}

	@Test
	void testAddFileSetToSourceSet() throws Exception {
		Method addFileSetToSourceSetMethod = EnhancerMojo.class.getDeclaredMethod(
				"addFileSetToSourceSet",
				new Class[] { FileSet.class});
		addFileSetToSourceSetMethod.setAccessible(true);
		File fooClassFile = new File(fooFolder, "Foo.class");
		fooClassFile.createNewFile();
		File bazFolder = new File(classesDirectory, "org/baz");
		bazFolder.mkdirs();
		File bazClassFile = new File(bazFolder, "Baz.class");
		bazClassFile.createNewFile();
		FileSet fileSet = new FileSet();
		fileSet.setDirectory(classesDirectory.getAbsolutePath());
		fileSet.addInclude("**/Foo*");
		fileSet.addInclude("**/*.class");
		fileSet.addExclude("**/baz/**");
		addFileSetToSourceSetMethod.invoke(enhanceMojo, fileSet);
		// verify log messages
		assertEquals(6, logMessages.size());
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.PROCESSING_FILE_SET));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.USING_BASE_DIRECTORY.formatted(classesDirectory)));
		assertTrue(logMessages.contains( INFO + EnhancerMojo.ADDED_FILE_TO_SOURCE_SET.formatted(barClassFile)));
		assertTrue(logMessages.contains( INFO + EnhancerMojo.ADDED_FILE_TO_SOURCE_SET.formatted(fooClassFile)));
		assertFalse(logMessages.contains( INFO + EnhancerMojo.ADDED_FILE_TO_SOURCE_SET.formatted(bazClassFile)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.SKIPPING_NON_CLASS_FILE.formatted(fooTxtFile)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.FILESET_PROCESSED_SUCCESFULLY));
	}

	@Test
	void testCreateClassLoader() throws Exception {
		Method createClassLoaderMethod = EnhancerMojo.class.getDeclaredMethod("createClassLoader");
		createClassLoaderMethod.setAccessible(true);
		ClassLoader classLoader = (ClassLoader)createClassLoaderMethod.invoke(enhanceMojo);
		assertNotNull(classLoader);
		URL fooResource = classLoader.getResource("bar/Foo.txt");
		assertNotNull(fooResource);
		assertEquals(fooTxtFile.toURI().toURL(), fooResource);
		// verify log messages
		// verify log messages
		assertEquals(1, logMessages.size());
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.CREATE_URL_CLASSLOADER_FOR_FOLDER.formatted(classesDirectory)));
	}

	@Test
	void testCreateEnhancementContext() throws Exception {
		Method createEnhancementContextMethod = EnhancerMojo.class.getDeclaredMethod("createEnhancementContext");
		createEnhancementContextMethod.setAccessible(true);
		EnhancementContext enhancementContext = (EnhancementContext)createEnhancementContextMethod.invoke(enhanceMojo);
		URLClassLoader classLoader = (URLClassLoader)enhancementContext.getLoadingClassLoader();
		assertEquals(classesDirectory.toURI().toURL(), classLoader.getURLs()[0]);
		assertFalse(enhancementContext.doBiDirectionalAssociationManagement(null));
		assertFalse(enhancementContext.doDirtyCheckingInline(null));
		assertFalse(enhancementContext.hasLazyLoadableAttributes(null));
		assertFalse(enhancementContext.isLazyLoadable(null));
		assertFalse(enhancementContext.doExtendedEnhancement(null));
		// verify log messages
		assertEquals(2, logMessages.size());
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.CREATE_ENHANCEMENT_CONTEXT));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.CREATE_URL_CLASSLOADER_FOR_FOLDER.formatted(classesDirectory)));
		logMessages.clear();
		Field enableAssociationManagementField = EnhancerMojo.class.getDeclaredField("enableAssociationManagement");
		enableAssociationManagementField.setAccessible(true);
		enableAssociationManagementField.set(enhanceMojo, Boolean.TRUE);
		enhancementContext = (EnhancementContext)createEnhancementContextMethod.invoke(enhanceMojo);
		assertEquals(classesDirectory.toURI().toURL(), classLoader.getURLs()[0]);
		assertTrue(enhancementContext.doBiDirectionalAssociationManagement(null));
		assertFalse(enhancementContext.doDirtyCheckingInline(null));
		assertFalse(enhancementContext.hasLazyLoadableAttributes(null));
		assertFalse(enhancementContext.isLazyLoadable(null));
		assertFalse(enhancementContext.doExtendedEnhancement(null));
		// verify log messages
		assertEquals(2, logMessages.size());
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.CREATE_ENHANCEMENT_CONTEXT));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.CREATE_URL_CLASSLOADER_FOR_FOLDER.formatted(classesDirectory)));
		logMessages.clear();
		Field enableDirtyTrackingField = EnhancerMojo.class.getDeclaredField("enableDirtyTracking");
		enableDirtyTrackingField.setAccessible(true);
		enableDirtyTrackingField.set(enhanceMojo, Boolean.TRUE);
		enhancementContext = (EnhancementContext)createEnhancementContextMethod.invoke(enhanceMojo);
		assertEquals(classesDirectory.toURI().toURL(), classLoader.getURLs()[0]);
		assertTrue(enhancementContext.doBiDirectionalAssociationManagement(null));
		assertTrue(enhancementContext.doDirtyCheckingInline(null));
		assertFalse(enhancementContext.hasLazyLoadableAttributes(null));
		assertFalse(enhancementContext.isLazyLoadable(null));
		assertFalse(enhancementContext.doExtendedEnhancement(null));
		// verify log messages
		assertEquals(2, logMessages.size());
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.CREATE_ENHANCEMENT_CONTEXT));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.CREATE_URL_CLASSLOADER_FOR_FOLDER.formatted(classesDirectory)));
		logMessages.clear();
		Field enableLazyInitializationField = EnhancerMojo.class.getDeclaredField("enableLazyInitialization");
		enableLazyInitializationField.setAccessible(true);
		enableLazyInitializationField.set(enhanceMojo, Boolean.TRUE);
		enhancementContext = (EnhancementContext)createEnhancementContextMethod.invoke(enhanceMojo);
		assertEquals(classesDirectory.toURI().toURL(), classLoader.getURLs()[0]);
		assertTrue(enhancementContext.doBiDirectionalAssociationManagement(null));
		assertTrue(enhancementContext.doDirtyCheckingInline(null));
		assertTrue(enhancementContext.hasLazyLoadableAttributes(null));
		assertTrue(enhancementContext.isLazyLoadable(null));
		assertFalse(enhancementContext.doExtendedEnhancement(null));
		// verify log messages
		assertEquals(2, logMessages.size());
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.CREATE_ENHANCEMENT_CONTEXT));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.CREATE_URL_CLASSLOADER_FOR_FOLDER.formatted(classesDirectory)));
		logMessages.clear();
		Field enableExtendedEnhancementField = EnhancerMojo.class.getDeclaredField("enableExtendedEnhancement");
		enableExtendedEnhancementField.setAccessible(true);
		enableExtendedEnhancementField.set(enhanceMojo, Boolean.TRUE);
		enhancementContext = (EnhancementContext)createEnhancementContextMethod.invoke(enhanceMojo);
		assertEquals(classesDirectory.toURI().toURL(), classLoader.getURLs()[0]);
		assertTrue(enhancementContext.doBiDirectionalAssociationManagement(null));
		assertTrue(enhancementContext.doDirtyCheckingInline(null));
		assertTrue(enhancementContext.hasLazyLoadableAttributes(null));
		assertTrue(enhancementContext.isLazyLoadable(null));
		assertTrue(enhancementContext.doExtendedEnhancement(null));
		// verify log messages
		assertEquals(2, logMessages.size());
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.CREATE_ENHANCEMENT_CONTEXT));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.CREATE_URL_CLASSLOADER_FOR_FOLDER.formatted(classesDirectory)));
		logMessages.clear();
	}

	@Test
	void testCreateEnhancer() throws Exception {
		Method createEnhancerMethod = EnhancerMojo.class.getDeclaredMethod("createEnhancer");
		createEnhancerMethod.setAccessible(true);
		Enhancer enhancer = (Enhancer)enhancerField.get(enhanceMojo);
		assertNull(enhancer);
		createEnhancerMethod.invoke(enhanceMojo);
		enhancer = (Enhancer)enhancerField.get(enhanceMojo);
		assertNotNull(enhancer);
		Field byteByddyEnhancementContextField = EnhancerImpl.class.getDeclaredField("enhancementContext");
		byteByddyEnhancementContextField.setAccessible(true);
		Object byteByddyEnhancementContext = byteByddyEnhancementContextField.get(enhancer);
		assertNotNull(byteByddyEnhancementContext);
		Field enhancementContextField = byteByddyEnhancementContext.getClass().getDeclaredField("enhancementContext");
		enhancementContextField.setAccessible(true);
		EnhancementContext enhancementContext = (EnhancementContext)enhancementContextField.get(byteByddyEnhancementContext);
		assertNotNull(enhancementContext);
		ClassLoader classLoader = enhancementContext.getLoadingClassLoader();
		assertNotNull(classLoader);
		assertNotNull(classLoader);
		URL fooResource = classLoader.getResource("bar/Foo.txt");
		assertNotNull(fooResource);
		assertEquals(fooTxtFile.toURI().toURL(), fooResource);
		// verify log messages
		assertEquals(3, logMessages.size());
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.CREATE_BYTECODE_ENHANCER));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.CREATE_ENHANCEMENT_CONTEXT));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.CREATE_URL_CLASSLOADER_FOR_FOLDER.formatted(classesDirectory)));
	}

	@Test
	void testDetermineClassName() throws Exception {
		Method determineClassNameMethod = EnhancerMojo.class.getDeclaredMethod(
				"determineClassName",
				new Class[] { File.class });
		determineClassNameMethod.setAccessible(true);
		assertEquals("org.foo.Bar", determineClassNameMethod.invoke(enhanceMojo, barClassFile));
		// check log messages
		assertEquals(1, logMessages.size());
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.DETERMINE_CLASS_NAME_FOR_FILE.formatted(barClassFile)));
	}

	@Test
	void testDiscoverTypesForClass() throws Exception {
		final List<Boolean> hasRun = new ArrayList<Boolean>();
		Method discoverTypesForClassMethod = EnhancerMojo.class.getDeclaredMethod(
				"discoverTypesForClass",
				new Class[] { File.class });
		discoverTypesForClassMethod.setAccessible(true);
		Enhancer enhancer = (Enhancer)Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class[] { Enhancer.class },
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if (method.getName().equals("discoverTypes")) {
							assertEquals("org.foo.Bar", args[0]);
							hasRun.add(0, true);
						}
						return null;
					}
				});
		enhancerField.set(enhanceMojo, enhancer);
		assertFalse(hasRun.contains(true));
		discoverTypesForClassMethod.invoke(enhanceMojo, barClassFile);
		assertTrue(hasRun.contains(true));
		// verify log messages
		assertEquals(3, logMessages.size());
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.TRYING_TO_DISCOVER_TYPES_FOR_CLASS_FILE.formatted(barClassFile)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.DETERMINE_CLASS_NAME_FOR_FILE.formatted(barClassFile)));
		assertTrue(logMessages.contains( INFO + EnhancerMojo.SUCCESFULLY_DISCOVERED_TYPES_FOR_CLASS_FILE.formatted(barClassFile)));
	}

	@Test
	void testDiscoverTypes() throws Exception {
		final List<Boolean> hasRun = new ArrayList<Boolean>();
		Method discoverTypesMethod = EnhancerMojo.class.getDeclaredMethod(
				"discoverTypes",
				new Class[] { });
		discoverTypesMethod.setAccessible(true);
		Enhancer enhancer = (Enhancer)Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class[] { Enhancer.class },
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if (method.getName().equals("discoverTypes")) {
							assertEquals("org.foo.Bar", args[0]);
							hasRun.add(0, true);
						}
						return null;
					}
				});
		enhancerField.set(enhanceMojo, enhancer);
		assertFalse(hasRun.contains(true));
		discoverTypesMethod.invoke(enhanceMojo);
		assertFalse(hasRun.contains(true));
		// verify the log messages
		assertEquals(2, logMessages.size());
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.STARTING_TYPE_DISCOVERY));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.ENDING_TYPE_DISCOVERY));
		logMessages.clear();
		List<File> sourceSet = new ArrayList<File>();
		sourceSet.add(barClassFile);
		sourceSetField.set(enhanceMojo, sourceSet);
		discoverTypesMethod.invoke(enhanceMojo);
		assertTrue(hasRun.contains(true));
		// verify the log messages
		assertEquals(5, logMessages.size());
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.STARTING_TYPE_DISCOVERY));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.TRYING_TO_DISCOVER_TYPES_FOR_CLASS_FILE.formatted(barClassFile)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.DETERMINE_CLASS_NAME_FOR_FILE.formatted(barClassFile)));
		assertTrue(logMessages.contains( INFO + EnhancerMojo.SUCCESFULLY_DISCOVERED_TYPES_FOR_CLASS_FILE.formatted(barClassFile)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.ENDING_TYPE_DISCOVERY));
	}

	@Test
	void testClearFile() throws Exception {
		Method clearFileMethod = EnhancerMojo.class.getDeclaredMethod(
				"clearFile",
				new Class[] { File.class });
		clearFileMethod.setAccessible(true);
		Files.writeString(fooTxtFile.toPath(), "foobar");
		fooTxtFile.setLastModified(0);
		assertEquals("foobar", new String(Files.readAllBytes(fooTxtFile.toPath())));
		boolean result = (boolean)clearFileMethod.invoke(enhanceMojo, new File("foobar"));
		assertFalse(result);
		result = (boolean)clearFileMethod.invoke(enhanceMojo, fooTxtFile);
		long modified = fooTxtFile.lastModified();
		assertTrue(result);
		// File should be empty
		assertTrue(Files.readAllBytes(fooTxtFile.toPath()).length == 0);
		// last modification 'after' should be after 'before'
		assertNotEquals(0, modified);
		assertTrue(modified > 0);
		// check log messages
		assertEquals(4, logMessages.size());
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.TRYING_TO_CLEAR_FILE.formatted("foobar")));
		assertTrue(logMessages.contains( ERROR + EnhancerMojo.UNABLE_TO_DELETE_FILE.formatted("foobar")));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.TRYING_TO_CLEAR_FILE.formatted(fooTxtFile)));
		assertTrue(logMessages.contains( INFO + EnhancerMojo.SUCCESFULLY_CLEARED_FILE.formatted(fooTxtFile)));
	}

	@Test
	void testWriteByteCodeToFile() throws Exception {
		Method writeByteCodeToFileMethod = EnhancerMojo.class.getDeclaredMethod(
				"writeByteCodeToFile",
				new Class[] { byte[].class, File.class});
		writeByteCodeToFileMethod.setAccessible(true);
		fooTxtFile.setLastModified(0);
		// File fooTxtFile is empty
		assertTrue(Files.readAllBytes(fooTxtFile.toPath()).length == 0);
		writeByteCodeToFileMethod.invoke(enhanceMojo, "foobar".getBytes(), fooTxtFile);
		long modified = fooTxtFile.lastModified();
		// last modification 'after' should be after 'before'
		assertNotEquals(0, modified);
		assertTrue(modified > 0);
		// File should be contain 'foobar'
		assertEquals(new String(Files.readAllBytes(fooTxtFile.toPath())), "foobar");
		// check log messages
		assertEquals(4, logMessages.size());
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.WRITING_BYTE_CODE_TO_FILE.formatted(fooTxtFile)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.TRYING_TO_CLEAR_FILE.formatted(fooTxtFile)));
		assertTrue(logMessages.contains( INFO + EnhancerMojo.SUCCESFULLY_CLEARED_FILE.formatted(fooTxtFile)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.AMOUNT_BYTES_WRITTEN_TO_FILE.formatted(6, fooTxtFile)));
	}

	@Test
	void testEnhanceClass() throws Exception {
		final List<Integer> calls = new ArrayList<Integer>();
		calls.add(0, 0);
		Method enhanceClassMethod = EnhancerMojo.class.getDeclaredMethod(
				"enhanceClass",
				new Class[] { File.class });
		enhanceClassMethod.setAccessible(true);
		Enhancer enhancer = (Enhancer)Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class[] { Enhancer.class },
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						calls.set(0, calls.get(0) + 1);
						if (method.getName().equals("enhance")) {
							assertEquals("org.foo.Bar", args[0]);
						}
						if (calls.get(0) == 1) {
							return "foobar".getBytes();
						} else if (calls.get(0) == 2) {
							return null;
						} else {
							throw new EnhancementException("foobar");
						}
					}
				});
		long beforeRuns = barClassFile.lastModified();
		// First Run -> file is modified
		enhancerField.set(enhanceMojo, enhancer);
		assertEquals(0, calls.get(0));
		enhanceClassMethod.invoke(enhanceMojo, barClassFile);
		long afterFirstRun = barClassFile.lastModified();
		assertEquals(1, calls.get(0));
		assertTrue(afterFirstRun >= beforeRuns);
		assertEquals("foobar", new String(Files.readAllBytes(barClassFile.toPath())));
		// verify log messages
		assertEquals(7, logMessages.size());
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.TRYING_TO_ENHANCE_CLASS_FILE.formatted(barClassFile)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.DETERMINE_CLASS_NAME_FOR_FILE.formatted(barClassFile)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.WRITING_BYTE_CODE_TO_FILE.formatted(barClassFile)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.TRYING_TO_CLEAR_FILE.formatted(barClassFile)));
		assertTrue(logMessages.contains( INFO + EnhancerMojo.SUCCESFULLY_CLEARED_FILE.formatted(barClassFile)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.AMOUNT_BYTES_WRITTEN_TO_FILE.formatted("foobar".length(), barClassFile)));
		assertTrue(logMessages.contains( INFO + EnhancerMojo.SUCCESFULLY_ENHANCED_CLASS_FILE.formatted(barClassFile)));
		// Second Run -> file is not modified
		logMessages.clear();
		enhanceClassMethod.invoke(enhanceMojo, barClassFile);
		long afterSecondRun = barClassFile.lastModified();
		assertEquals(2, calls.get(0));
		assertEquals(afterSecondRun, afterFirstRun);
		assertEquals("foobar", new String(Files.readAllBytes(barClassFile.toPath())));
		// verify log messages
		assertEquals(3, logMessages.size());
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.TRYING_TO_ENHANCE_CLASS_FILE.formatted(barClassFile)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.DETERMINE_CLASS_NAME_FOR_FILE.formatted(barClassFile)));
		assertTrue(logMessages.contains( INFO + EnhancerMojo.SKIPPING_FILE.formatted(barClassFile)));
		// Third Run -> exception!
		logMessages.clear();
		try {
			enhanceClassMethod.invoke(enhanceMojo, barClassFile);
			fail();
		} catch (Throwable e) {
			long afterThirdRun = barClassFile.lastModified();
			assertEquals(3, calls.get(0));
			assertEquals(afterThirdRun, afterFirstRun);
			assertEquals("foobar", new String(Files.readAllBytes(barClassFile.toPath())));
			// verify log messages
			assertEquals(3, logMessages.size());
			assertTrue(logMessages.contains( DEBUG + EnhancerMojo.TRYING_TO_ENHANCE_CLASS_FILE.formatted(barClassFile)));
			assertTrue(logMessages.contains( DEBUG + EnhancerMojo.DETERMINE_CLASS_NAME_FOR_FILE.formatted(barClassFile)));
			assertTrue(logMessages.contains( ERROR + EnhancerMojo.ERROR_WHILE_ENHANCING_CLASS_FILE.formatted(barClassFile)));
		}
	}

	@Test
	void testPerformEnhancement() throws Exception {
		final List<Boolean> hasRun = new ArrayList<Boolean>();
		Method performEnhancementMethod = EnhancerMojo.class.getDeclaredMethod(
				"performEnhancement",
				new Class[] { });
		performEnhancementMethod.setAccessible(true);
		Enhancer enhancer = (Enhancer)Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class[] { Enhancer.class },
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if (method.getName().equals("enhance")) {
							assertEquals("org.foo.Bar", args[0]);
							hasRun.add(0, true);
						}
						return "foobar".getBytes();
					}
				});
		enhancerField.set(enhanceMojo, enhancer);
		List<File> sourceSet = new ArrayList<File>();
		sourceSet.add(barClassFile);
		sourceSetField.set(enhanceMojo, sourceSet);
		long lastModified = barClassFile.lastModified();
		assertFalse(hasRun.contains(true));
		assertNotEquals("foobar", new String(Files.readAllBytes(barClassFile.toPath())));
		performEnhancementMethod.invoke(enhanceMojo);
		assertTrue(hasRun.contains(true));
		assertEquals("foobar", new String(Files.readAllBytes(barClassFile.toPath())));
		assertEquals(lastModified, barClassFile.lastModified());
		// verify the log messages
		assertEquals(9, logMessages.size());
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.STARTING_CLASS_ENHANCEMENT));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.TRYING_TO_ENHANCE_CLASS_FILE.formatted(barClassFile)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.DETERMINE_CLASS_NAME_FOR_FILE.formatted(barClassFile)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.WRITING_BYTE_CODE_TO_FILE.formatted(barClassFile)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.TRYING_TO_CLEAR_FILE.formatted(barClassFile)));
		assertTrue(logMessages.contains( INFO + EnhancerMojo.SUCCESFULLY_CLEARED_FILE.formatted(barClassFile)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.AMOUNT_BYTES_WRITTEN_TO_FILE.formatted("foobar".length(), barClassFile)));
		assertTrue(logMessages.contains( INFO + EnhancerMojo.SUCCESFULLY_ENHANCED_CLASS_FILE.formatted(barClassFile)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.ENDING_CLASS_ENHANCEMENT));
	}

	@Test
	void testExecute() throws Exception {
		Field enableDirtyTrackingField = EnhancerMojo.class.getDeclaredField( "enableDirtyTracking" );
		enableDirtyTrackingField.setAccessible( true );
		enableDirtyTrackingField.set( enhanceMojo, true );
		Method executeMethod = EnhancerMojo.class.getDeclaredMethod("execute", new Class[] {});
		executeMethod.setAccessible(true);
		final String barSource =
				"package org.foo;" +
						"import jakarta.persistence.Entity;" +
						"@Entity public class Bar { "+
						"    private String foo; " +
						"    String getFoo() {  return foo; } " +
						"    public void setFoo(String f) { foo = f; } " +
						"}";
		File barJavaFile = new File(fooFolder, "Bar.java");
		Files.writeString(barJavaFile.toPath(), barSource);
		final String fooSource =
				"package org.foo;" +
						"public class Foo { "+
						"    private Bar bar; " +
						"    Bar getBar() {  return bar; } " +
						"    public void setBar(Bar b) { bar = b; } " +
						"}";
		File fooJavaFile = new File(fooFolder, "Foo.java");
		Files.writeString(fooJavaFile.toPath(), fooSource);
		File fooClassFile = new File(fooFolder, "Foo.class");
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		URL url = Entity.class.getProtectionDomain().getCodeSource().getLocation();
		String classpath = new File(url.toURI()).getAbsolutePath();
		String[] options = List.of(
				"-cp",
				classpath,
				barJavaFile.getAbsolutePath(),
				fooJavaFile.getAbsolutePath()).toArray(new String[] {});
		compiler.run(null, null, null, options);
		String barBytesString = new String(Files.readAllBytes(barClassFile.toPath()));
		String fooBytesString = new String(Files.readAllBytes(fooClassFile.toPath()));
		List<File> sourceSet = new ArrayList<File>();
		sourceSet.add(barClassFile);
		sourceSet.add(fooClassFile);
		sourceSetField.set(enhanceMojo, sourceSet);
		assertTrue(logMessages.isEmpty());
		executeMethod.invoke(enhanceMojo);
		assertNotEquals(barBytesString, new String(Files.readAllBytes(barClassFile.toPath())));
		assertEquals(fooBytesString, new String(Files.readAllBytes(fooClassFile.toPath())));
		URLClassLoader classLoader = new URLClassLoader(
				new URL[] {classesDirectory.toURI().toURL()},
				getClass().getClassLoader());
		Class<?> barClass = classLoader.loadClass("org.foo.Bar");
		assertNotNull(barClass);
		Method m = barClass.getMethod("$$_hibernate_getEntityInstance", new Class[]{});
		assertNotNull(m);
		Class<?> fooClass = classLoader.loadClass("org.foo.Foo");
		try {
			m = fooClass.getMethod("$$_hibernate_getEntityInstance", new Class[]{});
			fail();
		} catch (NoSuchMethodException e) {
			assertEquals("org.foo.Foo.$$_hibernate_getEntityInstance()", e.getMessage());
		}
		classLoader.close();
		// verify in the log messages at least if all the needed methods have been invoked
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.STARTING_EXECUTION_OF_ENHANCE_MOJO));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.ADDED_DEFAULT_FILESET_WITH_BASE_DIRECTORY.formatted(classesDirectory)));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.STARTING_ASSEMBLY_OF_SOURCESET));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.CREATE_BYTECODE_ENHANCER));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.STARTING_TYPE_DISCOVERY));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.STARTING_CLASS_ENHANCEMENT));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.ENDING_EXECUTION_OF_ENHANCE_MOJO));
	}

	@Test
	void testProcessParameters() throws Exception {
		Method processParametersMethod = EnhancerMojo.class.getDeclaredMethod(
				"processParameters",
				new Class[] {});
		processParametersMethod.setAccessible(true);
		Field enableLazyInitializationField = EnhancerMojo.class.getDeclaredField("enableLazyInitialization");
		enableLazyInitializationField.setAccessible(true);
		Field enableDirtyTrackingField = EnhancerMojo.class.getDeclaredField("enableDirtyTracking");
		enableDirtyTrackingField.setAccessible(true);
		assertTrue(logMessages.isEmpty());
		assertNull(fileSetsField.get(enhanceMojo));
		processParametersMethod.invoke(enhanceMojo);
		assertEquals(3, logMessages.size());
		assertTrue(logMessages.contains( WARNING + EnhancerMojo.ENABLE_LAZY_INITIALIZATION_DEPRECATED));
		assertTrue(logMessages.contains( WARNING + EnhancerMojo.ENABLE_DIRTY_TRACKING_DEPRECATED));
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.ADDED_DEFAULT_FILESET_WITH_BASE_DIRECTORY.formatted(classesDirectory)));
		FileSet[] fileSets = (FileSet[])fileSetsField.get(enhanceMojo);
		assertNotNull(fileSets);
		assertEquals(1, fileSets.length);
		assertEquals(classesDirectory.getAbsolutePath(), fileSets[0].getDirectory());
		fileSetsField.set(enhanceMojo, null);
		logMessages.clear();
		enableLazyInitializationField.set(enhanceMojo, Boolean.TRUE);
		enableDirtyTrackingField.set(enhanceMojo, Boolean.TRUE);
		processParametersMethod.invoke(enhanceMojo);
		assertEquals(1, logMessages.size());
		assertTrue(logMessages.contains( DEBUG + EnhancerMojo.ADDED_DEFAULT_FILESET_WITH_BASE_DIRECTORY.formatted(classesDirectory)));
	}

	private Log createLog() {
		return (Log)Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class[] { Log.class},
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if ("info".equals(method.getName())) {
							logMessages.add(INFO + args[0]);
						} else if ("warn".equals(method.getName())) {
							logMessages.add(WARNING + args[0]);
						} else if ("error".equals(method.getName())) {
							logMessages.add(ERROR + args[0]);
						} else if ("debug".equals(method.getName())) {
							logMessages.add(DEBUG + args[0]);
						}
						return null;
					}
				});
	}

	static final String DEBUG = "[DEBUG] ";
	static final String ERROR = "[ERROR] ";
	static final String WARNING = "[WARNING] ";
	static final String INFO = "[INFO] ";

}
