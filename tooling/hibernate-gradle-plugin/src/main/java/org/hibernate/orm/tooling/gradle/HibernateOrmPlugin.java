/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import org.hibernate.orm.tooling.gradle.enhance.EnhancementTask;
import org.hibernate.orm.tooling.gradle.metamodel.JpaMetamodelGenerationTask;

/**
 * Hibernate ORM Gradle plugin
 */
public class HibernateOrmPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getPlugins().apply( JavaPlugin.class );

		project.getLogger().debug( "Adding Hibernate extensions to the build [{}]", project.getPath() );
		final HibernateOrmSpec ormDsl = project.getExtensions().create( HibernateOrmSpec.DSL_NAME,  HibernateOrmSpec.class, project );

		final Configuration hibernateOrm = project.getConfigurations().maybeCreate( "hibernateOrm" );
		project.getDependencies().add(
				"hibernateOrm",
				project.provider( () -> "org.hibernate.orm:hibernate-core:" + HibernateVersion.version )
		);
		project.getConfigurations().getByName( "implementation" ).extendsFrom( hibernateOrm );

		final JavaPluginConvention javaPluginConvention = project.getConvention().findPlugin( JavaPluginConvention.class );
		assert javaPluginConvention != null;

		final SourceSetContainer sourceSets = javaPluginConvention.getSourceSets();
		final SourceSet mainSourceSet = sourceSets.getByName( SourceSet.MAIN_SOURCE_SET_NAME );

		EnhancementTask.apply( ormDsl, mainSourceSet, project );
		JpaMetamodelGenerationTask.apply( ormDsl, mainSourceSet, project );

		project.getDependencies().add(
				"implementation",
				project.provider( () -> "org.hibernate.orm:hibernate-core:" + ormDsl.getHibernateVersionProperty().get() )
		);
	}
}
