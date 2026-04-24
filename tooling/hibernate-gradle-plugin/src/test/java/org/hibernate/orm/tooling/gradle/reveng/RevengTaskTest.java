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
package org.hibernate.orm.tooling.gradle.reveng;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.net.URL;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.hibernate.tool.reveng.api.reveng.RevengStrategy;
import org.hibernate.tool.reveng.internal.strategy.AbstractStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RevengTaskTest {
	
	private static ClassLoader USED_CLASS_LOADER;
	private static URL[] URLS = new URL[] {};
	
	private RevengTask abstractTask = null;
	
	private Field revengSpecField = null;
	private RevengSpec revengSpec = null;

	@BeforeEach
	void beforeEach() throws Exception {
		USED_CLASS_LOADER = null;
		Project project = ProjectBuilder.builder().build();
		abstractTask = project.getTasks().create("foo", FooTask.class);
		revengSpecField = RevengTask.class.getDeclaredField("revengSpec");
		revengSpecField.setAccessible(true);
		revengSpec = new RevengSpec();
	}
	
	@Test
	void testInitialize() throws Exception {
		assertNull(revengSpecField.get(abstractTask));
		abstractTask.initialize(revengSpec);
		assertSame(revengSpec, revengSpecField.get(abstractTask));
	}

	@Test
	void testGetRevengSpec() throws Exception {
		assertNull(abstractTask.getRevengSpec());
		revengSpecField.set(abstractTask, revengSpec);
		assertSame(revengSpec, abstractTask.getRevengSpec());
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
		revengSpec.revengStrategy = FooStrategy.class.getName();
		revengSpecField.set(abstractTask, revengSpec);
		RevengStrategy revengStrategy = abstractTask.setupReverseEngineeringStrategy();
		assertTrue(revengStrategy instanceof FooStrategy);
	}
	
	public static class FooStrategy extends AbstractStrategy {}
	
	public static class FooTask extends RevengTask {
		void doWork() {
			USED_CLASS_LOADER = Thread.currentThread().getContextClassLoader();
		}
		URL[] resolveProjectClassPath() {
			return URLS;
		}
	}

}
