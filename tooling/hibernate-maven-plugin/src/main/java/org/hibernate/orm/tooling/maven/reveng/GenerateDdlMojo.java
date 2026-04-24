/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.maven.reveng;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.reveng.internal.exporter.ddl.DdlExporter;
import org.hibernate.tool.schema.TargetType;

import java.io.File;
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
		DdlExporter ddl = DdlExporter.create(metadataDescriptor)
				.delimiter(delimiter)
				.format(format)
				.haltOnError(haltOnError);
		File outputFile = new File(outputDirectory, outputFileName);
		outputFile.getParentFile().mkdirs();
		boolean toScript = targetTypes.contains(TargetType.SCRIPT);
		boolean toDatabase = targetTypes.contains(TargetType.DATABASE);
		switch (schemaExportAction) {
			case CREATE:
				if (toScript) ddl.exportCreateDdl(outputFile);
				if (toDatabase) ddl.executeCreateDdl();
				break;
			case DROP:
				if (toScript) ddl.exportDropDdl(outputFile);
				if (toDatabase) ddl.executeDropDdl();
				break;
			case BOTH:
				if (toScript) ddl.exportBothDdl(outputFile);
				if (toDatabase) ddl.executeBothDdl();
				break;
			case NONE:
				break;
		}
	}
}
