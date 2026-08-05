/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/// Generates the human-readable and machine-readable SPI inventory from the
/// aggregate Jandex index.
///
/// @author Steve Ebersole
public abstract class SpiReportTask extends AbstractJandexAwareTask {
	private final RegularFileProperty reportFile;
	private final RegularFileProperty jsonReportFile;

	public SpiReportTask() {
		setDescription( "Generates the Hibernate provider SPI reports" );
		reportFile = getProject().getObjects().fileProperty();
		reportFile.convention( getProject().getLayout().getBuildDirectory().file( "orm/reports/spi.txt" ) );
		jsonReportFile = getProject().getObjects().fileProperty();
		jsonReportFile.convention( getProject().getLayout().getBuildDirectory().file( "orm/reports/spi.json" ) );
	}

	@Override
	protected Provider<RegularFile> getTaskReportFileReference() {
		return reportFile;
	}

	@OutputFile
	public Provider<RegularFile> getJsonReportFileReference() {
		return jsonReportFile;
	}

	@TaskAction
	public void generateSpiReport() {
		final SpiModel model = new SpiJandexClassifier().classify( getIndexManager().getIndex() );
		final SpiReportRenderer renderer = new SpiReportRenderer();
		write( getReportFileReference().get().getAsFile(), renderer.renderAsciiDoc( model ) );
		write( getJsonReportFileReference().get().getAsFile(), renderer.renderJson( model ) );
	}

	private static void write(File file, String contents) {
		try {
			Files.createDirectories( file.toPath().getParent() );
			Files.write( file.toPath(), contents.getBytes( StandardCharsets.UTF_8 ) );
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to write SPI report " + file.getAbsolutePath(), e );
		}
	}
}
