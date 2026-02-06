package org.hibernate.tool.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hibernate.tool.api.version.Version;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


import org.apache.maven.cli.MavenCli;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Objects;

public class ExamplesTestIT {

    public static final String MVN_HOME = "maven.multiModuleProjectDirectory";
    private static File baseFolder;
    private static File localRepo;

    private File projectFolder;

    @TempDir
    private File tempFolder;

    private String[] databaseCreationScript = new String[] {
            // This is the default database which can be overridden per test
            "create table PERSON (ID int not null, NAME varchar(20), primary key (ID))"
    };

    @BeforeAll
    public static void beforeAll() throws Exception {
        // The needed resource for this test are put in place
        // in the 'baseFolder' (normally 'target/test-classes')
        // by the 'build-helper-maven-plugin' execution.
        // See the 'pom.xml'
        baseFolder = determineBaseFolder();
        localRepo = new File(baseFolder.getParentFile(), "local-repo");
    }

    @Test
    public void test5MinuteTutorial() throws Exception {
        prepareProject("5-minute-tutorial");
        assertNotGeneratedYet("Person.java");
        runGenerateSources();
        assertNumberOfGeneratedFiles(1);
        assertGeneratedContains("Person.java", "public class Person");
    }

    @Test
    public void testJpaDefault() throws Exception {
        prepareProject("hbm2java/jpa-default");
        assertNotGeneratedYet("Person.java");
        runGenerateSources();
        assertNumberOfGeneratedFiles(1);
        assertGeneratedContains("Person.java","import jakarta.persistence.Entity;");
    }

    @Test
    public void testNoAnnotations() throws Exception {
        prepareProject("hbm2java/no-annotations");
        assertNotGeneratedYet("Person.java");
        runGenerateSources();
        assertNumberOfGeneratedFiles(1);
        assertGeneratedDoesNotContain("Person.java", "import jakarta.persistence.Entity;");
    }

    @Test
    public void testNoGenerics() throws Exception {
        databaseCreationScript = new String[] {
                "create table PERSON (ID int not null,  NAME varchar(20), primary key (ID))",
                "create table ITEM (ID int not null,  NAME varchar(20), OWNER_ID int not null, " +
                        "   primary key (ID), foreign key (OWNER_ID) references PERSON(ID))"
        };
        prepareProject("hbm2java/no-generics");
        assertNotGeneratedYet("Person.java");
        runGenerateSources();
        assertNumberOfGeneratedFiles(2);
        assertGeneratedDoesNotContain("Person.java", "Set<Item>");
    }

    @Test
    public void testOutputDirectory() throws Exception {
        System.setProperty("output.dir", "${project.basedir}/generated-classes");
        prepareProject("hbm2java/output-directory");
        File outputDirectory = new File(projectFolder, "generated-classes");
        File personFile = new File(outputDirectory, "Person.java");
        assertFalse(outputDirectory.exists());
        assertFalse(personFile.exists());
        runGenerateSources();
        assertEquals(1, Objects.requireNonNull(outputDirectory.list()).length); // 1 file is generated in 'generated-classes'
        assertTrue(personFile.exists()); // The Person.java file should have been generated
    }

    @Test
    public void testTemplatePath() throws Exception {
        System.setProperty("template.dir", "${project.basedir}/templates");
        prepareProject("hbm2java/template-path");
        assertNotGeneratedYet("Person.java");
        runGenerateSources();
        assertNumberOfGeneratedFiles(1);
        assertGeneratedContains("Person.java", "// This is just an example of a custom template");
    }

    @Test
    public void testUseGenerics() throws Exception {
        databaseCreationScript = new String[] {
                "create table PERSON (ID int not null,  NAME varchar(20), primary key (ID))",
                "create table ITEM (ID int not null,  NAME varchar(20), OWNER_ID int not null, " +
                        "   primary key (ID), foreign key (OWNER_ID) references PERSON(ID))"
        };
        prepareProject("hbm2java/use-generics");
        assertNotGeneratedYet("Person.java");
        runGenerateSources();
        assertNumberOfGeneratedFiles(2);
        assertGeneratedContains("Person.java", "Set<Item>");
    }

