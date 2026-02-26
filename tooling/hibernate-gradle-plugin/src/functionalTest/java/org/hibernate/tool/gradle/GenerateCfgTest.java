/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.gradle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.hibernate.tool.it.gradle.TestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenerateCfgTest extends TestTemplate {

	@BeforeEach
	public void beforeEach() {
		setGradleTaskToPerform("generateCfg");
		setDatabaseCreationScript(new String[] {
				"create table FOO (ID int not null, BAR varchar(20), primary key (ID))"
		});
	}

	@Test
	void testGenerateCfg() throws Exception {
		createProjectAndExecuteGradleCommand();
		File generatedSourcesFolder = new File(getProjectDir(), "app/generated-sources");
		assertTrue(getBuildResult().getOutput().contains("Starting CFG export to directory: "));
		File cfgFile = new File(generatedSourcesFolder, "hibernate.cfg.xml");
		assertTrue(cfgFile.exists());
		assertTrue(cfgFile.isFile());
		String cfgContents = Files.readString(cfgFile.toPath());
		assertTrue(cfgContents.contains("<mapping resource=\"Foo.hbm.xml\"/>"));
	}

}
