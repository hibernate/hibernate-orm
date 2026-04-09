/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2024-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.gradle;

import java.util.Map;

import org.gradle.api.Project;
import org.hibernate.tool.gradle.task.AbstractTask;
import org.hibernate.tool.gradle.task.GenerateCfgTask;
import org.hibernate.tool.gradle.task.GenerateDaoTask;
import org.hibernate.tool.gradle.task.GenerateHbmTask;
import org.hibernate.tool.gradle.task.GenerateJavaTask;
import org.hibernate.tool.gradle.task.RunSqlTask;

public class Plugin implements org.gradle.api.Plugin<Project> {
	
	private static Map<String, Class<?>> PLUGIN_TASK_MAP = Map.of(
			"runSql", RunSqlTask.class,
			"generateJava", GenerateJavaTask.class,
			"generateCfg", GenerateCfgTask.class,
			"generateHbm", GenerateHbmTask.class,
			"generateDao", GenerateDaoTask.class
		);
	
    @SuppressWarnings("unchecked")
	public void apply(Project project) {
    	Extension extension =  project.getExtensions().create("hibernateTools", Extension.class, project);
    	for (String key : PLUGIN_TASK_MAP.keySet()) {
    		Class<?> taskClass = PLUGIN_TASK_MAP.get(key);
    		project.getTasks().register(key, (Class<AbstractTask>)taskClass);
    		AbstractTask task = (AbstractTask)project.getTasks().getByName(key);
    		task.doFirst(w -> task.initialize(extension));
    	}
    }
    
}
