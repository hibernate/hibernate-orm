package org.hibernate.tool.ant;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.tools.ant.Project;
import org.hibernate.tool.ant.test.util.ProjectUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class HibernateToolTest {
	
	private static final String BUILD_XML = 
			"<project name='HibernateToolTest'>                           " +
	        "  <taskdef                                                   " +
			"      name='hibernatetool'                                   " +
	        "      classname='org.hibernate.tool.ant.HibernateToolTask' />" +
			"  <hibernatetool/>                                           " +
	        "</project>                                                   " ;
	
	@TempDir
	Path tempDir;
	
	@Test
	public void testHibernateTool() throws Exception {
		File buildXml = new File(tempDir.toFile(), "build.xml");
		Files.write(buildXml.toPath(), BUILD_XML.getBytes());
		Project project = ProjectUtil.createProject(buildXml);
		assertNotNull(project);
	}

}
