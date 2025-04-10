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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.gradle.testkit.runner.BuildResult;
import org.hibernate.tool.gradle.test.func.utils.FuncTestConstants;
import org.hibernate.tool.gradle.test.func.utils.FuncTestTemplate;
import org.junit.jupiter.api.Test;

class RunSqlTest extends FuncTestTemplate implements FuncTestConstants {

    private static final String BUILD_FILE_HIBERNATE_TOOLS_SECTION = 
    		"hibernateTools {\n" +
    		"  sqlToRun = 'create table foo (id int not null primary key, baz varchar(256))'\n" +
    		"  hibernateProperties = 'foo.bar'" +
    		"}\n";

	@Override
	public String getBuildFileHibernateToolsSection() {
	    return BUILD_FILE_HIBERNATE_TOOLS_SECTION;
	}
	
	@Override
	public String getHibernatePropertiesFileName() {
		return "foo.bar";
	}

    @Test 
    void testRunSql() throws IOException {
    	performTask("runSql", false);
    }
    
    @Override
    protected void verifyBuild(BuildResult buildResult) {
        assertTrue(buildResult.getOutput().contains("Running SQL: create table foo (id int not null primary key, baz varchar(256))"));
        assertTrue(new File(projectDir, DATABASE_FOLDER_NAME + "/" + DATABASE_FILE_NAME).exists());
    }

 }