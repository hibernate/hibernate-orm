/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.post;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Comparator;
import java.util.TreeSet;
import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import org.jboss.jandex.DotName;

/**
 * @author Steve Ebersole
 */
public class DeprecationReportTask extends AbstractJandexAwareTask {
	public static final String REMOVE_ANN_NAME = "org.hibernate.Remove";
	public static final String DEPRECATED_ANN_NAME = Deprecated.class.getName();

	@Inject
	public DeprecationReportTask(IndexManager indexManager, Project project) {
		super(
				indexManager,
				project.getLayout().getBuildDirectory().file( "orm/reports/deprecating.txt" )
		);
		setDescription( "Generates a report for things considered deprecating" );
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
