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

		EnhancementTask.apply( ormDsl, ormDsl.getSourceSetProperty().get(), project );
		JpaMetamodelGenerationTask.apply( ormDsl, ormDsl.getSourceSetProperty().get(), project );

//		project.getDependencies().add(
//				"implementation",
//				ormDsl.getHibernateVersionProperty().map( (ormVersion) -> Character.isDigit( ormVersion.charAt( 0 ) )
//						? "org.hibernate.orm:hibernate-core:" + ormVersion
//						: null
//				)
//		);
	}
}
