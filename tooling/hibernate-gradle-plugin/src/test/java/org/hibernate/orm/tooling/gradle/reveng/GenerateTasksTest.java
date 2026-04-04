/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.reveng;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class GenerateTasksTest {

	private static final URL[] URLS = new URL[] {};
	private Project project;

	@BeforeEach
	void setUp() {
		project = ProjectBuilder.builder().build();
	}

	@Test
	public void testGenerateJavaTaskCreation() throws Exception {
		TestableJavaTask task = project.getTasks().create("genJava", TestableJavaTask.class);
		assertNotNull(task);
		RevengSpec spec = new RevengSpec();
		task.initialize(spec);
		assertSame(spec, task.getRevengSpec());
	}

	@Test
	public void testGenerateDaoTaskCreation() throws Exception {
		TestableDaoTask task = project.getTasks().create("genDao", TestableDaoTask.class);
		assertNotNull(task);
		RevengSpec spec = new RevengSpec();
		task.initialize(spec);
		assertSame(spec, task.getRevengSpec());
	}

	@Test
	public void testGenerateCfgTaskCreation() throws Exception {
		TestableCfgTask task = project.getTasks().create("genCfg", TestableCfgTask.class);
		assertNotNull(task);
		RevengSpec spec = new RevengSpec();
		task.initialize(spec);
		assertSame(spec, task.getRevengSpec());
	}

	@Test
	public void testGenerateHbmTaskCreation() throws Exception {
		TestableHbmTask task = project.getTasks().create("genHbm", TestableHbmTask.class);
		assertNotNull(task);
		RevengSpec spec = new RevengSpec();
		task.initialize(spec);
		assertSame(spec, task.getRevengSpec());
	}

	@Test
	public void testGenerateJavaTaskPerform() {
		TestableJavaTask task = project.getTasks().create("genJava2", TestableJavaTask.class);
		task.perform();
		assertNotNull(TestableJavaTask.usedClassLoader);
	}

	@Test
	public void testGenerateDaoTaskPerform() {
		TestableDaoTask task = project.getTasks().create("genDao2", TestableDaoTask.class);
		task.perform();
		assertNotNull(TestableDaoTask.usedClassLoader);
	}

	@Test
	public void testGenerateCfgTaskPerform() {
		TestableCfgTask task = project.getTasks().create("genCfg2", TestableCfgTask.class);
		task.perform();
		assertNotNull(TestableCfgTask.usedClassLoader);
	}

	@Test
	public void testGenerateHbmTaskPerform() {
		TestableHbmTask task = project.getTasks().create("genHbm2", TestableHbmTask.class);
		task.perform();
		assertNotNull(TestableHbmTask.usedClassLoader);
	}

	@Test
	public void testRunSqlTaskCreation() {
		RunSqlTask task = project.getTasks().create("runSql", RunSqlTask.class);
		assertNotNull(task);
		assertNull(task.getRevengSpec());
	}

	// Testable subclasses that override doWork() and resolveProjectClassPath()
	// to avoid JDBC/Gradle API dependencies

	public static class TestableJavaTask extends GenerateJavaTask {
		static ClassLoader usedClassLoader;
		void doWork() { usedClassLoader = Thread.currentThread().getContextClassLoader(); }
		URL[] resolveProjectClassPath() { return URLS; }
	}

	public static class TestableDaoTask extends GenerateDaoTask {
		static ClassLoader usedClassLoader;
		void doWork() { usedClassLoader = Thread.currentThread().getContextClassLoader(); }
		URL[] resolveProjectClassPath() { return URLS; }
	}

	public static class TestableCfgTask extends GenerateCfgTask {
		static ClassLoader usedClassLoader;
		void doWork() { usedClassLoader = Thread.currentThread().getContextClassLoader(); }
		URL[] resolveProjectClassPath() { return URLS; }
	}

	public static class TestableHbmTask extends GenerateHbmTask {
		static ClassLoader usedClassLoader;
		void doWork() { usedClassLoader = Thread.currentThread().getContextClassLoader(); }
		URL[] resolveProjectClassPath() { return URLS; }
	}
}
