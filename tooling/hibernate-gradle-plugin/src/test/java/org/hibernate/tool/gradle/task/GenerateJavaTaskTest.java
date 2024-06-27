package org.hibernate.tool.gradle.task;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.hibernate.tool.api.reveng.RevengStrategy;
import org.hibernate.tool.gradle.Extension;
import org.hibernate.tool.internal.reveng.strategy.AbstractStrategy;
import org.junit.jupiter.api.Test;

public class GenerateJavaTaskTest {
	
	@Test
	public void testSetupReverseEngineeringStrategy() throws Exception {
		Project project = ProjectBuilder.builder().build();
		Extension extension = new Extension(project);
		extension.revengStrategy = FooStrategy.class.getName();
		GenerateJavaTask generateJavaTask = project.getTasks().create("foo", GenerateJavaTask.class);
		generateJavaTask.initialize(extension);
		Method method = GenerateJavaTask.class.getDeclaredMethod(
				"setupReverseEngineeringStrategy", 
				new Class[] {});
		method.setAccessible(true);
		RevengStrategy revengStrategy = (RevengStrategy)method.invoke(generateJavaTask, new Object[] {});
		assertTrue(revengStrategy instanceof FooStrategy);
	}
	
	public static class FooStrategy extends AbstractStrategy {}
	

}
