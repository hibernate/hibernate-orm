/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.hibernate.tool.reveng.api.java.DefaultJavaPrettyPrinterStrategy;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

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
