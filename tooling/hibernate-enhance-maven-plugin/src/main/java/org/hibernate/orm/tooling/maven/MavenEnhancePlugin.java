/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.tooling.maven;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;

/**
 * This plugin will enhance Entity objects.
 *
 * @author Jeremy Whiting
 * @author Luis Barreiro
 */
@Mojo(name = "enhance", defaultPhase = LifecyclePhase.COMPILE)
@Execute(goal = "enhance", phase = LifecyclePhase.COMPILE)
public class MavenEnhancePlugin extends AbstractMojo {

	/**
	 * The contexts to use during enhancement.
	 */
	private List<File> sourceSet = new ArrayList<File>();

	@Parameter(property = "dir", defaultValue = "${project.build.outputDirectory}")
	private String dir = null;

	@Parameter(property = "failOnError", defaultValue = "true")
	private boolean failOnError = true;

	@Parameter(property = "enableLazyInitialization", defaultValue = "true")
	private boolean enableLazyInitialization = true;

	@Parameter(property = "enableDirtyTracking", defaultValue = "true")
	private boolean enableDirtyTracking = true;

	@Parameter(property = "enableAssociationManagement", defaultValue = "true")
	private boolean enableAssociationManagement = true;

	private boolean shouldApply() {
		return enableLazyInitialization || enableDirtyTracking || enableAssociationManagement;
	}

	public void execute() throws MojoExecutionException, MojoFailureException {
		if ( !shouldApply() ) {
			return;
		}

		getLog().info( "Starting Hibernate enhancement for class sourceSet on " + dir );

		/** Perform a depth first search for sourceSet. */
		File root = new File( this.dir );
		walkDir( root );

		final ClassLoader classLoader = toClassLoader( Arrays.asList( root ) );

		EnhancementContext enhancementContext = new DefaultEnhancementContext() {
			@Override
			public ClassLoader getLoadingClassLoader() {
				return classLoader;
			}

			@Override
			public boolean doBiDirectionalAssociationManagement(CtField field) {
				return enableAssociationManagement;
			}

			@Override
			public boolean doDirtyCheckingInline(CtClass classDescriptor) {
				return enableDirtyTracking;
			}

			@Override
			public boolean hasLazyLoadableAttributes(CtClass classDescriptor) {
				return enableLazyInitialization;
			}

			@Override
			public boolean isLazyLoadable(CtField field) {
				return enableLazyInitialization;
			}
		};

		final Enhancer enhancer = new Enhancer( enhancementContext );
		final ClassPool classPool = new ClassPool( false );

		for ( File file : sourceSet ) {
			final CtClass ctClass = toCtClass( file, classPool );
			if ( ctClass == null ) {
				continue;
			}

			if ( !enhancementContext.isEntityClass( ctClass ) && !enhancementContext.isCompositeClass( ctClass ) ) {
				getLog().info( "Skipping class file [" + file.getAbsolutePath() + "], not an entity nor embeddable" );
				continue;
			}

			final byte[] enhancedBytecode = doEnhancement( ctClass, enhancer );
			writeOutEnhancedClass( enhancedBytecode, ctClass, file );

			getLog().info( "Successfully enhanced class [" + ctClass.getName() + "]" );
		}
	}

	private ClassLoader toClassLoader(List<File> runtimeClasspath) throws MojoExecutionException {
		List<URL> urls = new ArrayList<URL>();
		for ( File file : runtimeClasspath ) {
			try {
				urls.add( file.toURI().toURL() );
				getLog().debug( "Adding root " + file.getAbsolutePath() + " to classpath " );
			}
			catch (MalformedURLException e) {
				String msg = "Unable to resolve classpath entry to URL: " + file.getAbsolutePath();
				if ( failOnError ) {
					throw new MojoExecutionException( msg, e );
				}
				getLog().warn( msg );
			}
		}

		return new URLClassLoader( urls.toArray( new URL[urls.size()] ), Enhancer.class.getClassLoader() );
	}

	private CtClass toCtClass(File file, ClassPool classPool) throws MojoExecutionException {
		try {
			final InputStream is = new FileInputStream( file.getAbsolutePath() );

			try {
				return classPool.makeClass( is );
			}
			catch (IOException e) {
				String msg = "Javassist unable to load class in preparation for enhancing: " + file.getAbsolutePath();
				if ( failOnError ) {
					throw new MojoExecutionException( msg, e );
				}
				getLog().warn( msg );
				return null;
			}
			finally {
				try {
					is.close();
				}
				catch (IOException e) {
					getLog().info( "Was unable to close InputStream : " + file.getAbsolutePath(), e );
				}
			}
		}
		catch (FileNotFoundException e) {
			// should never happen, but...
			String msg = "Unable to locate class file for InputStream: " + file.getAbsolutePath();
			if ( failOnError ) {
				throw new MojoExecutionException( msg, e );
			}
			getLog().warn( msg );
			return null;
		}
	}

	private byte[] doEnhancement(CtClass ctClass, Enhancer enhancer) throws MojoExecutionException {
		try {
			return enhancer.enhance( ctClass.getName(), ctClass.toBytecode() );
		}
		catch (Exception e) {
			String msg = "Unable to enhance class: " + ctClass.getName();
			if ( failOnError ) {
				throw new MojoExecutionException( msg, e );
			}
			getLog().warn( msg );
			return null;
		}
	}

	/**
	 * Expects a directory.
	 */
	private void walkDir(File dir) {
		walkDir(
				dir,
				new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						return ( pathname.isFile() && pathname.getName().endsWith( ".class" ) );
					}
				},
				new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						return ( pathname.isDirectory() );
					}
				}
		);
	}

	private void walkDir(File dir, FileFilter classesFilter, FileFilter dirFilter) {
		File[] dirs = dir.listFiles( dirFilter );
		for ( File dir1 : dirs ) {
			walkDir( dir1, classesFilter, dirFilter );
		}
		File[] files = dir.listFiles( classesFilter );
		Collections.addAll( this.sourceSet, files );
	}

	private void writeOutEnhancedClass(byte[] enhancedBytecode, CtClass ctClass, File file) throws MojoExecutionException{
		if ( enhancedBytecode == null ) {
			return;
		}
		try {
			if ( file.delete() ) {
				if ( !file.createNewFile() ) {
					getLog().error( "Unable to recreate class file [" + ctClass.getName() + "]" );
				}
			}
			else {
				getLog().error( "Unable to delete class file [" + ctClass.getName() + "]" );
			}
		}
		catch (IOException e) {
			getLog().warn( "Problem preparing class file for writing out enhancements [" + ctClass.getName() + "]" );
		}

		try {
			FileOutputStream outputStream = new FileOutputStream( file, false );
			try {
				outputStream.write( enhancedBytecode );
				outputStream.flush();
			}
			catch (IOException e) {
				String msg = String.format( "Error writing to enhanced class [%s] to file [%s]", ctClass.getName(), file.getAbsolutePath() );
				if ( failOnError ) {
					throw new MojoExecutionException( msg, e );
				}
				getLog().warn( msg );
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
			String msg = "Error opening class file for writing: " + file.getAbsolutePath();
			if ( failOnError ) {
				throw new MojoExecutionException( msg, e );
			}
			getLog().warn( msg );
		}
	}
}
