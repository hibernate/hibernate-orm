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
package org.hibernate.tooling.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;

/**
 * The Hibernate Gradle plugin.  Adds Hibernate build-time capabilities into your Gradle-based build.
 *
 * @author Jeremy Whiting
 * @author Steve Ebersole
 */
@SuppressWarnings("serial")
public class HibernatePlugin implements Plugin<Project> {

	public static final String ENHANCE_TASK_NAME = "enhance";

	public void apply(Project project) {
		applyEnhancement( project );
	}

	private void applyEnhancement(Project project) {
		project.getLogger().debug( "Applying Hibernate enhancement to project." );

		// few things...

		// 1) would probably be best as a doLast Action attached to the compile task rather than
		// 		a task.  Really ideally would be a task association for "always run after", but Gradle
		//		does not yet have that (mustRunAfter is very different semantic, finalizedBy is closer but
		//		will run even if the first task fails).  The initial attempt here fell into the "maven" trap
		//		of trying to run a dependent task by attaching it to a task know to run after the we want to run after;
		//      which is a situation tailored made for Task.doLast

		// 2) would be better to allow specifying which SourceSet to apply this to.  For example, in the Hibernate
		//      build itself, this would be best applied to the 'test' sourceSet; though generally speaking the
		//      'main' sourceSet is more appropriate

		// for now, we'll just:
		// 1) use a EnhancerTask + finalizedBy
		// 2) apply to main sourceSet

		EnhancerTask enhancerTask = project.getTasks().create( ENHANCE_TASK_NAME, EnhancerTask.class );
		enhancerTask.setGroup( BasePlugin.BUILD_GROUP );

		// connect up the task in the task dependency graph
		Task classesTask = project.getTasks().getByName( JavaPlugin.CLASSES_TASK_NAME );
		enhancerTask.dependsOn( classesTask );
		classesTask.finalizedBy( enhancerTask );
	}
}
