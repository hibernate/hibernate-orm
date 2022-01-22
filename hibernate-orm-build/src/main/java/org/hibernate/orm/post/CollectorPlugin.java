/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.post;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;

import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME;

/**
 * @author Steve Ebersole
 */
public class CollectorPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType( JavaPluginExtension.class );
		final SourceSet projectMainSourceSet = javaPluginExtension.getSourceSets().getByName( MAIN_SOURCE_SET_NAME );
		final Provider<Directory> classesDirectory = projectMainSourceSet.getJava().getClassesDirectory();

		final IndexManager indexManager = new IndexManager( classesDirectory, project );

		final IndexerTask indexerTask = project.getTasks().create(
				"indexProject",
				IndexerTask.class,
				indexManager
		);

		// NOTE : `indexProject` implicitly depends on the compilation task.
		// 		it uses the `classesDirectory` from the `main` sourceSet.  Gradle
		// 		understands that the `classesDirectory` is generated from the
		//		compilation task and implicitly creates the task dependency

		final IncubatingCollectorTask incubatingTask = project.getTasks().create(
				"createIncubatingReport",
				IncubatingCollectorTask.class,
				indexManager
		);
		incubatingTask.dependsOn( indexerTask );

		final LoggingCollectorTask loggingTask = project.getTasks().create(
				"createLoggingReport",
				LoggingCollectorTask.class,
				indexManager
		);
		loggingTask.dependsOn( indexerTask );
	}
}
