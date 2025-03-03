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
 * @author Steve Ebersole
 */
public class AntlrHelper {
	private AntlrHelper() {
		// disallow direct instantiation
	}

	public static void stripSillyGeneratedFromLines(File outputDirectory, Project project) {
		// https://github.com/antlr/antlr4/issues/2634
		// :shrug:

		final File[] generatedJavaFiles = outputDirectory.listFiles( (dir, name) -> name.endsWith( ".java" ) );
		if ( generatedJavaFiles == null ) {
			// warn?
			return;
		}

		for ( int i = 0; i < generatedJavaFiles.length; i++ ) {
			stripSillyGeneratedFromLineFromFile( generatedJavaFiles[i], project );
		}

	}

	private static void stripSillyGeneratedFromLineFromFile(File generatedJavaFile, Project project) {
		try {
			final File tmpFile = project.getLayout()
					.getBuildDirectory()
					.get()
					.dir( "tmp" )
					.file( generatedJavaFile.getName() )
					.getAsFile();

			tmpFile.getParentFile().mkdirs();
			tmpFile.createNewFile();

			final BufferedReader reader = new BufferedReader( new FileReader( generatedJavaFile ) );
			final BufferedWriter writer = new BufferedWriter( new FileWriter( tmpFile ) );

			boolean found = false;
			String currentLine;

			while ( ( currentLine = reader.readLine() ) != null ) {
				if ( ! found && currentLine.startsWith( "// Generated from" ) ) {
					found = true;
					continue;
				}
				writer.write( currentLine + System.lineSeparator() );
			}

			writer.close();
			reader.close();

			generatedJavaFile.delete();
			tmpFile.renameTo( generatedJavaFile );
		}
		catch (IOException e) {
			project.getLogger().lifecycle( "Unable to remove the generated-from line added by Antlr to the generated file" );
		}
	}

}
