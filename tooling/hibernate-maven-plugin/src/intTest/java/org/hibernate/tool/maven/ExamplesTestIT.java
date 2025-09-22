package org.hibernate.tool.maven;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


import org.apache.maven.cli.MavenCli;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ExamplesTestIT {

    public static final String MVN_HOME = "maven.multiModuleProjectDirectory";
    private static File baseFolder;
    private static File localRepo;

    private File projectFolder;
    private MavenCli mavenCli;

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

    @BeforeEach
    public void beforeEach() throws Exception {
        createDatabase();
    }

    @Test
    public void test5MinuteTutorial() throws Exception {
        prepareProject("5-minute-tutorial");
        assertNotGeneratedYet();
        runGenerateSources();
        assertGeneratedContains("public class Person");
    }

    @Test
    public void testJpaDefault() throws Exception {
        prepareProject("hbm2java/jpa-default");
        assertNotGeneratedYet();
        runGenerateSources();
        assertGeneratedContains("import jakarta.persistence.Entity;");
    }

    @Test
    public void testNoAnnotations() throws Exception {
        prepareProject("hbm2java/no-annotations");
        assertNotGeneratedYet();
        runGenerateSources();
        assertGeneratedDoesNotContain("import jakarta.persistence.Entity;");
    }
    private void prepareProject(String projectName) throws Exception {
        projectFolder = new File(baseFolder, projectName);
        assertTrue(projectFolder.exists());
        System.setProperty(MVN_HOME, projectFolder.getAbsolutePath());
        createHibernatePropertiesFile(projectFolder);
    }

    @Test
    public void testNoGenerics() throws Exception {
        databaseCreationScript = new String[] {
                "create table PERSON (ID int not null,  NAME varchar(20), primary key (ID))",
                "create table ITEM (ID int not null,  NAME varchar(20), OWNER_ID int not null, " +
                        "   primary key (ID), foreign key (OWNER_ID) references PERSON(ID))"
        };
        prepareProject("hbm2java/no-generics");
        assertNotGeneratedYet();
        runGenerateSources();
        assertGeneratedDoesNotContain("Set<Item>");
    }

    private void createHibernatePropertiesFile(File projectFolder) throws Exception {
        File projectResourcesFolder = new File(projectFolder, "src/main/resources");
        projectResourcesFolder.mkdirs();
        File hibernatePropertiesFile = new File(projectResourcesFolder, "hibernate.properties");
        assertFalse(hibernatePropertiesFile.exists());
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

    private void runGenerateSources() {
        new MavenCli().doMain(
                new String[]{"-Dmaven.repo.local=" + localRepo.getAbsolutePath(), "generate-sources"},
                projectFolder.getAbsolutePath(),
                null,
                null);
    }

    private void assertNotGeneratedYet() {
        assertFalse(new File(projectFolder, "target/generated-sources/Person.java").exists());
    }

    private void assertGeneratedContains(String contents) throws Exception {
        assertTrue(readGeneratedContents().contains(contents));
    }

    private void assertGeneratedDoesNotContain(String contents) throws Exception {
        assertFalse(readGeneratedContents().contains(contents));
    }

    private String readGeneratedContents() throws Exception {
        File generatedPersonFile = new File(projectFolder, "target/generated-sources/Person.java");
        assertTrue(generatedPersonFile.exists());
        return new String(Files.readAllBytes(generatedPersonFile.toPath()));
    }

    private static File determineBaseFolder() throws Exception {
        return new File(ExamplesTestIT.class.getClassLoader().getResource("5-minute-tutorial/pom.xml").toURI())
                .getParentFile().getParentFile();
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
        return "jdbc:h2:" + tempFolder.getAbsolutePath() + "/database/test;AUTO_SERVER=TRUE";
    }

}
