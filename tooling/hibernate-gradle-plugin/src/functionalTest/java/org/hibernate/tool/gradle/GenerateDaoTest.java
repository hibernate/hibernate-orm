/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.gradle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.hibernate.tool.it.gradle.TestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenerateDaoTest extends TestTemplate {

	@BeforeEach
	public void beforeEach() {
		setGradleTaskToPerform("generateDao");
		setDatabaseCreationScript(new String[] {
				"create table FOO (ID int not null, BAR varchar(20), primary key (ID))"
		});
	}

	@Test
	void testGenerateDao() throws Exception {
		setHibernateToolsExtensionSection(
				"hibernateTools { \n" +
				"  packageName = 'foo.model'\n" +
				"}"
		);
		createProjectAndExecuteGradleCommand();
		File generatedSourcesFolder = new File(getProjectDir(), "app/generated-sources");
		assertTrue(getBuildResult().getOutput().contains("Starting DAO export to directory: "));
		assertTrue(generatedSourcesFolder.exists());
		assertTrue(generatedSourcesFolder.isDirectory());
		File fooFile = new File(generatedSourcesFolder, "foo/model/FooHome.java");
		assertTrue(fooFile.exists());
		assertTrue(fooFile.isFile());
	}

}
