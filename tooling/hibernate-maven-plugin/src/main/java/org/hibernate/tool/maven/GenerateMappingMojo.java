/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2016-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.maven;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_RESOURCES;

import java.io.File;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.reveng.models.exporter.mapping.MappingXmlExporter;

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
