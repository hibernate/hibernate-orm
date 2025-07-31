package org.hibernate.tool.ant.hbm2java;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;

import org.hibernate.tool.it.ant.TestTemplate;
import org.junit.jupiter.api.Test;

public class NoAnnotationsTestIT extends TestTemplate {
	
    @Test
    public void testNoAnnotations() throws Exception {
		setHibernateToolTaskXml(
		"""
						<hibernatetool destdir='generated'>                         \s
							<jdbcconfiguration propertyfile='hibernate.properties'/>\s
							<hbm2java ejb3='false'/>                                \s
						</hibernatetool>                                            \s
				"""
		);
		setDatabaseCreationScript(new String[] {
				"create table PERSON (ID int not null, NAME varchar(20), primary key (ID))"
		});
    	createBuildXmlFile();
    	createDatabase();
    	createHibernatePropertiesFile();
    	runAntBuild();
    	verifyResult();
    }

	private void verifyResult() throws Exception {
		File generatedOutputFolder = new File(getProjectDir(), "generated");
		assertTrue(generatedOutputFolder.exists());
		assertTrue(generatedOutputFolder.isDirectory());
		assertEquals(1, generatedOutputFolder.list().length);
		File generatedPersonJavaFile = new File(generatedOutputFolder, "Person.java");
		assertTrue(generatedPersonJavaFile.exists());
		assertTrue(generatedPersonJavaFile.isFile());
		String generatedPersonJavaFileContents = new String(
				Files.readAllBytes(generatedPersonJavaFile.toPath()));
		assertFalse(generatedPersonJavaFileContents.contains("import jakarta.persistence.Entity;"));
		assertTrue(generatedPersonJavaFileContents.contains("public class Person "));
	}
	
}
