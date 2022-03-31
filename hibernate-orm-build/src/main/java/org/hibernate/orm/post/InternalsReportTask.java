/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.post;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.TreeSet;
import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import org.jboss.jandex.DotName;

/**
 * @author Steve Ebersole
 */
public abstract class InternalsReportTask extends AbstractJandexAwareTask {
	public static final String INTERNAL_ANN_NAME = "org.hibernate.Internal";

	@Inject
	public InternalsReportTask(IndexManager indexManager, Project project) {
		super(
				indexManager,
				project.getLayout().getBuildDirectory().file( "orm/reports/internal.txt" )
		);
	}

	@TaskAction
	public void generateInternalsReport() {
		final TreeSet<Inclusion> internals = new TreeSet<>( Comparator.comparing( Inclusion::getPath ) );
		internals.addAll( getIndexManager().getInternalPackageNames() );
		processAnnotations( DotName.createSimple( INTERNAL_ANN_NAME ), internals );

		writeReport( internals );
	}

}
