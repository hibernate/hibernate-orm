/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;

import org.hibernate.orm.tooling.gradle.enhance.EnhancementHelper;
import org.hibernate.orm.tooling.gradle.reveng.GenerateCfgTask;
import org.hibernate.orm.tooling.gradle.reveng.GenerateDaoTask;
import org.hibernate.orm.tooling.gradle.reveng.GenerateHbmTask;
import org.hibernate.orm.tooling.gradle.reveng.GenerateJavaTask;
import org.hibernate.orm.tooling.gradle.reveng.GenerateSchemaAnnotationsTask;
import org.hibernate.orm.tooling.gradle.reveng.RevengTask;
import org.hibernate.orm.tooling.gradle.reveng.RunSqlTask;

/**
 * Hibernate ORM Gradle plugin
 */
public class HibernateOrmPlugin implements Plugin<Project> {

	private static final Map<String, Class<? extends RevengTask>> REVENG_TASK_MAP = Map.of(
			"runSql", RunSqlTask.class,
			"generateJava", GenerateJavaTask.class,
			"generateCfg", GenerateCfgTask.class,
			"generateHbm", GenerateHbmTask.class,
			"generateDao", GenerateDaoTask.class
	);

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
			prepareReveng( ormDsl, project );

			project
					.getExtensions()
					.getByType( JavaPluginExtension.class )
					.getSourceSets()
					.configureEach( sourceSet -> project.getDependencies().add(
							sourceSet.getImplementationConfigurationName(),
							ormDsl.getUseSameVersion().zip(ormDsl.getSourceSet(), (use, sourceSetName) ->
									(use && sourceSetName.equals( sourceSet.getName() ))
											? "org.hibernate.orm:hibernate-core:" + HibernateVersion.version
											: null
							)
					) );
		} );
	}

	private void prepareEnhancement(HibernateOrmSpec ormDsl, Project project) {
		project.getGradle().getTaskGraph().whenReady( (graph) -> {
			if ( !ormDsl.getEnhancement().isPresent() ) {
				return;
			}

			SourceSet sourceSet = resolveSourceSet( ormDsl.getSourceSet().get(), project );
			final Set<String> languages = ormDsl.getLanguages().getOrNull();
			if ( languages == null ) {
				return;
			}

			for ( String language : languages ) {
				final String languageCompileTaskName = sourceSet.getCompileTaskName( language );
				project.getTasks()
						.matching( task -> task.getName().equals( languageCompileTaskName ) )
						.configureEach( task -> {
							FileCollection classesDirs = sourceSet.getOutput().getClassesDirs();
							Provider<FileCollection> dependencyFiles = project
									.getConfigurations()
									.named( sourceSet.getCompileClasspathConfigurationName() )
									.map(FileCollection.class::cast);
							//noinspection Convert2Lambda
							task.doLast(new Action<>() {
								@Override
								public void execute(Task t) {
									try {
										final Method getDestinationDirectory = task.getClass().getMethod("getDestinationDirectory");
										final DirectoryProperty classesDirectory = (DirectoryProperty) getDestinationDirectory.invoke(task);
										final ClassLoader classLoader = Helper.toClassLoader(classesDirs, dependencyFiles.get().getFiles());
										EnhancementHelper.enhance(classesDirectory, classLoader, ormDsl);
									}
									catch (Exception e) {
										throw new RuntimeException(e);
									}
								}
							});
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

	@SuppressWarnings("unchecked")
	private void prepareReveng(HibernateOrmSpec ormDsl, Project project) {
		for ( Map.Entry<String, Class<? extends RevengTask>> entry : REVENG_TASK_MAP.entrySet() ) {
			project.getTasks().register( entry.getKey(), entry.getValue(), task -> {
				task.doFirst( w -> task.initialize( ormDsl.getReveng() ) );
			} );
		}
		project.getTasks().register( "generateSchemaAnnotations", GenerateSchemaAnnotationsTask.class );
	}
}
