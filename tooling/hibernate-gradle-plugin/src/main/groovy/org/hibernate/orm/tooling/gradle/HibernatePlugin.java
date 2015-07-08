/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.tooling.gradle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;

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
			return;
		}

		for ( final SourceSet sourceSet : hibernateExtension.getSourceSets() ) {
			project.getLogger().debug( "Applying Hibernate enhancement action to SourceSet.{}", sourceSet.getName() );

			final Task compileTask = project.getTasks().findByName( sourceSet.getCompileJavaTaskName() );

			final ClassLoader classLoader = toClassLoader( sourceSet.getRuntimeClasspath() );

			compileTask.doLast(
					new Action<Task>() {
						@Override
						public void execute(Task task) {
							project.getLogger().debug( "Starting Hibernate enhancement on SourceSet.{}", sourceSet.getName() );

							EnhancementContext enhancementContext = new DefaultEnhancementContext() {
								@Override
								public ClassLoader getLoadingClassLoader() {
									return classLoader;
								}

								@Override
								public boolean doBiDirectionalAssociationManagement(CtField field) {
									return hibernateExtension.enhance.getEnableAssociationManagement();
								}

								@Override
								public boolean doDirtyCheckingInline(CtClass classDescriptor) {
									return hibernateExtension.enhance.getEnableDirtyTracking();
								}

								@Override
								public boolean hasLazyLoadableAttributes(CtClass classDescriptor) {
									return hibernateExtension.enhance.getEnableLazyInitialization();
								}

								@Override
								public boolean isLazyLoadable(CtField field) {
									return hibernateExtension.enhance.getEnableLazyInitialization();
								}
							};

							final Enhancer enhancer = new Enhancer( enhancementContext );
							final ClassPool classPool = new ClassPool( false );


							final FileTree fileTree = project.fileTree( sourceSet.getOutput().getClassesDir() );
							for ( File file : fileTree ) {
								if ( !file.getName().endsWith( ".class" ) ) {
									continue;
								}

								final CtClass ctClass = toCtClass( file, classPool );

								if ( !enhancementContext.isEntityClass( ctClass )
										&& !enhancementContext.isCompositeClass( ctClass ) ) {
									logger.info( "Skipping class [" + file.getAbsolutePath() + "], not an entity nor embeddable" );
									continue;
								}

								final byte[] enhancedBytecode = doEnhancement( ctClass, enhancer );
								writeOutEnhancedClass( enhancedBytecode, ctClass, file );

								logger.info( "Successfully enhanced class [" + ctClass.getName() + "]" );
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
			}
			catch (MalformedURLException e) {
				throw new GradleException( "Unable to resolve classpath entry to URL : " + file.getAbsolutePath(), e );
			}
		}

		return new URLClassLoader(
				urls.toArray( new URL[urls.size()] ),
				ClassLoader.getSystemClassLoader().getParent()
		);
	}

	private CtClass toCtClass(File file, ClassPool classPool) {
		try {
			final InputStream is = new FileInputStream( file.getAbsolutePath() );

			try {
				return classPool.makeClass( is );
			}
			catch (IOException e) {
				throw new GradleException( "Javassist unable to load class in preparation for enhancing : " + file.getAbsolutePath(), e );
			}
			finally {
				try {
					is.close();
				}
				catch (IOException e) {
					logger.info( "Was unable to close InputStream : " + file.getAbsolutePath(), e );
				}
			}
		}
		catch (FileNotFoundException e) {
			// should never happen, but...
			throw new GradleException( "Unable to locate class file for InputStream: " + file.getAbsolutePath(), e );
		}
	}

	private byte[] doEnhancement(CtClass ctClass, Enhancer enhancer) {
		try {
			return enhancer.enhance( ctClass.getName(), ctClass.toBytecode() );
		}
		catch (Exception e) {
			throw new GradleException( "Unable to enhance class : " + ctClass.getName(), e );
		}
	}

	private void writeOutEnhancedClass(byte[] enhancedBytecode, CtClass ctClass, File file) {
		try {
			if ( file.delete() ) {
				if ( !file.createNewFile() ) {
					logger.error( "Unable to recreate class file [" + ctClass.getName() + "]" );
				}
			}
			else {
				logger.error( "Unable to delete class file [" + ctClass.getName() + "]" );
			}
		}
		catch (IOException e) {
			logger.warn( "Problem preparing class file for writing out enhancements [" + ctClass.getName() + "]" );
		}

		try {
			FileOutputStream outputStream = new FileOutputStream( file, false );
			try {
				outputStream.write( enhancedBytecode );
				outputStream.flush();
			}
			catch (IOException e) {
				throw new GradleException( "Error writing to enhanced class [" + ctClass.getName() + "] to file [" + file.getAbsolutePath() + "]", e );
			}
			finally {
				try {
					outputStream.close();
					ctClass.detach();
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
