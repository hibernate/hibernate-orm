package org.hibernate.tool.ant;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

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
    public void test5MinuteTutorial() throws Exception {
        File buildFile = new File(baseFolder, "5-minute-tutorial/build.xml");
        Project project = createProject(buildFile);
        assertNotNull(project);
        File personFile = new File(baseFolder, "5-minute-tutorial/generated/Person.java");
        assertFalse(personFile.exists());
        project.executeTarget("reveng");
        assertTrue(personFile.exists());
    }

    @Test
    public void testClasspath() throws Exception {
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
    public void testConfiguration() throws Exception {
        File buildFile = new File(baseFolder, "configuration/build.xml");
        Project project = createProject(buildFile);
        assertNotNull(project);
        File cfgXmlFile = new File(baseFolder, "configuration/generated/hibernate.cfg.xml");
        assertFalse(cfgXmlFile.exists());
        project.executeTarget("reveng");
        assertTrue(cfgXmlFile.exists());
    }

    @Test
    public void testJpa() throws Exception {
        File buildFile = new File(baseFolder, "jpa/build.xml");
        Project project = createProject(buildFile);
        assertNotNull(project);
        File barSqlFile = new File(baseFolder, "jpa/generated/bar.sql");
        assertFalse(barSqlFile.exists());
        project.executeTarget("reveng");
        assertTrue(barSqlFile.exists());
    }

    @Test
    public void testNative() throws Exception {
        File buildFile = new File(baseFolder, "native/build.xml");
        Project project = createProject(buildFile);
        assertNotNull(project);
        File fooSqlFile = new File(baseFolder, "native/generated/foo.sql");
        assertFalse(fooSqlFile.exists());
        project.executeTarget("reveng");
        assertTrue(fooSqlFile.exists());
    }

    @Test
    public void testProperties() throws Exception {
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

    private Project createProject(File buildXmlFile) throws Exception {
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
        StringBuffer xmlFileContents = new StringBuffer(
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
                "hibernate.connection.driver_class=org.h2.Driver\n" +
                        "hibernate.connection.url=" + constructJdbcConnectionString() + "\n" +
                        "hibernate.connection.username=\n" +
                        "hibernate.connection.password=\n" +
                        "hibernate.default_catalog=TEST\n" +
                        "hibernate.default_schema=PUBLIC\n";
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
        return "jdbc:h2:" + baseFolder.getAbsolutePath() + "/database/test;AUTO_SERVER=TRUE";
    }

    private static File determineBaseFolder() throws Exception {
        return new File(ExamplesTestIT.class.getClassLoader().getResource("common/included.xml").toURI())
                .getParentFile().getParentFile();
    }

}
