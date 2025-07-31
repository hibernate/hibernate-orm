package org.hibernate.tool.ant.hbm2java;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;

import org.hibernate.tool.it.ant.TestTemplate;
import org.junit.jupiter.api.Test;

public class UseGenericsTestIT extends TestTemplate {
	
    @Test
    public void testUseGenerics() throws Exception {
		setHibernateToolTaskXml(
		"""
						<hibernatetool destdir='generated'>                         \s
							<jdbcconfiguration propertyfile='hibernate.properties'/>\s
							<hbm2java/>                                             \s
						</hibernatetool>                                            \s
				"""
		);
		setDatabaseCreationScript(new String[] {
				"create table PERSON (ID int not null, NAME varchar(20), " +
						"primary key (ID))",
				"create table ITEM (ID int not null,  NAME varchar(20), OWNER_ID int not null, " +
						"primary key (ID), foreign key (OWNER_ID) references PERSON(ID))"
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
		assertEquals(2, generatedOutputFolder.list().length);
		File generatedPersonJavaFile = new File(generatedOutputFolder, "Person.java");
		assertTrue(generatedPersonJavaFile.exists());
		assertTrue(generatedPersonJavaFile.isFile());
		String generatedPersonJavaFileContents = new String(
				Files.readAllBytes(generatedPersonJavaFile.toPath()));
		assertTrue(generatedPersonJavaFileContents.contains("public class Person "));
		assertTrue(generatedPersonJavaFileContents.contains("Set<Item>"));
		File generatedItemJavaFile = new File(generatedOutputFolder, "Item.java");
		assertTrue(generatedItemJavaFile.exists());
		assertTrue(generatedItemJavaFile.isFile());
		String generatedItemJavaFileContents = new String(
				Files.readAllBytes(generatedItemJavaFile.toPath()));
		assertTrue(generatedItemJavaFileContents.contains("public class Item "));
	}
	
}
