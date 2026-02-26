/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.gradle;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hibernate.tool.it.gradle.TestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunSqlTest extends TestTemplate {

	@BeforeEach
	public void beforeEach() {
		setGradleTaskToPerform("runSql");
	}

	@Test
	void testRunSql() throws Exception {
		setHibernateToolsExtensionSection(
				"hibernateTools {\n" +
				"  sqlToRun = 'create table foo (id int not null primary key, baz varchar(256))'\n" +
				"}\n"
		);
		assertNull(getDatabaseFile());
		createProjectAndExecuteGradleCommand();
		assertTrue(getBuildResult().getOutput().contains("Running SQL: create table foo (id int not null primary key, baz varchar(256))"));
		assertNotNull(getDatabaseFile());
		assertTrue(getDatabaseFile().exists());
	}

}
