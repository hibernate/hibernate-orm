/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle;

import java.lang.reflect.Method;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;

import org.hibernate.orm.tooling.gradle.enhance.EnhancementHelper;

/**
 * Hibernate ORM Gradle plugin
 */
public class HibernateOrmPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getPlugins().withType( JavaPlugin.class, javaPlugin -> {

			project.getLogger().debug( "Adding Hibernate extensions to the build [{}]", project.getPath() );
			final HibernateOrmSpec ormDsl = project.getExtensions().create(
					HibernateOrmSpec.DSL_NAME,
					HibernateOrmSpec.class,
					project
			);

			prepareEnhancement( ormDsl, project );
			prepareHbmTransformation( ormDsl, project );


			//noinspection ConstantConditions
			project.getDependencies().add(
					"implementation",
					ormDsl.getUseSameVersion().map( (use) -> use
							? "org.hibernate.orm:hibernate-core:" + HibernateVersion.version
							: null
					)
			);
		} );
	}

	private void prepareEnhancement(HibernateOrmSpec ormDsl, Project project) {
		project.getGradle().getTaskGraph().whenReady( (graph) -> {
			if ( !ormDsl.isEnhancementEnabled() ) {
				return;
			}

			final SourceSet sourceSet = ormDsl.getSourceSet().get();
			final Set<String> languages = ormDsl.getLanguages().getOrNull();
			if ( languages == null ) {
				return;
			}

			for ( String language : languages ) {
				final String languageCompileTaskName = sourceSet.getCompileTaskName( language );
				final Task languageCompileTask = project.getTasks().findByName( languageCompileTaskName );
				if ( languageCompileTask == null ) {
					continue;
				}

				//noinspection Convert2Lambda
				languageCompileTask.doLast(new Action<>() {
					@Override
					public void execute(Task t) {
						try {
							final Method getDestinationDirectory = languageCompileTask.getClass().getMethod("getDestinationDirectory");
							final DirectoryProperty classesDirectory = (DirectoryProperty) getDestinationDirectory.invoke(languageCompileTask);
							final ClassLoader classLoader = Helper.toClassLoader(sourceSet, project);
							EnhancementHelper.enhance(classesDirectory, classLoader, ormDsl, project);
						}
						catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				});
			}
		} );
	}

	private void prepareHbmTransformation(HibernateOrmSpec ormDsl, Project project) {

	}
}
