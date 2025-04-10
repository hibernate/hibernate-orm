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
import java.nio.file.Files;

import org.gradle.testkit.runner.BuildResult;
import org.hibernate.tool.gradle.test.func.utils.FuncTestConstants;
import org.hibernate.tool.gradle.test.func.utils.FuncTestTemplate;
import org.junit.jupiter.api.Test;

class GenerateCfgTest extends FuncTestTemplate implements FuncTestConstants {

    @Test 
    void testGenerateCfg() throws IOException {
    	performTask("generateCfg", true);
    }
    
    @Override
    public void verifyBuild(BuildResult buildResult) {
    	try {
	        File generatedSourcesFolder = new File(projectDir, "generated-sources");
	        assertTrue(buildResult.getOutput().contains("Starting CFG export to directory: " + generatedSourcesFolder.getCanonicalPath()));
	        assertTrue(generatedSourcesFolder.exists());
	        assertTrue(generatedSourcesFolder.isDirectory());
	        File cfgFile = new File(generatedSourcesFolder, "hibernate.cfg.xml");
	        assertTrue(cfgFile.exists());
	        assertTrue(cfgFile.isFile());
	        String cfgContents = Files.readString(cfgFile.toPath());
	        assertTrue(cfgContents.contains("<mapping resource=\"Foo.hbm.xml\"/>"));
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }

 }
