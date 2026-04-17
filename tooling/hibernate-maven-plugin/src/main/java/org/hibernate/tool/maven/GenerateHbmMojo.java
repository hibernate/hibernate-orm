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

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.exporter.hbm.HbmXmlExporter;

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

    protected void executeExporter(MetadataDescriptor metadataDescriptor) {
        String[] tPath = templatePath != null
                ? new String[] { templatePath } : new String[0];
        getLog().info("Starting HBM export to directory: "
                + outputDirectory + "...");
        HbmXmlExporter.create(metadataDescriptor, tPath)
                .exportAll(outputDirectory);
    }

}
