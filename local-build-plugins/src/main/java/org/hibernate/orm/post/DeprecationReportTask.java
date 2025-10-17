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
public abstract class DeprecationReportTask extends AbstractJandexAwareTask {
	public static final String REMOVE_ANN_NAME = "org.hibernate.Remove";
	public static final String DEPRECATED_ANN_NAME = Deprecated.class.getName();

	private final Property<RegularFile> reportFile;

	@Inject
	public DeprecationReportTask(ProjectLayout layout, ObjectFactory objects) {
		super( objects );
		setDescription( "Generates a report for things considered deprecated" );
		reportFile = objects.fileProperty();
		reportFile.convention( layout.getBuildDirectory().file( "orm/reports/deprecated.txt" ) );
	}

	@Override
	protected Provider<RegularFile> getTaskReportFileReference() {
		return reportFile;
	}

	@TaskAction
	public void generateIncubationReport() {
		final TreeSet<Inclusion> deprecations = new TreeSet<>( Comparator.comparing( Inclusion::getPath ) );

		processAnnotations(
				deprecations::add,
				DotName.createSimple( REMOVE_ANN_NAME ),
				DotName.createSimple( DEPRECATED_ANN_NAME )
		);

		// NOTE : at this point, `deprecations` contains a set of deprecation
		// names ordered alphabetically..

		writeReport( deprecations );
	}

	@Override
	protected void writeReportHeader(OutputStreamWriter fileWriter) {
		super.writeReportHeader( fileWriter );

		try {
			fileWriter.write( "# All API elements considered deprecated - union of @Deprecated and @Remove" );
			fileWriter.write( '\n' );
			fileWriter.write( '\n' );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}
}
