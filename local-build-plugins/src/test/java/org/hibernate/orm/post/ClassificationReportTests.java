/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.hibernate.orm.post.ClassificationModel.Category.API;
import static org.hibernate.orm.post.ClassificationModel.Category.INTERNAL;
import static org.hibernate.orm.post.ClassificationModel.ClassificationStatus.RESOLVED;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.METHOD;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.PACKAGE;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.TYPE;
import static org.hibernate.orm.post.ClassificationModel.LifecycleOriginKind.ENCLOSING_TYPE;
import static org.hibernate.orm.post.ClassificationModel.LifecycleState.DEPRECATED;
import static org.hibernate.orm.post.ClassificationModel.LifecycleState.INCUBATING;
import static org.hibernate.orm.post.ClassificationModel.LifecycleState.REMOVAL;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.DIRECT;
import static org.hibernate.orm.post.ClassificationModel.OriginKind.ORDINARY_API;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Canonical category/lifecycle projection and Gradle integration tests.
///
/// @author Steve Ebersole
public class ClassificationReportTests {
	private static final String SHARED = "type:fixture.lifecycle.Shared";

	@Test
	public void projectionsPreserveCompactEstablishedFormat() {
		final ClassificationModel model = model();
		final ClassificationReportRenderer renderer = new ClassificationReportRenderer();
		assertEquals(
				"# All elements considered internal for Hibernate's own use\n\nfixture.internal.*\n",
				renderer.render(
						model,
						(element) -> element.getClassificationStatus() == RESOLVED && element.getCategory() == INTERNAL,
						"# All elements considered internal for Hibernate's own use"
				)
		);
		assertEquals(
				"# All elements considered incubating\n\nfixture.lifecycle.Shared\n",
				renderer.render(
						model,
						(element) -> element.getLifecycle().isIncubating(),
						"# All elements considered incubating"
				)
		);
		assertEquals(
				"# All elements considered deprecated\n\n"
						+ "fixture.lifecycle.Methods#old\nfixture.lifecycle.Shared\n",
				renderer.render(
						model,
						(element) -> element.getLifecycle().isDeprecated(),
						"# All elements considered deprecated"
				)
		);
		assertEquals(
				"# All elements scheduled for removal\n\n"
						+ "fixture.lifecycle.RemovalOnly\nfixture.lifecycle.Shared\n",
				renderer.render(
						model,
						(element) -> element.getLifecycle().isRemoval(),
						"# All elements scheduled for removal"
				)
		);
	}

	@Test
	public void incubationReviewRollsUpVersionThenGroupWithoutOriginFluff() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		type( builder, "type:fixture.review.Shared", null, API );
		incubation( builder, "type:fixture.review.Shared", "7.4", "shared-feature" );
		incubation( builder, "type:fixture.review.Shared", "8.0", "alpha" );
		type( builder, "type:fixture.review.GroupA", null, API );
		incubation( builder, "type:fixture.review.GroupA", "8.0", "alpha" );
		type( builder, "type:fixture.review.GroupB", null, API );
		incubation( builder, "type:fixture.review.GroupB", "8.0", "beta" );
		builder.declaration(
				"package:fixture.review.packaged",
				PACKAGE,
				null,
				ClassificationModel.Structure.UNKNOWN,
				"test"
		);
		category( builder, "package:fixture.review.packaged", API, DIRECT, "package:fixture.review.packaged" );
		incubation( builder, "package:fixture.review.packaged", "8.0", "beta" );
		type( builder, "type:fixture.review.Ungrouped", null, API );
		incubation( builder, "type:fixture.review.Ungrouped", "8.0", null );
		type( builder, "type:fixture.review.Later", null, API );
		incubation( builder, "type:fixture.review.Later", "10.0", null );

