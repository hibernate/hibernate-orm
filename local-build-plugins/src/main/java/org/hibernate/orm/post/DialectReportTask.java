/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;

import org.hibernate.build.HibernateVersion;
import org.hibernate.build.OrmBuildDetails;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;

/**
 * Generates a report on Dialect information
 *
 * @author Steve Ebersole
 */
public abstract class DialectReportTask extends AbstractJandexAwareTask {
	private final Property<RegularFile> reportFile;
	private final Property<Boolean> generateHeading;

	public DialectReportTask() {
		setDescription( "Generates a report of the supported Dialects" );
		reportFile = getProject().getObjects().fileProperty();
		reportFile.convention( getProject().getLayout().getBuildDirectory().file( "orm/generated/dialect/dialect.adoc" ) );
		generateHeading = getProject().getObjects().property( Boolean.class ).convention( true );
	}

	@OutputFile
	public Property<RegularFile> getReportFile() {
		return reportFile;
	}

	@Input
	public Property<Boolean> getGenerateHeading() {
		return generateHeading;
	}

	@Override
	protected Provider<RegularFile> getTaskReportFileReference() {
		return reportFile;
	}

	@TaskAction
	public void generateDialectReport() {
		// the ones we want are all in the hibernate-core project
		final Project coreProject = getProject().getRootProject().project( "hibernate-core" );
		final SourceSetContainer sourceSets = coreProject.getExtensions().getByType( SourceSetContainer.class );
		final SourceSet sourceSet = sourceSets.getByName( SourceSet.MAIN_SOURCE_SET_NAME );
		final ClassLoader classLoader = Helper.asClassLoader( sourceSet, coreProject.getConfigurations().getByName( "testRuntimeClasspath" ) );

		final DialectClassDelegate dialectClassDelegate = new DialectClassDelegate( classLoader );

		final Index index = getIndexManager().getIndex();
		final Collection<ClassInfo> allDialectClasses = index.getAllKnownSubclasses( DialectClassDelegate.DIALECT_CLASS_NAME );
		if ( allDialectClasses.isEmpty() ) {
			throw new RuntimeException( "Unable to find Dialects" );
		}

		final List<DialectDelegate> dialectDelegates = collectDialectInfo( dialectClassDelegate, allDialectClasses );
		generateReport( dialectDelegates );
	}

	private List<DialectDelegate> collectDialectInfo(
			DialectClassDelegate dialectClassDelegate,
			Collection<ClassInfo> allDialectClasses) {
		final List<DialectDelegate> results = new ArrayList<>();

		allDialectClasses.forEach( (dialectImplClassInfo) -> {
			final String dialectImplClassName = dialectImplClassInfo.name().toString();
			final DialectDelegate dialectDelegate = dialectClassDelegate.createDialectDelegate( dialectImplClassName );
			if ( dialectDelegate == null ) {
				return;
			}
			results.add( dialectDelegate );
		} );

		results.sort( Comparator.comparing( DialectDelegate::getSimpleName ) );
		return results;
	}

	private void generateReport(List<DialectDelegate> dialectDelegates) {
		final File reportFile = prepareReportFile();
		try ( final OutputStreamWriter fileWriter = new OutputStreamWriter( new FileOutputStream( reportFile ) ) ) {
			writeDialectReport( dialectDelegates, fileWriter );
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException( "Should never happen" );
		}
		catch (IOException e) {
			throw new RuntimeException( "Error writing to report file", e );
		}
	}

	private void writeDialectReport(
			List<DialectDelegate> dialectDelegates,
			OutputStreamWriter fileWriter) {
		writeDialectReportHeader( fileWriter );

		dialectDelegates.forEach( (dialectDelegate) -> writeDialectReportEntry( dialectDelegate, fileWriter ) );

		writeDialectReportFooter( fileWriter );

	}

	private void writeDialectReportHeader(OutputStreamWriter fileWriter) {
		try {
			if ( this.generateHeading.get() ) {
				fileWriter.write( "= Supported Dialects\n\n" );
				fileWriter.write(
						"Supported Dialects along with the minimum supported version of the underlying database.\n\n\n" );

				HibernateVersion ormVersion = getProject().getExtensions().getByType( OrmBuildDetails.class ).getHibernateVersion();
				fileWriter.write( "NOTE: Hibernate version " + ormVersion.getFamily() + "\n\n" );
			}

			fileWriter.write( "[cols=\"a,a\", options=\"header\"]\n" );
			fileWriter.write( "|===\n" );
			fileWriter.write( "|Dialect |Minimum Database Version\n" );
		}
		catch (Exception e) {
			throw new RuntimeException( "Error writing report header", e );
		}
	}

