package org.hibernate.tool.ant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.hibernate.tool.ant.test.util.ProjectUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class HibernateToolTaskTest {	
	
	@TempDir
	Path tempDir;
	
	@Test
	public void testHibernateToolTask() throws Exception {
		String buildXmlString = 
				"<project name='HibernateToolTaskTest'>                       " +
				"  <taskdef                                                   " +
                "      name='hibernatetool'                                   " +
				"      classname='org.hibernate.tool.ant.HibernateToolTask' />" +
		        "  <target name='testHibernateToolTask'>                      " +
				"    <hibernatetool/>                                         " +
		        "  </target>                                                  " +
		        "</project>                                                   " ;
		File buildXml = new File(tempDir.toFile(), "build.xml");
		Files.write(buildXml.toPath(), buildXmlString.getBytes());
		Project project = ProjectUtil.createProject(buildXml);
		Class<?> hibernateToolTaskDefinition = project.getTaskDefinitions().get("hibernatetool");
		assertEquals(hibernateToolTaskDefinition, HibernateToolTask.class);
		Target testHibernateToolTaskTarget = project.getTargets().get("testHibernateToolTask");
		Task[] tasks = testHibernateToolTaskTarget.getTasks();
		assertTrue(tasks.length == 1);
		Task hibernateToolTask = tasks[0];
		assertEquals("hibernatetool", hibernateToolTask.getTaskName());
	}

	@Test
	public void testCreateMetadata() {
		HibernateToolTask htt = new HibernateToolTask();
		MetadataTask mdt = htt.createMetadata();
		assertNotNull(mdt);
	}

}
