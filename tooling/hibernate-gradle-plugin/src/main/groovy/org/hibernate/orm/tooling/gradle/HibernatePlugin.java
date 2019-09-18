/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.tooling.gradle;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.SourceSet;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;
import org.hibernate.cfg.Environment;

/**
 * The Hibernate Gradle plugin.  Adds Hibernate build-time capabilities into your Gradle-based build.
 *
 * @author Jeremy Whiting
 * @author Steve Ebersole
 */
@SuppressWarnings("serial")
public class HibernatePlugin implements Plugin<Project> {
	private final Logger logger = Logging.getLogger( HibernatePlugin.class );

	public void apply(Project project) {
		project.getPlugins().apply( "java" );

		final HibernateExtension hibernateExtension = new HibernateExtension( project );

		project.getLogger().debug( "Adding Hibernate extensions to the build [{}]", project.getName() );
		project.getExtensions().add( "hibernate", hibernateExtension );

		project.afterEvaluate(
				p -> {
					if ( hibernateExtension.enhance != null ) {
						applyEnhancement( p, hibernateExtension );
					}
				}
		);
	}

	private void applyEnhancement(final Project project, final HibernateExtension hibernateExtension) {
		if ( !hibernateExtension.enhance.shouldApply() ) {
			project.getLogger().warn( "Skipping Hibernate bytecode enhancement since no feature is enabled" );
			return;
		}

		for ( final SourceSet sourceSet : hibernateExtension.getSourceSets() ) {
			project.getLogger().debug( "Applying Hibernate enhancement action to SourceSet.{}", sourceSet.getName() );

			final Task compileTask = project.getTasks().findByName( sourceSet.getCompileJavaTaskName() );
			assert compileTask != null;
			compileTask.doLast(
					task -> EnhancementHelper.enhance( sourceSet, hibernateExtension.enhance, project )
			);
		}
	}

}
