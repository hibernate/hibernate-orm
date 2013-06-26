/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.bytecode.enhance.plugins;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.hibernate.bytecode.enhance.plugins.EnhanceTask;
import org.hibernate.bytecode.enhance.plugins.EnhancePluginConvention;

/**
 * This plugin will add Entity enhancement behaviour to the build lifecycle.
 * 
 * @author Jeremy Whiting
 */
public class EnhancePlugin implements Plugin<Project> {

	public static final String ENHANCE_TASK_NAME = "enhance";
	public static final String PLUGIN_CONVENTION_NAME = "enhance";
	public static final String HAPPENS_BEFORE_ENHANCE_TASK_NAME = JavaPlugin.CLASSES_TASK_NAME;
	public static final String HAPPENS_AFTER_ENHANCE_TASK_NAME = JavaPlugin.JAR_TASK_NAME;

	public void apply(Project project) {
		project.getLogger().debug( "Applying enhance plugin to project." );
		project.getConvention().getPlugins().put( PLUGIN_CONVENTION_NAME, new EnhancePluginConvention() );
		configureTask( project );
		project.getLogger().debug( String.format( "DAG has been configured with enhance task dependent on [%s].", HAPPENS_BEFORE_ENHANCE_TASK_NAME ) );
	}

	private void configureTask(Project project) {
		EnhanceTask enhanceTask = project.getTasks().create( ENHANCE_TASK_NAME, EnhanceTask.class );
		// connect up the task in the task dependency graph
		final Configuration config = enhanceTask.getProject().getConfigurations().getByName( JavaPlugin.COMPILE_CONFIGURATION_NAME );
		Task classesTask = project.getTasks().getByName( JavaPlugin.CLASSES_TASK_NAME );
		enhanceTask.dependsOn( classesTask );
		enhanceTask.mustRunAfter( HAPPENS_BEFORE_ENHANCE_TASK_NAME );

		Task jarTask = project.getTasks().getByName( HAPPENS_AFTER_ENHANCE_TASK_NAME );
		jarTask.dependsOn( enhanceTask );
	}
}
