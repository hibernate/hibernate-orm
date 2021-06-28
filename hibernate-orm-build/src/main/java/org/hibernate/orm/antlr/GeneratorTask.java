/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.antlr;

import java.io.File;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

/**
 * @author Steve Ebersole
 */
public abstract class GeneratorTask extends DefaultTask {
	private final Provider<RegularFile> grammarFile;
	private final Provider<Directory> outputDirectory;

	@Inject
	public GeneratorTask(GrammarDescriptor grammarDescriptor, Antlr4Spec antlrSpec) {
		final String relativePackagePath = grammarDescriptor.getPackageName().replace( '.', '/' );

		grammarFile = antlrSpec.getGrammarBaseDirectory().file( relativePackagePath + "/" + grammarDescriptor.getGrammarName() + ".g4" );
		outputDirectory = antlrSpec.getOutputBaseDirectory().dir( relativePackagePath );
	}

	@InputFile
	public Provider<RegularFile> getGrammarFile() {
		return grammarFile;
	}

	@OutputDirectory
	public Provider<Directory> getOutputDirectory() {
		return outputDirectory;
	}

	@TaskAction
	public void generate() {
		final File grammarFileAsFile = grammarFile.get().getAsFile();
		final File outputDirectoryAsFile = outputDirectory.get().getAsFile();

		getProject().getLogger().info(
				"Starting Antlr grammar generation `{}` -> `{}`",
				grammarFileAsFile.getName(),
				outputDirectoryAsFile.getAbsolutePath()
		);

		outputDirectoryAsFile.mkdirs();

		getProject().javaexec(
				(javaExecSpec) -> {
					javaExecSpec.setMain( "org.antlr.v4.Tool" );
					javaExecSpec.classpath( getProject().getConfigurations().getByName( "antlr" ) );
					javaExecSpec.args(
							"-o", outputDirectoryAsFile.getAbsolutePath(),
							"-long-messages",
							"-listener",
							"-visitor",
							grammarFileAsFile.getAbsolutePath()
					);
				}
		);
	}
}
