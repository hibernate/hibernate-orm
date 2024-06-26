package org.hibernate.tool.gradle.task;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Field;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.hibernate.tool.gradle.Extension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AbstractTaskTest {
	
	private AbstractTask abstractTask = null;
	
	private Field extensionField = null;
	private Extension extension = null;
	
	@BeforeEach
	void beforeEach() throws Exception {
		Project project = ProjectBuilder.builder().build();
		abstractTask = project.getTasks().create("foo", FooTask.class);
		extensionField = AbstractTask.class.getDeclaredField("extension");
		extensionField.setAccessible(true);
		extension = new Extension(project);
	}
	
	@Test
	void testInitialize() throws Exception {
		assertNull(extensionField.get(abstractTask));
		abstractTask.initialize(extension);
		assertSame(extension, extensionField.get(abstractTask));
	}
	
	@Test
	void testGetExtension() throws Exception {
		assertNull(abstractTask.getExtension());
		extensionField.set(abstractTask, extension);
		assertSame(extension, abstractTask.getExtension());
	}
	
	public static class FooTask extends AbstractTask {}

}
