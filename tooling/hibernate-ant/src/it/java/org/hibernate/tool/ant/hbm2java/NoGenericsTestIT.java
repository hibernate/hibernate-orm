package org.hibernate.tool.ant.hbm2java;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.hibernate.tool.it.ant.TestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class NoGenericsTestIT extends TestTemplate {
	
	private File buildXmlFile;
	private ByteArrayOutputStream output;
	private File databaseFile;
	private File personFile;
	
	@BeforeEach
	public void beforeEach() {
		output = new ByteArrayOutputStream();
		databaseFile = new File(getProjectDir(), "database/test.mv.db");
		assertFalse(databaseFile.exists());
		personFile = new File(getProjectDir(), "generated/Person.java");
		assertFalse(personFile.exists());
	}
	
    @Test
    public void testUseGenerics() throws Exception {
    	createBuildXmlFile();
    	createDatabase();
    	createHibernatePropertiesFile();
    	runAntBuild();
    	verifyResult();
    }
    
    private void createBuildXmlFile() throws Exception {
    	buildXmlFile = new File(getProjectDir(), "build.xml");
    	assertFalse(buildXmlFile.exists());
    	Files.writeString(buildXmlFile.toPath(), buildXmlFileContents);
    }
    
	private void createDatabase() throws Exception {
		String CREATE_PERSON_TABLE = "create table PERSON (ID int not null, NAME varchar(20), primary key (ID))";
		String CREATE_ITEM_TABLE =
				"create table ITEM (ID int not null,  NAME varchar(20), OWNER_ID int not null, " +
						"   primary key (ID), foreign key (OWNER_ID) references PERSON(ID))";
		Connection connection = DriverManager.getConnection(constructJdbcConnectionString());
		Statement statement = connection.createStatement();
		statement.execute(CREATE_PERSON_TABLE);
		statement.execute(CREATE_ITEM_TABLE);
		statement.close();
		connection.close();	
		assertTrue(databaseFile.exists());
		assertTrue(databaseFile.isFile());
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
	
    private void runAntBuild() {
    	Project project = new Project();
    	project.setBaseDir(getProjectDir());
    	project.addBuildListener(getConsoleLogger());
        ProjectHelper.getProjectHelper().parse(project, buildXmlFile);
   	    project.executeTarget(project.getDefaultTarget());
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
	
    private DefaultLogger getConsoleLogger() {
        DefaultLogger consoleLogger = new DefaultLogger();
        consoleLogger.setErrorPrintStream(System.err);
        consoleLogger.setOutputPrintStream(new PrintStream(output, true));
        consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
        return consoleLogger;
    }
    
	private String constructJdbcConnectionString() {
		return "jdbc:h2:" + getProjectDir().getAbsolutePath() + "/database/test;AUTO_SERVER=TRUE";
	}
	    
    private static final String buildXmlFileContents = 
    		
    		"<project name='tutorial' default='reveng'>                           \n" +
    		"    <taskdef                                                         \n" + 
    		"            name='hibernatetool'                                     \n" +
    	    "            classname='org.hibernate.tool.ant.HibernateToolTask'/>   \n" +    		
    		"    <target name='reveng'>                                           \n" +
    		"        <hibernatetool destdir='generated'>                          \n" +
    		"            <jdbcconfiguration propertyfile='hibernate.properties'/> \n" +
    		"            <hbm2java jdk5='false'/>                                 \n" +
    		"        </hibernatetool>                                             \n" +
    		"    </target>                                                        \n" +		
    		"</project>                                                           \n" ;

}
