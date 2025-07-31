package org.hibernate.tool.ant.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.hibernate.tool.it.ant.TestTemplate;
import org.junit.jupiter.api.Test;

public class TutorialTestIT extends TestTemplate {
	
    @Test
    public void testTutorial() throws Exception {
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
				"create table PERSON (ID int not null, NAME varchar(20), primary key (ID))"
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
	
	private void verifyResult() {
		File generatedOutputFolder = new File(getProjectDir(), "generated");
		assertTrue(generatedOutputFolder.exists());
		assertTrue(generatedOutputFolder.isDirectory());
		assertEquals(1, generatedOutputFolder.list().length);
		File generatedPersonJavaFile = new File(generatedOutputFolder, "Person.java");
		assertTrue(generatedPersonJavaFile.exists());
		assertTrue(generatedPersonJavaFile.isFile());
	}
	
	private static final String hibernateToolTaskXml =
			"        <hibernatetool destdir='generated'>                          \n" +
			"            <jdbcconfiguration propertyfile='hibernate.properties'/> \n" +
			"            <hbm2java/>                                              \n" +
			"        </hibernatetool>                                             \n" ;
}
