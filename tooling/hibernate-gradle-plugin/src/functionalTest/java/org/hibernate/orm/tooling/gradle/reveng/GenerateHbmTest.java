/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.reveng;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenerateHbmTest extends TestTemplate {

	@BeforeEach
	public void beforeEach() {
		setGradleTaskToPerform("generateHbm");
		setDatabaseCreationScript(new String[] {
				"create table FOO (ID int not null, BAR varchar(20), primary key (ID))"
		});
	}

	@Test
	void testGenerateHbm() throws Exception {
		setRevengExtensionSection(
				"    packageName = 'foo.model'"
		);
		createProjectAndExecuteGradleCommand();
		File generatedSourcesFolder = new File(getProjectDir(), "app/generated-sources");
		assertTrue(getBuildResult().getOutput().contains("Starting HBM export to directory: "));
		assertTrue(generatedSourcesFolder.exists());
		assertTrue(generatedSourcesFolder.isDirectory());
		File fooFile = new File(generatedSourcesFolder, "foo/model/Foo.hbm.xml");
		assertTrue(fooFile.exists());
		assertTrue(fooFile.isFile());
	}

}
