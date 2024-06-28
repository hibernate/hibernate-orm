package org.hibernate.tool.gradle.test.func.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.io.TempDir;

public class FuncTestTemplate implements FuncTestConstants {

	@TempDir
    protected File projectDir;

	protected File getBuildFile() {
        return new File(projectDir, GRADLE_BUILD_FILE_NAME);
    }

	protected File getSettingsFile() {
        return new File(projectDir, GRADLE_SETTINGS_FILE_NAME);
    }
    
	protected File getDatabaseFile() {
    	File databaseDir = new File(projectDir, DATABASE_FOLDER_NAME);
    	databaseDir.mkdirs();
    	return new File(databaseDir, DATABASE_FILE_NAME);
    }
    
	protected File getHibernatePropertiesFile() {
    	File resourcesDir = new File(projectDir, RESOURCES_FOLDER_PATH);
    	resourcesDir.mkdirs();
    	return new File(resourcesDir, getHibernatePropertiesFileName());
    }
	
	protected String getHibernatePropertiesContents() {
		return HIBERNATE_PROPERTIES_CONTENTS.replace("${projectDir}", projectDir.getAbsolutePath());
	}
    
	protected void copyDatabase() {
    	try {
    		Files.copy(
    				new File(getClass().getClassLoader().getResource(DATABASE_FILE_NAME).toURI()).toPath(), 
    				getDatabaseFile().toPath());
    	} catch (URISyntaxException | IOException e) {
    		throw new RuntimeException(e);
    	}
    }
	    
    protected void writeString(File file, String string) {
        try (Writer writer = new FileWriter(file)) {
            writer.write(string);
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
    }
    
    protected void performTask(String taskName, boolean needDatabase) {
    	prepareBuild(needDatabase);
    	verifyBuild(runBuild(taskName));
    }
    
    protected void prepareBuild(boolean needDatabase) {
        writeString(getSettingsFile(), "");
        writeString(getBuildFile(),getBuildFileContents());
        writeString(getHibernatePropertiesFile(), getHibernatePropertiesContents());
        if (needDatabase) {
        	copyDatabase();
        }
    }
    
    protected BuildResult runBuild(String taskName) {
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments(taskName);
        runner.withProjectDir(projectDir);
        return runner.build();
    }
    
    protected void verifyBuild(BuildResult buildResult) {}
    
}
