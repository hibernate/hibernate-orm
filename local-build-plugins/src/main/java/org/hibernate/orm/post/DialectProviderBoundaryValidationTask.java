/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

import static org.hibernate.orm.post.ReportGenerationPlugin.TASK_GROUP_NAME;

/// Validates compiled Dialect-provider code against the supported Hibernate
/// classification and implementation boundaries.
///
/// This repository task is deliberately explicit and is not part of the
/// normal `check` lifecycle.
///
/// @author Steve Ebersole
public abstract class DialectProviderBoundaryValidationTask extends DefaultTask {
	private final RegularFileProperty classificationMetadataFile;
	private final ConfigurableFileCollection providerArtifacts;
	private final ConfigurableFileCollection upstreamArtifacts;
	private final ConfigurableFileCollection engineClasspath;
	private final ListProperty<String> providerPackagePrefixes;
	private final RegularFileProperty reportFile;
	private final RegularFileProperty jsonReportFile;

	public DialectProviderBoundaryValidationTask() {
		setGroup( TASK_GROUP_NAME );
		setDescription( "Validates compiled Dialect-provider dependencies and override points" );
		classificationMetadataFile = getProject().getObjects().fileProperty();
		classificationMetadataFile.convention(
				getProject().getLayout().getBuildDirectory().file( "orm/reports/classifications.json" )
		);
		providerArtifacts = getProject().getObjects().fileCollection();
		upstreamArtifacts = getProject().getObjects().fileCollection();
		engineClasspath = getProject().getObjects().fileCollection();
		providerPackagePrefixes = getProject().getObjects().listProperty( String.class );
		providerPackagePrefixes.convention( java.util.List.of( "org.hibernate.community." ) );
		reportFile = getProject().getObjects().fileProperty();
		reportFile.convention(
				getProject().getLayout().getBuildDirectory().file( "orm/reports/dialect-provider-boundary-validation.txt" )
		);
		jsonReportFile = getProject().getObjects().fileProperty();
		jsonReportFile.convention(
				getProject().getLayout().getBuildDirectory().file( "orm/reports/dialect-provider-boundary-validation.json" )
		);
	}

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public Provider<RegularFile> getClassificationMetadataFileReference() {
		return classificationMetadataFile;
	}

	@Classpath
	public ConfigurableFileCollection getProviderArtifacts() {
		return providerArtifacts;
	}

	@Classpath
	public ConfigurableFileCollection getUpstreamArtifacts() {
		return upstreamArtifacts;
	}

	@Classpath
	public ConfigurableFileCollection getEngineClasspath() {
		return engineClasspath;
	}

	@Input
	public ListProperty<String> getProviderPackagePrefixes() {
		return providerPackagePrefixes;
	}

	@OutputFile
	public Provider<RegularFile> getReportFileReference() {
		return reportFile;
	}

	@OutputFile
	public Provider<RegularFile> getJsonReportFileReference() {
		return jsonReportFile;
	}

	@TaskAction
	public void validate() {
		final List<String> arguments = new ArrayList<>();
		arguments.addAll( List.of(
				"validate",
				"--metadata", classificationMetadataFile.get().getAsFile().getAbsolutePath(),
				"--text-report", reportFile.get().getAsFile().getAbsolutePath(),
				"--json-report", jsonReportFile.get().getAsFile().getAbsolutePath()
		) );
		for ( String providerPackage : providerPackagePrefixes.get() ) {
			arguments.addAll( List.of( "--provider-package", providerPackage ) );
		}
		providerArtifacts.getFiles().stream()
				.sorted( Comparator.comparing( java.io.File::getAbsolutePath ) )
				.forEach( artifact -> arguments.addAll( List.of( "--provider", artifact.getAbsolutePath() ) ) );
		upstreamArtifacts.getFiles().stream()
				.sorted( Comparator.comparing( java.io.File::getAbsolutePath ) )
				.forEach( artifact -> arguments.addAll( List.of( "--upstream", artifact.getAbsolutePath() ) ) );
		getExecOperations().javaexec( spec -> {
			spec.classpath( engineClasspath );
			spec.getMainClass().set( "org.hibernate.orm.tooling.dialectprovider.internal.ProviderBoundaryRunner" );
			spec.args( arguments );
		} ).rethrowFailure();
	}

	@Inject
	protected abstract ExecOperations getExecOperations();
}
