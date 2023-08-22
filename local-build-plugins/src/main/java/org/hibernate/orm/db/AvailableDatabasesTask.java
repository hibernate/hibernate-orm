/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.db;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Steve Ebersole
 */
public abstract class AvailableDatabasesTask extends DefaultTask {
	public static final String TASK_NAME = "listAvailableDatabases";

	@TaskAction
	public void listAvailableDatabases() {
		final DatabasesExtension databasesExtension = getProject().getExtensions().getByType( DatabasesExtension.class );
		getLogger().lifecycle( "Available Testing Databases {" );
		databasesExtension.getAvailable().forEach( (database) -> {
			if ( databasesExtension.getAvailable().equals( database ) ) {
				getLogger().lifecycle( "  {} (*) {", database.getName() );
			}
			else {
				getLogger().lifecycle( "  {} {", database.getName() );
			}
			getLogger().lifecycle( "    dialect = {}", database.getDialect().get() );
			getLogger().lifecycle( "    driver = {}", database.getDriver().get() );
			getLogger().lifecycle( "    url = {}", database.getUrl().get() );
			getLogger().lifecycle( "  }" );
		} );
		getLogger().lifecycle( "}" );
	}
}
