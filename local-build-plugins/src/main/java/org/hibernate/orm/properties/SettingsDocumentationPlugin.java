/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.properties;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.hibernate.build.OrmBuildDetails;
import org.hibernate.build.aspects.ModuleAspect;

import static org.hibernate.orm.properties.SettingsDocExtension.EXTENSION_NAME;
import static org.hibernate.orm.properties.SettingsDocGenerationTask.TASK_NAME;

/**
 * Integrates collection of documentation about Hibernate configuration properties
 * from the Javadoc of the project, and generates an Asciidoc document from it
 * which is then included into the User Guide.
 */
public class SettingsDocumentationPlugin implements Plugin<Project> {
	public static final String TASK_GROUP_NAME = "documentation";

	@Override
	public void apply(Project project) {
		// if not already, so we can access HibernateVersion
		project.getPluginManager().apply( ModuleAspect.class );

		// create and register the DSL extension
		final SettingsDocExtension dslExtension = project.getExtensions()
				.create( SettingsDocExtension.class, EXTENSION_NAME, SettingsDocExtension.class );
		dslExtension.getJavadocDirectory().convention( project.getLayout().getBuildDirectory().dir( "javadocs" ) );
		dslExtension.getPublishedDocsUrl().convention( "https://docs.jboss.org/hibernate/orm" );
		dslExtension.getOutputFile().convention( project.getLayout().getBuildDirectory().file( "asciidoc/fragments/config-settings.adoc" ) );

		// create the generation task
		final OrmBuildDetails details = project.getExtensions().getByType( OrmBuildDetails.class );
		final TaskProvider<SettingsDocGenerationTask> settingsDocGenerationTask = project.getTasks()
				.register( TASK_NAME, SettingsDocGenerationTask.class, dslExtension );
		settingsDocGenerationTask.configure( task -> task.getOrmBuildDetails().set( details ) );
	}
}
