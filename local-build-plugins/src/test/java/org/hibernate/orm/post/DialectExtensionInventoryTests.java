/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.hibernate.boot.registry.selector.internal.DefaultDialectSelector;
import org.hibernate.boot.registry.selector.spi.DialectSelector;
import org.hibernate.community.dialect.ExternalDialect;
import org.hibernate.community.dialect.internal.ProviderHelper;
import org.hibernate.dialect.Database;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SampleDialect;
import org.hibernate.orm.post.fixture.DialectCaller;
import org.hibernate.sql.ast.internal.InternalTranslator;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Phase 0 Dialect inventory model, bytecode linkage, rendering, and Gradle
/// registration tests.
///
/// @author Steve Ebersole
public class DialectExtensionInventoryTests {
	@Test
	public void checkedInDecisionOverlayIsWellFormed() {
		final Path overlay = Path.of( "..", "design", "dialect-extension-decisions.tsv" );
		assertTrue( Files.isRegularFile( overlay ), overlay.toAbsolutePath().toString() );
		DialectExtensionDecisionOverlay.read( overlay );
		final Path familyOverlay = Path.of( "..", "design", "dialect-family-decisions.tsv" );
		assertTrue( Files.isRegularFile( familyOverlay ), familyOverlay.toAbsolutePath().toString() );
		DialectFamilyDecisionOverlay.read( familyOverlay );
	}

