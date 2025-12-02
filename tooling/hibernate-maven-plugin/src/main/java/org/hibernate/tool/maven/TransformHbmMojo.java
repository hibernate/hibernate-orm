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

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_RESOURCES;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import org.hibernate.tool.internal.export.mapping.MappingExporter;

@Mojo(
	name = "hbm2orm", 
	defaultPhase = GENERATE_RESOURCES,
	requiresDependencyResolution = ResolutionScope.RUNTIME)
public class TransformHbmMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}/src/main/resources")
    private File inputFolder;

    @Parameter(defaultValue = "true")
    private boolean format;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(createClassLoader(original));
            getLog().info("Starting " + this.getClass().getSimpleName() + "...");
            MappingExporter mappingExporter = new MappingExporter();
            mappingExporter.setHbmFiles(getHbmFiles(inputFolder));
            mappingExporter.setFormatResult(format);
            mappingExporter.start();
            getLog().info("Finished " + this.getClass().getSimpleName() + "!");
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    private List<File> getHbmFiles(File f) {
        List<File> result = new ArrayList<>();
        if (f.isFile()) {
            if (f.getName().endsWith("hbm.xml")) {
                result.add(f);
            }
        }
        else {
            for (File child : Objects.requireNonNull( f.listFiles() ) ) {
                result.addAll(getHbmFiles(child));
            }
        }
        return result;
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
