/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
