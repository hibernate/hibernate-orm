package org.hibernate.tool.gradle.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.util.Properties;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AbstractTaskTest {
	
	private AbstractTask abstractTask = null;
	
	private Properties properties = null;
	
	@BeforeEach
	void beforeEach() throws Exception {
		Project project = ProjectBuilder.builder().build();
		abstractTask = project.getTasks().create("baz", FooTask.class);
		Field propertiesField = AbstractTask.class.getDeclaredField("properties");
		propertiesField.setAccessible(true);
		properties = (Properties)propertiesField.get(abstractTask);
	}
	
	@Test
	void testSetProperty() {
		assertNull(properties.getProperty("foo"));
		abstractTask.setProperty("foo", "bar");
		assertEquals("bar", properties.getProperty("foo"));
	}
	
	@Test
	void testGetProperty() {
		assertNull(abstractTask.getProperty("foo"));
		properties.setProperty("foo", "bar");
		assertEquals("bar", abstractTask.getProperty("foo"));
	}
	
	public static class FooTask extends AbstractTask {};

}