		final String review = ClassificationReportsTask.renderIncubationReview(
				builder.build(),
				new ClassificationReportRenderer()
		);
		assertEquals(
				"= Incubating review by version and group\n"
						+ "\n== Since 7.4\n"
						+ "\n=== Group: shared-feature\n"
						+ "* fixture.review.Shared\n"
						+ "\n== Since 8.0\n"
						+ "\n=== Group: alpha\n"
						+ "* fixture.review.GroupA\n"
						+ "* fixture.review.Shared\n"
						+ "\n=== Group: beta\n"
						+ "* fixture.review.GroupB\n"
						+ "* fixture.review.packaged.*\n"
						+ "\n=== Ungrouped\n"
						+ "* fixture.review.Ungrouped\n"
						+ "\n== Since 10.0\n"
						+ "\n=== Ungrouped\n"
						+ "* fixture.review.Later\n",
				review
		);
		assertFalse( review.contains( "origin=" ) );
	}

	@Test
	public void everyProjectionEntryResolvesToCanonicalRecord() {
		final ClassificationModel model = model();
		final ClassificationReportRenderer renderer = new ClassificationReportRenderer();
		final List<Predicate<ClassificationModel.Element>> selectors = List.of(
				(element) -> element.getClassificationStatus() == RESOLVED && element.getCategory() == INTERNAL,
				(element) -> element.getLifecycle().isIncubating(),
				(element) -> element.getLifecycle().isDeprecated(),
				(element) -> element.getLifecycle().isRemoval()
		);
		for ( Predicate<ClassificationModel.Element> selector : selectors ) {
			for ( ClassificationReportRenderer.ProjectionEntry entry : renderer.project( model, selector ) ) {
				assertNotNull( model.getElement( entry.getElementId() ), entry.getElementId() );
			}
		}

		final Set<String> incubatingIds = projectedIds(
				renderer.project( model, (element) -> element.getLifecycle().isIncubating() )
		);
		final Set<String> deprecatedIds = projectedIds(
				renderer.project( model, (element) -> element.getLifecycle().isDeprecated() )
		);
		final Set<String> removalIds = projectedIds(
				renderer.project( model, (element) -> element.getLifecycle().isRemoval() )
		);
		assertTrue( incubatingIds.contains( SHARED ) );
		assertTrue( deprecatedIds.contains( SHARED ) );
		assertTrue( removalIds.contains( SHARED ) );
	}

	@Test
	public void tasksConsumeMetadataAndAggregateEveryProjection(@TempDir Path temporaryDirectory) throws Exception {
		final Project project = ProjectBuilder.builder().withProjectDir( temporaryDirectory.toFile() ).build();
		project.getLayout().getBuildDirectory().set( temporaryDirectory.resolve( "target" ).toFile() );
		new ReportGenerationPlugin().apply( project );

		final Task metadata = project.getTasks().getByName( "generateClassificationMetadata" );
		final Task aggregate = project.getTasks().getByName( "generateReports" );
		final ClassificationMigrationValidationTask migration = (ClassificationMigrationValidationTask) project.getTasks()
				.getByName( "validateMigrationCompatibility" );
		final ClassificationReportsTask reports = (ClassificationReportsTask) project.getTasks()
				.getByName( "generateClassificationReports" );
		assertSame(
				reports.getMetadataManager(),
				((ClassificationValidationTask) project.getTasks().getByName( "validateClassifications" ))
						.getMetadataManager()
		);
		final Path metadataFile = temporaryDirectory.resolve( "target/orm/reports/classifications.json" );
		Files.createDirectories( metadataFile.getParent() );
		Files.writeString(
				metadataFile,
				new ClassificationMetadataJson().write( new ClassificationMetadata( "8.1", "8.1.0-SNAPSHOT", model() ) ),
				StandardCharsets.UTF_8
		);

		assertEquals( metadataFile, reports.getClassificationMetadataFileReference().get().getAsFile().toPath() );
		assertTrue( reports.getTaskDependencies().getDependencies( reports ).contains( metadata ) );
		assertTrue( aggregate.getTaskDependencies().getDependencies( aggregate ).contains( reports ) );
		assertTrue( migration.getTaskDependencies().getDependencies( migration ).contains( metadata ) );
		assertNotNull( project.getExtensions().findByType( MigrationCompatibilityExtension.class ) );
		assertNotNull( project.getConfigurations().findByName( ReportGenerationPlugin.MIGRATION_COMPATIBILITY_CONFIG_NAME ) );
		assertNotNull( project.getTasks().findByName( "generateMigrationReview" ) );
		reports.generateReports();
		assertTrue( reports.getInternalsReportFileReference().get().getAsFile().isFile() );
		assertTrue( reports.getIncubationReportFileReference().get().getAsFile().isFile() );
		assertEquals(
				"# All elements considered incubating\n\nfixture.lifecycle.Shared\n",
				Files.readString( reports.getIncubationReportFileReference().get().getAsFile().toPath() )
		);
		assertTrue( reports.getIncubationReviewReportFileReference().get().getAsFile().isFile() );
		assertEquals(
				"= Incubating review by version and group\n"
						+ "\n== Since 8.0\n"
						+ "\n=== Group: report-fixture\n"
						+ "* fixture.lifecycle.Shared\n",
				Files.readString( reports.getIncubationReviewReportFileReference().get().getAsFile().toPath() )
		);
		assertTrue( reports.getDeprecationReportFileReference().get().getAsFile().isFile() );
		assertTrue( reports.getRemovalReportFileReference().get().getAsFile().isFile() );

		for ( String aliasName : List.of(
				"generateInternalsReport",
				"generateIncubationReport",
				"generateDeprecationReport",
				"generateRemovalReport" ) ) {
			final Task alias = project.getTasks().getByName( aliasName );
			assertTrue( alias.getTaskDependencies().getDependencies( alias ).contains( reports ) );
			assertTrue( alias.getOutputs().getFiles().isEmpty() );
		}
	}

	@Test
	public void completedProjectionTaskHasNoJandexInput() {
		assertFalse( AbstractJandexAwareTask.class.isAssignableFrom( ClassificationReportsTask.class ) );
	}

	@Test
	public void metadataManagerParsesOnce() {
		final AtomicInteger parseCount = new AtomicInteger();
		final ClassificationMetadata expected = new ClassificationMetadata( "8.1", "8.1.0-test", model() );
		final ClassificationMetadataManager manager = new ClassificationMetadataManager(
				(path) -> {
					parseCount.incrementAndGet();
					return expected;
				}
		);
		final Path path = Path.of( "target/orm/reports/classifications.json" );
		assertSame( expected, manager.getMetadata( path ) );
		assertSame( expected, manager.getMetadata( path ) );
		assertEquals( 1, parseCount.get() );
	}

	private static Set<String> projectedIds(List<ClassificationReportRenderer.ProjectionEntry> entries) {
		return entries.stream().map( ClassificationReportRenderer.ProjectionEntry::getElementId ).collect( java.util.stream.Collectors.toSet() );
	}

	private static ClassificationModel model() {
		final ClassificationModel.Builder builder = ClassificationModel.builder();
		builder.declaration(
				"package:fixture.internal",
				PACKAGE,
				null,
				ClassificationModel.Structure.UNKNOWN,
				"test"
		);
		category( builder, "package:fixture.internal", INTERNAL, DIRECT, "package:fixture.internal" );
		type( builder, "type:fixture.internal.Helper", "package:fixture.internal", INTERNAL );

		type( builder, SHARED, null, API );
		lifecycle( builder, SHARED, INCUBATING, ClassificationModel.LifecycleOriginKind.DIRECT, SHARED );
		lifecycle( builder, SHARED, DEPRECATED, ClassificationModel.LifecycleOriginKind.DIRECT, SHARED );
		lifecycle( builder, SHARED, REMOVAL, ClassificationModel.LifecycleOriginKind.DIRECT, SHARED );
		method( builder, "method:fixture.lifecycle.Shared#operation()", SHARED );
		lifecycle(
				builder,
				"method:fixture.lifecycle.Shared#operation()",
				INCUBATING,
				ENCLOSING_TYPE,
				SHARED
		);

		type( builder, "type:fixture.lifecycle.Methods", null, API );
		method( builder, "method:fixture.lifecycle.Methods#old()", "type:fixture.lifecycle.Methods" );
		method( builder, "method:fixture.lifecycle.Methods#old(java.lang.String)", "type:fixture.lifecycle.Methods" );
		lifecycle(
				builder,
				"method:fixture.lifecycle.Methods#old()",
				DEPRECATED,
				ClassificationModel.LifecycleOriginKind.DIRECT,
				"method:fixture.lifecycle.Methods#old()"
		);
		lifecycle(
				builder,
				"method:fixture.lifecycle.Methods#old(java.lang.String)",
				DEPRECATED,
				ClassificationModel.LifecycleOriginKind.DIRECT,
				"method:fixture.lifecycle.Methods#old(java.lang.String)"
		);

		final String removalOnly = "type:fixture.lifecycle.RemovalOnly";
		type( builder, removalOnly, null, API );
		lifecycle(
				builder,
				removalOnly,
				REMOVAL,
				ClassificationModel.LifecycleOriginKind.DIRECT,
				removalOnly
		);
		return builder.build();
	}

	private static void type(
			ClassificationModel.Builder builder,
			String id,
			String owner,
			ClassificationModel.Category category) {
		builder.declaration(
				id,
				TYPE,
				owner,
				new ClassificationModel.Structure( Modifier.PUBLIC, false, false ),
				"test"
		);
		category(
				builder,
				id,
				category,
				category == API ? ORDINARY_API : ClassificationModel.OriginKind.INTERNAL_PACKAGE,
				owner == null ? id : owner
		);
	}

	private static void method(ClassificationModel.Builder builder, String id, String owner) {
		builder.declaration(
				id,
				METHOD,
				owner,
				new ClassificationModel.Structure( Modifier.PUBLIC, false, false ),
				"test"
		);
		category( builder, id, API, ORDINARY_API, id );
	}

	private static void category(
			ClassificationModel.Builder builder,
			String id,
			ClassificationModel.Category category,
			ClassificationModel.OriginKind kind,
			String source) {
		builder.addClassificationOrigin(
				id,
				new ClassificationModel.ClassificationOrigin( category, kind, source, Collections.emptySet() ),
				Collections.emptySet()
		);
	}

	private static void lifecycle(
			ClassificationModel.Builder builder,
			String id,
			ClassificationModel.LifecycleState state,
			ClassificationModel.LifecycleOriginKind kind,
			String source) {
		builder.addLifecycleOrigin(
				id,
				state == INCUBATING
						? new ClassificationModel.LifecycleOrigin( state, kind, source, "8.0", "report-fixture" )
						: new ClassificationModel.LifecycleOrigin( state, kind, source )
		);
	}

	private static void incubation(
			ClassificationModel.Builder builder,
			String id,
			String since,
			String group) {
		builder.addLifecycleOrigin(
				id,
				new ClassificationModel.LifecycleOrigin(
						INCUBATING,
						ClassificationModel.LifecycleOriginKind.DIRECT,
						id,
						since,
						group
				)
		);
	}

}
