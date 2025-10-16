/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.antlr;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;
import java.io.File;

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
			ExecOperations execOperations,
			ProjectLayout layout,
			ProviderFactory providers
	) {
		this.grammarDescriptor = grammarDescriptor;
		this.execOperations = execOperations;

		lexerGrammarFile = providers.provider( () -> {
			final Directory grammarBaseDirectory = antlrSpec.getGrammarBaseDirectory().get();
			final Directory grammarDirectory = grammarBaseDirectory.dir( grammarDescriptor.getPackageName().get().replace( '.', '/' ) );
			return grammarDirectory.file( grammarDescriptor.getLexerFileName().get() );
		} );

		parserGrammarFile = providers.provider( () -> {
			final Directory grammarBaseDirectory = antlrSpec.getGrammarBaseDirectory().get();
			final Directory grammarDirectory = grammarBaseDirectory.dir( grammarDescriptor.getPackageName().get().replace( '.', '/' ) );
			return grammarDirectory.file( grammarDescriptor.getParserFileName().get() );
		} );

		generationDirectory = layout.getBuildDirectory()
				.dir( "tmp/antlr" )
				.map(dir -> dir.dir(grammarDescriptor.getPackageName().get().replace('.', '/')));

		outputDirectory = antlrSpec.getOutputBaseDirectory()
				.map(dir -> dir.dir(grammarDescriptor.getPackageName().get().replace('.', '/')));
	}

	@InputFiles
	@Classpath
	public abstract ConfigurableFileCollection getAntlrClasspath();

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

		stripSillyGeneratedFromLines( generationDir, outputDir, getLogger() );
	}


	private void generateLexer(File outputDir) {
		final File lexerFile = getLexerGrammarFile().get().getAsFile();

		getLogger().lifecycle(
				"Starting Antlr lexer grammar generation `{}` : `{}` -> `{}`",
				grammarDescriptor.getName(),
				lexerFile.getAbsolutePath(),
				outputDir.getAbsolutePath()
		);


		execOperations.javaexec(
				(javaExecSpec) -> {
					javaExecSpec.getMainClass().set( "org.antlr.v4.Tool" );
					javaExecSpec.classpath( getAntlrClasspath() );
					javaExecSpec.args(
							"-o", outputDir.getAbsolutePath(),
							"-long-messages",
							lexerFile.getAbsolutePath()
					);
				}
		);
	}

	private void generateParser(File outputDir) {
		final File parserFile = getParserGrammarFile().get().getAsFile();

		getLogger().lifecycle(
				"Starting Antlr parser grammar generation `{}` : `{}` -> `{}`",
				grammarDescriptor.getName(),
				parserFile.getAbsolutePath(),
				outputDir.getAbsolutePath()
		);


		execOperations.javaexec(
				(javaExecSpec) -> {
					javaExecSpec.getMainClass().set( "org.antlr.v4.Tool" );
					javaExecSpec.classpath( getAntlrClasspath() );
					javaExecSpec.args(
							"-o", outputDir.getAbsolutePath(),
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
