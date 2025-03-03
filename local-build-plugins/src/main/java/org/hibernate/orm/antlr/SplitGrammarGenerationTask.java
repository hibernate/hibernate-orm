/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.antlr;

import java.io.File;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import static org.hibernate.orm.antlr.AntlrHelper.stripSillyGeneratedFromLines;

/**
 * @author Steve Ebersole
 */
@CacheableTask
public abstract class SplitGrammarGenerationTask extends DefaultTask {
	private final SplitGrammarDescriptor grammarDescriptor;

	private final Provider<RegularFile> lexerGrammarFile;
	private final Provider<RegularFile> parserGrammarFile;
	private final Provider<Directory> outputDirectory;

	@Inject
	public SplitGrammarGenerationTask(SplitGrammarDescriptor grammarDescriptor, AntlrSpec antlrSpec) {
		this.grammarDescriptor = grammarDescriptor;

		lexerGrammarFile = getProject().provider( () -> {
			final Directory grammarBaseDirectory = antlrSpec.getGrammarBaseDirectory().get();
			final Directory grammarDirectory = grammarBaseDirectory.dir( grammarDescriptor.getPackageName().get().replace( '.', '/' ) );
			return grammarDirectory.file( grammarDescriptor.getLexerFileName().get() );
		} );

		parserGrammarFile = getProject().provider( () -> {
			final Directory grammarBaseDirectory = antlrSpec.getGrammarBaseDirectory().get();
			final Directory grammarDirectory = grammarBaseDirectory.dir( grammarDescriptor.getPackageName().get().replace( '.', '/' ) );
			return grammarDirectory.file( grammarDescriptor.getParserFileName().get() );
		} );

		outputDirectory = getProject().provider( () -> {
			final Directory outputBaseDirectory = antlrSpec.getOutputBaseDirectory().get();
			return outputBaseDirectory.dir( grammarDescriptor.getPackageName().get().replace( '.', '/' ) );
		} );
	}

	@InputFile
	@PathSensitive( PathSensitivity.RELATIVE )
	public Provider<RegularFile> getLexerGrammarFile() {
		return lexerGrammarFile;
	}

	@InputFile
	@PathSensitive( PathSensitivity.RELATIVE )
	public Provider<RegularFile> getParserGrammarFile() {
		return parserGrammarFile;
	}

	@OutputDirectory
	public Provider<Directory> getOutputDirectory() {
		return outputDirectory;
	}

	@TaskAction
	public void generateLexerAndParser() {
		final File outputDir = outputDirectory.get().getAsFile();
		outputDir.mkdirs();

		generateLexer( outputDir );
		generateParser( outputDir );

		stripSillyGeneratedFromLines( outputDir, getProject() );
	}


	private void generateLexer(File outputDir) {
		final File lexerFile = getLexerGrammarFile().get().getAsFile();

		getProject().getLogger().info(
				"Starting Antlr lexer grammar generation `{}` : `{}` -> `{}`",
				grammarDescriptor.getName(),
				lexerFile.getAbsolutePath(),
				outputDir.getAbsolutePath()
		);


		getProject().javaexec(
				(javaExecSpec) -> {
					javaExecSpec.setMain( "org.antlr.v4.Tool" );
					javaExecSpec.classpath( getProject().getConfigurations().getByName( "antlr" ) );
					javaExecSpec.args(
							"-o", getProject().relativePath( outputDir.getAbsolutePath() ),
							"-long-messages",
							lexerFile.getAbsolutePath()
					);
				}
		);
	}

	private void generateParser(File outputDir) {
		final File parserFile = getParserGrammarFile().get().getAsFile();

		getProject().getLogger().info(
				"Starting Antlr parser grammar generation `{}` : `{}` -> `{}`",
				grammarDescriptor.getName(),
				parserFile.getAbsolutePath(),
				outputDir.getAbsolutePath()
		);


		getProject().javaexec(
				(javaExecSpec) -> {
					javaExecSpec.setMain( "org.antlr.v4.Tool" );
					javaExecSpec.classpath( getProject().getConfigurations().named( "antlr" ) );
					javaExecSpec.args(
							"-o", getProject().relativePath( outputDir.getAbsolutePath() ),
							"-long-messages",
							parserFile.getAbsolutePath()
					);

					if ( grammarDescriptor.generateListener().get() ) {
						javaExecSpec.args( "-listener" );
					}
					if ( grammarDescriptor.generateVisitor().get() ) {
						javaExecSpec.args( "-visitor" );
					}
				}
		);
	}
}
