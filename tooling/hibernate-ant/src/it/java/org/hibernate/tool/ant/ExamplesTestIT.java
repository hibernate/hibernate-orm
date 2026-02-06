package org.hibernate.tool.ant;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ExamplesTestIT {

    private static File baseFolder;

    @BeforeAll
    public static void beforeAll() throws Exception {
        // The needed resource for this test are put in place
        // in the 'baseFolder' (normally 'target/test-classes')
        // by the 'build-helper-maven-plugin' execution.
        // See the 'pom.xml'
        baseFolder = determineBaseFolder();
        editIncludedXml();
        overwriteHibernateProperties();
        createDatabase();
    }

    @Test
    public void test5MinuteTutorial() {
        File buildFile = new File(baseFolder, "5-minute-tutorial/build.xml");
        Project project = createProject(buildFile);
        assertNotNull(project);
        File personFile = new File(baseFolder, "5-minute-tutorial/generated/Person.java");
        assertFalse(personFile.exists());
        project.executeTarget("reveng");
        assertTrue(personFile.exists());
    }

    @Test
    public void testCfgXml() throws Exception {
        File buildFile = new File(baseFolder, "cfgxml/build.xml");
        File cfgXmlFile = new File(baseFolder, "cfgxml/hibernate.cfg.xml");
        String cfgXmlFileContents = Files.readString(cfgXmlFile.toPath())
            .replace("jdbc:h2:tcp://localhost/./sakila", constructJdbcConnectionString())
            .replace(">sa<", "><")
            .replace(">SAKILA<", ">TEST<");
        Files.writeString(cfgXmlFile.toPath(), cfgXmlFileContents);
        Project project = createProject(buildFile);
        assertNotNull(project);
        File personFile = new File(baseFolder, "cfgxml/generated/Person.java");
        assertFalse(personFile.exists());
        project.executeTarget("reveng");
        assertTrue(personFile.exists());
    }

    @Test
    public void testClasspath() {
        PrintStream savedOut = System.out;
        try {
            File buildFile = new File(baseFolder, "classpath/build.xml");
            Project project = createProject(buildFile);
            assertNotNull(project);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            assertFalse(out.toString().contains("Hello from Exporter!"));
            System.setOut(new PrintStream(out));
            project.executeTarget("reveng");
            assertTrue(out.toString().contains("Hello from Exporter!"));
        } finally {
            System.setOut(savedOut);
        }
    }

    @Test
    public void testConfigurationDefault() {
        File buildFile = new File(baseFolder, "configuration/default/build.xml");
        Project project = createProject(buildFile);
        assertNotNull(project);
        File cfgXmlFile = new File(baseFolder, "configuration/default/generated/hibernate.cfg.xml");
        assertFalse(cfgXmlFile.exists());
        project.executeTarget("reveng");
        assertTrue(cfgXmlFile.exists());
    }

    @Test
    public void testConfigurationFileset() {
        File buildFile = new File(baseFolder, "configuration/fileset/build.xml");
        Project project = createProject(buildFile);
        assertNotNull(project);
        File cfgXmlFile = new File(baseFolder, "configuration/fileset/generated/Foo.java");
        assertFalse(cfgXmlFile.exists());
        project.executeTarget("reveng");
        assertTrue(cfgXmlFile.exists());
    }

    @Test
    public void testJpa() {
        File buildFile = new File(baseFolder, "jpa/build.xml");
        Project project = createProject(buildFile);
        assertNotNull(project);
        File barSqlFile = new File(baseFolder, "jpa/generated/bar.sql");
        assertFalse(barSqlFile.exists());
        project.executeTarget("reveng");
        assertTrue(barSqlFile.exists());
    }

    @Test
    public void testNative() {
        File buildFile = new File(baseFolder, "native/build.xml");
        Project project = createProject(buildFile);
        assertNotNull(project);
        File fooSqlFile = new File(baseFolder, "native/generated/foo.sql");
        assertFalse(fooSqlFile.exists());
        project.executeTarget("reveng");
        assertTrue(fooSqlFile.exists());
    }

    @Test
    public void testProperties() {
        PrintStream savedOut = System.out;
        try {
            File buildFile = new File(baseFolder, "properties/build.xml");
            Project project = createProject(buildFile);
            assertNotNull(project);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            System.setOut(new PrintStream(out));
            assertFalse(out.toString().contains("Hello, World!"));
            assertFalse(out.toString().contains("Hello, Foo!"));
            assertFalse(out.toString().contains("Hello, Bar!"));
            project.executeTarget("reveng");
            assertTrue(out.toString().contains("Hello, World!"));
            assertTrue(out.toString().contains("Hello, Foo!"));
            assertTrue(out.toString().contains("Hello, Bar!"));
        } finally {
            System.setOut(savedOut);
        }
    }

    @Test
    public void testTemplatePath() throws Exception {
        File buildFile = new File(baseFolder, "templatepath/build.xml");
        Project project = createProject(buildFile);
        assertNotNull(project);
        File personFile = new File(baseFolder, "templatepath/generated/Person.java");
        assertFalse(personFile.exists());
        project.executeTarget("reveng");
        assertTrue(personFile.exists());
        String personFileContents = new String(Files.readAllBytes(personFile.toPath()));
        assertTrue(personFileContents.contains("// This is just an example of a custom template"));
    }

    private Project createProject(File buildXmlFile) {
        Project result = new Project();
        ProjectHelper projectHelper = ProjectHelper.getProjectHelper();
        result.addReference(MagicNames.REFID_PROJECT_HELPER, projectHelper);
        result.setBaseDir(buildXmlFile.getParentFile());
        result.addBuildListener(createConsoleLogger());
        projectHelper.parse(result, buildXmlFile);
        return result;
    }

    private DefaultLogger createConsoleLogger() {
        DefaultLogger consoleLogger = new DefaultLogger();
        consoleLogger.setErrorPrintStream(System.err);
        consoleLogger.setOutputPrintStream(System.out);
        consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
        return consoleLogger;
    }

    private static void editIncludedXml() throws Exception {
        File xmlFile = new File(baseFolder, "common/included.xml");
        StringBuilder xmlFileContents = new StringBuilder(
                new String(Files.readAllBytes(xmlFile.toPath())));
        int start = xmlFileContents.indexOf("<ivy:cachepath");
        int end = xmlFileContents.indexOf("<ivy:cachepath", start + 1);
        xmlFileContents.replace(start, end, "");
        start = xmlFileContents.indexOf("<path refid=\"hibernate-tools.path\"/>");
        end = xmlFileContents.indexOf("<path refid=", start + 1);
        xmlFileContents.replace(start, end, "");
        Files.writeString(xmlFile.toPath(), xmlFileContents.toString());
    }

    private static void overwriteHibernateProperties() throws Exception {
        File hibernatePropertiesFile = new File(baseFolder, "common/hibernate.properties");
        String hibernatePropertiesFileContents =
                "hibernate.connection.driver_class=org.h2.Driver"               + System.lineSeparator() +
                "hibernate.connection.url=" + constructJdbcConnectionString()   + System.lineSeparator() +
                "hibernate.connection.username="                                + System.lineSeparator() +
                "hibernate.connection.password="                                + System.lineSeparator() +
                "hibernate.default_catalog=TEST"                                + System.lineSeparator() +
                "hibernate.default_schema=PUBLIC"                               + System.lineSeparator();
        Files.writeString(hibernatePropertiesFile.toPath(), hibernatePropertiesFileContents);
    }

    private static void createDatabase() throws Exception {
        File databaseFile = new File(baseFolder, "database/test.mv.db");
        assertFalse(databaseFile.exists());
        assertFalse(databaseFile.isFile());
        Connection connection = DriverManager.getConnection(constructJdbcConnectionString());
        Statement statement = connection.createStatement();
        statement.execute("create table PERSON (ID int not null, NAME varchar(20), primary key (ID))");
        statement.close();
        connection.close();
        assertTrue(databaseFile.exists());
        assertTrue(databaseFile.isFile());
    }

    private static String constructJdbcConnectionString() {
        return "jdbc:h2:" + baseFolder.getAbsolutePath().replace("\\", "/") + "/database/test;AUTO_SERVER=TRUE";
    }

    private static File determineBaseFolder() throws Exception {
        return new File(
                Objects.requireNonNull(
                    ExamplesTestIT.class.getClassLoader().getResource(
                        "common/included.xml")).toURI())
                .getParentFile().getParentFile();
    }

}
