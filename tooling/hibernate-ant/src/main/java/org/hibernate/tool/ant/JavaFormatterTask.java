/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
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
package org.hibernate.tool.ant;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.hibernate.tool.api.java.DefaultJavaPrettyPrinterStrategy;

public class JavaFormatterTask extends Task {
	
	final List<FileSet> fileSets = new ArrayList<FileSet>();
	boolean failOnError;

	public void addConfiguredFileSet(FileSet fileSet) {
		fileSets.add(fileSet);
	}

	private Properties readConfig(File cfgfile) throws IOException {
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(cfgfile))) {
            final Properties settings = new Properties();
            settings.load(stream);
            return settings;
        }
    }

	
	public void execute() throws BuildException {
		
		File[] files = getFiles();
		
		int failed = 0;
	
		if(files.length>0) {
			
			DefaultJavaPrettyPrinterStrategy formatter = new DefaultJavaPrettyPrinterStrategy();
            for (File file : files) {
                try {
                    boolean ok = formatter.formatFile(file);
                    if (!ok) {
                        failed++;
                        getProject().log(this, "Formatting failed - skipping " + file, Project.MSG_WARN);
                    }
                    else {
                        getProject().log(this, "Formatted " + file, Project.MSG_VERBOSE);
                    }
                }
                catch (RuntimeException ee) {
                    failed++;
                    if (failOnError) {
                        throw new BuildException("Java formatting failed on " + file, ee);
                    }
                    else {
                        getProject().log(this, "Java formatting failed on " + file + ", " + ee.getLocalizedMessage(), Project.MSG_ERR);
                    }
                }
            }
		}
		
		getProject().log( this, "Java formatting of " + files.length + " files completed. Skipped " + failed + " file(s).", Project.MSG_INFO );
		
	}

	private File[] getFiles() {

		List<File> files = new LinkedList<File>();
        for (FileSet fs : fileSets) {

            DirectoryScanner ds = fs.getDirectoryScanner(getProject());

            String[] dsFiles = ds.getIncludedFiles();
            for (String dsFile : dsFiles) {
                File f = new File(dsFile);
                if (!f.isFile()) {
                    f = new File(ds.getBasedir(), dsFile);
                }

                files.add(f);
            }
        }

		return files.toArray(new File[0]);
	}

    public Object clone() throws CloneNotSupportedException {
        JavaFormatterTask jft = (JavaFormatterTask) super.clone();
        jft.fileSets.addAll(this.fileSets);
        jft.failOnError = this.failOnError;
        return jft;
    }

}
