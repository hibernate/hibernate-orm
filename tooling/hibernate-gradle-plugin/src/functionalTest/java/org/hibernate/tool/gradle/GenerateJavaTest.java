package org.hibernate.tool.gradle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.gradle.testkit.runner.BuildResult;
import org.hibernate.tool.gradle.test.func.utils.FuncTestConstants;
import org.hibernate.tool.gradle.test.func.utils.FuncTestTemplate;
import org.junit.jupiter.api.Test;

class GenerateJavaTest extends FuncTestTemplate implements FuncTestConstants {

    private static final String BUILD_FILE_HIBERNATE_TOOLS_SECTION = 
            "hibernateTools {\n" +
            "  packageName = 'foo.model'\n" +
            "}\n";

	@Override
	public String getBuildFileHibernateToolsSection() {
	    return BUILD_FILE_HIBERNATE_TOOLS_SECTION;
	}

    @Test 
    void testGenerateJava() throws IOException {
    	performTask("generateJava", true);
    }
    
    @Override
    protected void verifyBuild(BuildResult buildResult) {
    	try {
	        File generatedSourcesFolder = new File(projectDir, "generated-sources");
	        assertTrue(buildResult.getOutput().contains(
	        		"Starting Java export to directory: " + generatedSourcesFolder.getCanonicalPath()));
	        assertTrue(generatedSourcesFolder.exists());
	        assertTrue(generatedSourcesFolder.isDirectory());
	        File fooFile = new File(generatedSourcesFolder, "foo/model/Foo.java");
	        assertTrue(fooFile.exists());
	        assertTrue(fooFile.isFile());
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }
    
  }
