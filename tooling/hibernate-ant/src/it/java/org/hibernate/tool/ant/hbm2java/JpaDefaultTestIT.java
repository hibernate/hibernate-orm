package org.hibernate.tool.ant.hbm2java;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;

import org.hibernate.tool.it.ant.TestTemplate;
import org.junit.jupiter.api.Test;

public class JpaDefaultTestIT extends TestTemplate {

    @Test
    public void testJpaDefault() throws Exception {
		setHibernateToolTaskXml(
		"""
						<hibernatetool destdir='generated'>                         \s
							<jdbcconfiguration propertyfile='hibernate.properties'/>\s
							<hbm2java/>                                             \s
						</hibernatetool>                                            \s
				"""
		);
		setDatabaseCreationScript(new String[] {
				"create table PERSON (ID int not null, NAME varchar(20), primary key (ID))"
		});
		createProjectAndBuild();
		assertFolderExists("generated", 1);
		assertFileExists("generated/Person.java");
		String generatedPersonJavaFileContents = getFileContents("generated/Person.java");
		assertTrue(generatedPersonJavaFileContents.contains("import jakarta.persistence.Entity;"));
		assertTrue(generatedPersonJavaFileContents.contains("public class Person "));
   }

}
