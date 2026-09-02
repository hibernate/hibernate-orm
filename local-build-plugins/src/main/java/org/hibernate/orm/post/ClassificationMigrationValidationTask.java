/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import static org.hibernate.orm.post.ReportGenerationPlugin.TASK_GROUP_NAME;

/// Validates API and role-sensitive SPI migration compatibility against an
/// explicitly configured baseline metadata document and artifact set.
///
/// The task is intentionally not attached to a build lifecycle. Establishing
/// or changing the reviewed baseline remains an explicit release action.
///
/// @author Steve Ebersole
public abstract class ClassificationMigrationValidationTask extends DefaultTask {
	private final RegularFileProperty baselineClassificationMetadataFile;
	private final Property<String> baselineFamily;
	private final Property<Boolean> bootstrapBaseline;
	private final RegularFileProperty currentClassificationMetadataFile;
	private final ConfigurableFileCollection baselineArtifacts;
	private final ConfigurableFileCollection currentArtifacts;
	private final RegularFileProperty reportFile;

	public ClassificationMigrationValidationTask() {
		setGroup( TASK_GROUP_NAME );
		setDescription( "Validates API and role-sensitive SPI migration compatibility" );
		baselineClassificationMetadataFile = getProject().getObjects().fileProperty();
		baselineFamily = getProject().getObjects().property( String.class );
		bootstrapBaseline = getProject().getObjects().property( Boolean.class );
		bootstrapBaseline.convention( false );
		currentClassificationMetadataFile = getProject().getObjects().fileProperty();
		currentClassificationMetadataFile.convention(
				getProject().getLayout().getBuildDirectory().file( "orm/reports/classifications.json" )
		);
		baselineArtifacts = getProject().getObjects().fileCollection();
		currentArtifacts = getProject().getObjects().fileCollection();
		reportFile = getProject().getObjects().fileProperty();
		reportFile.convention(
				getProject().getLayout().getBuildDirectory().file( "orm/reports/migration-compatibility-validation.txt" )
		);
	}

	@InputFile
	@Optional
	@PathSensitive(PathSensitivity.RELATIVE)
	public RegularFileProperty getBaselineClassificationMetadataFile() {
		return baselineClassificationMetadataFile;
	}

	@Input
	@Optional
	public Property<String> getBaselineFamily() {
		return baselineFamily;
	}

	@Input
	public Property<Boolean> getBootstrapBaseline() {
		return bootstrapBaseline;
	}

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public RegularFileProperty getCurrentClassificationMetadataFile() {
		return currentClassificationMetadataFile;
	}

	@Classpath
	public ConfigurableFileCollection getBaselineArtifacts() {
		return baselineArtifacts;
	}

	@Classpath
	public ConfigurableFileCollection getCurrentArtifacts() {
		return currentArtifacts;
	}

	@OutputFile
	public RegularFileProperty getReportFile() {
		return reportFile;
	}

	@TaskAction
	public void validateMigration() {
		if ( !baselineClassificationMetadataFile.isPresent()
				|| !baselineClassificationMetadataFile.get().getAsFile().isFile() ) {
			if ( baselineFamily.isPresent() && !bootstrapBaseline.get() ) {
				write( "Hibernate ORM migration compatibility: FAILED\n\nConfiguration error: no published baseline was resolved for "
						+ baselineFamily.get() + '\n' );
				throw new GradleException( "No migration compatibility baseline was resolved for " + baselineFamily.get() );
			}
			final String reason = baselineFamily.isPresent()
					? "first published classification baseline for family " + baselineFamily.get()
					: "the current release begins a new major compatibility horizon";
			write( "Hibernate ORM migration compatibility: NOT_APPLICABLE\n\nReason: " + reason + '\n' );
			return;
		}
		if ( bootstrapBaseline.get() ) {
			write( "Hibernate ORM migration compatibility: FAILED\n\nConfiguration error: bootstrap was requested but a baseline already exists\n" );
			throw new GradleException( "Cannot bootstrap an established migration compatibility baseline" );
		}
		if ( baselineArtifacts.isEmpty() ) {
			throw new GradleException( "No baseline artifacts configured for " + getPath() );
		}
		if ( currentArtifacts.isEmpty() ) {
			throw new GradleException( "No current artifacts configured for " + getPath() );
		}
		final ClassificationMigrationValidator.Result result;
		try {
			final ClassificationMetadataJson metadataJson = new ClassificationMetadataJson();
			final ClassificationMetadata baseline = metadataJson.read(
					baselineClassificationMetadataFile.get().getAsFile().toPath()
			);
			final ClassificationMetadata current = metadataJson.read(
					currentClassificationMetadataFile.get().getAsFile().toPath()
			);
			final Set<String> classifiedTypes = new TreeSet<>();
			collectTypes( baseline, classifiedTypes );
			collectTypes( current, classifiedTypes );
			final JavaMigrationCompatibilityAnalyzer.Analysis javaAnalysis = new JavaMigrationCompatibilityAnalyzer().analyze(
					baselineArtifacts.getFiles(),
					currentArtifacts.getFiles(),
					classifiedTypes
			);
			result = new ClassificationMigrationValidator().validate( baseline, current, javaAnalysis );
		}
		catch (IllegalArgumentException e) {
			write( "Hibernate ORM migration compatibility: FAILED\n\nConfiguration error: " + e.getMessage() + '\n' );
			throw new GradleException( "Invalid classification migration input; see " + reportFile.get().getAsFile(), e );
		}
		write( new ClassificationMigrationReportRenderer().render( result ) );
		if ( result.hasFailures() ) {
			throw new GradleException( "Classification migration compatibility failed; see " + reportFile.get().getAsFile() );
		}
	}

	private static void collectTypes(ClassificationMetadata metadata, Collection<String> types) {
		for ( ClassificationModel.Element element : metadata.getModel().getElements() ) {
			if ( metadata.isMigrationCompatibilityElement( element )
					&& (element.getKind() == ClassificationModel.ElementKind.TYPE
					|| element.getKind() == ClassificationModel.ElementKind.ANNOTATION_TYPE) ) {
				types.add( element.getId() );
			}
		}
	}

	private void write(String contents) {
		final File file = reportFile.get().getAsFile();
		try {
			Files.createDirectories( file.toPath().getParent() );
			Files.write( file.toPath(), contents.getBytes( StandardCharsets.UTF_8 ) );
		}
		catch (IOException e) {
			throw new GradleException( "Unable to write migration compatibility report " + file, e );
		}
	}
}
