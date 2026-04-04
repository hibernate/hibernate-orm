/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2017-2025 Red Hat, Inc.
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
package org.hibernate.tool.internal.metadata;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jakarta.persistence.Entity;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.tool.api.metadata.MetadataDescriptor;

public class NativeMetadataDescriptor implements MetadataDescriptor {

    private final Properties properties = new Properties();
    private final File cfgXmlFile;
    private final File[] mappingFiles;

    private List<ClassDetails> entityClassDetails;
    private ModelsContext modelsContext;

    // Exposed for legacy tests that add annotated classes via reflection
    @SuppressWarnings("unused")
    private final MetadataSources metadataSources = new MetadataSources();

    public NativeMetadataDescriptor(
            File cfgXmlFile,
            File[] mappingFiles,
            Properties properties) {
        this.cfgXmlFile = cfgXmlFile;
        this.mappingFiles = mappingFiles;
        if (properties != null) {
            this.properties.putAll(properties);
        }
    }

    public Properties getProperties() {
        Properties result = new Properties();
        result.putAll(properties);
        return result;
    }

    public Metadata createMetadata() {
        BootstrapServiceRegistry bsr =
                new BootstrapServiceRegistryBuilder().build();
        StandardServiceRegistryBuilder ssrb =
                new StandardServiceRegistryBuilder(bsr);
        if (cfgXmlFile != null) {
            ssrb.configure(cfgXmlFile);
        }
        ssrb.applySettings(getProperties());
        MetadataSources sources = new MetadataSources(bsr);
        addMappingFiles(sources);
        for (Class<?> c : metadataSources.getAnnotatedClasses()) {
            sources.addAnnotatedClass(c);
        }
        return sources.buildMetadata(ssrb.build());
    }

    @Override
    public List<ClassDetails> getEntityClassDetails() {
        if (entityClassDetails == null) {
            buildEntityClassDetails();
        }
        return entityClassDetails;
    }

    @Override
    public ModelsContext getModelsContext() {
        if (modelsContext == null) {
            buildEntityClassDetails();
        }
        return modelsContext;
    }

    private void buildEntityClassDetails() {
        BootstrapServiceRegistry bsr =
                new BootstrapServiceRegistryBuilder().build();
        StandardServiceRegistryBuilder ssrb =
                new StandardServiceRegistryBuilder(bsr);
        if (cfgXmlFile != null) {
            ssrb.configure(cfgXmlFile);
        }
        ssrb.applySettings(getProperties());
        MetadataSources sources = new MetadataSources(bsr);
        addMappingFiles(sources);
        MetadataBuilderImpl builder =
                (MetadataBuilderImpl) sources.getMetadataBuilder(
                        ssrb.build());
        Metadata metadata = builder.build();
        BootstrapContext bootstrapContext = builder.getBootstrapContext();
        this.modelsContext = bootstrapContext.getModelsContext();
        List<ClassDetails> entities = new ArrayList<>();
        // First try annotation-based discovery (works for annotated Java entities)
        modelsContext.getClassDetailsRegistry().forEachClassDetails(cd -> {
            if (cd.hasAnnotationUsage(Entity.class, modelsContext)) {
                entities.add(cd);
            }
        });
        // If no annotated entities found, fall back to Metadata entity bindings
        // (handles hbm.xml entities where ClassDetails lack @Entity annotation)
        if (entities.isEmpty()) {
            for (PersistentClass pc : metadata.getEntityBindings()) {
                String className = pc.getClassName();
                if (className != null) {
                    ClassDetails cd = modelsContext.getClassDetailsRegistry()
                            .findClassDetails(className);
                    if (cd != null) {
                        entities.add(cd);
                    }
                }
            }
        }
        this.entityClassDetails = entities;
    }

    private void addMappingFiles(MetadataSources sources) {
        if (mappingFiles != null) {
            for (File file : mappingFiles) {
                if (file.getName().endsWith(".jar")) {
                    sources.addJar(file);
                } else {
                    sources.addFile(file);
                }
            }
        }
    }

}
