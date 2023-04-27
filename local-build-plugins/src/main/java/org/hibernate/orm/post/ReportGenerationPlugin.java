/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.post;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
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

		final Task groupingTask = project.getTasks().maybeCreate( "generateHibernateReports" );
		groupingTask.setGroup( TASK_GROUP_NAME );

		final IndexManager indexManager = new IndexManager( artifactsToProcess, project );
		final IndexerTask indexerTask = project.getTasks().create(
				"buildAggregatedIndex",
				IndexerTask.class,
				indexManager
		);
		groupingTask.dependsOn( indexerTask );

		final IncubationReportTask incubatingTask = project.getTasks().create(
				"generateIncubationReport",
				IncubationReportTask.class,
				indexManager
		);
		incubatingTask.dependsOn( indexerTask );
		groupingTask.dependsOn( incubatingTask );

		final DeprecationReportTask deprecationTask = project.getTasks().create(
				"generateDeprecationReport",
				DeprecationReportTask.class,
				indexManager
		);
		deprecationTask.dependsOn( indexerTask );
		groupingTask.dependsOn( deprecationTask );

		final InternalsReportTask internalsTask = project.getTasks().create(
				"generateInternalsReport",
				InternalsReportTask.class,
				indexManager
		);
		internalsTask.dependsOn( indexerTask );
		groupingTask.dependsOn( internalsTask );

		final LoggingReportTask loggingTask = project.getTasks().create(
				"generateLoggingReport",
				LoggingReportTask.class,
				indexManager
		);
		loggingTask.dependsOn( indexerTask );
		groupingTask.dependsOn( loggingTask );

		final DialectReportTask dialectTask = project.getTasks().create(
				"generateDialectReport",
				DialectReportTask.class,
				indexManager
		);
		dialectTask.dependsOn( indexerTask );
		groupingTask.dependsOn( dialectTask );
	}
}
