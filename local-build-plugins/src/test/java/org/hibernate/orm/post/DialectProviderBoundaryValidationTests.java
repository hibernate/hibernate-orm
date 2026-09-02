/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.nio.file.Path;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the repository adapter lifecycle for the tooling-owned provider-
/// boundary engine.
///
/// Analyzer behavior is tested by the publishing tooling project which owns
/// the implementation.
///
/// @author Steve Ebersole
public class DialectProviderBoundaryValidationTests {
	@Test
	public void providerValidationIsExplicitAndDelegatesToTheToolingEngine(@TempDir Path directory) {
		final Project project = ProjectBuilder.builder().withProjectDir( directory.toFile() ).build();
		project.getLayout().getBuildDirectory().set( directory.resolve( "target" ).toFile() );
		new ReportGenerationPlugin().apply( project );

		final Task validation = project.getTasks().getByName( "validateDialectProviderBoundaries" );
		final Task metadata = project.getTasks().getByName( "generateClassificationMetadata" );
		final Task reports = project.getTasks().getByName( "generateReports" );
		assertTrue( validation instanceof DialectProviderBoundaryValidationTask );
		assertTrue( validation.getTaskDependencies().getDependencies( validation ).contains( metadata ) );
		assertFalse( reports.getTaskDependencies().getDependencies( reports ).contains( validation ) );
		assertTrue( project.getConfigurations().getNames().contains( "dialectProviderValidationSources" ) );
		assertTrue( project.getConfigurations().getNames().contains( "dialectProviderValidationUpstream" ) );
		assertTrue( project.getConfigurations().getNames().contains( "dialectProviderValidationEngine" ) );
		assertFalse( project.getConfigurations().getByName( "dialectProviderValidationSources" ).isTransitive() );
		assertFalse( project.getConfigurations().getByName( "dialectProviderValidationUpstream" ).isTransitive() );
		assertTrue(
				((DialectProviderBoundaryValidationTask) validation).getEngineClasspath()
						.getFrom()
						.contains( project.getConfigurations().getByName( "dialectProviderValidationEngine" ) )
		);
		assertTrue(
				((ClassificationValidationTask) project.getTasks().getByName( "validateClassifications" ))
						.getProviderArtifacts()
						.getFrom()
						.contains( project.getConfigurations().getByName( "dialectProviderValidationSources" ) )
		);
	}
}
