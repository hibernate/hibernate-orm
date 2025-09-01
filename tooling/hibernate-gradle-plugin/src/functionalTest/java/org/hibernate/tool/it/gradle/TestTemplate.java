package org.hibernate.tool.it.gradle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.io.TempDir;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

public class TestTemplate {

    protected static final String[] GRADLE_INIT_PROJECT_ARGUMENTS = new String[] {
            "init", "--type", "java-application", "--dsl", "groovy", "--test-framework", "junit-jupiter", "--java-version", "17"};

    @TempDir
    private File projectDir;

    private File gradlePropertiesFile;
    private File gradleBuildFile;
    private File databaseFile;

    private String[] databaseCreationScript;
    private String hibernateToolsExtensionSection;

    protected File getProjectDir() { return projectDir; }
    protected File getGradlePropertiesFile() { return gradlePropertiesFile; }
    protected void setGradlePropertiesFile(File f) { this.gradlePropertiesFile = f; }
    protected File getGradleBuildFile() { return gradleBuildFile; }
    protected void setGradleBuildFile(File f) { gradleBuildFile = f; }
    protected File getDatabaseFile() { return databaseFile; }
    protected void setDatabaseFile(File f) { databaseFile = f; }
    protected String[] getDatabaseCreationScript() { return databaseCreationScript; }
    protected void setDatabaseCreationScript(String[] script) { databaseCreationScript = script; }
    protected String getHibernateToolsExtensionSection() { return hibernateToolsExtensionSection; }
    protected void setHibernateToolsExtensionSection(String s) { hibernateToolsExtensionSection = s; }

    protected void executeGradleCommand(String ... gradleCommandLine) {
        GradleRunner runner = GradleRunner.create();
        runner.withArguments(gradleCommandLine);
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withProjectDir(getProjectDir());
        BuildResult buildResult = runner.build();
        assertTrue(buildResult.getOutput().contains("BUILD SUCCESSFUL"));
    }

    protected void createProject() throws Exception {
        initGradleProject();
        editGradleBuildFile();
        editGradlePropertiesFile();
        createDatabase();
        createHibernatePropertiesFile();
    }

    protected void initGradleProject() throws Exception {
        executeGradleCommand(GRADLE_INIT_PROJECT_ARGUMENTS);
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

    protected void editGradlePropertiesFile() throws Exception {
        // The Hibernate Tools Gradle plugin does not support the configuration cache.
        // As this is enabled by default when initializing a new Gradle project, the setting needs to be commented out
        // in the gradle.properties file.
        StringBuffer gradlePropertiesFileContents = new StringBuffer(
                new String(Files.readAllBytes(getGradlePropertiesFile().toPath())));
        int pos = gradlePropertiesFileContents.indexOf("org.gradle.configuration-cache=true");
        gradlePropertiesFileContents.insert(pos, "#");
        Files.writeString(getGradlePropertiesFile().toPath(), gradlePropertiesFileContents.toString());
    }

    protected void createHibernatePropertiesFile() throws Exception {
        File hibernatePropertiesFile = new File(getProjectDir(), "app/src/main/resources/hibernate.properties");
        StringBuffer hibernatePropertiesFileContents = new StringBuffer();
        hibernatePropertiesFileContents
                .append("hibernate.connection.driver_class=org.h2.Driver\n")
                .append("hibernate.connection.url=" + constructJdbcConnectionString() + "\n")
                .append("hibernate.connection.username=\n")
                .append("hibernate.connection.password=\n")
                .append("hibernate.default_catalog=TEST\n")
                .append("hibernate.default_schema=PUBLIC\n");
        Files.writeString(hibernatePropertiesFile.toPath(), hibernatePropertiesFileContents.toString());
        assertTrue(hibernatePropertiesFile.exists());
    }

    protected void createDatabase() throws Exception {
        Connection connection = DriverManager.getConnection(constructJdbcConnectionString());
        Statement statement = connection.createStatement();
        for (String sql : getDatabaseCreationScript()) {
            statement.execute(sql);
        }
        statement.close();
        connection.close();
        assertTrue(getDatabaseFile().exists());
        assertTrue(getDatabaseFile().isFile());
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
        String extension = getHibernateToolsExtensionSection();
        if (extension != null) {
            int pos = gradleBuildFileContents.indexOf("dependencies {");
            pos = gradleBuildFileContents.indexOf("}", pos);
            gradleBuildFileContents.insert(pos + 1, "\n\n" + extension);
        }
    }

}


