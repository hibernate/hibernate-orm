package org.hibernate.tool.gradle;

import org.gradle.api.Project;

public class Plugin implements org.gradle.api.Plugin<Project> {
    public void apply(Project project) {
         project.getTasks().register("hibernate", task -> {
            task.doLast(s -> System.out.println("Hello from plugin 'org.hibernate.tool.gradle.Plugin'"));
        });
    }
}
