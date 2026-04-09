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