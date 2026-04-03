/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.enhance;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EnhancementTaskTest {

	@TempDir
	private File tempDir;

	private Project antProject;

	@BeforeEach
	public void setUp() {
		antProject = new Project();
		antProject.init();
	}

	@Test
	public void testExecuteNoBaseThrows() {
		EnhancementTask task = new EnhancementTask();
		task.setProject(antProject);
		// base is required
		assertThrows(BuildException.class, task::execute);
	}

	@Test
	public void testExecuteAllFeaturesDisabled() {
		EnhancementTask task = new EnhancementTask();
		task.setProject(antProject);
		task.setBase(tempDir.getAbsolutePath());
		task.setDir(tempDir.getAbsolutePath());
		task.setEnableLazyInitialization(false);
		task.setEnableDirtyTracking(false);
		task.setEnableAssociationManagement(false);
		task.setEnableExtendedEnhancement(false);
		// Should skip because no features are enabled
		assertDoesNotThrow(task::execute);
	}

	@Test
	public void testExecuteDirNotSubdirectoryOfBase() {
		EnhancementTask task = new EnhancementTask();
		task.setProject(antProject);
		task.setBase("/some/base");
		task.setDir("/different/dir");
		assertThrows(BuildException.class, task::execute);
	}

	@Test
	public void testExecuteDirDoesNotExist() {
		EnhancementTask task = new EnhancementTask();
		task.setProject(antProject);
		String base = tempDir.getAbsolutePath();
		task.setBase(base);
		task.setDir(base + "/nonexistent");
		// Should skip gracefully when dir doesn't exist
		assertDoesNotThrow(task::execute);
	}

	@Test
	public void testExecuteEmptyDir() {
		EnhancementTask task = new EnhancementTask();
		task.setProject(antProject);
		String base = tempDir.getAbsolutePath();
		File classesDir = new File(tempDir, "classes");
		classesDir.mkdirs();
		task.setBase(base);
		task.setDir(classesDir.getAbsolutePath());
		// Should skip because no .class files exist
		assertDoesNotThrow(task::execute);
	}

	@Test
	public void testExecuteWithClassFiles() throws IOException {
		EnhancementTask task = new EnhancementTask();
		task.setProject(antProject);
		String base = tempDir.getAbsolutePath();
		File classesDir = new File(tempDir, "com/example");
		classesDir.mkdirs();
		// Create a dummy .class file (won't be valid bytecode, so enhancement will fail)
		File dummyClass = new File(classesDir, "Dummy.class");
		try (FileOutputStream fos = new FileOutputStream(dummyClass)) {
			fos.write(new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE});
		}
		task.setBase(base);
		task.setDir(base);
		task.setFailOnError(false); // Don't fail on invalid class file
		// Should attempt enhancement but handle errors gracefully
		assertDoesNotThrow(task::execute);
	}

	@Test
	public void testFilesetAndDirConflict() {
		EnhancementTask task = new EnhancementTask();
		task.setProject(antProject);
		task.setBase(tempDir.getAbsolutePath());
		task.setDir(tempDir.getAbsolutePath());
		FileSet fs = new FileSet();
		fs.setProject(antProject);
		fs.setDir(tempDir);
		task.addFileset(fs);
		assertThrows(BuildException.class, task::execute);
	}

	@Test
	public void testFilesetEmptySourceSet() {
		EnhancementTask task = new EnhancementTask();
		task.setProject(antProject);
		task.setBase(tempDir.getAbsolutePath());
		// dir is null, so fileset path is taken
		File emptyDir = new File(tempDir, "empty");
		emptyDir.mkdirs();
		FileSet fs = new FileSet();
		fs.setProject(antProject);
		fs.setDir(emptyDir);
		fs.setIncludes("**/*.class");
		task.addFileset(fs);
		// No .class files => skips gracefully
		assertDoesNotThrow(task::execute);
	}

	@Test
	public void testSetters() {
		EnhancementTask task = new EnhancementTask();
		task.setBase("/tmp/base");
		task.setDir("/tmp/base/classes");
		task.setFailOnError(true);
		task.setEnableLazyInitialization(true);
		task.setEnableDirtyTracking(true);
		task.setEnableAssociationManagement(true);
		task.setEnableExtendedEnhancement(true);
	}
}
