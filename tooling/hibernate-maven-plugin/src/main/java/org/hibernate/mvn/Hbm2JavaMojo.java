package org.hibernate.mvn;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;

import java.io.File;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataDescriptor;

/**
 * Mojo to generate Java JPA Entities from an existing database.
 * <p>
 * See: https://docs.jboss.org/tools/latest/en/hibernatetools/html_single/#d0e4821
 */
@Mojo(name = "hbm2java", defaultPhase = GENERATE_SOURCES)
public class Hbm2JavaMojo extends AbstractHbm2xMojo {

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/")
    private File outputDirectory;
    @Parameter(defaultValue = "false")
    private boolean ejb3;
    @Parameter(defaultValue = "false")
    private boolean jdk5;
    @Parameter
    private String templatePath;

    protected void executeExporter(MetadataDescriptor metadataDescriptor) {
        Exporter pojoExporter = ExporterFactory.createExporter(ExporterType.POJO);
        pojoExporter.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
        pojoExporter.getProperties().put(ExporterConstants.OUTPUT_FOLDER, outputDirectory);
        if (templatePath != null) {
            getLog().info("Setting template path to: " + templatePath);
            pojoExporter.getProperties().put(ExporterConstants.TEMPLATE_PATH, new String[] {templatePath});
        }
        pojoExporter.getProperties().setProperty("ejb3", String.valueOf(ejb3));
        pojoExporter.getProperties().setProperty("jdk5", String.valueOf(jdk5));
        getLog().info("Starting POJO export to directory: " + outputDirectory + "...");
        pojoExporter.start();
    }


}
