/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle.enhance;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.work.InputChanges;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;
import org.hibernate.cfg.Environment;
import org.hibernate.orm.tooling.gradle.Helper;

import static org.hibernate.orm.tooling.gradle.Helper.determineClassName;

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

		inputChanges.getFileChanges( classesDirectoryProperty ).forEach( (change) -> {
			switch ( change.getChangeType() ) {
				case ADDED:
				case MODIFIED: {
					final File changedFile = change.getFile();
					if ( changedFile.getName().endsWith( ".class" ) ) {
						final String classFilePath = changedFile.getAbsolutePath();
						if ( classFilePath.startsWith( classesDirPath ) ) {
							// we found the directory it came from - use that to determine the class name
							final String className = determineClassName( classesDirFile, changedFile );

							final long lastModified = changedFile.lastModified();

							enhance( changedFile, className, enhancer, project );

							final boolean timestampReset = changedFile.setLastModified( lastModified );
							if ( !timestampReset ) {
								project.getLogger().debug( "`{}`.setLastModified failed", project.relativePath( changedFile ) );
							}

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
		} );
	}

	private static void enhance(
			File javaClassFile,
			String className,
			Enhancer enhancer,
			Project project) {
		final byte[] enhancedBytecode = doEnhancement( javaClassFile, className, enhancer );
		if ( enhancedBytecode != null ) {
			writeOutEnhancedClass( enhancedBytecode, javaClassFile, project.getLogger() );
			project.getLogger().info( "Successfully enhanced class : " + className );
		}
		else {
			project.getLogger().info( "Skipping class : " + className );
		}
	}

	private static byte[] doEnhancement(File javaClassFile, String className, Enhancer enhancer) {
		try {
			return enhancer.enhance( className, Files.readAllBytes( javaClassFile.toPath() ) );
		}
		catch (Exception e) {
			throw new GradleException( "Unable to enhance class : " + className, e );
		}
	}

	private static Enhancer generateEnhancer(Directory classesDir, EnhancementSpec enhancementDsl) {
		final ClassLoader classLoader = Helper.toClassLoader( classesDir );

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

		//noinspection deprecation
		return Environment.getBytecodeProvider().getEnhancer( enhancementContext );
	}

	private static void writeOutEnhancedClass(byte[] enhancedBytecode, File file, Logger logger) {
		try {
			if ( file.delete() ) {
				if ( !file.createNewFile() ) {
					logger.error( "Unable to recreate class file : " + file.getAbsolutePath() );
				}
			}
			else {
				logger.error( "Unable to delete class file : " + file.getAbsolutePath() );
			}
		}
		catch (IOException e) {
			logger.warn( "Problem preparing class file for writing out enhancements [" + file.getAbsolutePath() + "]" );
		}

		try {
			Files.write( file.toPath(), enhancedBytecode );
		}
		catch (FileNotFoundException e) {
			throw new GradleException( "Error opening class file for writing : " + file.getAbsolutePath(), e );
		}
		catch (IOException e) {
			throw new GradleException( "Error writing enhanced class to file [" + file.getAbsolutePath() + "]", e );
		}
	}

	private EnhancementHelper() {
	}
}
