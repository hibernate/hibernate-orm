package org.hibernate.tool.ant;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.tools.ant.Project;
import org.hibernate.tool.ant.test.util.ProjectUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class HibernateToolTaskTest {	
	
	@TempDir
	Path tempDir;
	
	@Test
	public void testHibernateToolTask() throws Exception {
		String buildXmlString = 
				"<project name='HibernateToolTest'>                           " +
		        "  <taskdef                                                   " +
				"      name='hibernatetool'                                   " +
		        "      classname='org.hibernate.tool.ant.HibernateToolTask' />" +
				"  <hibernatetool/>                                           " +
		        "</project>                                                   " ;
		File buildXml = new File(tempDir.toFile(), "build.xml");
		Files.write(buildXml.toPath(), buildXmlString.getBytes());
		Project project = ProjectUtil.createProject(buildXml);
		assertNotNull(project);
	}

}
