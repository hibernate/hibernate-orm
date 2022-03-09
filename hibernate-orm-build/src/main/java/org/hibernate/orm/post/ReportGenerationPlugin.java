/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.post;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

/**
 * @author Steve Ebersole
 */
public class ReportGenerationPlugin implements Plugin<Project> {
	public static final String CONFIG_NAME = "reportAggregation";
	public static final String TASK_GROUP_NAME = "hibernate-reports";

	@Override
	public void apply(Project project) {
		final Configuration artifactsToProcess = project.getConfigurations()
				.maybeCreate( CONFIG_NAME )
				.setDescription( "Used to collect the jars with classes files to be used in the aggregation reports for `@Internal`, `@Incubating`, etc" );

		final IndexManager indexManager = new IndexManager( artifactsToProcess, project );
		final IndexerTask indexerTask = project.getTasks().create(
				"buildAggregatedIndex",
				IndexerTask.class,
				indexManager
		);

		final IncubationReportTask incubatingTask = project.getTasks().create(
				"createIncubationReport",
				IncubationReportTask.class,
				indexManager
		);
		incubatingTask.dependsOn( indexerTask );

		final InternalsReportTask internalsTask = project.getTasks().create(
				"createInternalsReport",
				InternalsReportTask.class,
				indexManager
		);
		internalsTask.dependsOn( indexerTask );

		final LoggingReportTask loggingTask = project.getTasks().create(
				"createLoggingReport",
				LoggingReportTask.class,
				indexManager
		);
		loggingTask.dependsOn( indexerTask );
	}
}
