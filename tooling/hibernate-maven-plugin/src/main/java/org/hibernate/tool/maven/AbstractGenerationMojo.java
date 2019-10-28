package org.hibernate.tool.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.BuildException;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.ReverseEngineeringSettings;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategyFactory;

public abstract class AbstractGenerationMojo extends AbstractMojo {

    // For reveng strategy
    /** The default package name to use when mappings for classes are created. */
    @Parameter
    private String packageName;
   
    /** The name of a property file, e.g. hibernate.properties. */
    @Parameter
    private File revengFile;
    
    /** The class name of the reverse engineering strategy to use.
     * Extend the DefaultReverseEngineeringStrategy and override the corresponding methods, e.g.
     * to adapt the generate class names or to provide custom type mappings. */
    @Parameter(defaultValue = "org.hibernate.tool.internal.reveng.DefaultReverseEngineeringStrategy")
    private String revengStrategy;

    /** If true, tables which are pure many-to-many link tables will be mapped as such.
     * A pure many-to-many table is one which primary-key contains exactly two foreign-keys pointing
     * to other entity tables and has no other columns. */
    @Parameter(defaultValue = "true")
    private boolean detectManyToMany;

    /** If true, a one-to-one association will be created for each foreignkey found. */
    @Parameter(defaultValue = "true")
    private boolean detectOneToOne;

    /** If true, columns named VERSION or TIMESTAMP with appropriate types will be mapped with the appropriate
     * optimistic locking corresponding to &lt;version&gt; or &lt;timestamp&gt;. */
    @Parameter(defaultValue = "true")
    private boolean detectOptimisticLock;

    /** If true, a collection will be mapped for each foreignkey. */
    @Parameter(defaultValue = "true")
    private boolean createCollectionForForeignKey;

    /** If true, a many-to-one association will be created for each foreignkey found. */
    @Parameter(defaultValue = "true")
    private boolean createManyToOneForForeignKey;

    // For configuration
    /** The name of a property file, e.g. hibernate.properties. */
    @Parameter(defaultValue = "${project.basedir}/src/main/hibernate/hibernate.properties")
    private File propertyFile;

    // Not exposed for now
    private boolean preferBasicCompositeIds = true;

    public void execute() {
        getLog().info("Starting " + this.getClass().getSimpleName() + "...");
        ReverseEngineeringStrategy strategy = setupReverseEngineeringStrategy();
        Properties properties = loadPropertiesFile();
        MetadataDescriptor jdbcDescriptor = createJdbcDescriptor(strategy, properties);
        executeExporter(jdbcDescriptor);
        getLog().info("Finished " + this.getClass().getSimpleName() + "!");
    }

    private ReverseEngineeringStrategy setupReverseEngineeringStrategy() {
    	File[] revengFiles = null;
    	if (revengFile != null) {
    		revengFiles = new File[] { revengFile };
    	}
        ReverseEngineeringStrategy strategy = 
        		ReverseEngineeringStrategyFactory.createReverseEngineeringStrategy(
        				revengStrategy, 
        				revengFiles);
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
        try (FileInputStream is = new FileInputStream(propertyFile)) {
            properties.load(is);
            return properties;
        } catch (FileNotFoundException e) {
            throw new BuildException(propertyFile + " not found.", e);
        } catch (IOException e) {
            throw new BuildException("Problem while loading " + propertyFile, e);
        }
    }

    private MetadataDescriptor createJdbcDescriptor(ReverseEngineeringStrategy strategy, Properties properties) {
    	properties.put(MetadataDescriptor.PREFER_BASIC_COMPOSITE_IDS, preferBasicCompositeIds);
        return MetadataDescriptorFactory
                .createJdbcDescriptor(
                        strategy,
                        properties);
    }

    protected abstract void executeExporter(MetadataDescriptor metadataDescriptor);
}
