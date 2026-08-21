/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import static org.hibernate.orm.post.ReportGenerationPlugin.TASK_GROUP_NAME;

/// Common Gradle inputs, failure handling, and report output for canonical
/// classification validators.
///
/// @author Steve Ebersole
public abstract class AbstractClassificationValidationTask extends DefaultTask {
	private final Provider<ClassificationMetadataManager> metadataManager;
	private final RegularFileProperty classificationMetadataFile;
	private final RegularFileProperty allowlistFile;
	private final RegularFileProperty reportFile;

	protected AbstractClassificationValidationTask(String description, String reportName) {
		setGroup( TASK_GROUP_NAME );
		setDescription( description );
		metadataManager = getProject().provider(
				() -> getProject().getExtensions().getByType( ClassificationMetadataManager.class )
		);
		classificationMetadataFile = getProject().getObjects().fileProperty();
		classificationMetadataFile.convention(
				getProject().getLayout().getBuildDirectory().file( "orm/reports/classifications.json" )
		);
		allowlistFile = getProject().getObjects().fileProperty();
		allowlistFile.convention(
				getProject().getRootProject().getLayout().getProjectDirectory()
						.file( "gradle/classification-validation-allowlist.json" )
		);
		reportFile = getProject().getObjects().fileProperty();
		reportFile.convention( getProject().getLayout().getBuildDirectory().file( "orm/reports/" + reportName ) );
	}

	@Internal
	protected ClassificationMetadataManager getMetadataManager() {
		return metadataManager.get();
	}

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public Provider<RegularFile> getClassificationMetadataFileReference() {
		return classificationMetadataFile;
	}

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public Provider<RegularFile> getAllowlistFileReference() {
		return allowlistFile;
	}

	@OutputFile
	public Provider<RegularFile> getReportFileReference() {
		return reportFile;
	}

	@TaskAction
	public void validate() {
		final File report = reportFile.get().getAsFile();
		final ValidationAllowlist allowlist;
		try {
			allowlist = ValidationAllowlist.read( allowlistFile.get().getAsFile() );
		}
		catch (IllegalArgumentException e) {
			write( report, title() + ": FAILED\n\nConfiguration error: " + e.getMessage() + '\n' );
			throw new GradleException( "Invalid classification validation allowlist; see " + report.getAbsolutePath(), e );
		}

		final ClassificationModel model = getMetadataManager()
				.getMetadata( classificationMetadataFile.get().getAsFile().toPath() )
				.getModel();
		final ValidationResult result = validate( model, allowlist );
		write( report, new ValidationReportRenderer().render( title(), result ) );
		if ( result.hasFailures() ) {
			throw new GradleException( title() + " failed; see " + report.getAbsolutePath() );
		}
	}

	protected abstract ValidationResult validate(ClassificationModel model, ValidationAllowlist allowlist);

	protected abstract String title();

	private static void write(File file, String contents) {
		try {
			Files.createDirectories( file.toPath().getParent() );
			Files.write( file.toPath(), contents.getBytes( StandardCharsets.UTF_8 ) );
		}
		catch (IOException e) {
			throw new GradleException( "Unable to write validation report " + file.getAbsolutePath(), e );
		}
	}
}
