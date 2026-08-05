/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.post;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/// Validates Hibernate's provider SPI classifications and supported signature
/// boundary from the aggregate Jandex index.
///
/// @author Steve Ebersole
public abstract class SpiValidationTask extends AbstractJandexAwareTask {
	private final RegularFileProperty reportFile;
	private final RegularFileProperty allowlistFile;

	public SpiValidationTask() {
		setDescription( "Validates the Hibernate provider SPI surface" );
		reportFile = getProject().getObjects().fileProperty();
		reportFile.convention( getProject().getLayout().getBuildDirectory().file( "orm/reports/spi-validation.txt" ) );
		allowlistFile = getProject().getObjects().fileProperty();
		allowlistFile.convention(
				getProject().getRootProject().getLayout().getProjectDirectory().file( "gradle/spi-validation-allowlist.json" )
		);
	}

	@Override
	protected Provider<RegularFile> getTaskReportFileReference() {
		return reportFile;
	}

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public Provider<RegularFile> getAllowlistFileReference() {
		return allowlistFile;
	}

	@TaskAction
	public void validateSpi() {
		final File report = getReportFileReference().get().getAsFile();
		final SpiValidationAllowlist allowlist;
		try {
			allowlist = SpiValidationAllowlist.read( getAllowlistFileReference().get().getAsFile() );
		}
		catch (IllegalArgumentException e) {
			write( report, "Hibernate ORM SPI validation: FAILED\n\nConfiguration error: " + e.getMessage() + '\n' );
			throw new GradleException( "Invalid SPI validation allowlist; see " + report.getAbsolutePath(), e );
		}

		final SpiModel model = new SpiJandexClassifier().classify( getIndexManager().getIndex() );
		final SpiValidator.Result result = new SpiValidator().validate(
				model,
				SpiValidator.Evidence.NONE,
				allowlist
		);
		write( report, new SpiValidationReportRenderer().render( result ) );
		if ( result.hasFailures() ) {
			throw new GradleException( "SPI validation failed; see " + report.getAbsolutePath() );
		}
	}

	private static void write(File file, String contents) {
		try {
			Files.createDirectories( file.toPath().getParent() );
			Files.write( file.toPath(), contents.getBytes( StandardCharsets.UTF_8 ) );
		}
		catch (IOException e) {
			throw new GradleException( "Unable to write SPI validation report " + file.getAbsolutePath(), e );
		}
	}
}
