/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.build;

import java.util.Set;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;


/**
 * @author Christian Beikov
 */
public class HibernateBuildPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		Provider<DatabaseService> provider = project.getGradle().getSharedServices().registerIfAbsent(
				"db",
				DatabaseService.class,
				spec -> {
					spec.getMaxParallelUsages().set( 1 );
				}
		);
		Set<Task> tasks = project.getTasksByName( "test", false );
		for ( Task task : tasks ) {
			task.usesService( provider );
		}
	}
}
