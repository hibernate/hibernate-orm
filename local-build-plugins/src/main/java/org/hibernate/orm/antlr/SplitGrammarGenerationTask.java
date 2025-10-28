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
import org.gradle.process.ExecOperations;

import static org.hibernate.orm.antlr.AntlrHelper.stripSillyGeneratedFromLines;

/**
 * @author Steve Ebersole
 */
@CacheableTask
public abstract class SplitGrammarGenerationTask extends DefaultTask {
	private final SplitGrammarDescriptor grammarDescriptor;
	private final ExecOperations execOperations;

	private final Provider<RegularFile> lexerGrammarFile;
	private final Provider<RegularFile> parserGrammarFile;

	private final Provider<Directory> generationDirectory;
	private final Provider<Directory> outputDirectory;

	@Inject
	public SplitGrammarGenerationTask(
			SplitGrammarDescriptor grammarDescriptor,
			AntlrSpec antlrSpec,
			ExecOperations execOperations) {
		this.grammarDescriptor = grammarDescriptor;
		this.execOperations = execOperations;

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

		generationDirectory = getProject().provider( () -> {
			final Directory baseDirectory = getProject().getLayout().getBuildDirectory().dir( "tmp/antlr" ).get();
			return baseDirectory.dir( grammarDescriptor.getPackageName().get().replace( '.', '/' ) );
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
	public Provider<Directory> getGenerationDirectory() {
		return generationDirectory;
	}

	@OutputDirectory
	public Provider<Directory> getOutputDirectory() {
		return outputDirectory;
	}

	@TaskAction
	public void generateLexerAndParser() {
		final File generationDir = generationDirectory.get().getAsFile();
		generationDir.mkdirs();

		final File outputDir = outputDirectory.get().getAsFile();
		outputDir.mkdirs();

		generateLexer( generationDir );
		generateParser( generationDir );

		stripSillyGeneratedFromLines( generationDir, outputDir, getProject() );
	}


	private void generateLexer(File outputDir) {
		final File lexerFile = getLexerGrammarFile().get().getAsFile();

		getProject().getLogger().lifecycle(
				"Starting Antlr lexer grammar generation `{}` : `{}` -> `{}`",
				grammarDescriptor.getName(),
				lexerFile.getAbsolutePath(),
				outputDir.getAbsolutePath()
		);


		execOperations.javaexec(
				(javaExecSpec) -> {
					javaExecSpec.getMainClass().set( "org.antlr.v4.Tool" );
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

		getProject().getLogger().lifecycle(
				"Starting Antlr parser grammar generation `{}` : `{}` -> `{}`",
				grammarDescriptor.getName(),
				parserFile.getAbsolutePath(),
				outputDir.getAbsolutePath()
		);


		execOperations.javaexec(
				(javaExecSpec) -> {
					javaExecSpec.getMainClass().set( "org.antlr.v4.Tool" );
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
