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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.internal.util.DummyDialect;

/**
 * Base class for mojos that read Hibernate mapping files
 * (hbm.xml, mapping.xml, and/or hibernate.cfg.xml) and produce
 * output via the new models-based exporters.
 *
 * <p>Unlike {@link AbstractGenerationMojo} which reverse-engineers
 * a live database, this base creates a {@code NativeMetadataDescriptor}
 * from existing mapping files.</p>
 */
public abstract class AbstractNativeMojo extends AbstractMojo {

    /** Hibernate configuration file (hibernate.cfg.xml). Optional. */
    @Parameter
    private File configFile;

    /**
     * Directory containing mapping files.
     * All files ending in {@code .hbm.xml} or {@code .mapping.xml}
     * are collected recursively.
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/resources")
    private File mappingDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    public void execute() throws MojoFailureException {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(createClassLoader(original));
            getLog().info("Starting " + this.getClass().getSimpleName() + "...");
            MetadataDescriptor md = createNativeDescriptor();
            executeExporter(md);
            getLog().info("Finished " + this.getClass().getSimpleName() + "!");
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    protected abstract void executeExporter(MetadataDescriptor metadataDescriptor);

    private MetadataDescriptor createNativeDescriptor() {
        File[] hbmFiles = collectHbmFiles(mappingDirectory);
        getLog().info("Found " + hbmFiles.length + " mapping file(s) in " + mappingDirectory);
        Properties properties = new Properties();
        // Use a dummy dialect so no database connection is needed
        properties.put(JdbcSettings.DIALECT, DummyDialect.class.getName());
        return MetadataDescriptorFactory.createNativeDescriptor(
                configFile, hbmFiles, properties);
    }

    private File[] collectHbmFiles(File dir) {
        List<File> result = new ArrayList<>();
        collectHbmFiles(dir, result);
        return result.toArray(new File[0]);
    }

    private void collectHbmFiles(File f, List<File> result) {
        if (f.isFile()) {
            String name = f.getName();
            if (name.endsWith(".hbm.xml") || name.endsWith(".mapping.xml")) {
                result.add(f);
            }
        } else if (f.isDirectory()) {
            for (File child : Objects.requireNonNull(f.listFiles())) {
                collectHbmFiles(child, result);
            }
        }
    }

    private ClassLoader createClassLoader(ClassLoader parent) {
        ArrayList<URL> urls = new ArrayList<>();
        try {
            for (String cpe : project.getRuntimeClasspathElements()) {
                urls.add(new File(cpe).toURI().toURL());
            }
        } catch (DependencyResolutionRequiredException | MalformedURLException e) {
            throw new RuntimeException("Problem while constructing project classloader", e);
        }
        return new URLClassLoader(urls.toArray(new URL[0]), parent);
    }

}
