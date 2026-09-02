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

import org.hibernate.build.OrmBuildDetails;

/**
 * @author Steve Ebersole
 */
public class ReportGenerationPlugin implements Plugin<Project> {
	public static final String TASK_GROUP_NAME = "hibernate-reports";
	public static final String AGGREGATE_CONFIG_NAME = "reportAggregation";
	public static final String MIGRATION_COMPATIBILITY_CONFIG_NAME = "migrationCompatibilityArtifacts";
	public static final String DIALECT_CONFIG_NAME = "dialectReportSources";
	public static final String COMMUNITY_DIALECT_CONFIG_NAME = "communityDialectReportSources";
	public static final String DIALECT_PROVIDER_VALIDATION_CONFIG_NAME = "dialectProviderValidationSources";
	public static final String DIALECT_PROVIDER_UPSTREAM_CONFIG_NAME = "dialectProviderValidationUpstream";
	public static final String DIALECT_PROVIDER_ENGINE_CONFIG_NAME = "dialectProviderValidationEngine";

	@Override
	public void apply(Project project) {
		final Configuration artifactsToProcess = project.getConfigurations()
				.maybeCreate( AGGREGATE_CONFIG_NAME )
				.setDescription( "Used to collect the jars with classes files to be used in the aggregation reports for `@Internal`, `@Incubating`, etc" );
		final Configuration migrationCompatibilityArtifacts = project.getConfigurations()
				.maybeCreate( MIGRATION_COMPATIBILITY_CONFIG_NAME )
				.setDescription( "Artifacts covered by API and SPI migration compatibility validation" );
		migrationCompatibilityArtifacts.setTransitive( false );

		final var indexManager = new IndexManager( artifactsToProcess, project );
		project.getExtensions().add( "indexManager", indexManager );
		project.getExtensions().add( "classificationMetadataManager", new ClassificationMetadataManager() );
		final MigrationCompatibilityExtension migrationCompatibility = project.getExtensions().create(
				"migrationCompatibility",
				MigrationCompatibilityExtension.class
		);
		migrationCompatibility.getClassificationMetadataBaseUrl().convention( "https://docs.hibernate.org/orm" );
		migrationCompatibility.getBootstrapBaseline().convention( false );
		final OrmBuildDetails buildDetails = project.getExtensions().findByType( OrmBuildDetails.class );
		if ( buildDetails != null ) {
			final String defaultBaseline = MigrationCompatibilityFamilies.defaultBaseline(
					buildDetails.getHibernateVersionName()
			);
			if ( defaultBaseline != null ) {
				migrationCompatibility.getBaselineFamily().convention( defaultBaseline );
				migrationCompatibility.getReviewFamily().convention( migrationCompatibility.getBaselineFamily() );
			}
		}

		final var indexerTask = project.getTasks().register(
				"buildAggregatedIndex",
				IndexerTask.class
		);

		final var classificationMetadataTask = project.getTasks().register(
				"generateClassificationMetadata",
				ClassificationMetadataTask.class,
				(task) -> {
					task.dependsOn( indexerTask );
					task.getMigrationCompatibilityArtifacts().from( migrationCompatibilityArtifacts );
				}
		);

		final var classificationReportsTask = project.getTasks().register(
				"generateClassificationReports",
				ClassificationReportsTask.class,
				(task) -> task.dependsOn( classificationMetadataTask )
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

		final Configuration dialectProviderValidationSources = project.getConfigurations()
				.maybeCreate( DIALECT_PROVIDER_VALIDATION_CONFIG_NAME )
				.setDescription( "Compiled external Dialect providers used for provider-boundary validation" );
		dialectProviderValidationSources.setTransitive( false );
		final Configuration dialectProviderValidationUpstream = project.getConfigurations()
				.maybeCreate( DIALECT_PROVIDER_UPSTREAM_CONFIG_NAME )
				.setDescription( "Upstream Hibernate artifacts used for provider-boundary validation" );
		dialectProviderValidationUpstream.setTransitive( false );
		final Configuration dialectProviderValidationEngine = project.getConfigurations()
				.maybeCreate( DIALECT_PROVIDER_ENGINE_CONFIG_NAME )
				.setDescription( "Tooling-owned provider-boundary validation engine" );

		final var classificationValidationTask = project.getTasks().register(
				"validateClassifications",
				ClassificationValidationTask.class,
				(task) -> {
					task.dependsOn( classificationMetadataTask );
					task.getProviderArtifacts().from( dialectProviderValidationSources );
				}
		);

		project.getTasks().register(
				"validateSpi",
				SpiValidationTask.class,
				(task) -> {
					task.dependsOn( classificationMetadataTask );
					task.dependsOn( classificationValidationTask );
				}
		);

		project.getTasks().register(
				"validateDialectProviderBoundaries",
				DialectProviderBoundaryValidationTask.class,
				(task) -> {
					task.dependsOn( classificationMetadataTask );
					task.getProviderArtifacts().from( dialectProviderValidationSources );
					task.getUpstreamArtifacts().from( dialectProviderValidationUpstream );
					task.getEngineClasspath().from( dialectProviderValidationEngine );
				}
		);

		final var baselineMetadataTask = project.getTasks().register(
				"resolveMigrationCompatibilityBaselineMetadata",
				ResolveClassificationMetadataTask.class,
				task -> {
					task.setGroup( TASK_GROUP_NAME );
					task.setDescription( "Resolves authenticated migration compatibility baseline metadata" );
					task.getFamily().set( migrationCompatibility.getBaselineFamily() );
					task.getClassificationMetadataBaseUrl().set( migrationCompatibility.getClassificationMetadataBaseUrl() );
					task.getClassificationMetadataFile().set( migrationCompatibility.getBaselineClassificationMetadataFile() );
					task.getOffline().set( project.getGradle().getStartParameter().isOffline() );
					task.getRefreshDependencies().set( project.getGradle().getStartParameter().isRefreshDependencies() );
					task.getAllowMissing().set( migrationCompatibility.getBootstrapBaseline() );
					task.getSharedCacheDirectory().set(
						project.file( new java.io.File( project.getGradle().getGradleUserHomeDir(), "caches/hibernate-orm/classifications" ) )
					);
					task.getResolvedMetadataFile().set(
						project.getLayout().getBuildDirectory().file(
								"orm/migration-compatibility/baseline/classifications.json.gz"
						)
					);
					task.onlyIf( ignored -> migrationCompatibility.getBaselineFamily().isPresent() );
				}
		);
		final var baselineArtifactsTask = project.getTasks().register(
				"resolveMigrationCompatibilityBaselineArtifacts",
				ResolveClassificationArtifactsTask.class,
				task -> {
					task.setGroup( TASK_GROUP_NAME );
					task.setDescription( "Resolves exact migration compatibility baseline artifacts" );
					task.dependsOn( baselineMetadataTask );
					task.getClassificationMetadataFile().set(
						baselineMetadataTask.flatMap( ResolveClassificationMetadataTask::getResolvedMetadataFile )
					);
					task.getArtifactsDirectory().set(
						project.getLayout().getBuildDirectory().dir( "orm/migration-compatibility/baseline/artifacts" )
					);
					task.onlyIf(
						ignored -> baselineMetadataTask.get().getResolvedMetadataFile().get().getAsFile().isFile()
					);
				}
		);
		final var resolvedBaselineMetadata = baselineMetadataTask
				.flatMap( ResolveClassificationMetadataTask::getResolvedMetadataFile )
				.filter( file -> file.getAsFile().isFile() );
		project.getTasks().register(
				"validateMigrationCompatibility",
				ClassificationMigrationValidationTask.class,
				(task) -> {
					task.dependsOn( classificationMetadataTask );
					task.dependsOn( baselineArtifactsTask );
					task.getBaselineFamily().set( migrationCompatibility.getBaselineFamily() );
					task.getBootstrapBaseline().set( migrationCompatibility.getBootstrapBaseline() );
					task.getBaselineClassificationMetadataFile().set( resolvedBaselineMetadata );
					task.getBaselineArtifacts().from(
						baselineArtifactsTask.flatMap( ResolveClassificationArtifactsTask::getArtifactsDirectory )
								.map( directory -> project.fileTree( directory ).matching( pattern -> pattern.include( "*.jar" ) ) )
					);
					task.getCurrentArtifacts().from( migrationCompatibilityArtifacts );
				}
		);

		final var reviewMetadataTask = project.getTasks().register(
				"resolveMigrationReviewMetadata",
				ResolveClassificationMetadataTask.class,
				task -> {
					task.setGroup( TASK_GROUP_NAME );
					task.setDescription( "Resolves authenticated migration-review metadata" );
					task.getFamily().set( migrationCompatibility.getReviewFamily() );
					task.getClassificationMetadataBaseUrl().set( migrationCompatibility.getClassificationMetadataBaseUrl() );
					task.getClassificationMetadataFile().set( migrationCompatibility.getReviewClassificationMetadataFile() );
					task.getOffline().set( project.getGradle().getStartParameter().isOffline() );
					task.getRefreshDependencies().set( project.getGradle().getStartParameter().isRefreshDependencies() );
					task.getAllowMissing().set( false );
					task.getSharedCacheDirectory().set(
						project.file( new java.io.File( project.getGradle().getGradleUserHomeDir(), "caches/hibernate-orm/classifications" ) )
					);
					task.getResolvedMetadataFile().set(
						project.getLayout().getBuildDirectory().file( "orm/migration-review/baseline/classifications.json.gz" )
					);
					task.onlyIf( ignored -> migrationCompatibility.getReviewFamily().isPresent() );
				}
		);
		final var reviewArtifactsTask = project.getTasks().register(
				"resolveMigrationReviewArtifacts",
				ResolveClassificationArtifactsTask.class,
				task -> {
					task.setGroup( TASK_GROUP_NAME );
					task.setDescription( "Resolves exact migration-review artifacts" );
					task.dependsOn( reviewMetadataTask );
					task.getClassificationMetadataFile().set(
						reviewMetadataTask.flatMap( ResolveClassificationMetadataTask::getResolvedMetadataFile )
					);
					task.getArtifactsDirectory().set(
						project.getLayout().getBuildDirectory().dir( "orm/migration-review/baseline/artifacts" )
					);
					task.onlyIf( ignored -> reviewMetadataTask.get().getResolvedMetadataFile().get().getAsFile().isFile() );
				}
		);
		project.getTasks().register(
				"generateMigrationReview",
				MigrationReviewTask.class,
				task -> {
					task.dependsOn( classificationMetadataTask );
					task.dependsOn( reviewArtifactsTask );
					task.getReviewFamily().set( migrationCompatibility.getReviewFamily() );
					task.getBaselineClassificationMetadataFile().set(
						reviewMetadataTask.flatMap( ResolveClassificationMetadataTask::getResolvedMetadataFile )
					);
					task.getBaselineArtifacts().from(
						reviewArtifactsTask.flatMap( ResolveClassificationArtifactsTask::getArtifactsDirectory )
								.map( directory -> project.fileTree( directory ).matching( pattern -> pattern.include( "*.jar" ) ) )
					);
					task.getCurrentArtifacts().from( migrationCompatibilityArtifacts );
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
