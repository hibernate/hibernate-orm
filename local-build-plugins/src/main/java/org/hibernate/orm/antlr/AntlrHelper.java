/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.antlr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.gradle.api.Project;

/**
 * This is intended to deal with <a href="https://github.com/antlr/antlr4/issues/2634">this Antlr issue</a>
 *
 * @author Steve Ebersole
 */
public class AntlrHelper {
	private AntlrHelper() {
		// disallow direct instantiation
	}

	/**
	 * Rewriting the files generated from a Gradle task has the danger of messing with
	 * up-to-date chacking and the build cache.  The paradigm used here is that we generate
	 * the lexer/parser Java files into a temporary directory and then copy them to
	 * {@code $buildDir/generated/sources/antlr} stripping out the stupid line as we copy.
	 *
	 * @param generationDirectory The (temp) directory where we did the generation, including package directory structure
	 * @param outputDirectory The {@code $buildDir/generated/sources/antlr} subdirectory (including package directory structure) into which to copy the fixed files.
	 * @param project The Gradle project ref
	 */
	public static void stripSillyGeneratedFromLines(
			File generationDirectory,
			File outputDirectory,
			Project project) {
		final File[] generatedJavaFiles = generationDirectory.listFiles( (dir, name) -> name.endsWith( ".java" ) );
		if ( generatedJavaFiles == null ) {
			// warn?
			return;
		}

		for ( int i = 0; i < generatedJavaFiles.length; i++ ) {
			stripSillyGeneratedFromLineFromFile( generatedJavaFiles[i], outputDirectory, project );
		}
	}

	private static void stripSillyGeneratedFromLineFromFile(
			File generatedJavaFile,
			File outputDirectory,
			Project project) {
		project.getLogger().lifecycle( "Stripping silly generated-from line from {} into {}",
				generatedJavaFile.getAbsolutePath(),
				outputDirectory.getAbsolutePath() );

		final File outputFile = new File( outputDirectory, generatedJavaFile.getName() );

		try (BufferedReader reader = new BufferedReader( new FileReader( generatedJavaFile ) )) {
			try (BufferedWriter writer = new BufferedWriter( new FileWriter( outputFile ) )) {
				boolean found = false;
				String currentLine;

				while ( ( currentLine = reader.readLine() ) != null ) {
					if ( ! found && currentLine.startsWith( "// Generated from" ) ) {
						found = true;
						continue;
					}
					writer.write( currentLine + System.lineSeparator() );
				}
			}
		}
		catch (IOException e) {
			project.getLogger().lifecycle( "Unable to remove the generated-from line added by Antlr to the generated file: {}", e.getMessage() );
		}
	}
}
