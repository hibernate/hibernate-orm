package org.hibernate.tool.ant.tutorial;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TutorialTestIT {
	
	@TempDir
	private File projectDir;
	
	private File buildXmlFile;
	private ByteArrayOutputStream output;
	private File databaseFile;
	private File personFile;
	
	@BeforeEach
	public void beforeEach() {
		output = new ByteArrayOutputStream();
		databaseFile = new File(projectDir, "database/test.mv.db");
		assertFalse(databaseFile.exists());
		personFile = new File(projectDir, "generated/Person.java");
		assertFalse(personFile.exists());
	}
	
    @Test
    public void testTutorial() throws Exception {
    	createBuildXmlFile();
    	createDatabase();
    	createHibernatePropertiesFile();
    	runAntBuild();
    	verifyResult();
    }
    
    private void createBuildXmlFile() throws Exception {
    	buildXmlFile = new File(projectDir, "build.xml");
    	assertFalse(buildXmlFile.exists());
    	Files.writeString(buildXmlFile.toPath(), buildXmlFileContents);
    }
    
	private void createDatabase() throws Exception {
		String CREATE_PERSON_TABLE = "create table PERSON (ID int not null, NAME varchar(20), primary key (ID))";
		Connection connection = DriverManager.getConnection(constructJdbcConnectionString());
		Statement statement = connection.createStatement();
		statement.execute(CREATE_PERSON_TABLE);
		statement.execute("insert into PERSON values (1, 'foo')");
		statement.close();
		connection.close();	
		assertTrue(databaseFile.exists());
		assertTrue(databaseFile.isFile());
	}
	
	private void createHibernatePropertiesFile() throws Exception {
		File hibernatePropertiesFile = new File(projectDir, "hibernate.properties");
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
    	project.setBaseDir(projectDir);
    	project.addBuildListener(getConsoleLogger());
        ProjectHelper.getProjectHelper().parse(project, buildXmlFile);
   	    project.executeTarget(project.getDefaultTarget());
    }
    
	private void verifyResult() {
		File generatedOutputFolder = new File(projectDir, "generated");
		assertTrue(generatedOutputFolder.exists());
		assertTrue(generatedOutputFolder.isDirectory());
		assertEquals(1, generatedOutputFolder.list().length);
		File generatedPersonJavaFile = new File(generatedOutputFolder, "Person.java");
		assertTrue(generatedPersonJavaFile.exists());
		assertTrue(generatedPersonJavaFile.isFile());
	}
	
    private DefaultLogger getConsoleLogger() {
        DefaultLogger consoleLogger = new DefaultLogger();
        consoleLogger.setErrorPrintStream(System.err);
        consoleLogger.setOutputPrintStream(new PrintStream(output, true));
        consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
        return consoleLogger;
    }
    
	private String constructJdbcConnectionString() {
		return "jdbc:h2:" + projectDir.getAbsolutePath() + "/database/test;AUTO_SERVER=TRUE";
	}
	    
    private static final String buildXmlFileContents = 
    		
    		"<project name='tutorial' default='reveng'>                           \n" +
    		"    <taskdef                                                         \n" + 
    		"            name='hibernatetool'                                     \n" +
    	    "            classname='org.hibernate.tool.ant.HibernateToolTask'/>   \n" +    		
    		"    <target name='reveng'>                                           \n" +
    		"        <echo message='hello from hibernate tools' />                \n" +
    		"        <hibernatetool destdir='generated'>                          \n" +
    		"            <jdbcconfiguration propertyfile='hibernate.properties'/> \n" +
    		"            <hbm2java/>                                              \n" +
    		"        </hibernatetool>                                             \n" +
    		"    </target>                                                        \n" +		
    		"</project>                                                           \n" ;

}
