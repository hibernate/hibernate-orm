/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2024-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.gradle.task;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.net.URL;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.gradle.Extension;
import org.hibernate.tool.internal.reveng.strategy.AbstractStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AbstractTaskTest {
	
	private static ClassLoader USED_CLASS_LOADER;
	private static URL[] URLS = new URL[] {};
	
	private AbstractTask abstractTask = null;
	
	private Field extensionField = null;
	private Extension extension = null;
	
	@BeforeEach
	void beforeEach() throws Exception {
		USED_CLASS_LOADER = null;
		Project project = ProjectBuilder.builder().build();
		abstractTask = project.getTasks().create("foo", FooTask.class);
		extensionField = AbstractTask.class.getDeclaredField("extension");
		extensionField.setAccessible(true);
		extension = new Extension(project);
	}
	
	@Test
	void testInitialize() throws Exception {
		assertNull(extensionField.get(abstractTask));
		abstractTask.initialize(extension);
		assertSame(extension, extensionField.get(abstractTask));
	}
	
	@Test
	void testGetExtension() throws Exception {
		assertNull(abstractTask.getExtension());
		extensionField.set(abstractTask, extension);
		assertSame(extension, abstractTask.getExtension());
	}
	
	@Test
	void testPerform() {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		assertNull(USED_CLASS_LOADER);
		abstractTask.perform();
		assertNotNull(USED_CLASS_LOADER);
		assertNotSame(contextClassLoader, USED_CLASS_LOADER);
	}
	
	@Test
	void testResolveProjectClassPath() {
		assertSame(URLS, abstractTask.resolveProjectClassPath());
	}
	
	@Test
	public void testSetupReverseEngineeringStrategy() throws Exception {
		extension.revengStrategy = FooStrategy.class.getName();
		extensionField.set(abstractTask, extension);
		RevengStrategy revengStrategy = abstractTask.setupReverseEngineeringStrategy();
		assertTrue(revengStrategy instanceof FooStrategy);
	}
	
	public static class FooStrategy extends AbstractStrategy {}
	
	public static class FooTask extends AbstractTask {
		void doWork() {
			USED_CLASS_LOADER = Thread.currentThread().getContextClassLoader();
		}
		URL[] resolveProjectClassPath() {
			return URLS;
		}
	}

}
