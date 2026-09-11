/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

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

import static org.hibernate.orm.post.ClassificationModel.Category.INTERNAL;
import static org.hibernate.orm.post.ClassificationModel.ClassificationStatus.RESOLVED;
import static org.hibernate.orm.post.ReportGenerationPlugin.TASK_GROUP_NAME;

/// Generates every human classification and lifecycle projection from one
/// parse of the canonical classification metadata.
///
/// @author Steve Ebersole
public abstract class ClassificationReportsTask extends DefaultTask {
	private static final Pattern RELEASE_FAMILY = Pattern.compile( "[0-9]+\\.[0-9]+" );

	private final Provider<ClassificationMetadataManager> metadataManager;
	private final RegularFileProperty classificationMetadataFile;
	private final RegularFileProperty internalsReportFile;
	private final RegularFileProperty incubationReportFile;
	private final RegularFileProperty incubationReviewReportFile;
	private final RegularFileProperty deprecationReportFile;
	private final RegularFileProperty removalReportFile;

	public ClassificationReportsTask() {
		setGroup( TASK_GROUP_NAME );
		setDescription( "Generates all human classification and lifecycle reports" );
		metadataManager = getProject().provider(
				() -> getProject().getExtensions().getByType( ClassificationMetadataManager.class )
		);
		classificationMetadataFile = getProject().getObjects().fileProperty();
		classificationMetadataFile.convention(
				getProject().getLayout().getBuildDirectory().file( "orm/reports/classifications.json" )
		);
		internalsReportFile = reportFile( "internal.txt" );
		incubationReportFile = reportFile( "incubating.txt" );
		incubationReviewReportFile = reportFile( "incubating-review.adoc" );
		deprecationReportFile = reportFile( "deprecated.txt" );
		removalReportFile = reportFile( "removal.txt" );
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

	@OutputFile
	public Provider<RegularFile> getInternalsReportFileReference() {
		return internalsReportFile;
	}

	@OutputFile
	public Provider<RegularFile> getIncubationReportFileReference() {
		return incubationReportFile;
	}

	@OutputFile
	public Provider<RegularFile> getIncubationReviewReportFileReference() {
		return incubationReviewReportFile;
	}

	@OutputFile
	public Provider<RegularFile> getDeprecationReportFileReference() {
		return deprecationReportFile;
	}

	@OutputFile
	public Provider<RegularFile> getRemovalReportFileReference() {
		return removalReportFile;
	}

	@TaskAction
	public void generateReports() {
		final ClassificationMetadata metadata = getMetadataManager().getMetadata(
				classificationMetadataFile.get().getAsFile().toPath()
		);
		final ClassificationModel model = metadata.getModel();
		final ClassificationReportRenderer renderer = new ClassificationReportRenderer();
		write(
				internalsReportFile.get().getAsFile(),
				renderer.render(
						model,
						(element) -> element.getClassificationStatus() == RESOLVED && element.getCategory() == INTERNAL,
						"# All elements considered internal for Hibernate's own use"
				)
		);
		write(
				incubationReportFile.get().getAsFile(),
				renderer.render( model, (element) -> element.getLifecycle().isIncubating(), "# All elements considered incubating" )
		);
		write( incubationReviewReportFile.get().getAsFile(), renderIncubationReview( model, renderer ) );
		write(
				deprecationReportFile.get().getAsFile(),
				renderer.render( model, (element) -> element.getLifecycle().isDeprecated(), "# All elements considered deprecated" )
		);
		write(
				removalReportFile.get().getAsFile(),
				renderer.render( model, (element) -> element.getLifecycle().isRemoval(), "# All elements scheduled for removal" )
		);
	}

	static String renderIncubationReview(
			ClassificationModel model,
			ClassificationReportRenderer renderer) {
		final SortedSet<IncubationCoordinate> coordinates = new TreeSet<>();
		for ( ClassificationModel.Element element : model.getElements() ) {
			for ( ClassificationModel.LifecycleOrigin origin : element.getLifecycle().getOrigins() ) {
				if ( origin.getState() == ClassificationModel.LifecycleState.INCUBATING ) {
					coordinates.add( IncubationCoordinate.from( origin ) );
				}
			}
		}

		final StringBuilder report = new StringBuilder( "= Incubating review by version and group\n" );
		String currentSince = null;
		for ( IncubationCoordinate coordinate : coordinates ) {
			if ( !coordinate.since.equals( currentSince ) ) {
				report.append( "\n== Since " ).append( coordinate.since ).append( '\n' );
				currentSince = coordinate.since;
			}
			report.append( "\n=== " )
					.append( coordinate.group == null ? "Ungrouped" : "Group: " + coordinate.group )
					.append( '\n' );
			for ( ClassificationReportRenderer.ProjectionEntry entry : renderer.project(
					model,
					element -> hasIncubationCoordinate( element, coordinate )
			) ) {
				report.append( "* " ).append( entry.getPath() );
				if ( entry.isPackage() ) {
					report.append( ".*" );
				}
				report.append( '\n' );
			}
		}
		return report.toString();
	}

	private static boolean hasIncubationCoordinate(
			ClassificationModel.Element element,
			IncubationCoordinate coordinate) {
		for ( ClassificationModel.LifecycleOrigin origin : element.getLifecycle().getOrigins() ) {
			if ( origin.getState() == ClassificationModel.LifecycleState.INCUBATING
					&& coordinate.matches( origin ) ) {
				return true;
			}
		}
		return false;
	}

	private static final class IncubationCoordinate implements Comparable<IncubationCoordinate> {
		private final String since;
		private final String group;

		private IncubationCoordinate(String since, String group) {
			this.since = since;
			this.group = group;
		}

		private static IncubationCoordinate from(ClassificationModel.LifecycleOrigin origin) {
			return new IncubationCoordinate(
					origin.getSince() == null ? "unknown" : origin.getSince(),
					normalizeGroup( origin.getGroup() )
			);
		}

		private boolean matches(ClassificationModel.LifecycleOrigin origin) {
			return since.equals( origin.getSince() == null ? "unknown" : origin.getSince() )
					&& compareNullable( group, normalizeGroup( origin.getGroup() ) ) == 0;
		}

		@Override
		public int compareTo(IncubationCoordinate other) {
			final int versionComparison = compareVersions( since, other.since );
			return versionComparison == 0 ? compareNullable( group, other.group ) : versionComparison;
		}

		private static String normalizeGroup(String group) {
			return group == null || group.isEmpty() ? null : group;
		}

		private static int compareVersions(String first, String second) {
			if ( "unknown".equals( first ) ) {
				return "unknown".equals( second ) ? 0 : 1;
			}
			if ( "unknown".equals( second ) ) {
				return -1;
			}
			final boolean firstValid = RELEASE_FAMILY.matcher( first ).matches();
			final boolean secondValid = RELEASE_FAMILY.matcher( second ).matches();
			if ( !firstValid || !secondValid ) {
				if ( firstValid != secondValid ) {
					return firstValid ? -1 : 1;
				}
				return first.compareTo( second );
			}
			final String[] firstParts = first.split( "\\.", -1 );
			final String[] secondParts = second.split( "\\.", -1 );
			int comparison = new BigInteger( firstParts[0] ).compareTo( new BigInteger( secondParts[0] ) );
			if ( comparison == 0 ) {
				comparison = new BigInteger( firstParts[1] ).compareTo( new BigInteger( secondParts[1] ) );
			}
			return comparison == 0 ? first.compareTo( second ) : comparison;
		}

		private static int compareNullable(String first, String second) {
			if ( first == null ) {
				return second == null ? 0 : 1;
			}
			return second == null ? -1 : first.compareTo( second );
		}
	}

	private RegularFileProperty reportFile(String name) {
		final RegularFileProperty file = getProject().getObjects().fileProperty();
		file.convention( getProject().getLayout().getBuildDirectory().file( "orm/reports/" + name ) );
		return file;
	}

	private static void write(File file, String contents) {
		try {
			Files.createDirectories( file.toPath().getParent() );
			Files.writeString( file.toPath(), contents, StandardCharsets.UTF_8 );
		}
		catch (IOException e) {
			throw new GradleException( "Unable to write classification report " + file.getAbsolutePath(), e );
		}
	}
}
