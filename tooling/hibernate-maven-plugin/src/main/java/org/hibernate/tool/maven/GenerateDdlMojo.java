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
import org.hibernate.boot.Metadata;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import java.io.File;
import java.util.EnumSet;
import java.util.Set;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_RESOURCES;

/**
 * Mojo to generate DDL Scripts from an existing database.
 * <p>
 * See https://docs.jboss.org/tools/latest/en/hibernatetools/html_single/#d0e4651
 */
@Mojo(
	name = "hbm2ddl", 
	defaultPhase = GENERATE_RESOURCES,
	requiresDependencyResolution = ResolutionScope.RUNTIME)
public class GenerateDdlMojo extends AbstractGenerationMojo {

    /** The directory into which the DDLs will be generated. */
    @Parameter(defaultValue = "${project.build.directory}/generated-resources/")
    private File outputDirectory;

    /** The default filename of the generated DDL script. */
    @Parameter(defaultValue = "schema.ddl")
    private String outputFileName;

    /** The type of output to produce.
     * <ul>
     *   <li>DATABASE: Export to the database.</li>
     *   <li>SCRIPT (default): Write to a script file.</li>
     *   <li>STDOUT: Write to {@link System#out}.</li>
     * </ul> */
    @Parameter(defaultValue = "SCRIPT")
    private Set<TargetType> targetTypes;

    /**
     * The DDLs statements to create.
     * <ul>
     *   <li>NONE: None - duh :P.</li>
     *   <li>CREATE (default): Create only.</li>
     *   <li>DROP: Drop only.</li>
     *   <li>BOTH: Drop and then create.</li>
     * </ul>
     */
    @Parameter(defaultValue = "CREATE")
    private SchemaExport.Action schemaExportAction;

    /** Set the end of statement delimiter. */
    @Parameter(defaultValue = ";")
    private String delimiter;

    /** Should we format the sql strings? */
    @Parameter(defaultValue = "true")
    private boolean format;
 
    /** Should we stop once an error occurs? */
    @Parameter(defaultValue = "true")
    private boolean haltOnError;


    @Override
    protected void executeExporter(MetadataDescriptor metadataDescriptor) {
        Metadata metadata = metadataDescriptor.createMetadata();

        SchemaExport export = new SchemaExport();
        export.setOutputFile(new File(outputDirectory, outputFileName).toString());
        export.setDelimiter(delimiter);
        export.setHaltOnError(haltOnError);
        export.setFormat(format);
        export.execute(EnumSet.copyOf(this.targetTypes), schemaExportAction, metadata);
    }
}
