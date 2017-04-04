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
				new Action<Project>() {
					@Override
					public void execute(Project project) {
						if ( hibernateExtension.enhance != null ) {
							applyEnhancement( project, hibernateExtension );
						}
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
			compileTask.doLast(
					new Action<Task>() {
						@Override
						public void execute(Task task) {
							project.getLogger().debug( "Starting Hibernate enhancement on SourceSet.{}", sourceSet.getName() );

							final ClassLoader classLoader = toClassLoader( sourceSet.getRuntimeClasspath() );

							EnhancementContext enhancementContext = new DefaultEnhancementContext() {
								@Override
								public ClassLoader getLoadingClassLoader() {
									return classLoader;
								}

								@Override
								public boolean doBiDirectionalAssociationManagement(UnloadedField field) {
									return hibernateExtension.enhance.getEnableAssociationManagement();
								}

								@Override
								public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
									return hibernateExtension.enhance.getEnableDirtyTracking();
								}

								@Override
								public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
									return hibernateExtension.enhance.getEnableLazyInitialization();
								}

								@Override
								public boolean isLazyLoadable(UnloadedField field) {
									return hibernateExtension.enhance.getEnableLazyInitialization();
								}

								@Override
								public boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
									return hibernateExtension.enhance.getEnableExtendedEnhancement();
								}
							};

							if ( hibernateExtension.enhance.getEnableExtendedEnhancement() ) {
								logger.warn("Extended enhancement is enabled. Classes other than entities may be modified. You should consider access the entities using getter/setter methods and disable this property. Use at your own risk." );
							}

							final Enhancer enhancer = Environment.getBytecodeProvider().getEnhancer( enhancementContext );

							final FileTree fileTree = project.fileTree( sourceSet.getOutput().getClassesDir() );
							for ( File file : fileTree ) {
								if ( !file.getName().endsWith( ".class" ) ) {
									continue;
								}

								final byte[] enhancedBytecode = doEnhancement( sourceSet.getOutput().getClassesDir(), file, enhancer );
								if ( enhancedBytecode != null ) {
									writeOutEnhancedClass( enhancedBytecode, file );
									logger.info( "Successfully enhanced class [" + file + "]" );
								}
								else {
									logger.info( "Skipping class [" + file.getAbsolutePath() + "], not an entity nor embeddable" );
								}
							}
						}
					}
			);
		}
	}

	private ClassLoader toClassLoader(FileCollection runtimeClasspath) {
		List<URL> urls = new ArrayList<URL>();
		for ( File file : runtimeClasspath ) {
			try {
				urls.add( file.toURI().toURL() );
				logger.debug( "Adding classpath entry for " + file.getAbsolutePath() );
			}
			catch (MalformedURLException e) {
				throw new GradleException( "Unable to resolve classpath entry to URL : " + file.getAbsolutePath(), e );
			}
		}
		return new URLClassLoader( urls.toArray( new URL[urls.size()] ), Enhancer.class.getClassLoader() );
	}

	private byte[] doEnhancement(File root, File javaClassFile, Enhancer enhancer) {
		try {
			String className = javaClassFile.getAbsolutePath().substring(
					root.getAbsolutePath().length() + 1,
					javaClassFile.getAbsolutePath().length() - ".class".length()
			).replace( File.separatorChar, '.' );
			ByteArrayOutputStream originalBytes = new ByteArrayOutputStream();
			FileInputStream fileInputStream = new FileInputStream( javaClassFile );
			try {
				byte[] buffer = new byte[1024];
				int length;
				while ( ( length = fileInputStream.read( buffer ) ) != -1 ) {
					originalBytes.write( buffer, 0, length );
				}
			}
			finally {
				fileInputStream.close();
			}
			return enhancer.enhance( className, originalBytes.toByteArray() );
		}
		catch (Exception e) {
			throw new GradleException( "Unable to enhance class : " + javaClassFile, e );
		}
	}

	private void writeOutEnhancedClass(byte[] enhancedBytecode, File file) {
		try {
			if ( file.delete() ) {
				if ( !file.createNewFile() ) {
					logger.error( "Unable to recreate class file [" + file.getName() + "]" );
				}
			}
			else {
				logger.error( "Unable to delete class file [" + file.getName() + "]" );
			}
		}
		catch (IOException e) {
			logger.warn( "Problem preparing class file for writing out enhancements [" + file.getName() + "]" );
		}

		try {
			FileOutputStream outputStream = new FileOutputStream( file, false );
			try {
				outputStream.write( enhancedBytecode );
				outputStream.flush();
			}
			catch (IOException e) {
				throw new GradleException( "Error writing to enhanced class [" + file.getName() + "] to file [" + file.getAbsolutePath() + "]", e );
			}
			finally {
				try {
					outputStream.close();
				}
				catch (IOException ignore) {
				}
			}
		}
		catch (FileNotFoundException e) {
			throw new GradleException( "Error opening class file for writing : " + file.getAbsolutePath(), e );
		}

	}

}
