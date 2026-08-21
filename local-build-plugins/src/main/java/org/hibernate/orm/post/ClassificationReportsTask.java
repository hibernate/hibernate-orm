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

import static org.hibernate.orm.post.ClassificationModel.Category.INTERNAL;
import static org.hibernate.orm.post.ClassificationModel.ClassificationStatus.RESOLVED;
import static org.hibernate.orm.post.ReportGenerationPlugin.TASK_GROUP_NAME;

/// Generates every human classification and lifecycle projection from one
/// parse of the canonical classification metadata.
///
/// @author Steve Ebersole
public abstract class ClassificationReportsTask extends DefaultTask {
	private final Provider<ClassificationMetadataManager> metadataManager;
	private final RegularFileProperty classificationMetadataFile;
	private final RegularFileProperty spiReportFile;
	private final RegularFileProperty internalsReportFile;
	private final RegularFileProperty incubationReportFile;
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
		spiReportFile = reportFile( "spi.txt" );
		internalsReportFile = reportFile( "internal.txt" );
		incubationReportFile = reportFile( "incubating.txt" );
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
	public Provider<RegularFile> getSpiReportFileReference() {
		return spiReportFile;
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
		write( spiReportFile.get().getAsFile(), new SpiReportRenderer().render( metadata ) );
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
		write(
				deprecationReportFile.get().getAsFile(),
				renderer.render( model, (element) -> element.getLifecycle().isDeprecated(), "# All elements considered deprecated" )
		);
		write(
				removalReportFile.get().getAsFile(),
				renderer.render( model, (element) -> element.getLifecycle().isRemoval(), "# All elements scheduled for removal" )
		);
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
