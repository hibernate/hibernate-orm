/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Comparator;
import java.util.TreeSet;

import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;

import org.jboss.jandex.DotName;


/**
 * @author Steve Ebersole
 */
public abstract class InternalsReportTask extends AbstractJandexAwareTask {
	public static final String INTERNAL_ANN_NAME = "org.hibernate.Internal";

	private final Property<RegularFile> reportFile;

	public InternalsReportTask() {
		setDescription( "Generates a report of things consider internal" );
		reportFile = getProject().getObjects().fileProperty();
		reportFile.convention( getProject().getLayout().getBuildDirectory().file( "orm/reports/internal.txt" ) );
	}

	@Override
	protected Provider<RegularFile> getTaskReportFileReference() {
		return reportFile;
	}

	@TaskAction
	public void generateInternalsReport() {
		final TreeSet<Inclusion> internals = new TreeSet<>( Comparator.comparing( Inclusion::getPath ) );
		internals.addAll( getIndexManager().getInternalPackageNames() );
		processAnnotations( DotName.createSimple( INTERNAL_ANN_NAME ), internals );

		writeReport( internals );
	}

	@Override
	protected void writeReportHeader(OutputStreamWriter fileWriter) {
		super.writeReportHeader( fileWriter );

		try {
			fileWriter.write( "# All API elements considered internal for Hibernate's own use" );
			fileWriter.write( '\n' );
			fileWriter.write( '\n' );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

}
