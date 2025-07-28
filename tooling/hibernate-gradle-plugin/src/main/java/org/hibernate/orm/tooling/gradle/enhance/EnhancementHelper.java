/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle.enhance;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.logging.Logger;

import org.gradle.api.logging.Logging;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.bytecode.enhance.spi.UnloadedField;
import org.hibernate.orm.tooling.gradle.HibernateOrmSpec;

import static org.hibernate.bytecode.internal.BytecodeProviderInitiator.buildDefaultBytecodeProvider;
import static org.hibernate.orm.tooling.gradle.Helper.determineClassName;

/**
 * @author Steve Ebersole
 */
public class EnhancementHelper {
	private static final Logger logger = Logging.getLogger( EnhancementHelper.class);

	public static void enhance(
			DirectoryProperty classesDirectoryProperty,
			ClassLoader classLoader,
			HibernateOrmSpec ormDsl) {
		final Directory classesDirectory = classesDirectoryProperty.get();
		final File classesDir = classesDirectory.getAsFile();

		final EnhancementSpec enhancementDsl = ormDsl.getEnhancement().get();

		List<String> classesToEnhance = enhancementDsl.getClassNames().get();

		if ( !enhancementDsl.getEnableLazyInitialization().get() ) {
			logger.warn( "The 'enableLazyInitialization' configuration is deprecated and will be removed. Set the value to 'true' to get rid of this warning" );
		}
		if ( !enhancementDsl.getEnableDirtyTracking().get() ) {
			logger.warn( "The 'enableDirtyTracking' configuration is deprecated and will be removed. Set the value to 'true' to get rid of this warning" );
		}
		if ( enhancementDsl.getEnableExtendedEnhancement().get() ) {
			logger.warn("Extended enhancement is deprecated and will be removed. Set the value to 'false' to get rid of this warning" );
		}
		final Enhancer enhancer = generateEnhancer( classLoader, enhancementDsl );

		discoverTypes( classesDir, classesDir, enhancer, ormDsl.getFileOperations() );
		doEnhancement( classesDir, classesDir, enhancer, ormDsl.getFileOperations(), classesToEnhance );
	}

	private static void discoverTypes(File classesDir, File dir, Enhancer enhancer, FileOperations fileOperations) {
		for ( File subLocation : dir.listFiles() ) {
			if ( subLocation.isDirectory() ) {
				discoverTypes( classesDir, subLocation, enhancer, fileOperations );
			}
			else if ( subLocation.isFile() && subLocation.getName().endsWith( ".class" ) ) {
				final String className = determineClassName( classesDir, subLocation );
				final long lastModified = subLocation.lastModified();

				discoverTypes( subLocation, className, enhancer);

				final boolean timestampReset = subLocation.setLastModified( lastModified );
				if ( !timestampReset ) {
					logger.debug( "`{}`.setLastModified failed", fileOperations.relativePath( subLocation ) );
				}

			}
		}
	}

	private static void doEnhancement(File classesDir, File dir, Enhancer enhancer, FileOperations fileOperations, List<String> classesToEnhance) {
		for ( File subLocation : dir.listFiles() ) {
			if ( subLocation.isDirectory() ) {
				doEnhancement( classesDir, subLocation, enhancer, fileOperations, classesToEnhance );
			}
			else if ( subLocation.isFile() && subLocation.getName().endsWith( ".class" ) ) {
				final String className = determineClassName( classesDir, subLocation );
				final long lastModified = subLocation.lastModified();

				if(! (classesToEnhance.size()==0 || classesToEnhance.contains(className))) {
					continue;
				}

				enhance( subLocation, className, enhancer );

				final boolean timestampReset = subLocation.setLastModified( lastModified );
				if ( !timestampReset ) {
					logger.debug( "`{}`.setLastModified failed", fileOperations.relativePath( subLocation ) );
				}

			}
		}
	}

	private static void discoverTypes(
			File javaClassFile,
			String className,
			Enhancer enhancer) {
		try {
			enhancer.discoverTypes( className, Files.readAllBytes( javaClassFile.toPath() ) );
			logger.info( "Successfully discovered types for class : " + className );
		}
		catch (Exception e) {
			throw new GradleException( "Unable to discover types for class : " + className, e );
		}
	}

	private static void enhance(
			File javaClassFile,
			String className,
			Enhancer enhancer) {
		final byte[] enhancedBytecode = doEnhancement( javaClassFile, className, enhancer );
		if ( enhancedBytecode != null ) {
			writeOutEnhancedClass( enhancedBytecode, javaClassFile );
			logger.info( "Successfully enhanced class : " + className );
		}
		else {
			logger.info( "Skipping class : " + className );
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

	public static Enhancer generateEnhancer(ClassLoader classLoader, EnhancementSpec enhancementDsl) {

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

		//TODO allow the Gradle plugin to configure the bytecode enhancer?
		return buildDefaultBytecodeProvider().getEnhancer( enhancementContext );
	}

	private static void writeOutEnhancedClass(byte[] enhancedBytecode, File file) {
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