	private void writeDialectReportEntry(DialectDelegate dialectDelegate, OutputStreamWriter fileWriter) {
		try {
			final String version = dialectDelegate.getMinimumVersion();
			fileWriter.write( '|' );
			fileWriter.write( dialectDelegate.getDialectImplClass().getSimpleName() );
			fileWriter.write( '|' );
			fileWriter.write( version );
			fileWriter.write( "\n" );
		}
		catch (Exception e) {
			throw new RuntimeException( "Unable to access Dialect : " + dialectDelegate.getDialectReference(), e );
		}
	}

	private void writeDialectReportFooter(OutputStreamWriter fileWriter) {
		try {
			fileWriter.write( "|===\n" );
		}
		catch (Exception e) {
			throw new RuntimeException( "Error writing report footer", e );
		}
	}

	private class DialectClassDelegate {
		public static final String DIALECT_CLASS_NAME = "org.hibernate.dialect.Dialect";
		public static final String MIN_VERSION_METHOD_NAME = "getMinimumSupportedVersion";

		private final Class<?> loadedDialectClass;
		private final Method versionMethod;
		private final ClassLoader classLoader;

		public DialectClassDelegate(ClassLoader classLoader) {
			this.classLoader = classLoader;
			try {
				loadedDialectClass = classLoader.loadClass( DIALECT_CLASS_NAME );
				versionMethod = loadedDialectClass.getDeclaredMethod( MIN_VERSION_METHOD_NAME );
				versionMethod.setAccessible( true );
			}
			catch (ClassNotFoundException e) {
				throw new RuntimeException( "Could not load " + DIALECT_CLASS_NAME, e );
			}
			catch (NoSuchMethodException e) {
				throw new RuntimeException( "Could not locate method " + MIN_VERSION_METHOD_NAME, e );
			}
		}

		public DialectDelegate createDialectDelegate(String dialectImplClassName) {
			try {
				final Class<?> dialectImplClass;
				try {
					dialectImplClass = classLoader.loadClass( dialectImplClassName );
				}
				catch (Exception e) {
					// assume it is from project other than hibernate-core
					getLogger().debug( "Skipping Dialect " + dialectImplClassName + " - could not instantiate", e );
					return null;
				}

				if ( Modifier.isAbstract( dialectImplClass.getModifiers() ) ) {
					getLogger().debug( "Skipping Dialect " + dialectImplClassName + " - abstract" );
					return null;
				}

				if ( dialectImplClass.isAnnotationPresent( Deprecated.class ) ) {
					getLogger().debug( "Skipping Dialect " + dialectImplClassName + " - deprecated" );
					return null;
				}

				return DialectDelegate.from( dialectImplClass, this );
			}
			catch (Exception e) {
				throw new RuntimeException( "Unable to access Dialect class : " + dialectImplClassName, e );
			}
		}

		public Class<?> getLoadedDialectClass() {
			return loadedDialectClass;
		}

		public Method getVersionMethod() {
			return versionMethod;
		}
	}

	private static class DialectDelegate {
		private final Class<?> dialectImplClass;
		private final DialectClassDelegate dialectClassDelegate;

		private final Object dialectRef;

		public static DialectDelegate from(Class<?> dialectImplClass, DialectClassDelegate dialectClassDelegate) {
			return new DialectDelegate( dialectImplClass, dialectClassDelegate );
		}

		public DialectDelegate(Class<?> dialectImplClass, DialectClassDelegate dialectClassDelegate) {
			this.dialectImplClass = dialectImplClass;
			this.dialectClassDelegate = dialectClassDelegate;
			try {
				this.dialectRef = dialectImplClass.getConstructor().newInstance();
			}
			catch (Exception e) {
				throw new RuntimeException( "Unable to create DialectDelegate for " + dialectImplClass.getName(), e );
			}
		}

		public String getSimpleName() {
			return dialectImplClass.getSimpleName();
		}

		public DialectClassDelegate getDialectClassDelegate() {
			return dialectClassDelegate;
		}

		public Class<?> getDialectImplClass() {
			return dialectImplClass;
		}

		public Object getDialectReference() {
			return dialectRef;
		}

		public String getMinimumVersion() {
			try {
				final Object versionRef = dialectClassDelegate.getVersionMethod().invoke( dialectRef );
				return versionRef.toString();
			}
			catch (Exception e) {
				throw new RuntimeException( "Unable to access " + DialectClassDelegate.MIN_VERSION_METHOD_NAME + " for " + dialectClassDelegate.loadedDialectClass.getName(), e );
			}
		}
	}
}
