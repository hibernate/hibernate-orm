/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven.reveng;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.exporter.hbm.HbmXmlExporter;

import java.io.File;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;

/**
 * Mojo to generate hbm.xml files from an existing database.
 * <p>
 * See: https://docs.jboss.org/tools/latest/en/hibernatetools/html_single/#d0e4821
 */
@Deprecated(forRemoval = true)
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

	protected void executeExporter(MetadataDescriptor metadataDescriptor) {
		getLog().warn( "The generateHbm goal is deprecated and will be removed in a future version. "
				+ "Use the hbm2java goal to generate annotated Java entities instead." );
		String[] tPath = templatePath != null
				? new String[] { templatePath } : new String[0];
		getLog().info("Starting HBM export to directory: "
				+ outputDirectory + "...");
		HbmXmlExporter.create(metadataDescriptor, tPath)
				.exportAll(outputDirectory);
	}

}
