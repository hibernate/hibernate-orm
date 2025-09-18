package org.hibernate.tool.maven;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

    @BeforeAll
    public static void beforeAll() throws Exception {
        // The needed resource for this test are put in place
        // in the 'baseFolder' (normally 'target/test-classes')
        // by the 'build-helper-maven-plugin' execution.
        // See the 'pom.xml'
        baseFolder = determineBaseFolder();
        localRepo = new File(baseFolder.getParentFile(), "local-repo");
        createDatabase();
    }

    private MavenCli mavenCli;

    @Test
    public void test5MinuteTutorial() throws Exception {
        File projectFolder = prepareProjectFolder("5-minute-tutorial");
        File generatedPersonFile = new File(projectFolder, "target/generated-sources/Person.java");
        assertFalse(generatedPersonFile.exists());
        new MavenCli().doMain(
                new String[]{"-Dmaven.repo.local=" + localRepo.getAbsolutePath(), "generate-sources"},
                projectFolder.getAbsolutePath(),
                null,
                null);
        assertTrue(generatedPersonFile.exists());
        String personFileContents = new String(Files.readAllBytes(generatedPersonFile.toPath()));
        assertTrue(personFileContents.contains("public class Person"));
    }

    private File prepareProjectFolder(String projectName) throws Exception {
        File projectFolder = new File(baseFolder, projectName);
        assertTrue(projectFolder.exists());
        System.setProperty(MVN_HOME, projectFolder.getAbsolutePath());
        createHibernatePropertiesFile(projectFolder);
        return projectFolder;
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

    private static File determineBaseFolder() throws Exception {
        return new File(ExamplesTestIT.class.getClassLoader().getResource("5-minute-tutorial/pom.xml").toURI())
                .getParentFile().getParentFile();
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

}
