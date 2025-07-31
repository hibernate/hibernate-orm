package org.hibernate.tool.ant.hbm2java;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;

import org.hibernate.tool.it.ant.TestTemplate;
import org.junit.jupiter.api.Test;

public class NoGenericsTestIT extends TestTemplate {
	
    @Test
    public void testUseGenerics() throws Exception {
    	createBuildXmlFile();
    	createDatabase();
    	createHibernatePropertiesFile();
    	runAntBuild();
    	verifyResult();
    }

	protected String hibernateToolTaskXml() {
		return  hibernateToolTaskXml;
	}

	protected String[] createDatabaseScript() {
		return new String[] {
				"create table PERSON (ID int not null, NAME varchar(20), " +
						"primary key (ID))",
				"create table ITEM (ID int not null,  NAME varchar(20), OWNER_ID int not null, " +
						"primary key (ID), foreign key (OWNER_ID) references PERSON(ID))"
		};
	}

	private void createHibernatePropertiesFile() throws Exception {
		File hibernatePropertiesFile = new File(getProjectDir(), "hibernate.properties");
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
		assertFalse(generatedPersonJavaFileContents.contains("Set<Item>"));
		File generatedItemJavaFile = new File(generatedOutputFolder, "Item.java");
		assertTrue(generatedItemJavaFile.exists());
		assertTrue(generatedItemJavaFile.isFile());
		String generatedItemJavaFileContents = new String(
				Files.readAllBytes(generatedItemJavaFile.toPath()));
		assertTrue(generatedItemJavaFileContents.contains("public class Item "));
	}
	
	private static final String hibernateToolTaskXml =
			"        <hibernatetool destdir='generated'>                          \n" +
			"            <jdbcconfiguration propertyfile='hibernate.properties'/> \n" +
			"            <hbm2java jdk5='false'/>                                 \n" +
			"        </hibernatetool>                                             \n" ;

}
