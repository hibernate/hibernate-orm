/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.properties;

import org.hibernate.orm.properties.processor.ConfigPropertyHolder;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

public class ConfigPropertyCollectorPlugin implements Plugin<Project> {
	public static final String TASK_GROUP_NAME = "hibernate-properties";

	@Override
	public void apply(Project project) {
		final Task groupingTask = project.getTasks().maybeCreate( "generateHibernateConfigProperties" );
		groupingTask.setGroup( TASK_GROUP_NAME );

		ConfigPropertyHolder propertyHolder = new ConfigPropertyHolder();
		final ConfigPropertyCollectorTask configPropertyCollectorTask = project.getTasks().create(
				"generateConfigPropertiesMap",
				ConfigPropertyCollectorTask.class,
				propertyHolder
		);
		groupingTask.dependsOn( configPropertyCollectorTask );

		final ConfigPropertyWriterTask configPropertyWriterTask = project.getTasks().create(
				"writeConfigPropertiesMap",
				ConfigPropertyWriterTask.class,
				propertyHolder
		);
		groupingTask.dependsOn( configPropertyWriterTask );
		configPropertyWriterTask.dependsOn( configPropertyCollectorTask );
	}
}
