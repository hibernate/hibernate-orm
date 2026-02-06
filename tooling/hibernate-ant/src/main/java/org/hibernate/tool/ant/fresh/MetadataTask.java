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
package org.hibernate.tool.ant.fresh;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;

public class MetadataTask {
	
	enum Type { JDBC, JPA, NATIVE }
	
	String persistenceUnit = null;
	File propertyFile = null;
	File configFile = null;
	List<FileSet> fileSets = new ArrayList<FileSet>();	
	Type type = Type.NATIVE;
	
	public void setPersistenceUnit(String pU) {
		this.persistenceUnit = pU;
	}
	
	public void setPropertyFile(File file) {
		this.propertyFile = file;
	}
	
	public void setConfigFile(File file) {
		this.configFile = file;
	}
	
	public void setType(String type) {
		this.type = Type.valueOf(type.toUpperCase());
	}
	
	public void addConfiguredFileSet(FileSet fileSet) {
		fileSets.add(fileSet);
	}
	
	public MetadataDescriptor createMetadataDescriptor() {
		switch(type) {
			case NATIVE:
				return MetadataDescriptorFactory.createNativeDescriptor(
					this.configFile, 
					getFiles(), 
					getProperties());
			case JPA:
				return MetadataDescriptorFactory.createJpaDescriptor(
					persistenceUnit, 
					getProperties());
			default: return null;
		}
	}
	
	private File[] getFiles() {
		List<File> result = new ArrayList<File>();
		Iterator<FileSet> iterator = this.fileSets.iterator();
		while (iterator.hasNext()) {
			FileSet fileSet = iterator.next();
			DirectoryScanner scanner = fileSet.getDirectoryScanner();
			for (String fileName : scanner.getIncludedFiles()) {
				File file = new File(fileName);
				if (!file.isFile()) {
					file = new File(scanner.getBasedir(), fileName);
				}
				result.add(file);
			}
		}
		return (File[]) result.toArray(new File[result.size()]);
	}
	
	private Properties getProperties() {
		Properties result = new Properties();
		if (this.propertyFile != null) {
            try (FileInputStream is = new FileInputStream(propertyFile)) {
                result.load(is);
            } catch (FileNotFoundException e) {
                throw new BuildException(propertyFile + " not found.", e);
            } catch (IOException e) {
                throw new BuildException("Problem while loading " + propertyFile, e);
            }
        }
	    return result;
	}
	
}
