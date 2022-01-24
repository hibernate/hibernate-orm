/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.post;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;

/**
 * @author Steve Ebersole
 */
public abstract class IncubatingCollectorTask extends DefaultTask {
	public static final String INCUBATING_ANN_NAME = "org.hibernate.Incubating";

	private final IndexManager indexManager;
	private final Provider<RegularFile> reportFileReferenceAccess;

	@Inject
	public IncubatingCollectorTask(IndexManager indexManager, Project project) {
		this.indexManager = indexManager;
		this.reportFileReferenceAccess = project.getLayout().getBuildDirectory().file( "reports/orm/" + project.getName() + "-incubating.txt" );
	}

	@InputFile
	public Provider<RegularFile> getIndexFileReference() {
		return indexManager.getIndexFileReferenceAccess();
	}

	@OutputFile
	public Provider<RegularFile> getReportFileReferenceAccess() {
		return reportFileReferenceAccess;
	}

	@TaskAction
	public void collectIncubationDetails() {
		final Index index = indexManager.getIndex();

		final List<AnnotationInstance> usages = index.getAnnotations( DotName.createSimple( INCUBATING_ANN_NAME ) );
		final TreeSet<Incubation> incubations = new TreeSet<>( Comparator.comparing( Incubation::getPath ) );

		usages.forEach( (usage) -> {
			final AnnotationTarget usageLocation = usage.target();
			final Incubation incubation = determinePath( usageLocation );
			if ( incubation != null ) {
				incubations.add( incubation );
			}
		} );

		// NOTE : at this point, `usagePathSet` contains a set of incubating
		// names ordered alphabetically..

		reportIncubations( incubations );
	}

	private void reportIncubations(TreeSet<Incubation> incubations) {
		final File reportFile = prepareReportFile();
		assert reportFile.exists();

		try ( final OutputStreamWriter fileWriter = new OutputStreamWriter( new FileOutputStream( reportFile ) ) ) {
			generateReport( incubations, fileWriter );
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException( "Should never happen" );
		}
		catch (IOException e) {
			throw new RuntimeException( "Error writing to `@Incubating` report file", e );
		}
	}

	private File prepareReportFile() {
		final File reportFile = reportFileReferenceAccess.get().getAsFile();

		if ( reportFile.getParentFile().exists() ) {
			if ( reportFile.exists() ) {
				if ( !reportFile.delete() ) {
					throw new RuntimeException( "Unable to delete `@Incubating` report file" );
				}
			}
		}
		else {
			if ( !reportFile.getParentFile().mkdirs() ) {
				throw new RuntimeException( "Unable to create directories for `@Incubating` report file" );
			}
		}

		try {
			if ( !reportFile.createNewFile() ) {
				throw new RuntimeException( "Unable to create file for `@Incubating` report file" );
			}
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to create file for `@Incubating` report file" );
		}

		return reportFile;
	}

	private void generateReport(TreeSet<Incubation> incubations, OutputStreamWriter fileWriter) {
		String previousPath = null;
		for ( Incubation incubation : incubations ) {
			if ( previousPath != null && incubation.path.startsWith( previousPath ) ) {
				continue;
			}

			// `usagePath` is a path we want to document
			try {
				fileWriter.write( incubation.path );
				if ( incubation.isPackage ) {
					fileWriter.write( ".*" );
				}
				fileWriter.write( '\n' );
				fileWriter.flush();
			}
			catch (IOException e) {
				throw new RuntimeException( "Error writing entry (" + incubation.path + ") to `@Incubating` report file", e );
			}

			previousPath = incubation.path;
		}
	}

	private static class Incubation {
		private final String path;
		private final boolean isPackage;

		public Incubation(String path, boolean isPackage) {
			this.path = path;
			this.isPackage = isPackage;
		}

		public Incubation(String path) {
			this( path, false );
		}

		public String getPath() {
			return path;
		}

		public boolean isPackage() {
			return isPackage;
		}
	}

	private Incubation determinePath(AnnotationTarget usageLocation) {
		switch ( usageLocation.kind() ) {
			case CLASS: {
				final DotName name = usageLocation.asClass().name();
				if ( name.local().equals( "package-info" ) ) {
					return new Incubation( name.packagePrefix(), true );
				}
				return new Incubation( name.toString() );
			}
			case FIELD: {
				final FieldInfo fieldInfo = usageLocation.asField();
				final String path = fieldInfo.declaringClass().name().toString()
						+ "#"
						+ fieldInfo.name();
				return new Incubation( path );
			}
			case METHOD: {
				final MethodInfo methodInfo = usageLocation.asMethod();
				final String path = methodInfo.declaringClass().name().toString()
						+ "#"
						+ methodInfo.name();
				return new Incubation( path );
			}
			default: {
				return null;
			}
		}
	}
}
