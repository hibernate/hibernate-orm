package org.hibernate.tool.it.gradle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.io.TempDir;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

public class TestTemplate {

    protected static final List<String> GRADLE_INIT_PROJECT_ARGUMENTS = List.of(
            "init", "--type", "java-application", "--dsl", "groovy", "--test-framework", "junit-jupiter", "--java-version", "17");

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

    protected void createGradleProject() throws Exception {
        GradleRunner runner = GradleRunner.create();
        runner.withArguments(GRADLE_INIT_PROJECT_ARGUMENTS);
        runner.forwardOutput();
        runner.withProjectDir(getProjectDir());
        BuildResult buildResult = runner.build();
        assertTrue(buildResult.getOutput().contains("BUILD SUCCESSFUL"));
        setGradlePropertiesFile(new File(getProjectDir(), "gradle.properties"));
        assertTrue(getGradlePropertiesFile().exists());
        assertTrue(getGradlePropertiesFile().isFile());
        File appDir = new File(getProjectDir(), "app");
        assertTrue(appDir.exists());
        assertTrue(appDir.isDirectory());
        setGradleBuildFile(new File(appDir, "build.gradle"));
        assertTrue(getGradleBuildFile().exists());
        assertTrue(getGradleBuildFile().isFile());
        setDatabaseFile(new File(getProjectDir(), "database/test.mv.db"));
        assertFalse(getDatabaseFile().exists());
    }

    protected void editGradleBuildFile() throws Exception {
        StringBuffer gradleBuildFileContents = new StringBuffer(
                new String(Files.readAllBytes(getGradleBuildFile().toPath())));
        addHibernateToolsPluginLine(gradleBuildFileContents);
        addH2DatabaseDependencyLine(gradleBuildFileContents);
        addHibernateToolsExtension(gradleBuildFileContents);
        Files.writeString(getGradleBuildFile().toPath(), gradleBuildFileContents.toString());
    }

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

    protected void addH2DatabaseDependencyLine(StringBuffer gradleBuildFileContents) {
        int pos = gradleBuildFileContents.indexOf("dependencies {");
        pos = gradleBuildFileContents.indexOf("}", pos);
        gradleBuildFileContents.insert(pos, constructH2DatabaseDependencyLine() + "\n");
    }

    protected void addHibernateToolsPluginLine(StringBuffer gradleBuildFileContents) {
        int pos = gradleBuildFileContents.indexOf("plugins {");
        pos = gradleBuildFileContents.indexOf("}", pos);
        gradleBuildFileContents.insert(pos, constructHibernateToolsPluginLine() + "\n");
    }

    protected void addHibernateToolsExtension(StringBuffer gradleBuildFileContents) {

    }

}
