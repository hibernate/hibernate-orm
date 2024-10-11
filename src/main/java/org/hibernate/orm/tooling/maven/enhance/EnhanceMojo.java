/*
 * Copyright 20024 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.orm.tooling.maven.enhance;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.BytecodeProviderInitiator;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Maven mojo for performing build-time enhancement of entity objects.
 */
@Mojo(name = "enhance", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class EnhanceMojo extends AbstractMojo {

	private List<File> sourceSet = new ArrayList<File>();

    @Parameter(
			defaultValue = "${project.build.directory}/classes", 
			readonly = true, 
			required = true)
    private File classesDirectory;

    @Parameter(
            defaultValue = "false",
            readonly = true,
            required = true)
    private boolean enableAssociationManagement;

    @Parameter(
            defaultValue = "false",
            readonly = true,
            required = true)
    private boolean enableDirtyTracking;

    @Parameter(
            defaultValue = "false",
            readonly = true,
            required = true)
    private boolean enableLazyInitialization;

    @Parameter(
        defaultValue = "false",
        readonly = true,
        required = true)
    private boolean enableExtendedEnhancement;

    public void execute() throws MojoExecutionException {
        getLog().debug("Starting execution of enhance mojo");
        assembleSourceSet();
        Enhancer enhancer = createEnhancer();
        discoverTypes(enhancer);
        getLog().debug("Ending execution of enhance mojo");
   }

    private void assembleSourceSet() {
        getLog().debug("Starting assembly of the source set");
        addToSourceSetIfNeeded(classesDirectory);
        getLog().debug("Ending the assembly of the source set");
    }

    private void addToSourceSetIfNeeded(File file) {
        getLog().debug("Considering candidate source: " + file);
        if (file.isDirectory()) {
            getLog().debug("Iterating over the children of folder: " + file);
            for (File child : file.listFiles()) {
                addToSourceSetIfNeeded(child);
            }
        } else {
            if (file.getName().endsWith(".class")) {
                sourceSet.add(file);
                getLog().info("Added file to source set: " + file);
            } else {
                getLog().debug("Skipping non '.class' file: " + file);
            }
        }
    }

    private ClassLoader createClassLoader() {
        getLog().debug("Creating URL ClassLoader for folder: " + classesDirectory) ;
		List<URL> urls = new ArrayList<>();
        try {
            urls.add(classesDirectory.toURI().toURL());
        } catch (MalformedURLException e) {
            getLog().error("An unexpected error occurred while constructing the classloader", e);
        }
		return new URLClassLoader(
            urls.toArray(new URL[urls.size()]), 
            Enhancer.class.getClassLoader());
	}

    private EnhancementContext createEnhancementContext() {
        getLog().debug("Creating enhancement context") ;
        return new EnhancementContext(
            createClassLoader(), 
            enableAssociationManagement, 
            enableDirtyTracking, 
            enableLazyInitialization, 
            enableExtendedEnhancement);
    }

    private Enhancer createEnhancer() {
        getLog().debug("Creating bytecode enhancer") ;
        return BytecodeProviderInitiator
            .buildDefaultBytecodeProvider()
            .getEnhancer(createEnhancementContext());
    }

    private void discoverTypes(Enhancer enhancer) {
        getLog().debug("Starting type discovery") ;
        for (File classFile : sourceSet) {
            discoverTypesForClass(classFile, enhancer);
        }
        getLog().debug("Ending type discovery") ;
    }

    private void discoverTypesForClass(File classFile, Enhancer enhancer) {
        getLog().debug("Trying to discover types for classes in file: " + classFile);
        try {
            enhancer.discoverTypes(
                determineClassName(classFile), 
                Files.readAllBytes( classFile.toPath()));
            getLog().info("Succesfully discovered types for classes in file: " + classFile);
        } catch (IOException e) {
            getLog().error("Unable to discover types for classes in file: " + classFile, e);
        }
    }

    private String determineClassName(File classFile) {
        getLog().debug("Determining class name for file: " + classFile);
        String classFilePath = classFile.getAbsolutePath();
        String classesDirectoryPath = classesDirectory.getAbsolutePath();
        return classFilePath.substring(
                classesDirectoryPath.length() + 1,
                classFilePath.length() - ".class".length())
            .replace(File.separatorChar, '.');
    }

    private boolean clearFile(File file) {
        getLog().debug("Trying to clear the contents of file: " + file);
        boolean success = false;
        if ( file.delete() ) {
            try {
                if ( !file.createNewFile() ) {
                    getLog().error( "Unable to create file: " + file);
                } else {
                    getLog().info("Succesfully cleared the contents of file: " + file);
                    success = true;
                }
            }
            catch (IOException e) {
                getLog().warn( "Problem clearing file for writing out enhancements [" + file + "]", e);
            }
        }
        else {
            getLog().error( "Unable to delete file : " + file);
        }
    return success;
    }

}
