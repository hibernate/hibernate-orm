/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.build.gradle.javadoc

import org.gradle.process.internal.ExecAction
import org.gradle.process.internal.ExecException
import org.gradle.api.GradleException
import org.gradle.external.javadoc.JavadocExecHandleBuilder

import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.api.tasks.TaskAction
import org.gradle.api.file.FileCollection
import org.gradle.external.javadoc.MinimalJavadocOptions
import org.gradle.plugins.idea.model.Jdk
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.SourceTask
import org.gradle.util.ConfigureUtil
import org.gradle.util.GUtil

/**
 * This is largely a merging of the different classes in standard Gradle needed to perform Javadoc generation.
 *
 * @author Steve Ebersole
 */
class Javadoc extends SourceTask {
    private JavadocExecHandleBuilder javadocExecHandleBuilder = new JavadocExecHandleBuilder();

    private Jdk jdk;

    private File destinationDir;

    private boolean failOnError = true;

    private String title;

    private String maxMemory;

    private MinimalJavadocOptions options = new StandardJavadocDocletOptions();

// EmptyFileCollection not available in my project as is
//    private FileCollection classpath = new EmptyFileCollection();
    private FileCollection classpath;

    private String executable;

    @TaskAction
    protected void generate() {
        final File destinationDir = getDestinationDir();

        if ( options.getDestinationDirectory() == null ) {
            options.destinationDirectory(destinationDir);
        }

        options.classpath(new ArrayList<File>(getClasspath().getFiles()));

        if ( jdk != null ) {
            executable = jdk.getJavadocExecutable()
        }

        if ( !GUtil.isTrue(options.getWindowTitle()) && GUtil.isTrue(getTitle()) ) {
            options.windowTitle(getTitle());
        }
        if ( options instanceof StandardJavadocDocletOptions ) {
            StandardJavadocDocletOptions docletOptions = (StandardJavadocDocletOptions) options;
            if ( !GUtil.isTrue(docletOptions.getDocTitle()) && GUtil.isTrue(getTitle()) ) {
                docletOptions.setDocTitle(getTitle());
            }
        }

        if ( maxMemory != null ) {
            final List<String> jFlags = options.getJFlags();
            final Iterator<String> jFlagsIt = jFlags.iterator();
            boolean containsXmx = false;
            while ( !containsXmx && jFlagsIt.hasNext() ) {
                final String jFlag = jFlagsIt.next();
                if ( jFlag.startsWith("-Xmx") ) {
                    containsXmx = true;
                }
            }
            if ( !containsXmx ) {
                options.jFlags("-Xmx" + maxMemory);
            }
        }

        List<String> sourceNames = new ArrayList<String>();
        for ( File sourceFile: getSource() ) {
            sourceNames.add(sourceFile.getAbsolutePath());
        }
        options.setSourceNames(sourceNames);

        executeExternalJavadoc();
    }

    private void executeExternalJavadoc() {
        javadocExecHandleBuilder.execDirectory(getProject().getRootDir()).options(options).optionsFile(getOptionsFile());

        ExecAction execAction = javadocExecHandleBuilder.getExecHandle();
        if ( executable != null && executable.length() > 0 ) {
            execAction.setExecutable(executable);
        }

        if ( !failOnError ) {
            execAction.setIgnoreExitValue(true);
        }

        try {
            execAction.execute();
        }
        catch (ExecException e) {
            throw new GradleException("Javadoc generation failed.", e);
        }
    }

    void setJavadocExecHandleBuilder(JavadocExecHandleBuilder javadocExecHandleBuilder) {
        if (javadocExecHandleBuilder == null) {
            throw new IllegalArgumentException("javadocExecHandleBuilder == null!");
        }
        this.javadocExecHandleBuilder = javadocExecHandleBuilder;
    }

    /**
     * <p>Returns the directory to generate the documentation into.</p>
     *
     * @return The directory.
     */
    @OutputDirectory
    public File getDestinationDir() {
        return destinationDir;
    }

    /**
     * <p>Sets the directory to generate the documentation into.</p>
     */
    public void setDestinationDir(File destinationDir) {
        this.destinationDir = destinationDir;
    }

    /**
     * Returns the amount of memory allocated to this task.
     */
    public String getMaxMemory() {
        return maxMemory;
    }

    /**
     * Sets the amount of memory allocated to this task.
     *
     * @param maxMemory The amount of memory
     */
    public void setMaxMemory(String maxMemory) {
        this.maxMemory = maxMemory;
    }

    /**
     * <p>Returns the title for the generated documentation.</p>
     *
     * @return The title, possibly null.
     */
    public String getTitle() {
        return title;
    }

    /**
     * <p>Sets the title for the generated documentation.</p>
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns whether javadoc generation is accompanied by verbose output.
     *
     * @see #setVerbose(boolean)
     */
    public boolean isVerbose() {
        return options.isVerbose();
    }

    /**
     * Sets whether javadoc generation is accompanied by verbose output or not. The verbose output is done via println
     * (by the underlying ant task). Thus it is not catched by our logging.
     *
     * @param verbose Whether the output should be verbose.
     */
    public void setVerbose(boolean verbose) {
        if ( verbose ) {
            options.verbose();
        }
    }

    @InputFiles
    public FileCollection getClasspath() {
        return classpath;
    }

    public void setClasspath(FileCollection configuration) {
        this.classpath = configuration;
    }

    public MinimalJavadocOptions getOptions() {
        return options;
    }

    public void setOptions(MinimalJavadocOptions options) {
        this.options = options;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public File getOptionsFile() {
        return new File(getTemporaryDir(), "javadoc.options");
    }

    public String getExecutable() {
        return executable;
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    private Aggregator aggregator;

    public void aggregator(Closure closure) {
        if ( aggregator == null ) {
            aggregator = new Aggregator(this);
        }
        ConfigureUtil.configure(closure, aggregator);
    }
}
