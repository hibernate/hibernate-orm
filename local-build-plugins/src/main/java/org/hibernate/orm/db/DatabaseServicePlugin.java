/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
		final String database = (String) project.getProperties().get( "db" );

		// H2 and HSQLDB are in-memory, so there is no sharing that needs to be avoided
		if ( database != null && !"h2".equals( database ) && !"hsqldb".equals( database ) ) {
			project.getTasks().withType( Test.class ).forEach(
					test -> test.usesService( databaseServiceProvider )
			);
		}
	}
}
