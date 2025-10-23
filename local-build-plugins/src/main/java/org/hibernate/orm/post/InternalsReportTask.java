/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;
import org.jboss.jandex.DotName;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Comparator;
import java.util.TreeSet;


/**
 * @author Steve Ebersole
 */
public abstract class InternalsReportTask extends AbstractJandexAwareTask {
	public static final String INTERNAL_ANN_NAME = "org.hibernate.Internal";

	private final Property<RegularFile> reportFile;

	@Inject
	public InternalsReportTask(ProjectLayout layout, ObjectFactory objects) {
		super( objects );
		setDescription( "Generates a report of things consider internal" );
		reportFile = objects.fileProperty();
		reportFile.convention( layout.getBuildDirectory().file( "orm/reports/internal.txt" ) );
	}

	@Override
	protected Provider<RegularFile> getTaskReportFileReference() {
		return reportFile;
	}

	@TaskAction
	public void generateInternalsReport() {
		final TreeSet<Inclusion> internals = new TreeSet<>( Comparator.comparing( Inclusion::getPath ) );
		internals.addAll( getIndexManager().get().getInternalPackageNames() );
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
