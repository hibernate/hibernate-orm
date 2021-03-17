/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.tooling.gradle;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSet;

/**
 * The Hibernate Gradle plugin.  Adds Hibernate build-time capabilities into your Gradle-based build.
 *
 * @author Jeremy Whiting
 * @author Steve Ebersole
 */
@SuppressWarnings("serial")
public class HibernatePlugin implements Plugin<Project> {
	public void apply(Project project) {
		project.getPlugins().apply( "java" );

		final HibernateExtension hibernateExtension = new HibernateExtension( project );

		project.getLogger().debug( "Adding Hibernate extensions to the build [{}]", project.getName() );
		project.getExtensions().add( "hibernate", hibernateExtension );

		project.afterEvaluate(
				p -> applyEnhancement( p, hibernateExtension )
		);
	}

	private void applyEnhancement(final Project project, final HibernateExtension hibernateExtension) {
		if ( hibernateExtension.enhance == null || ! hibernateExtension.enhance.shouldApply() ) {
			project.getLogger().warn( "Skipping Hibernate bytecode enhancement since no feature is enabled" );
			return;
		}

		for ( final SourceSet sourceSet : hibernateExtension.getSourceSets() ) {
			project.getLogger().debug( "Applying Hibernate enhancement action to SourceSet.{}", sourceSet.getName() );

			final Task compileTask = project.getTasks().findByName( sourceSet.getCompileJavaTaskName() );
			assert compileTask != null;
			compileTask.doLast(new EnhancerAction( sourceSet, hibernateExtension, project ));
		}
	}

	/**
	 * Gradle doesn't allow lambdas in doLast or doFirst configurations and causing up-to-date checks
	 * to fail. Extracting the lambda to an inner class works around this issue.
	 *
	 * @link https://github.com/gradle/gradle/issues/5510
	 */
	private static class EnhancerAction implements Action<Task> {

		private final SourceSet sourceSet;

		private final HibernateExtension hibernateExtension;

		private final Project project;

		private EnhancerAction(SourceSet sourceSet, HibernateExtension hibernateExtension, Project project) {
			this.sourceSet = sourceSet;
			this.hibernateExtension = hibernateExtension;
			this.project = project;
		}

		@Override
		public void execute(Task task) {
			EnhancementHelper.enhance( sourceSet, hibernateExtension.enhance, project );
		}

	}

}
