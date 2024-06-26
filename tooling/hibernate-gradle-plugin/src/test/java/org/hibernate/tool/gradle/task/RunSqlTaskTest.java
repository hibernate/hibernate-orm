package org.hibernate.tool.gradle.task;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.hibernate.tool.gradle.Extension;
import org.junit.jupiter.api.Test;

public class RunSqlTaskTest {
	
	@Test
	void testInitialize() throws Exception {
		 Project project = ProjectBuilder.builder().build();
		 Extension extension = new Extension(project);
		 RunSqlTask runSqlTask = project.getTasks().create("runSql", RunSqlTask.class);
		 Field extensionField = RunSqlTask.class.getDeclaredField("extension");
		 extensionField.setAccessible(true);
		 assertNull(extensionField.get(runSqlTask));
		 runSqlTask.initialize(extension);
		 assertSame(extension, extensionField.get(runSqlTask));
	}

}
