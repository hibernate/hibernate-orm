/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.db;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import static org.hibernate.orm.db.DatabasesExtension.DSL_NAME;

/**
 * Contributes a DSL extension for standardized configuration of the databases
 * available for testing
 *
 * @author Steve Ebersole
 */
public class DatabasesPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getExtensions().add( DSL_NAME, DatabasesExtension.class );

		project.getTasks().register( AvailableDatabasesTask.TASK_NAME, AvailableDatabasesTask.class, (task) -> {
			task.setGroup( "verification" );
			task.setDescription( "Lists all databases available for testing" );
		} );
	}
}
