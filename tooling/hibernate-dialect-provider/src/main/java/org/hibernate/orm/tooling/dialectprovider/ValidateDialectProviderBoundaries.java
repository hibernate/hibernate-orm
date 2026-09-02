/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.dialectprovider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.hibernate.orm.tooling.dialectprovider.internal.ClassificationMetadata;
import org.hibernate.orm.tooling.dialectprovider.internal.ClassificationMetadataReader;
import org.hibernate.orm.tooling.dialectprovider.internal.ProviderBoundaryAnalyzer;
import org.hibernate.orm.tooling.dialectprovider.internal.ProviderBoundaryReports;

/// Validates provider bytecode against Hibernate ORM's supported provider
/// implementation boundary.
///
/// The task always writes complete text and JSON reports before failing for an
/// error or, when configured, for a warning.
///
/// @author Steve Ebersole
/// @since 8.0
@DisableCachingByDefault(because = "Reports retain provider and upstream artifact identity for diagnostics")
public abstract class ValidateDialectProviderBoundaries extends DefaultTask {
	@Classpath
	public abstract ConfigurableFileCollection getProviderArtifacts();

	@Classpath
	public abstract ConfigurableFileCollection getHibernateArtifacts();

	@Input
	public abstract ListProperty<String> getProviderPackages();

	/// Configure whether warning findings should fail this task.
	///
	/// Reports preserve the declared warning severity regardless of this build
	/// policy.
	@Input
	public abstract Property<Boolean> getWarningsAsErrors();

	@InputFile
	@PathSensitive(PathSensitivity.NONE)
	public abstract RegularFileProperty getClassificationMetadataFile();

	@OutputFile
	public abstract RegularFileProperty getTextReportFile();

	@OutputFile
	public abstract RegularFileProperty getJsonReportFile();

	@TaskAction
	public void validateBoundaries() {
		final Path metadataPath = getClassificationMetadataFile().get().getAsFile().toPath();
		final ClassificationMetadata metadata = new ClassificationMetadataReader().read( metadataPath );
		final ProviderBoundaryAnalyzer.Result result;
		try {
			result = new ProviderBoundaryAnalyzer().analyze(
					paths( getProviderArtifacts() ),
					paths( getHibernateArtifacts() ),
					getProviderPackages().get(),
					metadata
			);
		}
		catch (IllegalArgumentException e) {
			throw new GradleException( "Invalid Hibernate Dialect-provider validation configuration: " + e.getMessage(), e );
		}

		final ProviderBoundaryReports renderer = new ProviderBoundaryReports();
		final boolean warningsAsErrors = getWarningsAsErrors().get();
		write( getTextReportFile().get().getAsFile().toPath(), renderer.text( metadata, result, warningsAsErrors ) );
		write( getJsonReportFile().get().getAsFile().toPath(), renderer.json( metadata, result, warningsAsErrors ) );
		if ( result.hasWarnings() ) {
			getLogger().warn(
					"Hibernate ORM Dialect provider-boundary validation found {} warning(s); see {} and {}",
					result.warningCount(),
					getTextReportFile().get().getAsFile(),
					getJsonReportFile().get().getAsFile()
			);
		}
		if ( result.fails( warningsAsErrors ) ) {
			throw new GradleException(
					"Hibernate ORM Dialect provider-boundary validation found " + result.errorCount()
							+ " error(s) and " + result.warningCount() + " warning(s)"
							+ ( warningsAsErrors ? " with warnings-as-errors enabled" : "" )
							+ "; see " + getTextReportFile().get().getAsFile()
							+ " and " + getJsonReportFile().get().getAsFile()
			);
		}
	}

	private static List<Path> paths(ConfigurableFileCollection files) {
		return files.getFiles().stream()
				.map( java.io.File::toPath )
				.sorted( Comparator.comparing( Path::toString ) )
				.toList();
	}

	private static void write(Path path, String contents) {
		try {
			Files.createDirectories( path.getParent() );
			Files.writeString( path, contents, StandardCharsets.UTF_8 );
		}
		catch (IOException e) {
			throw new GradleException( "Unable to write Dialect-provider report " + path, e );
		}
	}
}
