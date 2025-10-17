/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.hibernate.build.OrmBuildDetails;

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

		final var indexManager = project.getExtensions().create( "indexManager", IndexManager.class );
		indexManager.getArtifactsToProcess().from( artifactsToProcess );

		final var details = project.getExtensions().getByType( OrmBuildDetails.class );
		project.getTasks().withType( AbstractJandexAwareTask.class )
				.configureEach(task -> {
					task.getOrmBuildDetails().set( details );
					task.getIndexManager().set( indexManager );
				});

		final var indexerTask = project.getTasks().register(
				"buildAggregatedIndex",
				IndexerTask.class,
				task -> task.getIndexManager().set( indexManager )
		);

		final var incubatingTask = project.getTasks().register(
				"generateIncubationReport",
				IncubationReportTask.class,
				(task) -> task.dependsOn( indexerTask )
		);

		final var deprecationTask = project.getTasks().register(
				"generateDeprecationReport",
				DeprecationReportTask.class,
				(task) -> task.dependsOn( indexerTask )
		);

		final var internalsTask = project.getTasks().register(
				"generateInternalsReport",
				InternalsReportTask.class,
				(task) -> task.dependsOn( indexerTask )
		);

		final var loggingTask = project.getTasks().register(
				"generateLoggingReport",
				LoggingReportTask.class,
				(task) -> task.dependsOn( indexerTask )
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

		project.getTasks().register( "generateReports", groupingTask -> {
			groupingTask.setGroup( TASK_GROUP_NAME );
			groupingTask.dependsOn(
					indexerTask,
					incubatingTask,
					deprecationTask,
					internalsTask,
					loggingTask,
					dialectTableTask,
					communityDialectTableTask
			);
		} );
	}
}
