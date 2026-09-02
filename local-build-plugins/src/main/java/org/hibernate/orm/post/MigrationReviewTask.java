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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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

import static org.hibernate.orm.post.ClassificationModel.Category.API;
import static org.hibernate.orm.post.ClassificationModel.Category.SPI;
import static org.hibernate.orm.post.ClassificationModel.ElementKind.PACKAGE;
import static org.hibernate.orm.post.ReportGenerationPlugin.TASK_GROUP_NAME;

/// Generates a non-gating API and SPI migration-guide review.
///
/// @author Steve Ebersole
/// @since 8.0
public abstract class MigrationReviewTask extends DefaultTask {
	private final Property<String> reviewFamily;
	private final RegularFileProperty baselineClassificationMetadataFile;
	private final RegularFileProperty currentClassificationMetadataFile;
	private final ConfigurableFileCollection baselineArtifacts;
	private final ConfigurableFileCollection currentArtifacts;
	private final RegularFileProperty reportFile;

	public MigrationReviewTask() {
		setGroup( TASK_GROUP_NAME );
		setDescription( "Generates an advisory API and SPI migration review" );
		reviewFamily = getProject().getObjects().property( String.class );
		baselineClassificationMetadataFile = getProject().getObjects().fileProperty();
		currentClassificationMetadataFile = getProject().getObjects().fileProperty();
		currentClassificationMetadataFile.convention(
				getProject().getLayout().getBuildDirectory().file( "orm/reports/classifications.json" )
		);
		baselineArtifacts = getProject().getObjects().fileCollection();
		currentArtifacts = getProject().getObjects().fileCollection();
		reportFile = getProject().getObjects().fileProperty();
		reportFile.convention( getProject().getLayout().getBuildDirectory().file( "orm/reports/migration-review.txt" ) );
	}

	@Input
	@Optional
	public Property<String> getReviewFamily() {
		return reviewFamily;
	}

