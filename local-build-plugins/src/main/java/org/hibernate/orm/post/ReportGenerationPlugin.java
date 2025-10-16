/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskProvider;

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
		project.getExtensions().add( "indexManager", indexManager );

		final TaskProvider<IndexerTask> indexerTask = project.getTasks().register(
				"buildAggregatedIndex",
				IndexerTask.class
		);

		final TaskProvider<IncubationReportTask> incubatingTask = project.getTasks().register(
				"generateIncubationReport",
				IncubationReportTask.class,
				(task) -> task.dependsOn( indexerTask )
		);

		final TaskProvider<DeprecationReportTask> deprecationTask = project.getTasks().register(
				"generateDeprecationReport",
				DeprecationReportTask.class,
				(task) -> task.dependsOn( indexerTask )
		);

		final TaskProvider<InternalsReportTask> internalsTask = project.getTasks().register(
				"generateInternalsReport",
				InternalsReportTask.class,
				(task) -> task.dependsOn( indexerTask )
		);

		final TaskProvider<LoggingReportTask> loggingTask = project.getTasks().register(
				"generateLoggingReport",
				LoggingReportTask.class,
				(task) -> task.dependsOn( indexerTask )
		);

		final TaskProvider<DialectReportTask> dialectTableTask = project.getTasks().register(
				"generateDialectTableReport",
				DialectReportTask.class,
				(task) -> {
					task.dependsOn( indexerTask );
					task.setProperty( "sourceProject", "hibernate-core" );
					task.setProperty( "sourcePackage", "org.hibernate.dialect" );
					task.setProperty( "reportFile", project.getLayout().getBuildDirectory().file( "orm/generated/dialect/dialect-table.adoc" ) );
				}
		);

		final TaskProvider<DialectReportTask> communityDialectTableTask = project.getTasks().register(
				"generateCommunityDialectTableReport",
				DialectReportTask.class,
				(task) -> {
					task.dependsOn( indexerTask );
					task.setProperty( "sourceProject", "hibernate-community-dialects" );
					task.setProperty( "sourcePackage", "org.hibernate.community.dialect" );
					task.setProperty( "reportFile", project.getLayout().getBuildDirectory().file( "orm/generated/dialect/dialect-table-community.adoc" ) );
				}
		);

		final Task groupingTask = project.getTasks().maybeCreate( "generateReports" );
		groupingTask.setGroup( TASK_GROUP_NAME );
		groupingTask.dependsOn( indexerTask );
		groupingTask.dependsOn( incubatingTask );
		groupingTask.dependsOn( deprecationTask );
		groupingTask.dependsOn( internalsTask );
		groupingTask.dependsOn( loggingTask );
		groupingTask.dependsOn( dialectTableTask );
		groupingTask.dependsOn( communityDialectTableTask );
	}
}
