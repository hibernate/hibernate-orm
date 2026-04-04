/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.misc;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransformationNamingTest {

	@Test
	public void testAreNoneDefinedWhenAllAbsent() {
		Project project = ProjectBuilder.builder().build();
		TransformHbmXmlTask task = project.getTasks().create("t", TransformHbmXmlTask.class);
		TransformationNaming naming = task.getRenaming();
		assertTrue(naming.areNoneDefined());
	}

	@Test
	public void testAreNoneDefinedWhenPrefixSet() {
		Project project = ProjectBuilder.builder().build();
		TransformHbmXmlTask task = project.getTasks().create("t", TransformHbmXmlTask.class);
		TransformationNaming naming = task.getRenaming();
		naming.getPrefix().set("pre-");
		assertFalse(naming.areNoneDefined());
	}

	@Test
	public void testAreNoneDefinedWhenSuffixSet() {
		Project project = ProjectBuilder.builder().build();
		TransformHbmXmlTask task = project.getTasks().create("t", TransformHbmXmlTask.class);
		TransformationNaming naming = task.getRenaming();
		naming.getSuffix().set("-suf");
		assertFalse(naming.areNoneDefined());
	}

	@Test
	public void testAreNoneDefinedWhenExtensionSet() {
		Project project = ProjectBuilder.builder().build();
		TransformHbmXmlTask task = project.getTasks().create("t", TransformHbmXmlTask.class);
		TransformationNaming naming = task.getRenaming();
		naming.getExtension().set("xml");
		assertFalse(naming.areNoneDefined());
	}
}
