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
import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.api.metadata.MetadataDescriptor;

public class NativeMetadataDescriptor implements MetadataDescriptor {

    private final Properties properties = new Properties();
    private final File cfgXmlFile;
    private final File[] mappingFiles;

    private final BootstrapServiceRegistry bootstrapServiceRegistry;
    private final StandardServiceRegistryBuilder ssrb;

    // Exposed for legacy tests that add annotated classes via reflection
    @SuppressWarnings("unused")
    private final MetadataSources metadataSources;

    public NativeMetadataDescriptor(
            File cfgXmlFile,
            File[] mappingFiles,
            Properties properties) {
        this.cfgXmlFile = cfgXmlFile;
        this.mappingFiles = mappingFiles;
        if (properties != null) {
            this.properties.putAll(properties);
        }
        bootstrapServiceRegistry =
                new BootstrapServiceRegistryBuilder()
                        .disableAutoClose()
                        .build();
        ssrb = new StandardServiceRegistryBuilder(bootstrapServiceRegistry);
        if (cfgXmlFile != null) {
            ssrb.configure(cfgXmlFile);
        }
        ssrb.applySettings(getProperties());
        metadataSources = new MetadataSources(bootstrapServiceRegistry);
        addMappingFiles(metadataSources);
    }

    public Properties getProperties() {
        Properties result = new Properties();
        result.putAll(properties);
        return result;
    }

    public Metadata createMetadata() {
        return metadataSources.buildMetadata(ssrb.build());
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
