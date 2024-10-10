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
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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

    public void execute() throws MojoExecutionException {
        logConfiguration();
        assembleSourceSet();
        EnhancementContext enhancementContext = createEnhancementContext();
        ClassLoader classLoader = enhancementContext.getLoadingClassLoader();
        try {
            classLoader.loadClass("java.lang.Object");
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException(e);
        }
    }

    private void logConfiguration() {
        Log log = getLog();
        log.info("Starting 'enhance' mojo execution with the following parameters :");
        log.info("  classesDirectory: " + classesDirectory);
        log.info("  enableAssociationManagement: " + enableAssociationManagement);
    }

    private void assembleSourceSet() {
        addToSourceSetIfNeeded(classesDirectory);
    }

    private void addToSourceSetIfNeeded(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                addToSourceSetIfNeeded(child);
            }
        } else {
            if (file.getName().endsWith(".class")) {
                sourceSet.add(file);
            }
        }
    }

    private ClassLoader createClassLoader() {
		List<URL> urls = new ArrayList<>();
        try {
            urls.add(classesDirectory.toURI().toURL());
        } catch (MalformedURLException e) {
            // swallow it?
        }
		return new URLClassLoader(
            urls.toArray(new URL[urls.size()]), 
            Enhancer.class.getClassLoader());
	}

    private EnhancementContext createEnhancementContext() {
        return new EnhancementContext(
            createClassLoader(), 
            enableAssociationManagement, 
            false, 
            false, 
            false);
    }

}