	@InputFile
	@Optional
	@PathSensitive(PathSensitivity.NONE)
	public RegularFileProperty getBaselineClassificationMetadataFile() {
		return baselineClassificationMetadataFile;
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
	public void generateReview() {
		if ( !reviewFamily.isPresent() ) {
			configurationFailure( "No migration review family is configured" );
		}
		if ( !baselineClassificationMetadataFile.isPresent()
				|| !baselineClassificationMetadataFile.get().getAsFile().isFile() ) {
			configurationFailure( "No classification metadata was resolved for review family " + reviewFamily.get() );
		}
		if ( baselineArtifacts.isEmpty() || currentArtifacts.isEmpty() ) {
			configurationFailure( "Migration review requires both baseline and current artifacts" );
		}

		try {
			final ClassificationMetadataJson json = new ClassificationMetadataJson();
			final ClassificationMetadata baseline = json.read(
					baselineClassificationMetadataFile.get().getAsFile().toPath()
			);
			final ClassificationMetadata current = json.read(
					currentClassificationMetadataFile.get().getAsFile().toPath()
			);
			final Set<String> types = new TreeSet<>();
			collectTypes( baseline, types );
			collectTypes( current, types );
			final JavaMigrationCompatibilityAnalyzer.Analysis javaAnalysis = new JavaMigrationCompatibilityAnalyzer().analyze(
					baselineArtifacts.getFiles(),
					currentArtifacts.getFiles(),
					types
			);
			write( render( baseline, current, javaAnalysis ) );
		}
		catch (IllegalArgumentException e) {
			configurationFailure( e.getMessage() );
		}
	}

	private static String render(
			ClassificationMetadata baseline,
			ClassificationMetadata current,
			JavaMigrationCompatibilityAnalyzer.Analysis javaAnalysis) {
		final StringBuilder report = new StringBuilder();
		report.append( "Hibernate ORM classification migration review\n" )
				.append( "Migration review — not compatibility enforcement\n\n" )
				.append( "Review family: " ).append( baseline.getHibernateVersion() ).append( '\n' )
				.append( "Baseline source version: " ).append( baseline.getSourceVersion() ).append( '\n' )
				.append( "Current family: " ).append( current.getHibernateVersion() ).append( '\n' )
				.append( "Current source version: " ).append( current.getSourceVersion() ).append( "\n\n" );

		final Map<String, ClassificationModel.Element> oldElements = publicElements( baseline );
		final Map<String, ClassificationModel.Element> newElements = publicElements( current );
		final Set<String> ids = new TreeSet<>( oldElements.keySet() );
		ids.addAll( newElements.keySet() );
		int findings = 0;
		final StringBuilder details = new StringBuilder();
		for ( String id : ids ) {
			final ClassificationModel.Element oldElement = oldElements.get( id );
			final ClassificationModel.Element newElement = newElements.get( id );
			if ( oldElement == null ) {
				findings++;
				append( details, "DECLARATION_ADDED", id, "absent -> " + description( newElement ) );
			}
			else if ( newElement == null ) {
				findings++;
				append(
						details,
						oldElement.getLifecycle().isDeprecated() || oldElement.getLifecycle().isForRemoval()
								? "DECLARATION_REMOVED" : "REMOVED_WITHOUT_PRIOR_DEPRECATION",
						id,
						description( oldElement ) + " -> absent"
				);
			}
			else {
				if ( oldElement.getCategory() != newElement.getCategory() ) {
					findings++;
					append( details, "CLASSIFICATION_CHANGED", id, oldElement.getCategory() + " -> " + newElement.getCategory() );
				}
				if ( !oldElement.getEffectiveRoles().equals( newElement.getEffectiveRoles() ) ) {
					findings++;
					append( details, "SPI_ROLES_CHANGED", id, oldElement.getEffectiveRoles() + " -> " + newElement.getEffectiveRoles() );
				}
				if ( !lifecycle( oldElement ).equals( lifecycle( newElement ) ) ) {
					findings++;
					append( details, "LIFECYCLE_CHANGED", id, lifecycle( oldElement ) + " -> " + lifecycle( newElement ) );
				}
			}
		}
		for ( JavaMigrationCompatibilityAnalyzer.Change change : javaAnalysis.getChanges() ) {
			if ( relevant( baseline, current, change ) ) {
				findings++;
				append(
						details,
						"JAVA_" + change.getCause(),
						change.getElementId(),
						String.valueOf( change.getBaselineValue() ) + " -> " + String.valueOf( change.getCurrentValue() )
				);
			}
		}
		report.append( "Review findings: " ).append( findings ).append( "\n\n" );
		return findings == 0
				? report.append( "No migration-review findings.\n" ).toString()
				: report.append( details ).toString();
	}

	private static Map<String, ClassificationModel.Element> publicElements(ClassificationMetadata metadata) {
		final Map<String, ClassificationModel.Element> result = new TreeMap<>();
		for ( ClassificationModel.Element element : metadata.getModel().getElements() ) {
			if ( metadata.isMigrationCompatibilityElement( element )
					&& element.getKind() != PACKAGE
					&& supported( element ) ) {
				result.put( element.getId(), element );
			}
		}
		return result;
	}

	private static boolean relevant(
			ClassificationMetadata baseline,
			ClassificationMetadata current,
			JavaMigrationCompatibilityAnalyzer.Change change) {
		final ClassificationModel.Element baselineElement = subject( baseline.getModel(), change );
		final ClassificationModel.Element currentElement = subject( current.getModel(), change );
		return supported( baselineElement ) && baseline.isMigrationCompatibilityElement( baselineElement )
				|| supported( currentElement ) && current.isMigrationCompatibilityElement( currentElement );
	}

	private static ClassificationModel.Element subject(
			ClassificationModel model,
			JavaMigrationCompatibilityAnalyzer.Change change) {
		final ClassificationModel.Element exact = model.getElement( change.getElementId() );
		return exact == null ? model.getElement( change.getOwnerId() ) : exact;
	}

	private static boolean supported(ClassificationModel.Element element) {
		return element != null && (element.getCategory() == API || element.getCategory() == SPI);
	}

	private static String description(ClassificationModel.Element element) {
		return element.getCategory() + " roles=" + element.getEffectiveRoles() + " lifecycle=" + lifecycle( element );
	}

	private static String lifecycle(ClassificationModel.Element element) {
		return "incubating=" + element.getLifecycle().isIncubating()
				+ ",deprecated=" + element.getLifecycle().isDeprecated()
				+ ",forRemoval=" + element.getLifecycle().isForRemoval()
				+ ",removal=" + element.getLifecycle().isRemoval();
	}

	private static void append(StringBuilder report, String cause, String id, String change) {
		report.append( cause ).append( '\n' )
				.append( "  Element: " ).append( id ).append( '\n' )
				.append( "  Change: " ).append( change ).append( "\n\n" );
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

	private void configurationFailure(String message) {
		write( "Hibernate ORM classification migration review: FAILED\n\nConfiguration error: " + message + '\n' );
		throw new GradleException( "Invalid migration review input; see " + reportFile.get().getAsFile() );
	}

	private void write(String contents) {
		final File file = reportFile.get().getAsFile();
		try {
			Files.createDirectories( file.toPath().getParent() );
			Files.write( file.toPath(), contents.getBytes( StandardCharsets.UTF_8 ) );
		}
		catch (IOException e) {
			throw new GradleException( "Unable to write migration review " + file, e );
		}
	}
}
