/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.properties;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Integrates collection of documentation about Hibernate configuration properties
 * from the Javadoc of the project, and generates an Asciidoc document from it
 * which is then included into the User Guide.
 */
public class SettingsDocumentationPlugin implements Plugin<Project> {
	public static final String TASK_GROUP_NAME = "documentation";

	@Override
	public void apply(Project project) {
		project.getTasks().register( SettingsDocGeneratorTask.TASK_NAME, SettingsDocGeneratorTask.class );
	}
}
