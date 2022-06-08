/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.JvmEcosystemPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.AbstractCompile;

import org.hibernate.orm.tooling.gradle.enhance.EnhancementHelper;
import org.hibernate.orm.tooling.gradle.metamodel.JpaMetamodelGenerationTask;

import static org.hibernate.orm.tooling.gradle.Helper.determineCompileSourceSetName;

/**
 * Hibernate ORM Gradle plugin
 */
public class HibernateOrmPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		// for SourceSet support and other JVM goodies
		project.getPlugins().apply( JvmEcosystemPlugin.class );

		project.getLogger().debug( "Adding Hibernate extensions to the build [{}]", project.getPath() );
		final HibernateOrmSpec ormDsl = project.getExtensions().create( HibernateOrmSpec.DSL_NAME,  HibernateOrmSpec.class, project );

		prepareEnhancement( ormDsl, project );

		JpaMetamodelGenerationTask.apply( ormDsl, ormDsl.getSourceSetProperty().get(), project );

//		project.getDependencies().add(
//				"implementation",
//				ormDsl.getHibernateVersionProperty().map( (ormVersion) -> Character.isDigit( ormVersion.charAt( 0 ) )
//						? "org.hibernate.orm:hibernate-core:" + ormVersion
//						: null
//				)
//		);
	}

	private void prepareEnhancement(HibernateOrmSpec ormDsl, Project project) {
		project.getGradle().getTaskGraph().whenReady( (graph) -> {
			if ( !ormDsl.getSupportEnhancementProperty().get() ) {
				return;
			}
			graph.getAllTasks().forEach( (task) -> {
				if ( task instanceof AbstractCompile ) {
					final SourceSet sourceSetLocal = ormDsl.getSourceSetProperty().get();

					final String compiledSourceSetName = determineCompileSourceSetName( task.getName() );
					if ( !sourceSetLocal.getName().equals( compiledSourceSetName ) ) {
						return;
					}

					final AbstractCompile compileTask = (AbstractCompile) task;
					//noinspection Convert2Lambda
					task.doLast( new Action<>() {
						@Override
						public void execute(Task t) {
							final DirectoryProperty classesDirectory = compileTask.getDestinationDirectory();
							final ClassLoader classLoader = Helper.toClassLoader( sourceSetLocal.getOutput().getClassesDirs() );

							EnhancementHelper.enhance( classesDirectory, classLoader, ormDsl, project );
						}
					} );

					task.finalizedBy( this );
				}
			} );
		} );
	}
}
