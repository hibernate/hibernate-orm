/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven.reveng;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;

import java.io.File;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.exporter.entity.EntityExporter;

/**
 * Mojo to generate Java JPA Entities from an existing database.
 * <p>
 * See: https://docs.jboss.org/tools/latest/en/hibernatetools/html_single/#d0e4821
 */
@Mojo(
	name = "hbm2java",
	defaultPhase = GENERATE_SOURCES,
	requiresDependencyResolution = ResolutionScope.RUNTIME)
public class GenerateJavaMojo extends AbstractGenerationMojo {

	/** The directory into which the JPA entities will be generated. */
	@Parameter(defaultValue = "${project.build.directory}/generated-sources/")
	private File outputDirectory;

	/** Code will contain JPA features, e.g. using annotations from jakarta.persistence
	 * and org.hibernate.annotations. */
	@Parameter(defaultValue = "true")
	private boolean ejb3;

	/** Code will contain JDK 5 constructs such as generics and static imports. */
	@Parameter(defaultValue = "true")
	private boolean jdk5;

	/** A path used for looking up user-edited templates. */
	@Parameter
	private String templatePath;

	protected void executeExporter(MetadataDescriptor metadataDescriptor) {
		String[] tPath = templatePath != null
				? new String[] { templatePath } : new String[0];
		getLog().info("Starting POJO export to directory: "
				+ outputDirectory + "...");
		EntityExporter.create(metadataDescriptor, ejb3, jdk5, tPath)
				.exportAll(outputDirectory);
	}

}
