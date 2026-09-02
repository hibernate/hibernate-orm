/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider;

import java.nio.file.Files;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests the public plugin model and deterministic contract-test generation.
///
/// @author Steve Ebersole
public class HibernateDialectProviderPluginTest {
	@TempDir
	java.nio.file.Path temporaryDirectory;

	@Test
	void registersThePublicModel() {
		final Project project = ProjectBuilder.builder().withProjectDir( temporaryDirectory.toFile() ).build();
		project.getPluginManager().apply( HibernateDialectProviderPlugin.class );

		assertNotNull( project.getExtensions().findByName( "hibernateDialectProvider" ) );
		assertNotNull( project.getTasks().findByName( "resolveDialectProviderClassificationMetadata" ) );
		assertNotNull( project.getTasks().findByName( "validateDialectProviderBoundaries" ) );
		assertNotNull( project.getTasks().findByName( "generateDialectProviderContractTests" ) );
		assertNotNull( project.getTasks().findByName( "dialectProviderTest" ) );
		assertNotNull( project.getTasks().findByName( "verifyDialectProvider" ) );
		assertFalse( project.getExtensions().getByType( HibernateDialectProviderExtension.class )
				.getWarningsAsErrors().get() );
	}

	@Test
	void generatesProfilesOnceAndInDeclaredOrder() throws Exception {
		final Project project = ProjectBuilder.builder().withProjectDir( temporaryDirectory.toFile() ).build();
		final GenerateDialectProviderContractTests task = project.getTasks().create(
				"generateProfiles",
				GenerateDialectProviderContractTests.class
		);
		task.getOutputDirectory().set( temporaryDirectory.resolve( "generated" ).toFile() );
		task.getContractProfiles().set( java.util.List.of( "org.example.FirstProfile", "org.example.SecondProfile" ) );

		task.generate();
		final String source = Files.readString( temporaryDirectory.resolve(
				"generated/org/hibernate/dialect/testing/generated/GeneratedDialectProviderContractTest.java"
		) );
		task.generate();
		final String regenerated = Files.readString( temporaryDirectory.resolve(
				"generated/org/hibernate/dialect/testing/generated/GeneratedDialectProviderContractTest.java"
		) );
		assertEquals( 1, occurrences( source, "instantiate(\"org.example.FirstProfile\")" ) );
		assertEquals( 1, occurrences( source, "instantiate(\"org.example.SecondProfile\")" ) );
		assertEquals( source, regenerated );
		assertTrue( source.indexOf( "FirstProfile" ) < source.indexOf( "SecondProfile" ) );
		assertFalse( source.contains( "implements DialectContractProfile" ) );
	}

	@Test
	void rejectsDuplicateAndBlankProfileClassNames() {
		assertThrows( IllegalArgumentException.class, () ->
				org.hibernate.orm.tooling.dialectprovider.internal.ContractTestGenerator.generate(
						temporaryDirectory.resolve( "duplicates" ),
						java.util.List.of( "org.example.Profile", "org.example.Profile" )
				)
		);
		assertThrows( IllegalArgumentException.class, () ->
				org.hibernate.orm.tooling.dialectprovider.internal.ContractTestGenerator.generate(
						temporaryDirectory.resolve( "blank" ),
						java.util.List.of( " " )
				)
		);
	}

	private static int occurrences(String text, String value) {
		return ( text.length() - text.replace( value, "" ).length() ) / value.length();
	}
}
