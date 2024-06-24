package org.hibernate.tool.gradle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.common.io.Files;

public class GenerateJavaFunctionalTest {

    private static final String DATABASE_NAME = "bardb";
    private static final String DATABASE_FILE_NAME = DATABASE_NAME + ".mv.db";
    private static final String DATABASE_FOLDER_NAME = "database";
    private static final String DATABASE_PATH = DATABASE_FOLDER_NAME + "/" + DATABASE_NAME;
    private static final String HIBERNATE_PROPERTIES_FILE_NAME = "hibernate.properties";
    private static final String RESOURCES_FOLDER_PATH = "src/main/resources";
    private static final String GRADLE_BUILD_FILE_NAME = "build.gradle";
    private static final String GRADLE_SETTINGS_FILE_NAME = "settions.gradle";
    private static final String PROJECT_DIR_PLACEHOLDER = "${projectDir}";

    private static final String HIBERNATE_PROPERTIES_CONTENTS = 
    		"hibernate.connection.driver_class=org.h2.Driver\n" +
    	    "hibernate.connection.url=jdbc:h2:" + PROJECT_DIR_PLACEHOLDER + "/" + DATABASE_PATH + "\n" +
    	    "hibernate.connection.username=sa\n" +
    	    "hibernate.connection.password=\n" 
    ;
    
    private static final String BUILD_FILE_CONTENTS = 
            "plugins {\n" +
            "  id('application')\n" +
            "  id('org.hibernate.tool.hibernate-tools-gradle')\n" +
            "}\n" +
            "repositories {\n" +
            "  mavenCentral()\n" +
            "}\n" +
            "dependencies {\n" +
            "  implementation('com.h2database:h2:2.1.214')\n" +
            "}\n" +
            "hibernateTools {\n" +
            "}\n";

	@TempDir
    File projectDir;

	private File getBuildFile() {
        return new File(projectDir, GRADLE_BUILD_FILE_NAME);
    }

    private File getSettingsFile() {
        return new File(projectDir, GRADLE_SETTINGS_FILE_NAME);
    }
    
    private File getDatabaseFile() {
    	File databaseDir = new File(projectDir, DATABASE_FOLDER_NAME);
    	databaseDir.mkdirs();
    	return new File(databaseDir, DATABASE_FILE_NAME);
    }
    
    private File getHibernatePropertiesFile() {
    	File resourcesDir = new File(projectDir, RESOURCES_FOLDER_PATH);
    	resourcesDir.mkdirs();
    	return new File(resourcesDir, HIBERNATE_PROPERTIES_FILE_NAME);
    }
    
    private void copyDatabase() {
    	try {
    		Files.copy(
    				new File(getClass().getClassLoader().getResource(DATABASE_FILE_NAME).toURI()), 
    				getDatabaseFile());
    	} catch (URISyntaxException | IOException e) {
    		throw new RuntimeException(e);
    	}
    }
	    
    @Test 
    void testGenerateJava() throws IOException {
    	// Set up the project
        writeString(getSettingsFile(), "");
        writeString(getBuildFile(),BUILD_FILE_CONTENTS);
        writeString(getHibernatePropertiesFile(), HIBERNATE_PROPERTIES_CONTENTS.replace("${projectDir}", projectDir.getAbsolutePath()));
        copyDatabase();        

        // Run the 'generateJava' task
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("generateJava");
        runner.withProjectDir(projectDir);
        BuildResult result = runner.build();

        // Verify the result
        File generatedSourcesFolder = new File(projectDir, "generated-sources");
        assertTrue(result.getOutput().contains("Starting POJO export to directory: " + generatedSourcesFolder.getAbsolutePath()));
        assertTrue(generatedSourcesFolder.exists());
        assertTrue(generatedSourcesFolder.isDirectory());
        File fooFile = new File(generatedSourcesFolder, "Foo.java");
        assertTrue(fooFile.exists());
        assertTrue(fooFile.isFile());
    }

    private void writeString(File file, String string) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            writer.write(string);
        }
    }
    
 }
