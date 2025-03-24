/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;

import org.hibernate.orm.tooling.gradle.enhance.EnhancementHelper;

/**
 * Hibernate ORM Gradle plugin
 */
public class HibernateOrmPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getPluginManager().withPlugin( "java", plugin -> {

			project.getLogger().debug( "Adding Hibernate extensions to the build [{}]", project.getPath() );
			final HibernateOrmSpec ormDsl = project.getExtensions().create(
					HibernateOrmSpec.DSL_NAME,
					HibernateOrmSpec.class
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

			Injected injected = project.getObjects().newInstance(Injected.class);
			SourceSet sourceSet = resolveSourceSet( ormDsl.getSourceSet().get(), project );
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

				FileCollection classesDirs = sourceSet.getOutput().getClassesDirs();
				Configuration compileConfig = project
						.getConfigurations()
						.getByName( sourceSet.getCompileClasspathConfigurationName() );
				Set<File> dependencyFiles = compileConfig.getFiles();
				Logger logger = project.getLogger();
				//noinspection Convert2Lambda
				languageCompileTask.doLast(new Action<>() {
					@Override
					public void execute(Task t) {
						try {
							final Method getDestinationDirectory = languageCompileTask.getClass().getMethod("getDestinationDirectory");
							final DirectoryProperty classesDirectory = (DirectoryProperty) getDestinationDirectory.invoke(languageCompileTask);
							final ClassLoader classLoader = Helper.toClassLoader(classesDirs, dependencyFiles);
							EnhancementHelper.enhance(classesDirectory, classLoader, ormDsl, logger, injected.getFileOperations());
						}
						catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				});
			}
		} );
	}

	private SourceSet resolveSourceSet(String name, Project project) {
		final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType( JavaPluginExtension.class );
		return javaPluginExtension.getSourceSets().getByName( name );
	}

	private void prepareHbmTransformation(HibernateOrmSpec ormDsl, Project project) {

	}
}
