/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.post;

import java.util.Comparator;
import java.util.TreeSet;
import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import org.jboss.jandex.DotName;

/**
 * @author Steve Ebersole
 */
public abstract class IncubationReportTask extends AbstractJandexAwareTask {
	public static final String INCUBATING_ANN_NAME = "org.hibernate.Incubating";

	@Inject
	public IncubationReportTask(IndexManager indexManager, Project project) {
		super(
				indexManager,
				project.getLayout().getBuildDirectory().file( "reports/orm/incubating.txt" )
		);
		setDescription( "Generates a report for things considered incubating" );
	}

	@TaskAction
	public void generateIncubationReport() {
		final TreeSet<Inclusion> incubations = new TreeSet<>( Comparator.comparing( Inclusion::getPath ) );

		processAnnotations( DotName.createSimple( INCUBATING_ANN_NAME ), incubations );

		// NOTE : at this point, `incubations` contains a set of incubating
		// names ordered alphabetically..

		writeReport( incubations );
	}

}
