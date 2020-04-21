/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 * 
 * Copyright 2004-2020 Red Hat, Inc.
 *
 * Licensed under the GNU Lesser General Public License (LGPL), 
 * version 2.1 or later (the "License").
 * You may not use this file except in compliance with the License.
 * You may read the licence in the 'lgpl.txt' file in the root folder of 
 * project or obtain a copy at
 *
 *     http://www.gnu.org/licenses/lgpl-2.1.html
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
import java.util.Iterator;
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

	private List<FileSet> fileSets = new ArrayList<FileSet>();
	private MetadataDescriptor metadataDescriptor;
	private File configurationFile;
	private File propertyFile;
	protected String entityResolver;
	
	public ConfigurationTask() {
		setDescription("Standard Configuration");
	}
	
	public void addConfiguredFileSet(FileSet fileSet) {
		fileSets.add(fileSet);	 	
	}

	/**
	 * @return
	 */
	public final MetadataDescriptor getMetadataDescriptor() {
		if (metadataDescriptor == null) {
			metadataDescriptor = createMetadataDescriptor();
		}
		return metadataDescriptor;
	}
	
	protected MetadataDescriptor createMetadataDescriptor() {
		return MetadataDescriptorFactory
				.createNativeDescriptor(
						configurationFile, 
						getFiles(), 
						loadPropertiesFile());
	}
	
	protected Properties loadPropertiesFile() {
		if (propertyFile!=null) { 
			Properties properties = new Properties(); // TODO: should we "inherit" from the ant projects properties ?
			FileInputStream is = null;
			try {
				is = new FileInputStream(propertyFile);
				properties.load(is);
				return properties;
			} 
			catch (FileNotFoundException e) {
				throw new BuildException(propertyFile + " not found.",e);					
			} 
			catch (IOException e) {
				throw new BuildException("Problem while loading " + propertyFile,e);				
			}
			finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
					}
				}
			}
		} else {
			return null;
		}
	}
	
	
	protected File[] getFiles() {

		List<File> files = new LinkedList<File>();
		for ( Iterator<FileSet> i = fileSets.iterator(); i.hasNext(); ) {

			FileSet fs = i.next();
			DirectoryScanner ds = fs.getDirectoryScanner( getProject() );

			String[] dsFiles = ds.getIncludedFiles();
			for (int j = 0; j < dsFiles.length; j++) {
				File f = new File(dsFiles[j]);
				if ( !f.isFile() ) {
					f = new File( ds.getBasedir(), dsFiles[j] );
				}

				files.add( f );
			}
		}

		return (File[]) files.toArray(new File[files.size()]);
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
	}
}
