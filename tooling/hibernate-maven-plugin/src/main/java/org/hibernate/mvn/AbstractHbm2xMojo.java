package org.hibernate.mvn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.BuildException;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.ReverseEngineeringSettings;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.hibernate.tool.internal.reveng.DefaultReverseEngineeringStrategy;
import org.hibernate.tool.internal.reveng.OverrideRepository;

public abstract class AbstractHbm2xMojo extends AbstractMojo {

    // For reveng strategy
    @Parameter
    private String packageName;
    @Parameter
    private File revengFile;
    /** The class name of the reverse engineering strategy to use.
     * Extend the DefaultReverseEngineeringStrategy and override the corresponding methods, e.g.
     * to adapt the generate class names or to provide custom type mappings. */
    @Parameter(defaultValue = "org.hibernate.cfg.reveng.DefaultReverseEngineeringStrategy")
    private String revengStrategy;
    @Parameter(defaultValue = "true")
    private boolean detectManyToMany;
    @Parameter(defaultValue = "true")
    private boolean detectOneToOne;
    @Parameter(defaultValue = "true")
    private boolean detectOptimisticLock;
    @Parameter(defaultValue = "true")
    private boolean createCollectionForForeignKey;
    @Parameter(defaultValue = "true")
    private boolean createManyToOneForForeignKey;

    // For configuration
    @Parameter(defaultValue = "${project.basedir}/src/main/hibernate/hibernate.properties")
    private File propertyFile;

    // Not exposed for now
    private boolean preferBasicCompositeIds = true;

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Starting " + this.getClass().getSimpleName() + "...");
        ReverseEngineeringStrategy strategy = setupReverseEngineeringStrategy();
        Properties properties = loadPropertiesFile();
        MetadataDescriptor jdbcDescriptor = createJdbcDescriptor(strategy, properties);
        executeExporter(jdbcDescriptor);
        getLog().info("Finished " + this.getClass().getSimpleName() + "!");
    }

    private ReverseEngineeringStrategy setupReverseEngineeringStrategy() {
        ReverseEngineeringStrategy strategy;
        try {
            strategy = ReverseEngineeringStrategy.class.cast(Class.forName(revengStrategy).newInstance());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | ClassCastException e) {
            throw new BuildException(revengStrategy + " not instanced.", e);
        }

        if (revengFile != null) {
            OverrideRepository override = new OverrideRepository();
            override.addFile(revengFile);
            strategy = override.getReverseEngineeringStrategy(strategy);
        }

        ReverseEngineeringSettings settings =
                new ReverseEngineeringSettings(strategy)
                        .setDefaultPackageName(packageName)
                        .setDetectManyToMany(detectManyToMany)
                        .setDetectOneToOne(detectOneToOne)
                        .setDetectOptimisticLock(detectOptimisticLock)
                        .setCreateCollectionForForeignKey(createCollectionForForeignKey)
                        .setCreateManyToOneForForeignKey(createManyToOneForForeignKey);

        strategy.setSettings(settings);
        return strategy;
    }

    private Properties loadPropertiesFile() {
        if (propertyFile == null) {
            return null;
        }

        Properties properties = new Properties();
        try (FileInputStream is = new FileInputStream(propertyFile);) {
            properties.load(is);
            return properties;
        } catch (FileNotFoundException e) {
            throw new BuildException(propertyFile + " not found.", e);
        } catch (IOException e) {
            throw new BuildException("Problem while loading " + propertyFile, e);
        }
    }

    private MetadataDescriptor createJdbcDescriptor(ReverseEngineeringStrategy strategy, Properties properties) {
        return MetadataDescriptorFactory
                .createJdbcDescriptor(
                        strategy,
                        properties,
                        preferBasicCompositeIds);
    }

    protected abstract void executeExporter(MetadataDescriptor metadataDescriptor);
}
