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
import java.util.List;
import java.util.TreeSet;
import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;

import static org.hibernate.orm.post.ReportGenerationPlugin.TASK_GROUP_NAME;

/**
 * @author Steve Ebersole
 */
public class AbstractJandexAwareTask extends DefaultTask {
	private final IndexManager indexManager;
	private final Provider<RegularFile> reportFileReferenceAccess;

	@Inject
	public AbstractJandexAwareTask(IndexManager indexManager, Provider<RegularFile> reportFileReferenceAccess) {
		this.indexManager = indexManager;
		this.reportFileReferenceAccess = reportFileReferenceAccess;
		setGroup( TASK_GROUP_NAME );
	}

	@Internal
	protected IndexManager getIndexManager() {
		return indexManager;
	}

	@InputFile
	public Provider<RegularFile> getIndexFileReference() {
		return indexManager.getIndexFileReferenceAccess();
	}

	@OutputFile
	public Provider<RegularFile> getReportFileReferenceAccess() {
		return reportFileReferenceAccess;
	}

	protected File prepareReportFile() {
		final File reportFile = getReportFileReferenceAccess().get().getAsFile();

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
		final Index index = getIndexManager().getIndex();
		final List<AnnotationInstance> usages = index.getAnnotations( annotationName );

		usages.forEach( (usage) -> {
			final AnnotationTarget usageLocation = usage.target();
			final Inclusion inclusion = determinePath( usageLocation );
			if ( inclusion != null ) {
				inclusions.add( inclusion );
			}
		} );
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

	private void writeReport(TreeSet<Inclusion> inclusions, OutputStreamWriter fileWriter) {
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
