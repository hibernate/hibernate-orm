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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;

/**
 * @author max
 *
 */
public class ConfigurationTask extends Task {

	List<FileSet> fileSets = new ArrayList<FileSet>();
	MetadataDescriptor metadataDescriptor;
	File configurationFile;
	File propertyFile;
	protected String entityResolver;

	public ConfigurationTask() {
		setDescription( "Standard Configuration" );
	}

	public void addConfiguredFileSet(FileSet fileSet) {
		fileSets.add( fileSet );
	}

	public final MetadataDescriptor getMetadataDescriptor() {
		if ( metadataDescriptor == null ) {
			metadataDescriptor = createMetadataDescriptor();
		}
		return metadataDescriptor;
	}

	protected MetadataDescriptor createMetadataDescriptor() {
		return MetadataDescriptorFactory
				.createNativeDescriptor(
						configurationFile,
						getFiles(),
						loadPropertiesFile() );
	}

	protected Properties loadPropertiesFile() {
		if ( propertyFile != null ) {
			Properties properties = new Properties(); // TODO: should we "inherit" from the ant projects properties ?
			try (FileInputStream is = new FileInputStream( propertyFile )) {
				properties.load( is );
				return properties;
			}
			catch (FileNotFoundException e) {
				throw new BuildException( propertyFile + " not found.", e );
			}
			catch (IOException e) {
				throw new BuildException( "Problem while loading " + propertyFile, e );
			}
		}
		else {
			return null;
		}
	}


	protected File[] getFiles() {

		List<File> files = new LinkedList<File>();
		for ( FileSet fs : fileSets ) {

			DirectoryScanner ds = fs.getDirectoryScanner( getProject() );

			String[] dsFiles = ds.getIncludedFiles();
			for ( String dsFile : dsFiles ) {
				File f = new File( dsFile );
				if ( !f.isFile() ) {
					f = new File( ds.getBasedir(), dsFile );
				}

				files.add( f );
			}
		}

		return files.toArray( new File[0] );
	}


	public File getConfigurationFile() {
		return configurationFile;
	}

	public void setConfigurationFile(File configurationFile) {
		this.configurationFile = configurationFile;
	}

	public File getPropertyFile() {
		return propertyFile;
	}

	public void setPropertyFile(File propertyFile) {
		this.propertyFile = propertyFile;
	}

	public void setEntityResolver(String entityResolverName) {
		this.entityResolver = entityResolverName;
	}

	public void setNamingStrategy(String namingStrategy) {
		log("setting unused naming strategy: " + namingStrategy);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		ConfigurationTask ct = (ConfigurationTask) super.clone();
		ct.fileSets.addAll( this.fileSets );
		ct.metadataDescriptor = this.metadataDescriptor;
		ct.propertyFile = this.propertyFile;
		ct.entityResolver = this.entityResolver;
		return ct;
	}

}
