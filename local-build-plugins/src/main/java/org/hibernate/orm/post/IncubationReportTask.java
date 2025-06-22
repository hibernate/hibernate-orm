/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Comparator;
import java.util.TreeSet;
import javax.inject.Inject;

import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;

import org.jboss.jandex.DotName;


/**
 * @author Steve Ebersole
 */
public abstract class IncubationReportTask extends AbstractJandexAwareTask {
	public static final String INCUBATING_ANN_NAME = "org.hibernate.Incubating";

	private final Property<RegularFile> reportFile;

	@Inject
	public IncubationReportTask() {
		setDescription( "Generates a report for things considered incubating" );
		reportFile = getProject().getObjects().fileProperty();
		reportFile.convention( getProject().getLayout().getBuildDirectory().file( "orm/reports/incubating.txt" ) );
	}

	@Override
	protected Provider<RegularFile> getTaskReportFileReference() {
		return reportFile;
	}

	@TaskAction
	public void generateIncubationReport() {
		final TreeSet<Inclusion> incubations = new TreeSet<>( Comparator.comparing( Inclusion::getPath ) );

		processAnnotations( DotName.createSimple( INCUBATING_ANN_NAME ), incubations );

		// NOTE : at this point, `incubations` contains a set of incubating
		// names ordered alphabetically..

		writeReport( incubations );
	}

	@Override
	protected void writeReportHeader(OutputStreamWriter fileWriter) {
		super.writeReportHeader( fileWriter );

		try {
			fileWriter.write( "# All API elements considered incubating" );
			fileWriter.write( '\n' );
			fileWriter.write( '\n' );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}
}
