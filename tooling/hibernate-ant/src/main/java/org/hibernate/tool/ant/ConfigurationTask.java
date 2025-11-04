/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptorFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

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
