/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.db;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildServiceRegistry;
import org.gradle.api.tasks.testing.Test;

import static org.hibernate.orm.db.DatabaseService.REGISTRATION_NAME;

/**
 * @author Steve Ebersole
 */
public class DatabaseServicePlugin implements Plugin<Project> {
	@Override
	@SuppressWarnings("UnstableApiUsage")
	public void apply(Project project) {
		// register the service used to restrict parallel execution
		// of tests - used to avoid database schema/catalog collisions
		final BuildServiceRegistry sharedServices = project.getGradle().getSharedServices();
		final Provider<DatabaseService> databaseServiceProvider = sharedServices.registerIfAbsent(
				REGISTRATION_NAME,
				DatabaseService.class,
				spec -> spec.getMaxParallelUsages().set( 1 )
		);

		project.getTasks().withType( Test.class ).forEach(
				test -> test.usesService( databaseServiceProvider )
		);
	}
}
