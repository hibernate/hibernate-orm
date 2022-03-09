/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.post;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Steve Ebersole
 */
public abstract class LoggingReportTask extends AbstractJandexAwareTask {
	@Inject
	public LoggingReportTask(IndexManager indexManager, Project project) {
		super(
				indexManager,
				project.getLayout().getBuildDirectory().file( "reports/orm/logging.txt" )
		);
	}

	@TaskAction
	public void generateLoggingReport() {
		getProject().getLogger().lifecycle( "Logging report not implemented yet" );
	}
}