	@Test
	public void inventoryCombinesJandexClassificationAndBytecodeFacts(@TempDir Path directory) throws Exception {
		final Index index = index(
				Dialect.class,
				SampleDialect.class,
				Database.class,
				Database.SAMPLE.getClass(),
				DialectSelector.class,
				DefaultDialectSelector.class,
				ExternalDialect.class,
				ProviderHelper.class,
				InternalTranslator.class,
				DialectCaller.class
		);
		final ClassificationModel classifications = new JandexClassificationClassifier( (id) -> "test" )
				.classify( index );
		final Path classes = Path.of(
				Dialect.class.getProtectionDomain().getCodeSource().getLocation().toURI()
		);
		final List<BytecodeLinkageAnalyzer.Link> links = new BytecodeLinkageAnalyzer().analyze(
				List.of( classes.toFile() )
		);
		final Path supportTable = directory.resolve( "dialect-table.adoc" );
		Files.writeString(
				supportTable,
				"|Dialect |Minimum Database Version\n|SampleDialect|1.0\n",
				StandardCharsets.UTF_8
		);
		final DialectExtensionInventory inventory = new DialectExtensionInventoryAnalyzer().analyze(
				index,
				classifications,
				links,
				Dialect.class.getClassLoader(),
				List.of( supportTable.toFile() )
		);

		assertNotNull( surface( inventory, "method:org.hibernate.dialect.Dialect#translate(java.lang.String)" ) );
		assertNotNull( surface( inventory, "method:org.hibernate.dialect.Dialect#render(java.lang.String)" ) );
		assertEquals(
				"GENERAL_CAPABILITY",
				surface( inventory, "method:org.hibernate.dialect.Dialect#supportsUsefulFeature()" ).getReviewGroup()
		);
		assertTrue(
				inventory.getDialectHierarchy().stream().anyMatch(
						(edge) -> edge.getSourceElementId().equals( "type:org.hibernate.community.dialect.ExternalDialect" )
								&& edge.getTargetElementId().equals( "type:org.hibernate.dialect.SampleDialect" )
				)
		);
		assertTrue(
				inventory.getOverrides().stream().anyMatch(
						(edge) -> edge.getSourceElementId().equals(
								"method:org.hibernate.community.dialect.ExternalDialect#translate(java.lang.String)"
						)
				)
		);
		assertTrue(
				inventory.getDialectCalls().stream().anyMatch(
						(link) -> link.getSourceClass().equals( DialectCaller.class.getName() )
								&& link.getTargetElementId().equals(
										"method:org.hibernate.dialect.Dialect#translate(java.lang.String)"
								)
				)
		);
		assertTrue(
				inventory.getCommunityInternalDependencies().stream().anyMatch(
						(dependency) -> dependency.getTargetElementId().startsWith(
								"constructor:org.hibernate.sql.ast.internal.InternalTranslator#<init>"
						)
				)
		);
		assertFalse(
				inventory.getCommunityInternalDependencies().stream().anyMatch(
						(dependency) -> dependency.getTargetElementId().contains( "org.hibernate.community." )
				)
		);
		assertTrue(
				inventory.getCommunityExtensionUses().stream().anyMatch(
						(use) -> use.getFamily().equals( "SQL_AST" )
				)
		);
		final DialectExtensionInventory.DialectSelection sampleSelection = inventory.getDialectSelections().stream()
				.filter( (selection) -> selection.getDialectClass().equals( SampleDialect.class.getName() ) )
				.findFirst()
				.orElseThrow();
		assertEquals( List.of( "dialect-table.adoc" ), sampleSelection.getDocumentationSources() );
		assertEquals( List.of( "NO_ARG" ), sampleSelection.getConfigurationConstructors() );
		assertEquals( List.of( "Sample", "SampleLegacy" ), names( sampleSelection.getShortNames() ) );
		assertEquals( List.of( "SAMPLE" ), names( sampleSelection.getAutomaticResolution() ) );
		assertEquals( 4, inventory.getFamilyCandidates().size() );

		final DialectExtensionInventoryRenderer renderer = new DialectExtensionInventoryRenderer();
		final String first = renderer.json( inventory, "8.1", "8.1.0-test" );
		final String second = renderer.json( inventory, "8.1", "8.1.0-test" );
		assertEquals( first, second );
		assertTrue( first.contains( "hibernate-dialect-extension-inventory" ) );
		final DialectExtensionDecisionOverlay emptyDecisions = DialectExtensionDecisionOverlay.empty();
		assertTrue(
				renderer.decisionOverlay( inventory, emptyDecisions )
						.contains( "reviewGroup\tdecisionStatus\ttargetCategory\troles\tdisposition\trationale" )
		);
		assertTrue( renderer.summary( inventory ).contains( "Community dependencies on internal contracts" ) );
		assertTrue( renderer.selectionMatrices( inventory ).contains( "Dialect Selection Evidence" ) );
		assertTrue( renderer.selectionMatrices( inventory ).contains( SampleDialect.class.getName() ) );
		assertTrue( renderer.familyInventory( inventory ).contains( "Dialect and Translator Family Evidence" ) );
		final String undecidedReview = renderer.review( inventory, emptyDecisions, "8.1", "8.1.0-test" );
		assertTrue( undecidedReview.contains( "Dialect Extension Surface Review" ) );
		assertTrue( undecidedReview.contains( "_Undecided_" ) );
		assertTrue( undecidedReview.contains( "Observed usage::" ) );

		final Path overlayFile = directory.resolve( "decisions.tsv" );
		Files.writeString(
				overlayFile,
				DialectExtensionDecisionOverlay.HEADER + '\n'
						+ "method:org.hibernate.dialect.Dialect#translate(java.lang.String)\tTRANSLATION\tPROPOSED\tSPI\t"
						+ "IMPLEMENT,SUPPLY\tRETAIN\tProviders override translation behavior\t\t\tPhase 2\t\n",
				StandardCharsets.UTF_8
		);
		final DialectExtensionDecisionOverlay decisions = DialectExtensionDecisionOverlay.read( overlayFile );
		final String decidedReview = renderer.review( inventory, decisions, "8.1", "8.1.0-test" );
		assertTrue( decidedReview.contains( "`SPI` with roles `IMPLEMENT,SUPPLY`" ) );
		assertTrue( decidedReview.contains( "Decision status:: `PROPOSED`" ) );
		assertTrue( decidedReview.contains( "Rationale:: Providers override translation behavior" ) );

		Files.writeString(
				overlayFile,
				DialectExtensionDecisionOverlay.HEADER + '\n'
						+ "field:org.hibernate.dialect.Dialect#POLICY\tGENERAL_CAPABILITY\tPROPOSED\tSPI\t"
						+ "IMPLEMENT\tRETAIN\tInvalid field role\t\t\tPhase 2\t\n",
				StandardCharsets.UTF_8
		);
		final DialectExtensionDecisionOverlay invalidFieldDecision = DialectExtensionDecisionOverlay.read( overlayFile );
		assertThrows( IllegalArgumentException.class, () -> invalidFieldDecision.validate( inventory ) );

		Files.writeString(
				overlayFile,
				DialectExtensionDecisionOverlay.HEADER + '\n'
						+ "method:org.hibernate.dialect.Dialect#removedHook()\tGENERAL_CAPABILITY\tCOMPLETED\tINTERNAL\t"
						+ "\tREMOVE\tRemoved provider hook\tReplacement\tdialect-removed-hook\tPhase 2\t\n",
				StandardCharsets.UTF_8
		);
		final DialectExtensionDecisionOverlay completedRemoval =
				DialectExtensionDecisionOverlay.read( overlayFile );
		completedRemoval.validate( inventory );

		Files.writeString(
				overlayFile,
				DialectExtensionDecisionOverlay.HEADER + '\n'
						+ "method:org.hibernate.dialect.Dialect#missingRetainedHook()\tGENERAL_CAPABILITY\tCOMPLETED\tSPI\t"
						+ "IMPLEMENT\tRETAIN\tMissing retained hook\t\t\tPhase 2\t\n",
				StandardCharsets.UTF_8
		);
		final DialectExtensionDecisionOverlay invalidCompletedRetention =
				DialectExtensionDecisionOverlay.read( overlayFile );
		assertThrows( IllegalArgumentException.class, () -> invalidCompletedRetention.validate( inventory ) );
	}

