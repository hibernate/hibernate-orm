package org.hibernate.mvn;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;

import java.io.File;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.export.pojo.POJOExporter;

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
        POJOExporter pojoExporter = new POJOExporter();
        pojoExporter.setMetadataDescriptor(metadataDescriptor);
        pojoExporter.setOutputDirectory(outputDirectory);
        if (templatePath != null) {
            getLog().info("Setting template path to: " + templatePath);
            pojoExporter.setTemplatePath(new String[]{templatePath});
        }
        pojoExporter.getProperties().setProperty("ejb3", String.valueOf(ejb3));
        pojoExporter.getProperties().setProperty("jdk5", String.valueOf(jdk5));
        getLog().info("Starting POJO export to directory: " + outputDirectory + "...");
        pojoExporter.start();
    }


}
