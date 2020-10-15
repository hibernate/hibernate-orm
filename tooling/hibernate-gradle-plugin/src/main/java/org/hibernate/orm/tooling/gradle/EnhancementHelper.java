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
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.work.InputChanges;

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
	public static void enhance(
			DirectoryProperty classesDirectoryProperty,
			InputChanges inputChanges,
			EnhancementSpec enhancementDsl,
			Project project) {
		final Directory classesDir = classesDirectoryProperty.get();
		final File classesDirFile = classesDir.getAsFile();

		final Enhancer enhancer = generateEnhancer( classesDir, enhancementDsl );

		final String classesDirPath = classesDirFile.getAbsolutePath();

		inputChanges.getFileChanges( classesDirectoryProperty ).forEach(
				change -> {
					switch ( change.getChangeType() ) {
						case ADDED:
						case MODIFIED: {
							final File changedFile = change.getFile();
							if ( changedFile.getName().endsWith( ".class" ) ) {
								final String classFilePath = changedFile.getAbsolutePath();
								if ( classFilePath.startsWith( classesDirPath ) ) {
									// we found the directory it came from
									//		-use that to determine the class name
									enhance( classesDirFile, changedFile, enhancer, project );
									break;
								}
							}
							break;
						}
						case REMOVED: {
							// nothing to do
							break;
						}
						default: {
							throw new UnsupportedOperationException( "Unexpected ChangeType : " + change.getChangeType().name() );
						}
					}
				}
		);
	}

	private static void enhance(
			File classesDir,
			File javaClassFile,
			Enhancer enhancer,
			Project project) {
		final byte[] enhancedBytecode = doEnhancement( classesDir, javaClassFile, enhancer );
		if ( enhancedBytecode != null ) {
			writeOutEnhancedClass( enhancedBytecode, javaClassFile, project.getLogger() );
			project.getLogger().info( "Successfully enhanced class [" + javaClassFile.getAbsolutePath() + "]" );
		}
		else {
			project.getLogger().info( "Skipping class [" + javaClassFile.getAbsolutePath() + "], not an entity nor embeddable" );
		}
	}

	private static byte[] doEnhancement(File root, File javaClassFile, Enhancer enhancer) {
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

	private static Enhancer generateEnhancer(Directory classesDir, EnhancementSpec enhancementDsl) {
		final ClassLoader classLoader = toClassLoader( classesDir.getAsFileTree() );

		final EnhancementContext enhancementContext = new DefaultEnhancementContext() {
			@Override
			public ClassLoader getLoadingClassLoader() {
				return classLoader;
			}

			@Override
			public boolean doBiDirectionalAssociationManagement(UnloadedField field) {
				return enhancementDsl.getEnableAssociationManagement().get();
			}

			@Override
			public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
				return enhancementDsl.getEnableDirtyTracking().get();
			}

			@Override
			public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
				return enhancementDsl.getEnableLazyInitialization().get();
			}

			@Override
			public boolean isLazyLoadable(UnloadedField field) {
				return enhancementDsl.getEnableLazyInitialization().get();
			}

			@Override
			public boolean doExtendedEnhancement(UnloadedClass classDescriptor) {
				return enhancementDsl.getEnableExtendedEnhancement().get();
			}
		};

		return Environment.getBytecodeProvider().getEnhancer( enhancementContext );
	}

	private static ClassLoader toClassLoader(FileCollection runtimeClasspath) {
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

		try ( FileOutputStream outputStream = new FileOutputStream( file, false ) ) {
			outputStream.write( enhancedBytecode );
			outputStream.flush();
		}
		catch (FileNotFoundException e) {
			throw new GradleException( "Error opening class file for writing : " + file.getAbsolutePath(), e );
		}
		catch (IOException e) {
			throw new GradleException( "Error writing to enhanced class [" + file.getName() + "] to file [" + file.getAbsolutePath() + "]", e );
		}
	}

	private EnhancementHelper() {
	}
}
