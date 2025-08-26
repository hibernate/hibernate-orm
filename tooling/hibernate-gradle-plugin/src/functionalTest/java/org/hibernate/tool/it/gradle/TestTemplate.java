package org.hibernate.tool.it.gradle;

import java.io.File;

import org.junit.jupiter.api.io.TempDir;

public class TestTemplate {

    @TempDir
    private File projectDir;

    private File gradlePropertiesFile;
    private File gradleBuildFile;

    protected File getProjectDir() { return projectDir; }
    protected File getGradlePropertiesFile() { return gradlePropertiesFile; }
    protected void setGradlePropertiesFile(File f) { this.gradlePropertiesFile = f; }
    protected File getGradleBuildFile() { return gradleBuildFile; }
    protected void setGradleBuildFile(File f) { gradleBuildFile = f; }

}
