package org.hibernate.tool.gradle;

import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.api.Project;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PluginTest {
	
    @Test 
    void testApply() {
        // Create a test project and apply the plugin
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("org.hibernate.tool.hibernate-tools-gradle");

        // Verify the result
        assertNotNull(project.getTasks().findByName("generateJava"));
        assertNotNull(project.getTasks().findByName("runSql"));
        
        Object extension = project.getExtensions().getByName("hibernateTools");
        assertNotNull(extension);
        assertTrue(extension instanceof Extension);
    }
    
}
