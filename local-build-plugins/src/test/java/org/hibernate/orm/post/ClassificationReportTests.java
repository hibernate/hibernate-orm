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
		reports.generateReports();
		assertTrue( reports.getSpiReportFileReference().get().getAsFile().isFile() );
		assertTrue( reports.getInternalsReportFileReference().get().getAsFile().isFile() );
		assertTrue( reports.getIncubationReportFileReference().get().getAsFile().isFile() );
		assertTrue( reports.getDeprecationReportFileReference().get().getAsFile().isFile() );
		assertTrue( reports.getRemovalReportFileReference().get().getAsFile().isFile() );

		for ( String aliasName : List.of(
				"generateSpiReport",
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
				"fixture.internal",
				"fixture.internal",
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
				packageName( id ),
				id,
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
				packageName( owner ),
				id,
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
		builder.addLifecycleOrigin( id, new ClassificationModel.LifecycleOrigin( state, kind, source ) );
	}

	private static String packageName(String id) {
		final String name = id.substring( id.indexOf( ':' ) + 1, id.contains( "#" ) ? id.indexOf( '#' ) : id.length() );
		return name.substring( 0, name.lastIndexOf( '.' ) );
	}
}
