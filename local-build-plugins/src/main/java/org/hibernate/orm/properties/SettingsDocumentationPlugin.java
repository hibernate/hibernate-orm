/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.properties;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import org.hibernate.orm.env.EnvironmentProjectPlugin;

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
		project.getPluginManager().apply( EnvironmentProjectPlugin.class );

		// create and register the DSL extension
		final SettingsDocExtension dslExtension = new SettingsDocExtension( project );
		project.getExtensions().add( EXTENSION_NAME, dslExtension );
		dslExtension.getJavadocDirectory().convention( project.getLayout().getBuildDirectory().dir( "javadocs" ) );
		dslExtension.getPublishedDocsUrl().convention( "https://docs.hibernate.org/orm" );
		dslExtension.getOutputFile().convention( project.getLayout().getBuildDirectory().file( "asciidoc/fragments/config-settings.adoc" ) );

		// create the generation task
		project.getTasks().register( TASK_NAME, SettingsDocGenerationTask.class, dslExtension, project );
	}
}
