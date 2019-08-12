package org.hibernate.tool.ant;

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
	
	File propertyFile = null;
	File configFile = null;
	List<FileSet> fileSets = new ArrayList<FileSet>();	
	Type type = Type.NATIVE;
	
	public void setPropertyFile(File file) {
		this.propertyFile = file;
	}
	
	public void setConfigFile(File file) {
		this.configFile = file;
	}
	
	public void setType(Type type) {
		this.type = type;
	}
	
	public void addConfiguredFileSet(FileSet fileSet) {
		fileSets.add(fileSet);
	}
	
	public MetadataDescriptor createMetadataDescriptor() {
		return MetadataDescriptorFactory.createNativeDescriptor(
				this.configFile, 
				getFiles(), 
				getProperties());
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
			FileInputStream is = null;
			try {
				is = new FileInputStream(propertyFile);
				result.load(is);
			} 
			catch (FileNotFoundException e) {
				throw new BuildException(propertyFile + " not found.", e);					
			} 
			catch (IOException e) {
				throw new BuildException("Problem while loading " + propertyFile, e);				
			}
			finally {
				if (is != null) {
					try {
						is.close();
					} 
					catch (IOException e) { /** ignore **/ }
				}
			}
		} 
	    return result;
	}
	
}
