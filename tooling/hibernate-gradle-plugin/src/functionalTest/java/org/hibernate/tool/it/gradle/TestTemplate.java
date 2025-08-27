package org.hibernate.tool.it.gradle;

import java.io.File;

import org.junit.jupiter.api.io.TempDir;

public class TestTemplate {

    @TempDir
    private File projectDir;

    private File gradlePropertiesFile;
    private File gradleBuildFile;
    private File databaseFile;

    protected File getProjectDir() { return projectDir; }
    protected File getGradlePropertiesFile() { return gradlePropertiesFile; }
    protected void setGradlePropertiesFile(File f) { this.gradlePropertiesFile = f; }
    protected File getGradleBuildFile() { return gradleBuildFile; }
    protected void setGradleBuildFile(File f) { gradleBuildFile = f; }
    protected File getDatabaseFile() { return databaseFile; }
    protected void setDatabaseFile(File f) { databaseFile = f; }

    protected String constructH2DatabaseDependencyLine() {
        return "    implementation 'com.h2database:h2:" + System.getenv("H2_VERSION") + "'";
    }

    protected String constructHibernateToolsPluginLine() {
        return "    id 'org.hibernate.tool.hibernate-tools-gradle' version '"
                + System.getenv("HIBERNATE_TOOLS_VERSION") + "'";
    }

    protected String constructJdbcConnectionString() {
        return "jdbc:h2:" + getProjectDir().getAbsolutePath() + "/database/test;AUTO_SERVER=TRUE";
    }

}
