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