    @Test
    public void testHbm2OrmSimpleDefault() throws Exception {
        projectFolder = new File(baseFolder, "hbm2orm/simple-default");
        File ormXmlFile = new File(projectFolder, "src/main/resources/simple.mapping.xml");
        assertFalse(ormXmlFile.exists());
        runMavenCommand("org.hibernate.tool:hibernate-tools-maven:" + Version.versionString() + ":hbm2orm");
        assertTrue(ormXmlFile.exists());
        String ormXmlContents = Files.readString( ormXmlFile.toPath() );
        assertTrue(ormXmlContents.contains("entity-mappings"));
    }

    private void prepareProject(String projectName) throws Exception {
        projectFolder = new File(baseFolder, projectName);
        assertTrue(projectFolder.exists());
        System.setProperty(MVN_HOME, projectFolder.getAbsolutePath());
        createHibernatePropertiesFile(projectFolder);
        createDatabase();
    }

    private void createHibernatePropertiesFile(File projectFolder) throws Exception {
        File projectResourcesFolder = new File(projectFolder, "src/main/resources");
        assertTrue(projectResourcesFolder.mkdirs());
        File hibernatePropertiesFile = new File(projectResourcesFolder, "hibernate.properties");
        assertFalse(hibernatePropertiesFile.exists());
        String hibernatePropertiesFileContents =
                "hibernate.connection.driver_class=org.h2.Driver"               + System.lineSeparator() +
                "hibernate.connection.url=" + constructJdbcConnectionString()   + System.lineSeparator() +
                "hibernate.connection.username="                                + System.lineSeparator() +
                "hibernate.connection.password="                                + System.lineSeparator() +
                "hibernate.default_catalog=TEST"                                + System.lineSeparator() +
                "hibernate.default_schema=PUBLIC"                               + System.lineSeparator();
        Files.writeString(hibernatePropertiesFile.toPath(), hibernatePropertiesFileContents);
        assertTrue(hibernatePropertiesFile.exists());
    }

    private void runGenerateSources() {
        runMavenCommand("generate-sources");
    }

    private void runMavenCommand(String command) {
        new MavenCli().doMain(
                new String[]{
                        "-Dmaven.repo.local=" + localRepo.getAbsolutePath(),
                        command},
                projectFolder.getAbsolutePath(),
                null,
                null);
    }

    private void assertNotGeneratedYet(String fileName) {
        assertFalse(new File(projectFolder, "target/generated-sources/" + fileName).exists());
    }

    private void assertGeneratedContains(String fileName, String contents) throws Exception {
        assertTrue(readGeneratedContents(fileName).contains(contents));
    }

    private void assertGeneratedDoesNotContain(String fileName, String contents) throws Exception {
        assertFalse(readGeneratedContents(fileName).contains(contents));
    }

    private void assertNumberOfGeneratedFiles(int amount) {
        assertEquals(
                amount,
                Objects.requireNonNull(
                    new File(projectFolder, "target/generated-sources").list()).length);
    }

    private String readGeneratedContents(String fileName) throws Exception {
        File generatedPersonFile = new File(projectFolder, "target/generated-sources/" + fileName);
        assertTrue(generatedPersonFile.exists());
        return new String(Files.readAllBytes(generatedPersonFile.toPath()));
    }

    private static File determineBaseFolder() throws Exception {
        Class<?> thisClass = ExamplesTestIT.class;
        URL classUrl = thisClass.getResource("/" + thisClass.getName().replace(".", "/") + ".class");
        assert classUrl != null;
        File result = new File(classUrl.toURI());
        for (int i = 0; i < thisClass.getName().chars().filter(ch -> ch == '.').count() + 1; i++) {
            result = result.getParentFile();
        }
        return result;
    }

    private void createDatabase() throws Exception {
        File databaseFile = new File(tempFolder, "database/test.mv.db");
        assertFalse(databaseFile.exists());
        assertFalse(databaseFile.isFile());
        Connection connection = DriverManager.getConnection(constructJdbcConnectionString());
        Statement statement = connection.createStatement();
        for (String s : databaseCreationScript) {
            statement.execute(s);
        }
        statement.close();
        connection.close();
        assertTrue(databaseFile.exists());
        assertTrue(databaseFile.isFile());
    }

    private String constructJdbcConnectionString() {
        return "jdbc:h2:"
                + tempFolder.getAbsolutePath().replace('\\', '/') // for tests on Windows
                + "/database/test;AUTO_SERVER=TRUE";
    }

}
