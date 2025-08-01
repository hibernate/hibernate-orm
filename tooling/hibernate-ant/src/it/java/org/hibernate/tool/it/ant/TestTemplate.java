package org.hibernate.tool.it.ant;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public abstract class TestTemplate {

    @TempDir
    private File projectDir;

    private String hibernateToolTaskXml;
    private String[] databaseCreationScript;

    protected File getProjectDir() {
        return projectDir;
    }

    protected void setHibernateToolTaskXml(String xml) {
        this.hibernateToolTaskXml = xml;
    }

    protected void setDatabaseCreationScript(String[] sqls) {
        this.databaseCreationScript = sqls;
    }

    protected String constructBuildXmlFileContents() {
        assertNotNull(hibernateToolTaskXml);
        return buildXmlFileContents.replace("@hibernateToolTaskXml@", hibernateToolTaskXml);
    }

    protected String getFileContents(String path) throws Exception {
        return new String(
                Files.readAllBytes(
                        new File(getProjectDir(), path).toPath()));
    }

    protected void createProjectAndBuild() throws Exception {
        createBuildXmlFile();
        createDatabase();
        createHibernatePropertiesFile();
        runAntBuild();
    }

    protected void assertFolderExists(String path, int amountOfChildren) {
        File generatedOutputFolder = new File(getProjectDir(), path);
        assertTrue(generatedOutputFolder.exists());
        assertTrue(generatedOutputFolder.isDirectory());
        assertEquals(amountOfChildren, Objects.requireNonNull(generatedOutputFolder.list()).length);
    }

    protected void assertFileExists(String path) {
        File generatedPersonJavaFile = new File(getProjectDir(), path);
        assertTrue(generatedPersonJavaFile.exists());
        assertTrue(generatedPersonJavaFile.isFile());
    }

    private void runAntBuild() {
        File buildXmlFile = new File(getProjectDir(), "build.xml");
        Project project = new Project();
        project.setBaseDir(getProjectDir());
        project.addBuildListener(getConsoleLogger());
        ProjectHelper.getProjectHelper().parse(project, buildXmlFile);
        project.executeTarget(project.getDefaultTarget());
    }

    private void createBuildXmlFile() throws Exception {
        File buildXmlFile = new File(getProjectDir(), "build.xml");
        assertFalse(buildXmlFile.exists());
        Files.writeString(buildXmlFile.toPath(), constructBuildXmlFileContents());
        assertTrue(buildXmlFile.exists());
    }

    private String constructJdbcConnectionString() {
        return "jdbc:h2:" + getProjectDir().getAbsolutePath() + "/database/test;AUTO_SERVER=TRUE";
    }

    private void createDatabase() throws Exception {
        File databaseFile = new File(getProjectDir(), "database/test.mv.db");
        assertFalse(databaseFile.exists());
        assertFalse(databaseFile.isFile());
        Connection connection = DriverManager.getConnection(constructJdbcConnectionString());
        Statement statement = connection.createStatement();
        for (String sql : databaseCreationScript) {
            statement.execute(sql);
        }
        statement.close();
        connection.close();
        assertTrue(databaseFile.exists());
        assertTrue(databaseFile.isFile());
    }

    private void createHibernatePropertiesFile() throws Exception {
        File hibernatePropertiesFile = new File(getProjectDir(), "hibernate.properties");
        String hibernatePropertiesFileContents =
                "hibernate.connection.driver_class=org.h2.Driver\n" +
                "hibernate.connection.url=" + constructJdbcConnectionString() + "\n" +
                "hibernate.connection.username=\n" +
                "hibernate.connection.password=\n" +
                "hibernate.default_catalog=TEST\n" +
                "hibernate.default_schema=PUBLIC\n";
        Files.writeString(hibernatePropertiesFile.toPath(), hibernatePropertiesFileContents);
        assertTrue(hibernatePropertiesFile.exists());
    }

    private DefaultLogger getConsoleLogger() {
        DefaultLogger consoleLogger = new DefaultLogger();
        consoleLogger.setErrorPrintStream(System.err);
        consoleLogger.setOutputPrintStream(System.out);
        consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
        return consoleLogger;
    }

    private static final String buildXmlFileContents =
            """
                    <project name='tutorial' default='reveng'>                          \s
                        <taskdef                                                        \s
                                name='hibernatetool'                                    \s
                                classname='org.hibernate.tool.ant.HibernateToolTask'/>  \s
                        <target name='reveng'>                                          \s
                    @hibernateToolTaskXml@\
                        </target>                                                       \s
                    </project>                                                          \s
                    """;

}
