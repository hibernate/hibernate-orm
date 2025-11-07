/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.maven;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.export.ExporterFactory;
import org.hibernate.tool.reveng.api.export.ExporterType;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;

import java.io.File;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;

/**
 * Mojo to generate hbm.xml files from an existing database.
 * <p>
 * See: https://docs.jboss.org/tools/latest/en/hibernatetools/html_single/#d0e4821
 */
@Mojo(
	name = "generateHbm",
	defaultPhase = GENERATE_SOURCES,
	requiresDependencyResolution = ResolutionScope.RUNTIME)
public class GenerateHbmMojo extends AbstractGenerationMojo {

	/** The directory into which the DAOs will be generated. */
	@Parameter(defaultValue = "${project.basedir}/src/main/resources")
	private File outputDirectory;

	@Parameter
	private String templatePath;

	protected void executeExporter(MetadataDescriptor metadataDescriptor) throws MojoFailureException {
		try {
			Exporter hbmExporter = ExporterFactory.createExporter(ExporterType.HBM);
			hbmExporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
			hbmExporter.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDirectory);
			if (templatePath != null) {
				getLog().info("Setting template path to: " + templatePath);
				hbmExporter.getProperties().put(ExporterConstants.TEMPLATE_PATH, new String[] {templatePath});
			}
			getLog().info("Starting HBM export to directory: " + outputDirectory + "...");
			hbmExporter.start();
		}
		catch (Exception e) {
			throw new MojoFailureException( e );
		}
	}


}