	@Test
	public void taskIsExplicitAndNotPartOfNormalReports(@TempDir Path directory) {
		final Project project = ProjectBuilder.builder().withProjectDir( directory.toFile() ).build();
		project.getLayout().getBuildDirectory().set( directory.resolve( "target" ).toFile() );
		new ReportGenerationPlugin().apply( project );

		final Task inventory = project.getTasks().getByName( "generateDialectExtensionInventory" );
		final Task index = project.getTasks().getByName( "buildAggregatedIndex" );
		final Task reports = project.getTasks().getByName( "generateReports" );
		final Task dialectTable = project.getTasks().getByName( "generateDialectTableReport" );
		final Task communityDialectTable = project.getTasks().getByName( "generateCommunityDialectTableReport" );
		assertTrue( inventory instanceof DialectExtensionInventoryTask );
		assertTrue( inventory.getTaskDependencies().getDependencies( inventory ).contains( index ) );
		assertTrue( inventory.getTaskDependencies().getDependencies( inventory ).contains( dialectTable ) );
		assertTrue( inventory.getTaskDependencies().getDependencies( inventory ).contains( communityDialectTable ) );
		assertFalse( reports.getTaskDependencies().getDependencies( reports ).contains( inventory ) );
	}

	private static DialectExtensionInventory.SurfaceDeclaration surface(
			DialectExtensionInventory inventory,
			String id) {
		return inventory.getDialectSurface().stream()
				.filter( (declaration) -> declaration.getElementId().equals( id ) )
				.findFirst()
				.orElse( null );
	}

	private static List<String> names(
			List<DialectExtensionInventory.SelectionRegistration> registrations) {
		return registrations.stream().map( DialectExtensionInventory.SelectionRegistration::getName ).toList();
	}

	private static Index index(Class<?>... classes) throws IOException {
		final Indexer indexer = new Indexer();
		for ( Class<?> type : classes ) {
			final String resourceName = "/" + type.getName().replace( '.', '/' ) + ".class";
			try ( InputStream stream = type.getResourceAsStream( resourceName ) ) {
				if ( stream == null ) {
					throw new IOException( "Missing test class resource " + resourceName );
				}
				indexer.index( stream );
			}
		}
		return indexer.complete();
	}
}
