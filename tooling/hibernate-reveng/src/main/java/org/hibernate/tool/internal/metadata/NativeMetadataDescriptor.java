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
import java.io.FileInputStream;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.api.metadata.MetadataDescriptor;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class NativeMetadataDescriptor implements MetadataDescriptor {

    private final Properties properties = new Properties();
    private final File cfgXmlFile;
    private final File[] mappingFiles;

    private final BootstrapServiceRegistry bootstrapServiceRegistry;

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
        metadataSources = new MetadataSources(bootstrapServiceRegistry);
        addMappingFiles(metadataSources);
    }

    public Properties getProperties() {
        Properties result = new Properties();
        result.putAll(properties);
        return result;
    }

    public Metadata createMetadata() {
        StandardServiceRegistryBuilder ssrb =
                new StandardServiceRegistryBuilder(bootstrapServiceRegistry);
        if (cfgXmlFile != null) {
            ssrb.configure(cfgXmlFile);
            addCfgXmlMappings(metadataSources, cfgXmlFile);
        }
        ssrb.applySettings(getProperties());
        return metadataSources.buildMetadata(ssrb.build());
    }

    public File getCfgXmlFile() {
        return cfgXmlFile;
    }

    public File[] getMappingFiles() {
        return mappingFiles;
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

    /**
     * Parses the hibernate.cfg.xml and adds any {@code <mapping>}
     * elements to the given MetadataSources.
     * {@code StandardServiceRegistryBuilder.configure()} only
     * processes properties from the cfg.xml — mapping references
     * are ignored. This method fills that gap.
     */
    private static void addCfgXmlMappings(MetadataSources sources,
                                           File cfgXml) {
        try {
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setFeature(
                    "http://apache.org/xml/features/"
                    + "nonvalidating/load-external-dtd", false);
            Document doc;
            try (FileInputStream fis = new FileInputStream(cfgXml)) {
                doc = factory.newDocumentBuilder().parse(fis);
            }
            NodeList mappings =
                    doc.getElementsByTagName("mapping");
            for (int i = 0; i < mappings.getLength(); i++) {
                Element mapping = (Element) mappings.item(i);
                String resource = mapping.getAttribute("resource");
                if (resource != null && !resource.isEmpty()) {
                    sources.addResource(resource);
                }
                String file = mapping.getAttribute("file");
                if (file != null && !file.isEmpty()) {
                    sources.addFile(file);
                }
                String jar = mapping.getAttribute("jar");
                if (jar != null && !jar.isEmpty()) {
                    sources.addJar(new File(jar));
                }
                String pkg = mapping.getAttribute("package");
                if (pkg != null && !pkg.isEmpty()) {
                    sources.addPackage(pkg);
                }
                String className = mapping.getAttribute("class");
                if (className != null && !className.isEmpty()) {
                    try {
                        sources.addAnnotatedClass(
                                Class.forName(className));
                    } catch (ClassNotFoundException ignored) {}
                }
            }
        } catch (Exception e) {
            // cfg.xml parsing failed — mappings from cfg.xml will
            // be unavailable but fileset mappings still work
        }
    }

}
