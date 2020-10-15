/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

/**
 * @author Steve Ebersole
 */
public class HibernateOrmPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getPlugins().apply( JavaPlugin.class );

		project.getLogger().debug( "Adding Hibernate extensions to the build [{}]", project.getName() );
		final HibernateOrmSpec ormDsl = project.getExtensions().create( HibernateOrmSpec.DSL_NAME,  HibernateOrmSpec.class, project );


		final JavaPluginConvention javaPluginConvention = project.getConvention().findPlugin( JavaPluginConvention.class );
		assert javaPluginConvention != null;

		final SourceSetContainer sourceSets = javaPluginConvention.getSourceSets();

		// apply the main source set by default...
		final SourceSet mainSourceSet = sourceSets.getByName( SourceSet.MAIN_SOURCE_SET_NAME );

		EnhancementTask.apply( ormDsl, mainSourceSet, project );
		MetamodelGenTask.apply( ormDsl, mainSourceSet, project );
	}
}
