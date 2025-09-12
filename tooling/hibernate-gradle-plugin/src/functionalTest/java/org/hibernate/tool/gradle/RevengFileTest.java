package org.hibernate.tool.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hibernate.tool.it.gradle.TestTemplate;

public class RevengFileTest extends TestTemplate {

    @BeforeEach
    public void beforeEach() throws Exception {
        setGradleTaskToPerform("generateJava");
        setDatabaseCreationScript(new String[] {
                "create table ALL_PERSONS (ID int not null, NAME varchar(20), primary key (ID))"
        });
    }

    @Test
    public void testTutorial() throws Exception {
        setHibernateToolsExtensionSection(
                "hibernateTools { \n" +
                "  revengFile = 'foo.reveng.xml' \n" +
                "}"
        );
        createProjectAndExecuteGradleCommand();
        File generatedOutputFolder = new File(getProjectDir(), "app/generated-sources");
        assertTrue(generatedOutputFolder.exists());
        assertTrue(generatedOutputFolder.isDirectory());
        assertEquals(1, generatedOutputFolder.list().length);
        File generatedPersonJavaFile = new File(generatedOutputFolder, "Person.java");
        assertTrue(generatedPersonJavaFile.exists());
        assertTrue(generatedPersonJavaFile.isFile());
    }

    protected void createProject() throws Exception {
        super.createProject();
        createRevengFile();
    }

    private void createRevengFile() throws Exception {
        String revengXml =
                "<hibernate-reverse-engineering>\n" +
                "  <table name=\"ALL_PERSONS\" class=\"Person\" />" +
                "</hibernate-reverse-engineering>";
        File resourcesFolder = new File(getProjectDir(), "app/src/main/resources");
        resourcesFolder.mkdirs();
        Files.writeString(new File(resourcesFolder, "foo.reveng.xml").toPath(), revengXml);
    }

}
