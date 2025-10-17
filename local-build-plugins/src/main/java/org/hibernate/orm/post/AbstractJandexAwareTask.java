/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputFile;
import org.hibernate.build.OrmBuildDetails;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;

import static org.hibernate.orm.post.ReportGenerationPlugin.TASK_GROUP_NAME;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractJandexAwareTask extends DefaultTask {
	private final Property<OrmBuildDetails> ormBuildDetails;

	@Inject
	public AbstractJandexAwareTask(ObjectFactory objects) {
		setGroup( TASK_GROUP_NAME );
		ormBuildDetails = objects.property( OrmBuildDetails.class );

		getInputs().property( "version", ormBuildDetails.map( OrmBuildDetails::getHibernateVersion ) );
	}

	@Internal
	protected abstract Provider<RegularFile> getTaskReportFileReference();

	@Nested
	protected abstract Property<IndexManager> getIndexManager();

	@Nested
	public Property<OrmBuildDetails> getOrmBuildDetails() {
		return ormBuildDetails;
	}

	@InputFile
	public Provider<RegularFile> getIndexFileReference() {
		return getIndexManager().get().getIndexFileReferenceAccess();
	}

	@OutputFile
	public Provider<RegularFile> getReportFileReference() {
		return getTaskReportFileReference();
	}

	protected File prepareReportFile() {
		final File reportFile = getReportFileReference().get().getAsFile();

		if ( reportFile.getParentFile().exists() ) {
			if ( reportFile.exists() ) {
				if ( !reportFile.delete() ) {
					throw new RuntimeException( "Unable to delete report file - " + reportFile.getAbsolutePath() );
				}
			}
		}
		else {
			if ( !reportFile.getParentFile().mkdirs() ) {
				throw new RuntimeException( "Unable to create report file directories - " + reportFile.getAbsolutePath() );
			}
		}

		try {
			if ( !reportFile.createNewFile() ) {
				throw new RuntimeException( "Unable to create report file - " + reportFile.getAbsolutePath() );
			}
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to create report file - " + reportFile.getAbsolutePath() );
		}

		return reportFile;
	}

	protected void processAnnotations(DotName annotationName, TreeSet<Inclusion> inclusions) {
		processAnnotations( inclusions::add, annotationName );
	}

	protected void processAnnotations(Consumer<Inclusion> inclusions, DotName... annotationNames) {
		final Index index = getIndexManager().get().getIndex();

		for ( int i = 0; i < annotationNames.length; i++ ) {
			final DotName annotationName = annotationNames[ i ];
			final List<AnnotationInstance> usages = index.getAnnotations( annotationName );

			usages.forEach( (usage) -> {
				final AnnotationTarget usageLocation = usage.target();
				final Inclusion inclusion = determinePath( usageLocation );
				if ( inclusion != null ) {
					inclusions.accept( inclusion );
				}
			} );
		}
	}

	protected void writeReport(TreeSet<Inclusion> inclusions) {
		final File reportFile = prepareReportFile();
		assert reportFile.exists();

		try ( final OutputStreamWriter fileWriter = new OutputStreamWriter( new FileOutputStream( reportFile ) ) ) {
			writeReport( inclusions, fileWriter );
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException( "Should never happen" );
		}
		catch (IOException e) {
			throw new RuntimeException( "Error writing to report file", e );
		}
	}

	protected void writeReportHeader(OutputStreamWriter fileWriter) {
		// by default, nothing to do
	}

	private void writeReport(TreeSet<Inclusion> inclusions, OutputStreamWriter fileWriter) {
		writeReportHeader( fileWriter );

		String previousPath = null;
		for ( Inclusion inclusion : inclusions ) {
			if ( previousPath != null && inclusion.getPath().startsWith( previousPath ) ) {
				continue;
			}

			// `inclusion` is a path we want to document
			try {
				fileWriter.write( inclusion.getPath() );
				if ( inclusion.isPackage() ) {
					fileWriter.write( ".*" );
				}
				fileWriter.write( '\n' );
				fileWriter.flush();
			}
			catch (IOException e) {
				throw new RuntimeException( "Error writing entry (" + inclusion.getPath() + ") to report file", e );
			}

			previousPath = inclusion.getPath();
		}
	}

	private Inclusion determinePath(AnnotationTarget usageLocation) {
		switch ( usageLocation.kind() ) {
			case CLASS: {
				final DotName name = usageLocation.asClass().name();
				if ( name.local().equals( "package-info" ) ) {
					return new Inclusion( name.packagePrefix(), true );
				}
				return new Inclusion( name.toString() );
			}
			case FIELD: {
				final FieldInfo fieldInfo = usageLocation.asField();
				final String path = fieldInfo.declaringClass().name().toString()
						+ "#"
						+ fieldInfo.name();
				return new Inclusion( path );
			}
			case METHOD: {
				final MethodInfo methodInfo = usageLocation.asMethod();
				final String path = methodInfo.declaringClass().name().toString()
						+ "#"
						+ methodInfo.name();
				return new Inclusion( path );
			}
			default: {
				return null;
			}
		}
	}
}
