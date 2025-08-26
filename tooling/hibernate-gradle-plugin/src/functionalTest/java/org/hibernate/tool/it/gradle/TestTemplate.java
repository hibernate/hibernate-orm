package org.hibernate.tool.it.gradle;

import java.io.File;

import org.junit.jupiter.api.io.TempDir;

public class TestTemplate {

    @TempDir
    private File projectDir;

    private File gradlePropertiesFile;

    protected File getProjectDir() {
        return projectDir;
    }

    protected File getGradlePropertiesFile() {
        return gradlePropertiesFile;
    }

    protected void setGradlePropertiesFile(File f) {
        this.gradlePropertiesFile = f;
    }

}
