/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.tooling.gradle;

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
			compileTask.doLast(
					task -> EnhancementHelper.enhance( sourceSet, hibernateExtension.enhance, project )
			);
		}
	}

}
