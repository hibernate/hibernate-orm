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
    private MetadataSources metadataSources = null;
    private final StandardServiceRegistryBuilder ssrb;

    public NativeMetadataDescriptor(
            File cfgXmlFile,
            File[] mappingFiles,
            Properties properties) {
        BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();
        ssrb = new StandardServiceRegistryBuilder(bsr);
        if (cfgXmlFile != null) {
            ssrb.configure(cfgXmlFile);
        }
        if (properties != null) {
            this.properties.putAll(properties);
        }
        ssrb.applySettings(getProperties());
        metadataSources = new MetadataSources(bsr);
        if (mappingFiles != null) {
            for (File file : mappingFiles) {
                if (file.getName().endsWith(".jar")) {
                    metadataSources.addJar(file);
                }
                else {
                    metadataSources.addFile(file);
                }
            }
        }
    }

    public Properties getProperties() {
        Properties result = new Properties();
        result.putAll(properties);
        return result;
    }

    public Metadata createMetadata() {
        return metadataSources.buildMetadata(ssrb.build());
    }

}
