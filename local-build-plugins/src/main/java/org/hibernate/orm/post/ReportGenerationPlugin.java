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
	public static final String TASK_GROUP_NAME = "hibernate-reports";
	public static final String AGGREGATE_CONFIG_NAME = "reportAggregation";
	public static final String DIALECT_CONFIG_NAME = "dialectReportSources";
	public static final String COMMUNITY_DIALECT_CONFIG_NAME = "communityDialectReportSources";

	@Override
	public void apply(Project project) {
		final Configuration artifactsToProcess = project.getConfigurations()
				.maybeCreate( AGGREGATE_CONFIG_NAME )
				.setDescription( "Used to collect the jars with classes files to be used in the aggregation reports for `@Internal`, `@Incubating`, etc" );

		final var indexManager = new IndexManager( artifactsToProcess, project );
		project.getExtensions().add( "indexManager", indexManager );
		project.getExtensions().add( "classificationMetadataManager", new ClassificationMetadataManager() );

		final var indexerTask = project.getTasks().register(
				"buildAggregatedIndex",
				IndexerTask.class
		);

		final var classificationMetadataTask = project.getTasks().register(
				"generateClassificationMetadata",
				ClassificationMetadataTask.class,
				(task) -> task.dependsOn( indexerTask )
		);

		final var classificationReportsTask = project.getTasks().register(
				"generateClassificationReports",
				ClassificationReportsTask.class,
				(task) -> task.dependsOn( classificationMetadataTask )
		);

		final var dialectExtensionInventoryTask = project.getTasks().register(
				"generateDialectExtensionInventory",
				DialectExtensionInventoryTask.class,
				(task) -> {
					task.dependsOn( indexerTask );
					task.getIndexedArtifacts().from( artifactsToProcess );
				}
		);
		registerReportAlias(
				project,
				"generateSpiReport",
				"Generates the Hibernate provider SPI report",
				classificationReportsTask
		);
		registerReportAlias(
				project,
				"generateIncubationReport",
				"Generates the report for things considered incubating",
				classificationReportsTask
		);
		registerReportAlias(
				project,
				"generateDeprecationReport",
				"Generates the report for things considered deprecated",
				classificationReportsTask
		);
		registerReportAlias(
				project,
				"generateRemovalReport",
				"Generates the report for things scheduled for removal",
				classificationReportsTask
		);
		registerReportAlias(
				project,
				"generateInternalsReport",
				"Generates the report of things considered internal",
				classificationReportsTask
		);

		final var loggingTask = project.getTasks().register(
				"generateLoggingReport",
				LoggingReportTask.class,
				(task) -> task.dependsOn( indexerTask )
		);

		final var classificationValidationTask = project.getTasks().register(
				"validateClassifications",
				ClassificationValidationTask.class,
				(task) -> task.dependsOn( classificationMetadataTask )
		);

		project.getTasks().register(
				"validateSpi",
				SpiValidationTask.class,
				(task) -> {
					task.dependsOn( classificationMetadataTask );
					task.dependsOn( classificationValidationTask );
				}
		);

		final var dialectConfig = project.getConfigurations()
				.maybeCreate( DIALECT_CONFIG_NAME )
				.setDescription( "Used to define classpath for performing reflection on Dialects for the Dialect report" );
		var dialectTableTask = project.getTasks().register(
				"generateDialectTableReport",
				DialectReportTask.class,
				(task) -> {
					task.dependsOn( indexerTask );
					task.getDialectReportSources().from( dialectConfig );
					task.getSourcePackage().set( "org.hibernate.dialect" );
					task.getReportFile().set( project.getLayout().getBuildDirectory().file( "orm/generated/dialect/dialect-table.adoc" ) );
				}
		);

		final var communityDialectConfig = project.getConfigurations()
				.maybeCreate( COMMUNITY_DIALECT_CONFIG_NAME )
				.setDescription( "Used to define classpath for performing reflection on Dialects for the Community Dialect report" );
		var communityDialectTableTask = project.getTasks().register(
				"generateCommunityDialectTableReport",
				DialectReportTask.class,
				(task) -> {
					task.dependsOn( indexerTask );
					task.getDialectReportSources().from( communityDialectConfig );
					task.getSourcePackage().set( "org.hibernate.community.dialect" );
					task.getReportFile().set( project.getLayout().getBuildDirectory().file( "orm/generated/dialect/dialect-table-community.adoc" ) );
				}
		);

		dialectExtensionInventoryTask.configure( (task) -> {
			task.dependsOn( dialectTableTask, communityDialectTableTask );
			task.getSupportDocumentationFiles().from( dialectTableTask.flatMap( DialectReportTask::getReportFile ) );
			task.getSupportDocumentationFiles().from( communityDialectTableTask.flatMap( DialectReportTask::getReportFile ) );
		} );

		final var groupingTask = project.getTasks().maybeCreate( "generateReports" );
		groupingTask.setGroup( TASK_GROUP_NAME );
		groupingTask.dependsOn( indexerTask );
		groupingTask.dependsOn( classificationMetadataTask );
		groupingTask.dependsOn( classificationReportsTask );
		groupingTask.dependsOn( loggingTask );
		groupingTask.dependsOn( dialectTableTask );
		groupingTask.dependsOn( communityDialectTableTask );
	}

	private static TaskProvider<Task> registerReportAlias(
			Project project,
			String name,
			String description,
			TaskProvider<ClassificationReportsTask> classificationReportsTask) {
		return project.getTasks().register(
				name,
				(task) -> {
					task.setGroup( TASK_GROUP_NAME );
					task.setDescription( description );
					task.dependsOn( classificationReportsTask );
				}
		);
	}
}
