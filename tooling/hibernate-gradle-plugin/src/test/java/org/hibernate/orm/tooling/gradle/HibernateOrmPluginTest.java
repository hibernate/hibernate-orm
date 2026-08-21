/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle;

import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.api.Project;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HibernateOrmPluginTest {

	@Test
	void testApply() {
		// Create a test project and apply the plugin
		Project project = ProjectBuilder.builder().build();
		project.getPlugins().apply("java");
		project.getPlugins().apply("org.hibernate.orm");

		// Verify the result
		assertNotNull(project.getTasks().findByName("generateJava"));
		assertNotNull(project.getTasks().findByName("runSql"));
		assertNotNull(project.getTasks().findByName("generateCfg"));
		assertNotNull(project.getTasks().findByName("generateHbm"));
		assertNotNull(project.getTasks().findByName("generateDao"));

		Object extension = project.getExtensions().getByName("hibernate");
		assertNotNull(extension);
		assertTrue(extension instanceof HibernateOrmSpec);
	}

}
