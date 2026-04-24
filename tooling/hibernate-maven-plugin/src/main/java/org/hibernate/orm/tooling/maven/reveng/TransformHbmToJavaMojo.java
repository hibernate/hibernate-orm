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
 * Mojo to generate annotated Java entity classes from existing
 * hbm.xml or mapping.xml files.
 *
 * <p>Unlike {@code hbm2java} which reverse-engineers a live database,
 * this goal reads mapping files and uses Hibernate's metadata model
 * to produce the entities.</p>
 */
@Mojo(
	name = "hbm2java-native",
	defaultPhase = GENERATE_SOURCES,
	requiresDependencyResolution = ResolutionScope.RUNTIME)
public class TransformHbmToJavaMojo extends AbstractNativeMojo {

	/** The directory into which the Java entities will be generated. */
	@Parameter(defaultValue = "${project.build.directory}/generated-sources")
	private File outputDirectory;

	/** Generate JPA annotations (@Entity, @Column, etc.). */
	@Parameter(defaultValue = "true")
	private boolean ejb3;

	/** Use Java generics (Set&lt;Item&gt; instead of raw Set). */
	@Parameter(defaultValue = "true")
	private boolean jdk5;

	/** A path used for looking up user-edited templates. */
	@Parameter
	private String templatePath;

	protected void executeExporter(MetadataDescriptor metadataDescriptor) {
		String[] tPath = templatePath != null
				? new String[] { templatePath } : new String[0];
		getLog().info("Starting entity export to directory: "
				+ outputDirectory + "...");
		EntityExporter.create(metadataDescriptor, ejb3, jdk5, tPath)
				.exportAll(outputDirectory);
	}

}
