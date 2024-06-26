package org.hibernate.tool.gradle.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.hibernate.tool.gradle.Extension;
import org.junit.jupiter.api.Test;

public class RunSqlTaskTest {
	
	@Test
	void testInitialize() {
		 Project project = ProjectBuilder.builder().build();
		 Extension extension = new Extension(project);
		 extension.sql = "foo";
		 RunSqlTask runSqlTask = project.getTasks().create("runSql", RunSqlTask.class);
		 assertNull(runSqlTask.getProperty("sqlToRun"));
		 runSqlTask.initialize(extension);
		 assertEquals("foo", runSqlTask.getProperty("sqlToRun"));
	}

}
