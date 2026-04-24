/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven.reveng;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_RESOURCES;

import java.io.File;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.exporter.mapping.MappingXmlExporter;

/**
 * Mojo to generate JPA mapping.xml files from an existing database.
 */
@Mojo(
	name = "generateMapping",
	defaultPhase = GENERATE_RESOURCES,
	requiresDependencyResolution = ResolutionScope.RUNTIME)
public class GenerateMappingMojo extends AbstractGenerationMojo {

	/** The directory into which the mapping.xml files will be generated. */
	@Parameter(defaultValue = "${project.build.directory}/generated-resources")
	private File outputDirectory;

	/** A path used for looking up user-edited templates. */
	@Parameter
	private String templatePath;

	protected void executeExporter(MetadataDescriptor metadataDescriptor) {
		String[] tPath = templatePath != null
				? new String[] { templatePath } : new String[0];
		getLog().info("Starting mapping.xml export to directory: "
				+ outputDirectory + "...");
		MappingXmlExporter.create(metadataDescriptor, tPath)
				.exportAll(outputDirectory);
	}

}
