/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.SourceSet;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;
import org.hibernate.cfg.Environment;

/**
 * @author Steve Ebersole
 */
public class EnhancementHelper {
	static void enhance(SourceSet sourceSet, EnhanceExtension options, Project project) {
		final ClassLoader classLoader = toClassLoader( sourceSet.getRuntimeClasspath() );

		final EnhancementContext enhancementContext = new DefaultEnhancementContext() {
			@Override
			public ClassLoader getLoadingClassLoader() {
				return classLoader;
			}

			@Override
			public boolean doBiDirectionalAssociationManagement(UnloadedField field) {
				return options.getEnableAssociationManagement();
			}

			@Override
			public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
				return options.getEnableDirtyTracking();
			}

			@Override
			public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
				return options.getEnableLazyInitialization();
			}

			@Override
			public boolean isLazyLoadable(UnloadedField field) {
				return options.getEnableLazyInitialization();
			}

			@Override
			public boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
				return options.getEnableExtendedEnhancement();
			}
		};

		if ( options.getEnableExtendedEnhancement() ) {
			project.getLogger().warn("Extended enhancement is enabled. Classes other than entities may be modified. You should consider access the entities using getter/setter methods and disable this property. Use at your own risk." );
		}

		final Enhancer enhancer = Environment.getBytecodeProvider().getEnhancer( enhancementContext );

		for ( File classesDir: sourceSet.getOutput().getClassesDirs() ) {
			final FileTree fileTree = project.fileTree( classesDir );
			for ( File file : fileTree ) {
				if ( !file.getName().endsWith( ".class" ) ) {
					continue;
				}

				final byte[] enhancedBytecode = doEnhancement( classesDir, file, enhancer );
				if ( enhancedBytecode != null ) {
					writeOutEnhancedClass( enhancedBytecode, file, project.getLogger() );
					project.getLogger().info( "Successfully enhanced class [" + file + "]" );
				}
				else {
					project.getLogger().info( "Skipping class [" + file.getAbsolutePath() + "], not an entity nor embeddable" );
				}
			}
		}
	}

	public static ClassLoader toClassLoader(FileCollection runtimeClasspath) {
		List<URL> urls = new ArrayList<>();
		for ( File file : runtimeClasspath ) {
			try {
				urls.add( file.toURI().toURL() );
			}
			catch (MalformedURLException e) {
				throw new GradleException( "Unable to resolve classpath entry to URL : " + file.getAbsolutePath(), e );
			}
		}
		return new URLClassLoader( urls.toArray( new URL[0] ), Enhancer.class.getClassLoader() );
	}

	@SuppressWarnings("WeakerAccess")
	static byte[] doEnhancement(File root, File javaClassFile, Enhancer enhancer) {
		try {
			final String className = determineClassName( root, javaClassFile );
			final ByteArrayOutputStream originalBytes = new ByteArrayOutputStream();
			try (final FileInputStream fileInputStream = new FileInputStream( javaClassFile )) {
				byte[] buffer = new byte[1024];
				int length;
				while ( ( length = fileInputStream.read( buffer ) ) != -1 ) {
					originalBytes.write( buffer, 0, length );
				}
			}
			return enhancer.enhance( className, originalBytes.toByteArray() );
		}
		catch (Exception e) {
			throw new GradleException( "Unable to enhance class : " + javaClassFile, e );
		}
	}

	private static String determineClassName(File root, File javaClassFile) {
		return javaClassFile.getAbsolutePath().substring(
				root.getAbsolutePath().length() + 1,
				javaClassFile.getAbsolutePath().length() - ".class".length()
		).replace( File.separatorChar, '.' );
	}

	private static void writeOutEnhancedClass(byte[] enhancedBytecode, File file, Logger logger) {
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

	private EnhancementHelper() {
	}
}
